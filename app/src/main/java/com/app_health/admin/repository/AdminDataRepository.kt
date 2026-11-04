package com.app_health.admin.repository

import android.content.Context
import com.app_health.app.feature.home.model.FoodItem
import com.app_health.app.feature.home.model.ExerciseItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AdminDataRepository(context: Context) {
    private val prefs = context.getSharedPreferences("AdminDataPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Xử lý Thực phẩm ---
    fun getCustomFoods(): List<FoodItem> {
        val json = prefs.getString("custom_foods", null) ?: return emptyList()
        val type = object : TypeToken<List<FoodItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addCustomFood(food: FoodItem) {
        val currentList = getCustomFoods().toMutableList()
        currentList.add(food)
        prefs.edit().putString("custom_foods", gson.toJson(currentList)).apply()
    }

    fun deleteCustomFood(foodName: String) {
        val currentList = getCustomFoods().toMutableList()
        currentList.removeAll { it.name == foodName }
        prefs.edit().putString("custom_foods", gson.toJson(currentList)).apply()
    }

    // --- Xử lý Bài tập ---
    fun getCustomExercises(): List<ExerciseItem> {
        val json = prefs.getString("custom_exercises", null) ?: return emptyList()
        val type = object : TypeToken<List<ExerciseItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addCustomExercise(exercise: ExerciseItem) {
        val currentList = getCustomExercises().toMutableList()
        currentList.add(exercise)
        prefs.edit().putString("custom_exercises", gson.toJson(currentList)).apply()
    }

    fun deleteCustomExercise(exerciseName: String) {
        val currentList = getCustomExercises().toMutableList()
        currentList.removeAll { it.name == exerciseName }
        prefs.edit().putString("custom_exercises", gson.toJson(currentList)).apply()
    }
}