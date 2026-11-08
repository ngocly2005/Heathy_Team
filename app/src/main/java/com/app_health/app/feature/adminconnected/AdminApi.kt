package com.app_health.app.feature.adminconnected

import com.app_health.app.core.network.SessionRequestTag
import com.google.gson.JsonElement
import retrofit2.http.*

data class AdminIdentity(val id: Long, val email: String, val full_name: String, val role: String, val status: String)
data class AdminPage<T>(val items: List<T>, val total: Int)
data class AdminUserRow(val id: Long, val email: String, val full_name: String, val role: String, val status: String)
data class AdminFood(val id: Long, val name: String, val calories_per_100g: Double,
    val protein_per_100g: Double, val carb_per_100g: Double, val fat_per_100g: Double, val is_active: Boolean)
data class AdminActivity(val id: Long, val name: String, val met: Double, val is_active: Boolean)
data class AdminAiLog(val id: Long, val user_id: Long, val kind: String, val context_date: String,
    val question: String?, val response: JsonElement?, val status: String, val error_code: Int?, val created_at: String)
data class AdminDay(val date: String, val new_users: Long, val food_logs: Long,
    val activity_logs: Long, val water_ml: Long, val ai_requests: Long)
data class AdminReport(val date_from: String, val date_to: String, val totals: Map<String, Long>, val daily: List<AdminDay>)

/** Same server routes and permissions as React Admin; every call is bound to its session owner. */
interface AdminApi {
    @GET("api/v1/auth/me") suspend fun me(@Tag owner: SessionRequestTag): AdminIdentity
    @GET("api/v1/admin/analytics") suspend fun analytics(@Tag owner: SessionRequestTag,
        @Query("date_from") from: String, @Query("date_to") to: String): AdminReport
    @GET("api/v1/admin/users") suspend fun users(@Tag owner: SessionRequestTag,
        @QueryMap query: Map<String, String>): AdminPage<AdminUserRow>
    @GET("api/v1/admin/foods") suspend fun foods(@Tag owner: SessionRequestTag,
        @QueryMap query: Map<String, String>): AdminPage<AdminFood>
    @GET("api/v1/admin/activities") suspend fun activities(@Tag owner: SessionRequestTag,
        @QueryMap query: Map<String, String>): AdminPage<AdminActivity>
    @GET("api/v1/admin/ai-logs") suspend fun logs(@Tag owner: SessionRequestTag,
        @QueryMap query: Map<String, String>): AdminPage<AdminAiLog>
    @PUT("api/v1/admin/users/{id}") suspend fun status(@Tag owner: SessionRequestTag,
        @Path("id") id: Long, @Body value: Map<String, String>): AdminUserRow
    @POST("api/v1/admin/foods") suspend fun createFood(@Tag owner: SessionRequestTag,
        @Body value: Map<String, @JvmSuppressWildcards Any?>): AdminFood
    @PUT("api/v1/admin/foods/{id}") suspend fun editFood(@Tag owner: SessionRequestTag,
        @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): AdminFood
    @POST("api/v1/admin/activities") suspend fun createActivity(@Tag owner: SessionRequestTag,
        @Body value: Map<String, @JvmSuppressWildcards Any?>): AdminActivity
    @PUT("api/v1/admin/activities/{id}") suspend fun editActivity(@Tag owner: SessionRequestTag,
        @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): AdminActivity
    @DELETE("api/v1/admin/{resource}/{id}") suspend fun deactivate(@Tag owner: SessionRequestTag,
        @Path("resource") resource: String, @Path("id") id: Long)
}
