package com.app_health.app.feature.integrated

import org.junit.Assert.*
import org.junit.Test

class HealthGoalWarningsTest {
    private fun profile(sex: String = "female") = ProfileDto("1996-01-01", sex, 165.0, 70.0,
        "moderate", "lose", 30, 25.7, if (sex == "female") 1420.0 else 1600.0, 2200.0)

    @Test fun macroWarningsRespectAdultReferenceBoundsAndRequireValidTotal() {
        assertTrue(macroGoalWarnings(10, 55, 35, 30).isEmpty())
        assertTrue(macroGoalWarnings(35, 45, 20, 30).isEmpty())
        assertEquals(3, macroGoalWarnings(50, 40, 10, 30).size)
        assertTrue(macroGoalWarnings(100, 0, 0, 30).size == 3)
        assertEquals(2, macroGoalWarnings(50, 50, 50, 30).size)
        assertTrue(macroGoalWarnings(30, 50, 20, 16).single().contains("19 tuổi"))
    }

    @Test fun weightWarningsDescribeTimeRatherThanRejectingLongTermGoal() {
        assertTrue(weightGoalWarnings(67.0, profile()).isEmpty())
        assertTrue(weightGoalWarnings(64.0, profile()).any { it.contains("6 tuần") })
        assertTrue(weightGoalWarnings(45.0, profile()).any { it.contains("BMI") })
        assertTrue(weightGoalWarnings(64.0, null).isEmpty())
    }

    @Test fun calorieWarningsUseSexAndBmrReviewThresholdAndTdeeUpperReview() {
        assertTrue(calorieGoalWarnings(1800.0, profile()).isEmpty())
        assertTrue(calorieGoalWarnings(1400.0, profile()).single().contains("BMR"))
        assertTrue(calorieGoalWarnings(1499.0, profile("male")).single().contains("1600"))
        assertTrue(calorieGoalWarnings(2701.0, profile()).single().contains("TDEE"))
        assertTrue(calorieGoalWarnings(null, profile()).isEmpty())
    }

    @Test fun waterWarningIsAReviewTriggerAndNotPresentedAsToxicLimit() {
        assertTrue(waterGoalWarnings(3000).isEmpty())
        assertTrue(waterGoalWarnings(4501).single().contains("không phải ngưỡng độc hại cố định"))
        assertTrue(waterGoalWarnings(4100, profile()).isEmpty())
        assertTrue(waterIntakeWarnings(2700, 2723).isEmpty())
        assertTrue(waterIntakeWarnings(3200, 2723).single().contains("vượt hơn 10%"))
        assertTrue(waterIntakeWarnings(4700, 2723).single().contains("theo ngày"))
    }
}
