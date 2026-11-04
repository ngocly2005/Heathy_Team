package com.app_health.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.app_health.admin.repository.AdminDataRepository
import com.app_health.app.feature.home.model.FoodItem
import com.app_health.app.feature.home.model.ExerciseItem
import com.app_health.app.feature.home.model.ExerciseDatabase // Nơi chứa dữ liệu mặc định
import com.app_health.app.feature.home.model.FoodDatabase


class AdminDataViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminDataRepository(application)

    private val _foodList = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodList: StateFlow<List<FoodItem>> = _foodList.asStateFlow()

    private val _exerciseList = MutableStateFlow<List<ExerciseItem>>(emptyList())
    val exerciseList: StateFlow<List<ExerciseItem>> = _exerciseList.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Lấy trực tiếp từ nguồn FoodDatabase.allFoods của phía User
        val defaultFoods = FoodDatabase.allFoods
        val defaultExercises = ExerciseDatabase.allActivities

        // Gộp danh sách Mặc định từ User + Custom do Admin thêm
        _foodList.value = defaultFoods + repository.getCustomFoods()
        _exerciseList.value = defaultExercises + repository.getCustomExercises()
    }

    fun addFood(food: FoodItem) {
        repository.addCustomFood(food)
        loadData() // Refresh UI
    }

    fun addExercise(exercise: ExerciseItem) {
        repository.addCustomExercise(exercise)
        loadData() // Refresh UI
    }

    fun deleteFood(foodId: String) {
        repository.deleteCustomFood(foodId)
        loadData()
    }
}