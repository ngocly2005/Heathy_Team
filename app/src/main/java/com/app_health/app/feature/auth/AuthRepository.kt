package com.app_health.app.feature.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.app_health.app.BuildConfig
import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.toUserMessage
import com.app_health.app.core.session.AuthState
import com.app_health.app.core.session.SessionStore
import com.app_health.app.core.session.StoredSession
import com.app_health.app.feature.home.components.LoginLogout.SharedDataStoreProvider
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepository internal constructor(
    private val context: Context,
    private val sessions: SessionStore,
    private val rawApi: AuthApi,
    private val authenticatedApi: AuthApi,
    private val origin: String
) {
    val state: StateFlow<AuthState> = sessions.state
    private val operations = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var restored = false
    private val mutableLogoutWarning = MutableStateFlow<String?>(null)
    val logoutWarning: StateFlow<String?> = mutableLogoutWarning.asStateFlow()
    private val credentialManager = CredentialManager.create(context)

    init { scope.launch { restoreSession() } }

    suspend fun restoreSession(force: Boolean = false) = operations.withLock {
        if (restored && !force) return@withLock
        restored = true
        val cached = try {
            withContext(Dispatchers.IO) { sessions.load() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            withContext(Dispatchers.IO) { runCatching { sessions.clear() } }
            return@withLock
        }
        if (cached == null) {
            withContext(Dispatchers.IO) { sessions.clear() }
            return@withLock
        }
        try {
            val user = authenticatedApi.me().toSessionUser()
            if (user.id != cached.user.id) {
                withContext(Dispatchers.IO) { sessions.clear() }
                return@withLock
            }
            withContext(Dispatchers.IO) { sessions.updateUser(user, cached.user.id) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The authenticator alone clears terminal failures (invalid refresh or fresh bearer).
            // If a session remains, transport/server failure must not erase cached credentials.
            withContext(Dispatchers.IO) { sessions.finishRestoreOffline(cached.user.id) }
        }
    }

    suspend fun signInWithGoogle(activityContext: Context) = operations.withLock {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            throw ApiFailure(503, "Chưa cấu hình GOOGLE_WEB_CLIENT_ID trong local.properties.")
        }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        // getCredential must receive a foreground Activity context for its account chooser.
        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ApiFailure(400, "Không nhận được tài khoản Google hợp lệ.")
        }
        val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        // Google ID token is sent once to /auth/google and is never persisted or logged.
        val pair = rawApi.google(GoogleLoginRequest(googleToken))
        if (pair.accessToken.isNullOrBlank() || pair.refreshToken.isNullOrBlank()) {
            throw ApiFailure(502, "Máy chủ trả về phiên chưa đầy đủ.")
        }
        try {
            val user = rawApi.me("Bearer ${pair.accessToken}").toSessionUser()
            // Keep existing physical-profile values. Never create a local password account.
            SharedDataStoreProvider.get(context, user.email).edit { preferences ->
                preferences[stringPreferencesKey("key_name")] = user.fullName
                preferences[stringPreferencesKey("key_email")] = user.email
                preferences.remove(stringPreferencesKey("key_password"))
            }
            withContext(Dispatchers.IO) {
                sessions.save(StoredSession(pair.accessToken, pair.refreshToken, user, origin))
            }
            mutableLogoutWarning.value = null
        } catch (error: Exception) {
            // Exchange succeeded but local sign-in did not: do not leave that refresh token active.
            if (sessions.snapshot()?.refreshToken != pair.refreshToken) {
                withContext(NonCancellable) { runCatching { rawApi.logout(RefreshRequest(pair.refreshToken)) } }
            }
            throw error
        }
    }

    suspend fun logout() = withContext(NonCancellable + Dispatchers.IO) {
        operations.withLock {
            val previous = sessions.snapshot()
            mutableLogoutWarning.value = null
            // Invalidate locally before network calls; a late refresh cannot resurrect this account.
            try {
                sessions.clear()
            } finally {
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                    .onFailure { mutableLogoutWarning.value = "Đã thoát tài khoản. Không thể xóa lựa chọn Google lúc này." }
                if (previous != null) runCatching { rawApi.logout(RefreshRequest(previous.refreshToken)) }
                    .onFailure { mutableLogoutWarning.value = "Đã thoát trên thiết bị. Chưa thu hồi được phiên trên máy chủ: ${it.toUserMessage()}" }
            }
        }
    }
}
