package com.app_health.app.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class HealthResponse(
    val status: String,
    @SerializedName("app_name") val appName: String?,
    val environment: String?
)

interface HealthApi {
    // /health belongs to the origin root, outside /api/v1.
    @GET("health")
    suspend fun health(): HealthResponse
}
