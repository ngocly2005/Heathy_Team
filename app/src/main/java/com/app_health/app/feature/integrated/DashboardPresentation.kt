package com.app_health.app.feature.integrated

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class CaloriePresentation(val remaining: Double?, val progress: Float?, val percent: Double?, val aboveFoodTarget: Boolean)

/** Food consumption progress is independent of exercise; remaining calories stay server-owned. */
fun caloriePresentation(day: DashboardDto): CaloriePresentation {
    val target = day.calorie_target?.takeIf { it.isFinite() && it > 0 }
    val food = day.calories_consumed.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    return CaloriePresentation(day.calories_remaining?.takeIf { it.isFinite() },
        target?.let { (food / it).toFloat().coerceIn(0f, 1f) }, target?.let { food / it * 100 },
        target != null && food > target)
}

fun weekBeginning(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
