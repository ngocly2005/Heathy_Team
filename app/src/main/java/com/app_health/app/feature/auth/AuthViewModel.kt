package com.app_health.app.feature.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(val busy: Boolean = false, val error: String? = null)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val authState = repository.state
    val logoutWarning = repository.logoutWarning
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState = mutableUiState.asStateFlow()
    private var identityRefresh: Job? = null

    /** Re-read BE role after returning to the app or reloading Account; coalesce callers. */
    fun refreshIdentity() {
        if (mutableUiState.value.busy || identityRefresh?.isActive == true) return
        identityRefresh = viewModelScope.launch {
            try { repository.restoreSession(force = true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutableUiState.value = mutableUiState.value.copy(error = error.toUserMessage())
            }
        }
    }

    fun logout() {
        if (mutableUiState.value.busy) return
        mutableUiState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                repository.logout()
                mutableUiState.value = AuthUiState()
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AuthUiState()
                throw cancelled
            } catch (error: Exception) {
                mutableUiState.value = AuthUiState(error = error.toUserMessage())
            }
        }
    }

    fun signIn(activityContext: Context) {
        if (mutableUiState.value.busy) return
        mutableUiState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                repository.signInWithGoogle(activityContext)
                mutableUiState.value = AuthUiState()
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AuthUiState()
                throw cancelled
            } catch (_: GetCredentialCancellationException) {
                mutableUiState.value = AuthUiState()
            } catch (_: NoCredentialException) {
                mutableUiState.value = AuthUiState(error = "Chưa có tài khoản Google khả dụng. Kiểm tra tài khoản và Google Play services trên thiết bị.")
            } catch (_: GetCredentialException) {
                mutableUiState.value = AuthUiState(error = "Không thể đăng nhập Google. Kiểm tra Web Client ID, Android OAuth Client và SHA-1.")
            } catch (error: Exception) {
                mutableUiState.value = AuthUiState(error = error.toUserMessage())
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repository) as T
            }
        }
    }
}
