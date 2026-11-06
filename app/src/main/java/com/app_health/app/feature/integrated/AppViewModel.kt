package com.app_health.app.feature.integrated

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.network.AppContainer
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val container = AppContainer.get(application)
    val api = container.retrofit.create(AppApi::class.java)
    private val features = installedFeatures().associateBy { it.route }
    private val data = AppDataController(viewModelScope,
        currentUserId = { container.sessionStore.snapshot()?.user?.id },
        loadFeature = { route, ctx -> features.getValue(route).load(this, ctx) })
    val state = data.state
    val photoBytes get() = data.photoBytes
    val photoMime get() = data.photoMime
    var pendingChat: String?
        get() = data.pendingChat
        set(value) { data.pendingChat = value }

    fun activate() = data.activate()
    fun release() = data.release()
    fun releaseIfSessionChanged() = data.releaseIfSessionChanged()
    fun context() = data.context()
    fun valid(ctx: RequestContext) = data.valid(ctx)
    fun publish(ctx: RequestContext, transform: (AppState) -> AppState) = data.publish(ctx, transform)
    fun showError(message: String?, ctx: RequestContext? = null) = data.showError(message, ctx)
    fun enter(route: String) = data.enter(route)
    fun chooseDate(date: LocalDate) = data.chooseDate(date)
    fun refresh() = data.refresh()
    fun write(success: () -> Unit = {}, operation: suspend (RequestContext) -> Unit) = data.write(success, operation)
    fun read(operation: suspend (RequestContext) -> Unit) = data.read(operation)
    fun setPhoto(bytes: ByteArray, mime: String) = data.setPhoto(bytes, mime)
}
