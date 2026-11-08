package com.app_health.app.feature.adminconnected

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.network.AppContainer
import com.app_health.app.core.network.SessionRequestTag
import com.app_health.app.core.session.SessionUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeAdminViewModel(private val container: AppContainer) : ViewModel() {
    val api: AdminApi = container.retrofit.create(AdminApi::class.java)
    private val controller = AdminController(viewModelScope, api,
        currentOwner = { container.sessionStore.snapshot()?.user?.id },
        identityUpdated = { identity -> withContext(Dispatchers.IO) {
            container.sessionStore.updateUser(SessionUser(identity.id, identity.email, identity.full_name, identity.role), identity.id)
        } })
    val state = controller.state
    fun activate() = controller.activate()
    fun release() = controller.release()
    fun refresh() = controller.refresh()
    fun select(query: AdminQuery) = controller.select(query)
    fun write(operation: suspend (SessionRequestTag) -> Unit, success: () -> Unit = {}) = controller.write(operation, success)
    override fun onCleared() { controller.release() }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NativeAdminViewModel::class.java))
                return NativeAdminViewModel(container) as T
            }
        }
    }
}
