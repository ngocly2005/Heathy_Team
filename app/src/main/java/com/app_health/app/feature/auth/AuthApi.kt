package com.app_health.app.feature.auth

import com.app_health.app.core.session.SessionUser
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

class GoogleLoginRequest(@SerializedName("id_token") val idToken: String)
class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)
class TokenPair(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)
data class ApiUser(val id: Long, val email: String, @SerializedName("full_name") val fullName: String, val role: String = "user") {
    fun toSessionUser(): SessionUser {
        require(id > 0 && email.isNotBlank()) { "Invalid server identity" }
        return SessionUser(id, email, fullName, role)
    }
}
data class AuthMessage(val message: String)

interface AuthApi {
    @POST("api/v1/auth/google")
    suspend fun google(@Body request: GoogleLoginRequest): TokenPair

    // Authenticator executes this synchronously on a separate client, never runBlocking Retrofit.
    @POST("api/v1/auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenPair>

    @GET("api/v1/auth/me")
    suspend fun me(@Header("Authorization") bearer: String? = null): ApiUser

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshRequest): AuthMessage

    @POST("api/v1/auth/logout")
    fun logoutSync(@Body request: RefreshRequest): Call<AuthMessage>
}
