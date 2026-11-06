package com.app_health.app.feature.integrated

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.network.AppContainer
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
    val photoDrafts: List<PhotoDraft> = emptyList()
)
data class RequestContext(val owner: SessionRequestTag, val date: String, val epoch: Long)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val container = AppContainer.get(application)
    val api = container.retrofit.create(AppApi::class.java)
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    private var epoch = 0L
    private var ownerId: Long? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    var photoBytes: ByteArray? = null
    var photoMime: String = "image/jpeg"
    var pendingChat: String? = null

    fun activate() {
        val id = container.sessionStore.snapshot()?.user?.id ?: return
        if (id == ownerId) return
        epoch++
        ownerId = id
        mutable.value = AppState()
        refresh()
    }
    fun release() {
        epoch++
        ownerId = null
        readJob?.cancel()
        writeJob?.cancel()
        photoBytes = null
        pendingChat = null
        mutable.value = AppState()
    }
    fun releaseIfSessionChanged() {
        if (container.sessionStore.snapshot()?.user?.id != ownerId) release()
    }
    fun context(): RequestContext {
        val id = ownerId ?: error("Vui lòng đăng nhập lại.")
        return RequestContext(SessionRequestTag(id), mutable.value.date.toString(), epoch)
    }
    fun valid(ctx: RequestContext) = ctx.epoch == epoch && ctx.owner.userId == ownerId &&
        container.sessionStore.snapshot()?.user?.id == ownerId && ctx.date == mutable.value.date.toString()
    fun publish(ctx: RequestContext, transform: (AppState) -> AppState) {
        if (valid(ctx)) mutable.update(transform)
    }
    fun showError(message: String?) { mutable.update { it.copy(error = message) } }
    fun chooseDate(date: LocalDate) {
        if (mutable.value.busy || mutable.value.uncertain || date > LocalDate.now()) return
        epoch++
        readJob?.cancel()
        mutable.update { it.copy(date = date, dashboard = null, daily = null, calendar = emptyList(), error = null,
            photo = null, photoDrafts = emptyList(), photoConfirmed = false) }
        refresh()
    }
    private suspend fun load(ctx: RequestContext) {
        for (feature in installedFeatures()) {
            ensureActiveContext(ctx)
            feature.load(this, ctx)
        }
    }
    private fun ensureActiveContext(ctx: RequestContext) {
        if (!valid(ctx)) throw CancellationException("Account or date changed")
    }
    fun refresh() {
        if (ownerId == null || mutable.value.busy) return
        readJob?.cancel()
        val ctx = context()
        mutable.update { it.copy(loading = true, error = null) }
        readJob = viewModelScope.launch {
            try {
                load(ctx)
                publish(ctx) { it.copy(uncertain = false) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) { publish(ctx) { it.copy(error = e.toUserMessage()) }
            } finally { publish(ctx) { it.copy(loading = false) } }
        }
    }
    fun write(success: () -> Unit = {}, operation: suspend (RequestContext) -> Unit) {
        if (mutable.value.busy || mutable.value.loading || mutable.value.uncertain || ownerId == null) return
        readJob?.cancel()
        val ctx = context()
        mutable.update { it.copy(busy = true, error = null) }
        writeJob = viewModelScope.launch {
            try {
                operation(ctx)
                ensureActiveContext(ctx)
                success()
                // The mutation succeeded even if a later refresh fails.
                try { load(ctx) } catch (cancelled: CancellationException) { throw cancelled
                } catch (e: Exception) { publish(ctx) { it.copy(error = "Đã lưu. Chưa tải lại được dữ liệu: ${e.toUserMessage()}") } }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) {
                val unknown = (e is IOException && e !is com.app_health.app.core.network.ApiFailure) ||
                    (e is HttpException && e.code() >= 500)
                publish(ctx) { it.copy(uncertain = unknown, error = if (unknown)
                    "Chưa rõ kết quả yêu cầu. Bấm Tải lại và kiểm tra dữ liệu trước khi gửi tiếp."
                    else e.toUserMessage()) }
            } finally { publish(ctx) { it.copy(busy = false) } }
        }
    }
    fun read(operation: suspend (RequestContext) -> Unit): Job {
        val ctx = context()
        return viewModelScope.launch {
            try { operation(ctx) } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) { publish(ctx) { it.copy(error = e.toUserMessage()) } }
        }
    }
    fun setPhoto(bytes: ByteArray, mime: String) {
        if (ownerId == null || container.sessionStore.snapshot()?.user?.id != ownerId || mutable.value.busy || mutable.value.uncertain) return
        photoBytes = bytes
        photoMime = mime
        mutable.update { it.copy(photo = null, photoDrafts = emptyList(), photoConfirmed = false, photoRevision = it.photoRevision + 1, error = null) }
    }
}
