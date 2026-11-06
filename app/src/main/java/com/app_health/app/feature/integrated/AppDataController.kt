package com.app_health.app.feature.integrated

import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.SessionRequestTag
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

data class AppState(
    val date: LocalDate = LocalDate.now(), val loading: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val uncertain: Boolean = false,
    val profile: ProfileDto? = null, val dashboard: DashboardDto? = null, val nutrition: NutritionDto? = null,
    val water: WaterSettingsDto? = null, val foods: List<FoodDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(), val meals: List<SavedMealDto> = emptyList(),
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
        "chat" -> listOf("profile", "chat")
        // Search and calendar perform their own parameterized reads.
        "photo", "food", "calendar" -> emptyList()
        else -> listOf(route)
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
        if (ownerId != null) publish(ctx ?: context()) { it.copy(error = message) }
    }

    fun enter(route: String) {
        if (ownerId == null) return
        if (route != mutable.value.route) {
            readRevision++
            readJob?.cancel()
            mutable.update { it.copy(route = route, loading = false,
                error = if (it.uncertain) it.error else null) }
        } else if (readJob?.isActive == true) return
        if (!mutable.value.busy && !mutable.value.uncertain && !dependencies(route).all { it in loaded }) startLoad()
    }

    fun chooseDate(date: LocalDate) {
        if (mutable.value.busy || mutable.value.uncertain || date > LocalDate.now() || date == mutable.value.date) return
        epoch++
        readRevision++
        readJob?.cancel()
        loaded.clear()
        mutable.update { it.copy(date = date, dashboard = null, daily = null, calendar = emptyList(),
            error = null, photo = null, photoDrafts = emptyList(), photoConfirmed = false) }
        startLoad()
    }

    fun refresh() {
        if (ownerId == null || mutable.value.busy) return
        loaded.clear()
        startLoad()
    }

    private fun ensureCurrent(ctx: RequestContext) {
        if (!valid(ctx)) throw CancellationException("Account, date or screen changed")
    }

    private fun startLoad(afterSave: Boolean = false) {
        readRevision++
        readJob?.cancel()
        val ctx = context()
        val plan = buildList {
            if (mutable.value.uncertain) {
                // Reconcile a possibly committed mutation before allowing another write.
                addAll(dependencies(uncertainRoute ?: ctx.route!!))
                add("diary")
            }
            addAll(dependencies(ctx.route!!))
        }.distinct()
        mutable.update { it.copy(loading = true, error = null, readRevision = readRevision) }
        readJob = scope.launch {
            try {
                for (feature in plan) {
                    ensureCurrent(ctx)
                    if (feature !in loaded) {
                        loadFeature(feature, ctx)
                        currentCoroutineContext().ensureActive()
                        ensureCurrent(ctx)
                        loaded.add(feature)
                    }
                }
                ensureCurrent(ctx)
                uncertainRoute = null
                publish(ctx) { it.copy(uncertain = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Cancelled transport calls must not become a connection-error banner.
                currentCoroutineContext().ensureActive()
                publish(ctx) { it.copy(error = (if (afterSave) "Đã lưu. Chưa tải lại được dữ liệu: " else "") + error.toUserMessage()) }
            } finally {
                publish(ctx) { it.copy(loading = false) }
            }
        }
    }

    fun write(success: () -> Unit = {}, operation: suspend (RequestContext) -> Unit) {
        if (mutable.value.busy || mutable.value.loading || mutable.value.uncertain || ownerId == null) return
        readRevision++
        readJob?.cancel()
        // Success may navigate to another screen; the write still belongs to this account/date.
        val ctx = requestContext(trackScreen = false)
        val originRoute = mutable.value.route
        mutable.update { it.copy(busy = true, error = null) }
        writeJob = scope.launch {
            var saved = false
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
                val unknown = !saved && ((error is IOException && error !is ApiFailure) ||
                    (error is HttpException && error.code() >= 500))
                if (valid(ctx) && unknown) {
                    loaded.clear()
                    uncertainRoute = originRoute
                }
                publish(ctx) { it.copy(uncertain = unknown, error = if (unknown)
                    "Chưa rõ kết quả yêu cầu. Bấm Tải lại và kiểm tra dữ liệu trước khi gửi tiếp."
                    else error.toUserMessage()) }
            } finally {
                if (valid(ctx)) {
                    mutable.update { it.copy(busy = false) }
                    if (saved) startLoad(afterSave = true)
                }
            }
        }
    }

    fun read(operation: suspend (RequestContext) -> Unit): Job {
        val ctx = context()
        return scope.launch {
            try {
                ensureCurrent(ctx)
                operation(ctx)
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
