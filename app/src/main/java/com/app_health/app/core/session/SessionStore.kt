package com.app_health.app.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only AppContainer constructs this store. Disk operations run on Dispatchers.IO. */
class SessionStore internal constructor(context: Context, private val origin: String) {
    private val preferences = context.getSharedPreferences("api_session_v1", Context.MODE_PRIVATE)
    private val lock = Any()
    private val gson = Gson()
    private var session: StoredSession? = null
    private var loaded = false
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = mutableState.asStateFlow()

    fun snapshot(): StoredSession? = synchronized(lock) { session }

    internal fun load(): StoredSession? = synchronized(lock) {
        if (loaded) return@synchronized session
        loaded = true
        val encrypted = preferences.getString("encrypted_session", null)
        session = if (encrypted == null) null else runCatching {
            val pieces = encrypted.split('.')
            require(pieces.size == 3 && pieces[0] == "v1")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(
                128, Base64.decode(pieces[1], Base64.NO_WRAP)
            ))
            cipher.updateAAD(origin.toByteArray(Charsets.UTF_8))
            val plain = cipher.doFinal(Base64.decode(pieces[2], Base64.NO_WRAP))
            gson.fromJson(String(plain, Charsets.UTF_8), StoredSession::class.java).also {
                require(it.origin == origin && it.accessToken.isNotBlank() && it.refreshToken.isNotBlank())
                require(it.user.id > 0 && it.user.email.isNotBlank())
            }
        }.getOrNull()
        // Keystore invalidation or another server URL means a new sign-in is required.
        if (encrypted != null && session == null) {
            preferences.edit().clear().commit()
            // A key invalidated by device security changes must not block the next Google login.
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    .deleteEntry("apphealth.api.session.v1")
            }
        }
        session
    }

    internal fun save(value: StoredSession) = synchronized(lock) {
        require(value.origin == origin)
        persist(value)
        session = value
        mutableState.value = AuthState.SignedIn(value.user)
    }

    internal fun replaceTokens(expectedRefresh: String, access: String, refresh: String): Boolean =
        synchronized(lock) {
            val current = session ?: return@synchronized false
            if (current.refreshToken != expectedRefresh) return@synchronized false
            val updated = StoredSession(access, refresh, current.user, origin)
            persist(updated) // One encrypted blob commits the pair together.
            session = updated
            if (mutableState.value is AuthState.SignedIn) {
                mutableState.value = AuthState.SignedIn(updated.user)
            }
            true
        }

    internal fun updateUser(user: SessionUser, expectedUserId: Long) = synchronized(lock) {
        val current = session ?: return@synchronized
        if (current.user.id != expectedUserId || user.id != expectedUserId) return@synchronized
        val updated = StoredSession(current.accessToken, current.refreshToken, user, origin)
        persist(updated)
        session = updated
        mutableState.value = AuthState.SignedIn(user)
    }

    internal fun finishRestoreOffline(expectedUserId: Long) = synchronized(lock) {
        val current = session ?: return@synchronized
        if (current.user.id == expectedUserId) mutableState.value = AuthState.SignedIn(current.user, offline = true)
    }

    internal fun markOnline(expectedUserId: Long) = synchronized(lock) {
        val current = session ?: return@synchronized
        if (current.user.id == expectedUserId && mutableState.value is AuthState.SignedIn) {
            mutableState.value = AuthState.SignedIn(current.user)
        }
    }

    internal fun clearIfAccessTokenMatches(access: String) = synchronized(lock) {
        if (session?.accessToken == access) clear()
    }

    internal fun clearIfRefreshTokenMatches(refresh: String) = synchronized(lock) {
        if (session?.refreshToken == refresh) clear()
    }

    internal fun clear() = synchronized(lock) {
        session = null
        mutableState.value = AuthState.SignedOut
        // commit() gives the next process the same view; never keep tokens only in apply()'s queue.
        if (!preferences.edit().clear().commit()) throw IOException("Không thể xóa phiên trên thiết bị.")
    }

    private fun persist(value: StoredSession) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(origin.toByteArray(Charsets.UTF_8))
            val encrypted = cipher.doFinal(gson.toJson(value).toByteArray(Charsets.UTF_8))
            val blob = "v1." + Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            if (!preferences.edit().putString("encrypted_session", blob).commit()) {
                throw IOException("Không thể lưu phiên trên thiết bị.")
            }
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("Không thể bảo vệ phiên trên thiết bị.", error)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val alias = "apphealth.api.session.v1"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }
}
