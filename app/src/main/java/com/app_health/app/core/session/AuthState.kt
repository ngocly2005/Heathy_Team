package com.app_health.app.core.session

data class SessionUser(val id: Long, val email: String, val fullName: String, val role: String = "user")

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    /** Cached identity permits showing existing local screens when connectivity is unavailable. */
    data class SignedIn(val user: SessionUser, val offline: Boolean = false) : AuthState
}

// Deliberately not a data class: accidental toString() must never reveal tokens.
class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val user: SessionUser,
    val origin: String
) {
    override fun toString(): String = "StoredSession(redacted)"
}
