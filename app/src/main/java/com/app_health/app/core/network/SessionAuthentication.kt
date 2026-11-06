package com.app_health.app.core.network

import com.app_health.app.core.session.SessionStore
import com.app_health.app.feature.auth.AuthApi
import com.app_health.app.feature.auth.RefreshRequest
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

private class RequestSession(val userId: Long, val accessToken: String)

/** Feature repositories attach their owner ID with Retrofit @Tag before suspending. */
data class SessionRequestTag(val userId: Long)

private fun HttpUrl.sameOrigin(other: HttpUrl): Boolean =
    scheme == other.scheme && host == other.host && port == other.port

class BearerInterceptor(private val sessions: SessionStore, private val origin: HttpUrl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isProtected = original.url.sameOrigin(origin) &&
            original.url.encodedPath.startsWith("/api/v1/") &&
            original.url.encodedPath !in setOf("/api/v1/auth/google", "/api/v1/auth/refresh", "/api/v1/auth/logout")
        val session = sessions.snapshot()
        val expected = original.tag(SessionRequestTag::class.java)
        if (isProtected && expected != null && session?.user?.id != expected.userId) {
            throw IOException("Tài khoản đã thay đổi. Vui lòng mở lại màn hình.")
        }
        val request = if (isProtected && session != null) original.newBuilder()
            .header("Authorization", "Bearer ${session.accessToken}")
            .tag(RequestSession::class.java, RequestSession(session.user.id, session.accessToken))
            .build() else original
        val response = chain.proceed(request)
        if (isProtected && session != null && response.isSuccessful) sessions.markOnline(session.user.id)
        return response
    }
}

/** One refresh at a time, even if multiple screens receive 401 concurrently. */
class SessionAuthenticator(
    private val sessions: SessionStore,
    private val rawAuthApi: AuthApi,
    private val origin: HttpUrl
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!response.request.url.sameOrigin(origin)) return null
        val sent = response.request.tag(RequestSession::class.java) ?: return null
        if (response.priorResponse != null) {
            // Fresh bearer also rejected. Do not loop or clear a newer account's session.
            val usedBearer = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (usedBearer != null) sessions.clearIfAccessTokenMatches(usedBearer)
            return null
        }
        val token = synchronized(refreshLock) {
            val current = sessions.snapshot() ?: return@synchronized null
            if (current.user.id != sent.userId) return@synchronized null
            if (current.accessToken != sent.accessToken) return@synchronized current.accessToken

            // This API uses a client with no bearer interceptor or authenticator.
            // IOException propagates: cached credentials survive temporary transport failure.
            val result = rawAuthApi.refresh(RefreshRequest(current.refreshToken)).execute()
            if (!result.isSuccessful) {
                result.errorBody()?.close()
                if (result.code() == 401) {
                    sessions.clearIfRefreshTokenMatches(current.refreshToken)
                    return@synchronized null
                }
                throw IOException("Không thể làm mới phiên lúc này (HTTP ${result.code()}).")
            }
            val pair = result.body() ?: throw IOException("Máy chủ trả về phiên trống.")
            if (pair.accessToken.isNullOrBlank() || pair.refreshToken.isNullOrBlank()) {
                throw IOException("Máy chủ trả về phiên chưa đầy đủ.")
            }
            if (sessions.replaceTokens(current.refreshToken, pair.accessToken, pair.refreshToken)) {
                pair.accessToken
            } else {
                // Logout or account switching won the race. Revoke the unused rotated token too.
                runCatching {
                    val revoke = rawAuthApi.logoutSync(RefreshRequest(pair.refreshToken)).execute()
                    revoke.errorBody()?.close()
                }
                null
            }
        } ?: return null
        // Exactly one replay after a definitive auth rejection, including for write requests.
        return response.request.newBuilder().header("Authorization", "Bearer $token").build()
    }
}
