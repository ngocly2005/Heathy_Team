package com.app_health.app.feature.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.network.toUserMessage
import com.app_health.app.core.network.retryTransientRead
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(val busy: Boolean = false, val error: String? = null,
    val account: ApiUser? = null, val message: String? = null)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val authState = repository.state
    val logoutWarning = repository.logoutWarning
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState = mutableUiState.asStateFlow()
    private var identityRefresh: Job? = null

    fun clearFeedback() {
        if (!mutableUiState.value.busy) mutableUiState.value = mutableUiState.value.copy(error = null, message = null)
    }

    fun signInEmail(email: String, password: String) {
        if (!validateCredentials(emailLoginErrors(email, password))) return
        credentials(errorMessage = ::credentialErrorMessage) { repository.signInEmail(email, password) }
    }
    fun register(email: String, password: String, fullName: String) {
        if (!validateCredentials(registrationErrors(email, fullName, password, password))) return
        credentials(errorMessage = ::credentialErrorMessage) { repository.register(email, password, fullName) }
    }

    fun requestPasswordReset(email: String) {
        if (mutableUiState.value.busy) return
        val problem = accountEmailError(email)
        if (problem != null) {
            mutableUiState.value = mutableUiState.value.copy(error = problem, message = null)
            return
        }
        credentials("Nếu email có tài khoản mật khẩu, hệ thống sẽ gửi mã đặt lại.", ::passwordResetError) {
            repository.requestPasswordReset(email)
        }
    }

    fun resetPassword(email: String, token: String, next: String) {
        if (mutableUiState.value.busy) return
        val problem = newPasswordError(next)
        val emailProblem = accountEmailError(email)
        if (problem != null || emailProblem != null || token.length != 8 ||
            token.any { it !in '0'..'9' }) {
            mutableUiState.value = mutableUiState.value.copy(
                error = problem ?: emailProblem ?: "Nhập mã gồm đúng 8 chữ số được gửi tới email.",
                message = null)
            return
        }
        credentials("Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.", ::passwordResetError) {
            repository.resetPassword(email, token, next)
        }
    }

    private fun passwordResetError(error: Exception): String = when ((error as? HttpException)?.code()) {
        400 -> "Mã đặt lại không hợp lệ, đã dùng hoặc hết hạn. Hãy yêu cầu mã mới."
        503 -> "Máy chủ chưa cấu hình gửi email. Liên hệ người quản trị để bật tính năng này."
        else -> error.toUserMessage()
    }

    private fun validateCredentials(errors: CredentialErrors): Boolean {
        if (mutableUiState.value.busy) return false
        if (errors.isValid) return true
        mutableUiState.value = mutableUiState.value.copy(error = errors.messages.first())
        return false
    }

    fun loadAccount(ownerId: Long) = credentials { retryTransientRead { repository.account(ownerId) } }

    fun updatePassword(ownerId: Long, current: String?, next: String) = credentials("Đã lưu mật khẩu.") {
        repository.updatePassword(ownerId, current, next)
        retryTransientRead { repository.account(ownerId) }
    }

    fun linkGoogle(ownerId: Long, context: Context) = credentials("Đã liên kết Google với tài khoản hiện tại.") {
        repository.linkGoogle(ownerId, context)
    }

    private fun credentials(message: String? = null, errorMessage: (Exception) -> String = { it.toUserMessage() }, operation: suspend () -> Any?) {
        if (mutableUiState.value.busy) return
        val previousAccount = mutableUiState.value.account
        mutableUiState.value = AuthUiState(busy = true, account = previousAccount)
        viewModelScope.launch {
            try {
                val result = operation()
                mutableUiState.value = AuthUiState(account = result as? ApiUser, message = message)
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AuthUiState()
                throw cancelled
            } catch (_: GetCredentialCancellationException) {
                mutableUiState.value = AuthUiState(account = previousAccount)
            } catch (_: GetCredentialException) {
                mutableUiState.value = AuthUiState(account = previousAccount, error = "Không liên kết được Google. Kiểm tra tài khoản và cấu hình Google.")
            } catch (error: Exception) {
                mutableUiState.value = AuthUiState(account = previousAccount, error = errorMessage(error))
            }
        }
    }

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
