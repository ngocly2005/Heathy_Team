package com.app_health.app.feature.integrated

import com.app_health.app.core.network.SessionRequestTag
import okhttp3.MultipartBody
import retrofit2.http.*

// Wire names intentionally match the shared OpenAPI contract.
data class ProfileDto(val birth_date: String, val sex: String, val height_cm: Double, val weight_kg: Double,
    val activity_level: String, val goal: String, val age: Int, val bmi: Double, val bmr_kcal: Double, val tdee_kcal: Double)
data class FoodDto(val id: Long, val name: String, val calories_per_100g: Double,
    val protein_per_100g: Double, val carb_per_100g: Double, val fat_per_100g: Double)
data class FoodLogDto(val id: Long, val food_id: Long?, val log_date: String, val meal_type: String,
    val meal_time: String?, val grams: Double, val food_name: String, val calories_kcal: Double,
    val protein_g: Double, val carb_g: Double, val fat_g: Double)
data class ActivityDto(val id: Long, val name: String, val met: Double)
data class ActivityLogDto(val id: Long, val log_date: String, val activity_name: String,
    val duration_minutes: Int, val calories_burned: Double, val met_snapshot: Double)
data class WaterLogDto(val id: Long, val log_date: String, val amount_ml: Int)
data class MacroDto(val protein_g: Double, val carb_g: Double, val fat_g: Double,
    val protein_pct: Double, val carb_pct: Double, val fat_pct: Double)
data class DashboardDto(val date: String, val calories_consumed: Double, val calories_burned: Double,
    val calorie_target: Double?, val calories_remaining: Double?, val net_calories: Double, val water_ml: Int,
    val macros: MacroDto, val food_logs: List<FoodLogDto>, val activity_logs: List<ActivityLogDto>)
data class NutritionDto(val calorie_target: Double?, val effective_calorie_target: Double?,
    val protein_pct: Int, val carb_pct: Int, val fat_pct: Int, val target_weight_kg: Double?,
    val protein_g: Double, val carb_g: Double, val fat_g: Double)
data class WaterSettingsDto(val target_ml: Int?, val serving_ml: Int, val effective_target_ml: Int)
data class WaterAdjustmentDto(val date: String, val total_ml: Int)
data class SavedItemDto(val food_id: Long?, val food_name: String, val grams: Double, val calories_per_100g: Double,
    val protein_per_100g: Double, val carb_per_100g: Double, val fat_per_100g: Double)
data class SavedMealDto(val id: Long, val name: String, val items: List<SavedItemDto>)
data class WeightDto(val id: Long, val measured_on: String, val weight_kg: Double)
data class DiaryDto(val content: String, val mood: String?)
data class DailyDto(val date: String, val dashboard: DashboardDto, val water_logs: List<WaterLogDto>,
    val weight: WeightDto?, val diary: DiaryDto?)
data class CalendarDto(val date: String, val status: String, val has_diary: Boolean, val weight_kg: Double?)
data class ChatAnswerDto(val answer: String, val suggestions: List<String>, val disclaimer: String)
data class ChatHistoryDto(val id: Long, val question: String?, val response: ChatAnswerDto?, val status: String,
    val error_code: Int?, val created_at: String)
data class PhotoItemDto(val name: String, val estimated_grams: Double, val calories_kcal: Double,
    val protein_g: Double, val carb_g: Double, val fat_g: Double, val confidence: Double)
data class PhotoDto(val items: List<PhotoItemDto>, val notes: String, val disclaimer: String)
data class PhotoDraft(val name: String, val grams: String, val calories: String, val protein: String,
    val carbs: String, val fat: String, val confidence: Double)

interface AppApi {
    @GET("api/v1/profile") suspend fun profile(@Tag owner: SessionRequestTag): ProfileDto
    @PUT("api/v1/profile") suspend fun saveProfile(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): ProfileDto
    @GET("api/v1/dashboard/daily") suspend fun dashboard(@Tag owner: SessionRequestTag, @Query("date") date: String): DashboardDto
    @GET("api/v1/nutrition-settings") suspend fun nutrition(@Tag owner: SessionRequestTag): NutritionDto
    @PUT("api/v1/nutrition-settings") suspend fun saveNutrition(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): NutritionDto
    @GET("api/v1/foods") suspend fun foods(@Tag owner: SessionRequestTag, @Query("q") query: String): List<FoodDto>
    @POST("api/v1/food-logs") suspend fun addFood(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): FoodLogDto
    @PUT("api/v1/food-logs/{id}") suspend fun editFood(@Tag owner: SessionRequestTag, @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): FoodLogDto
    @DELETE("api/v1/food-logs/{id}") suspend fun deleteFood(@Tag owner: SessionRequestTag, @Path("id") id: Long)
    @GET("api/v1/activity-types") suspend fun activities(@Tag owner: SessionRequestTag): List<ActivityDto>
    @POST("api/v1/activity-logs") suspend fun addActivity(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): ActivityLogDto
    @PUT("api/v1/activity-logs/{id}") suspend fun editActivity(@Tag owner: SessionRequestTag, @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): ActivityLogDto
    @DELETE("api/v1/activity-logs/{id}") suspend fun deleteActivity(@Tag owner: SessionRequestTag, @Path("id") id: Long)
    @GET("api/v1/water-settings") suspend fun waterSettings(@Tag owner: SessionRequestTag): WaterSettingsDto
    @PUT("api/v1/water-settings") suspend fun saveWater(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): WaterSettingsDto
    @POST("api/v1/water-logs/adjust") suspend fun adjustWater(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): WaterAdjustmentDto
    @PUT("api/v1/water-logs/{id}") suspend fun editWater(@Tag owner: SessionRequestTag, @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): WaterLogDto
    @DELETE("api/v1/water-logs/{id}") suspend fun deleteWater(@Tag owner: SessionRequestTag, @Path("id") id: Long)
    @GET("api/v1/saved-meals") suspend fun meals(@Tag owner: SessionRequestTag): List<SavedMealDto>
    @POST("api/v1/saved-meals") suspend fun saveMeal(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): SavedMealDto
    @PUT("api/v1/saved-meals/{id}") suspend fun editMeal(@Tag owner: SessionRequestTag, @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): SavedMealDto
    @DELETE("api/v1/saved-meals/{id}") suspend fun deleteMeal(@Tag owner: SessionRequestTag, @Path("id") id: Long)
    @POST("api/v1/saved-meals/{id}/log") suspend fun logMeal(@Tag owner: SessionRequestTag, @Path("id") id: Long, @Body value: Map<String, @JvmSuppressWildcards Any?>): List<FoodLogDto>
    @GET("api/v1/weight-entries") suspend fun weights(@Tag owner: SessionRequestTag): List<WeightDto>
    @PUT("api/v1/weight-entries/{date}") suspend fun saveWeight(@Tag owner: SessionRequestTag, @Path("date") date: String, @Body value: Map<String, @JvmSuppressWildcards Any?>): WeightDto
    @DELETE("api/v1/weight-entries/{date}") suspend fun deleteWeight(@Tag owner: SessionRequestTag, @Path("date") date: String)
    @GET("api/v1/history/daily") suspend fun daily(@Tag owner: SessionRequestTag, @Query("date") date: String): DailyDto
    @GET("api/v1/history/calendar") suspend fun calendar(@Tag owner: SessionRequestTag, @Query("month") month: String): List<CalendarDto>
    @PUT("api/v1/diary/{date}") suspend fun saveDiary(@Tag owner: SessionRequestTag, @Path("date") date: String, @Body value: Map<String, @JvmSuppressWildcards Any?>): DiaryDto
    @DELETE("api/v1/diary/{date}") suspend fun deleteDiary(@Tag owner: SessionRequestTag, @Path("date") date: String)
    @GET("api/v1/ai/chat/history") suspend fun chatHistory(@Tag owner: SessionRequestTag, @Query("before_id") before: Long? = null): List<ChatHistoryDto>
    @POST("api/v1/ai/chat") suspend fun chat(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): ChatAnswerDto
    @Multipart @POST("api/v1/ai/photo/analyze") suspend fun analyze(@Tag owner: SessionRequestTag, @Query("date") date: String, @Part image: MultipartBody.Part): PhotoDto
    @POST("api/v1/ai/photo/confirm") suspend fun confirm(@Tag owner: SessionRequestTag, @Body value: Map<String, @JvmSuppressWildcards Any?>): Map<String, List<FoodLogDto>>
}
