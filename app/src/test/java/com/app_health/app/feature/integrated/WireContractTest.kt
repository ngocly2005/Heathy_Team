package com.app_health.app.feature.integrated

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class WireContractTest {
    @Test fun waterAdjustmentHasDateAndNumericTotal() {
        val value=Gson().fromJson("""{"date":"2026-11-05","total_ml":300}""",WaterAdjustmentDto::class.java)
        assertEquals("2026-11-05",value.date)
        assertEquals(300,value.total_ml)
    }
    @Test fun quantitiesAcceptVietnameseDecimalAndRejectNonFiniteValues() {
        assertEquals(12.5,inputNumber("12,5")!!,0.0001)
        assertNull(inputNumber("NaN"))
        assertNull(inputNumber("Infinity"))
        assertNull(inputNumber("not a quantity"))
    }
    @Test fun mealTimeMatchesServerClockBounds() {
        assertTrue(validMealTime("00:00"))
        assertTrue(validMealTime("23:59"))
        assertFalse(validMealTime("24:00"))
        assertFalse(validMealTime("08:0"))
    }
}
