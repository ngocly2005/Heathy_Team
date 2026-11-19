package com.app_health.app.feature.integrated

import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.SessionRequestTag
import com.app_health.app.core.network.toUserMessage
import com.app_health.app.core.network.retryTransientRead
import com.google.gson.JsonParseException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

data class AppState(
    val date: LocalDate = LocalDate.now(), val loading: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val uncertain: Boolean = false, val canDismissUncertain: Boolean = false,
    val profile: ProfileDto? = null, val dashboard: DashboardDto? = null, val nutrition: NutritionDto? = null,
    val water: WaterSettingsDto? = null, val foods: List<FoodDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(), val activityHistory: List<ActivityLogDto> = emptyList(),
    val activityHistoryHasMore: Boolean = false,
    val meals: List<SavedMealDto> = emptyList(),
    val weights: List<WeightDto> = emptyList(), val daily: DailyDto? = null,
    val calendar: List<CalendarDto> = emptyList(), val chat: List<ChatHistoryDto> = emptyList(),
    val photo: PhotoDto? = null, val photoConfirmed: Boolean = false, val photoRevision: Int = 0,
    val photoDrafts: List<PhotoDraft> = emptyList(),
    val route: String = "home", val readRevision: Long = 0
)

data class RequestContext(
    val owner: SessionRequestTag, val date: String, val epoch: Long,
    val route: String? = null, val readRevision: Long = 0
)

/** Rejects late results after navigation, without cancelling or replaying committed writes. */
internal class AppDataController(
    private val scope: CoroutineScope,
    private val currentUserId: () -> Long?,
    private val loadFeature: suspend (String, RequestContext) -> Unit
) {
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    private var epoch = 0L
    private var readRevision = 0L
    private var ownerId: Long? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private val loaded = mutableSetOf<String>()
    private var uncertainRoute: String? = null
    private var uncertainReconcile: (suspend (RequestContext) -> Boolean)? = null
    private var readErrorContext: RequestContext? = null
    var photoBytes: ByteArray? = null
        private set
    var photoMime = "image/jpeg"
        private set
    var pendingChat: String? = null

    private fun dependencies(route: String): List<String> = when (route) {
        "home" -> listOf("profile", "home", "water")
        "profile" -> listOf("profile")
        "macros" -> listOf("home")
        "activity" -> listOf("profile", "home", "activity")
        "water" -> listOf("home", "water")
        "meals" -> listOf("home", "meals")
        "diary" -> listOf("diary", "water")
        "chat" -> listOf("profile", "chat")
        // Search and calendar perform their own parameterized reads.
        "photo", "food", "calendar" -> emptyList()
        else -> listOf(route)
    }

    /** Only an origin-related GET may clear an ambiguous write without a custom verifier. */
    private fun defaultReconciliationReads(route: String): List<String> = when (route) {
        "home", "macros" -> listOf("home")
        "profile" -> listOf("profile")
        "activity" -> listOf("activity")
        "water" -> listOf("home", "water")
        "meals" -> listOf("home", "meals")
        "diary", "photo" -> listOf("diary")
        "chat" -> listOf("chat")
        // Search/custom-food writes need an operation-specific verifier.
        "food", "calendar" -> emptyList()
        else -> dependencies(route)
    }

    fun activate() {
        val id = currentUserId() ?: return
        if (id == ownerId) return
        release()
        ownerId = id
    }

    fun release() {
        epoch++
        readRevision++
        ownerId = null
        readJob?.cancel()
        writeJob?.cancel()
        loaded.clear()
        uncertainRoute = null
        uncertainReconcile = null
        readErrorContext = null
        photoBytes = null
        pendingChat = null
        mutable.value = AppState()
    }

    fun releaseIfSessionChanged() {
        if (currentUserId() != ownerId) release()
    }

    fun context(): RequestContext = requestContext(trackScreen = true)

    private fun requestContext(trackScreen: Boolean) = RequestContext(
        SessionRequestTag(ownerId ?: error("Vui lòng đăng nhập lại.")),
        mutable.value.date.toString(), epoch,
        if (trackScreen) mutable.value.route else null, readRevision
    )

    fun valid(ctx: RequestContext): Boolean = ctx.epoch == epoch && ctx.owner.userId == ownerId &&
        currentUserId() == ownerId && ctx.date == mutable.value.date.toString() &&
        (ctx.route == null || ctx.route == mutable.value.route && ctx.readRevision == readRevision)

    fun publish(ctx: RequestContext, transform: (AppState) -> AppState) {
        if (valid(ctx)) mutable.update(transform)
    }

    fun showError(message: String?, ctx: RequestContext? = null) {
        if (ownerId == null) return
        val current = ctx ?: context()
        if (!valid(current) || mutable.value.uncertain) return
        readErrorContext = if (message != null) current else null
        publish(current) { it.copy(error = message) }
    }

    /** A successful read only dismisses the matching read failure, never a write warning. */
    fun readSucceeded(ctx: RequestContext, transform: (AppState) -> AppState = { it }) {
        if (!valid(ctx)) return
        val clear = !mutable.value.uncertain && readErrorContext == ctx
        if (clear) readErrorContext = null
        publish(ctx) {
            transform(it).copy(error = if (clear && !it.uncertain) null else it.error)
        }
    }

    fun enter(route: String) {
        if (ownerId == null) return
        if (route != mutable.value.route) {
            readRevision++
            readJob?.cancel()
            if (!mutable.value.uncertain) readErrorContext = null
            mutable.update { it.copy(route = route, loading = false,
                error = if (it.uncertain) it.error else null) }
        } else if (readJob?.isActive == true) return
        if (!mutable.value.busy && (mutable.value.uncertain || !dependencies(route).all { it in loaded })) startLoad()
    }

    fun chooseDate(date: LocalDate) {
        if (mutable.value.busy || mutable.value.uncertain || date > LocalDate.now() || date == mutable.value.date) return
        epoch++
        readRevision++
        readJob?.cancel()
        loaded.clear()
        readErrorContext = null
        mutable.update { it.copy(date = date, dashboard = null, daily = null, calendar = emptyList(),
            error = null, photo = null, photoDrafts = emptyList(), photoConfirmed = false) }
        startLoad()
    }

    fun refresh() {
        if (ownerId == null || mutable.value.busy) return
        loaded.clear()
        startLoad()
    }

    /** Let the user continue only after a successful read found no matching write. */
    fun dismissUncertain() {
        if (ownerId == null || mutable.value.busy || mutable.value.loading ||
            !mutable.value.uncertain || !mutable.value.canDismissUncertain) return
        uncertainRoute = null
        uncertainReconcile = null
        loaded.clear()
        mutable.update { it.copy(uncertain = false, canDismissUncertain = false, error = null) }
        startLoad()
    }

    private fun ensureCurrent(ctx: RequestContext) {
        if (!valid(ctx)) throw CancellationException("Account, date or screen changed")
    }

    private fun startLoad(afterSave: Boolean = false) {
        readRevision++
        readJob?.cancel()
        val ctx = context()
        readErrorContext = null
        val reconciling = mutable.value.uncertain
        val verifier = if (reconciling) uncertainReconcile else null
        val requiredReads = if (reconciling) defaultReconciliationReads(uncertainRoute ?: ctx.route!!) else emptyList()
        val plan = buildList {
            if (reconciling) addAll(requiredReads)
            addAll(dependencies(ctx.route!!))
        }.distinct()
        mutable.update { it.copy(loading = true, error = null, canDismissUncertain = false,
            readRevision = readRevision) }
        readJob = scope.launch {
            try {
                var firstFailure: Exception? = null
                var verified = false
                var verifierReadCompleted = false
                if (reconciling && verifier != null) {
                    // A slow server may commit just after the first read. Poll the
                    // read-only verifier briefly; never replay the original write.
                    for (attempt in 0 until 3) {
                        try {
                            verified = retryTransientRead(checkCurrent = { ensureCurrent(ctx) }) { verifier(ctx) }
                            currentCoroutineContext().ensureActive()
                            ensureCurrent(ctx)
                            verifierReadCompleted = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            currentCoroutineContext().ensureActive()
                            ensureCurrent(ctx)
                            firstFailure = failure
                        }
                        if (verified || firstFailure != null) break
                        if (attempt < 2) delay(700)
                    }
                }
                for (feature in plan) {
                    ensureCurrent(ctx)
                    if (feature !in loaded) {
                        try {
                            retryTransientRead(checkCurrent = { ensureCurrent(ctx) }) { loadFeature(feature, ctx) }
                            currentCoroutineContext().ensureActive()
                            ensureCurrent(ctx)
                            loaded.add(feature)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            currentCoroutineContext().ensureActive()
                            ensureCurrent(ctx)
                            if (firstFailure == null) firstFailure = failure
                            // An optional companion read must not prevent the screen's own data.
                        }
                    }
                }
                ensureCurrent(ctx)
                val reconciled = !reconciling || if (verifier != null) verified else
                    requiredReads.isNotEmpty() && requiredReads.all { it in loaded }
                if (reconciled) {
                    uncertainRoute = null
                    uncertainReconcile = null
                    publish(ctx) { it.copy(uncertain = false, canDismissUncertain = false, error = null) }
                }
                if (!reconciled) publish(ctx) { it.copy(
                    canDismissUncertain = if (verifier != null) verifierReadCompleted else
                        requiredReads.isNotEmpty() && requiredReads.all { feature -> feature in loaded },
                    error = if (firstFailure == null)
                        "Chưa tìm thấy kết quả yêu cầu. Yêu cầu có thể còn đang xử lý; hãy kiểm tra dữ liệu trước khi thử lại."
                    else "Chưa xác nhận được kết quả yêu cầu. Bấm Tải lại để đối chiếu trước khi gửi tiếp.") }
                firstFailure?.let { throw it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Cancelled transport calls must not become a connection-error banner.
                currentCoroutineContext().ensureActive()
                if (valid(ctx)) {
                    if (!mutable.value.uncertain) readErrorContext = ctx
                    publish(ctx) { it.copy(error = when {
                        it.uncertain && it.canDismissUncertain ->
                            "Đã đối chiếu yêu cầu nhưng dữ liệu phụ chưa tải được: ${error.toUserMessage()}. Kiểm tra dữ liệu trước khi thử lại."
                        it.uncertain -> "Chưa rõ kết quả yêu cầu. Chưa đối chiếu được dữ liệu: ${error.toUserMessage()}"
                        afterSave -> "Đã lưu. Chưa tải lại được dữ liệu: ${error.toUserMessage()}"
                        else -> error.toUserMessage()
                    }) }
                }
            } finally {
                publish(ctx) { it.copy(loading = false) }
            }
        }
    }

    fun write(
        success: () -> Unit = {},
        reconcile: (suspend (RequestContext) -> Boolean)? = null,
        operation: suspend (RequestContext) -> Unit
    ) {
        if (mutable.value.busy || mutable.value.loading || mutable.value.uncertain || ownerId == null) return
        readRevision++
        readJob?.cancel()
        readErrorContext = null
        // Success may navigate to another screen; the write still belongs to this account/date.
        val ctx = requestContext(trackScreen = false)
        val originRoute = mutable.value.route
        mutable.update { it.copy(busy = true, error = null) }
        writeJob = scope.launch {
            var saved = false
            var unknown = false
            try {
                operation(ctx)
                currentCoroutineContext().ensureActive()
                ensureCurrent(ctx)
                saved = true
                loaded.clear()
                success()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                unknown = !saved && ((error is IOException && error !is ApiFailure) || error is JsonParseException ||
                    (error is HttpException && error.code() >= 500))
                if (valid(ctx) && unknown) {
                    loaded.clear()
                    uncertainRoute = originRoute
                    uncertainReconcile = reconcile
                }
                publish(ctx) { it.copy(uncertain = unknown, canDismissUncertain = false, error = if (unknown)
                    "Chưa rõ kết quả yêu cầu. Đang đối chiếu dữ liệu trước khi gửi tiếp."
                    else error.toUserMessage()) }
            } finally {
                if (valid(ctx)) {
                    mutable.update { it.copy(busy = false) }
                    if (saved) startLoad(afterSave = true)
                    else if (unknown) startLoad()
                }
            }
        }
    }

    fun read(operation: suspend (RequestContext) -> Unit): Job {
        val ctx = context()
        return scope.launch {
            try {
                ensureCurrent(ctx)
                retryTransientRead(checkCurrent = { ensureCurrent(ctx) }) { operation(ctx) }
                currentCoroutineContext().ensureActive()
                readSucceeded(ctx)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                showError(error.toUserMessage(), ctx)
            }
        }
    }

    fun setPhoto(bytes: ByteArray, mime: String) {
        if (ownerId == null || currentUserId() != ownerId || mutable.value.route != "photo" ||
            mutable.value.busy || mutable.value.uncertain) return
        photoBytes = bytes
        photoMime = mime
        mutable.update { it.copy(photo = null, photoDrafts = emptyList(), photoConfirmed = false,
            photoRevision = it.photoRevision + 1, error = null) }
    }
}
