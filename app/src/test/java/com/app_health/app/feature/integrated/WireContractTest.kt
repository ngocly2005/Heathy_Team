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
    @Test fun catalogAndPersonalFoodsCanShareNumericIdsWithoutLosingPhotoFlags() {
        val foods = Gson().fromJson("""[
            {"id":1,"name":"Rice","calories_per_100g":100,"protein_per_100g":5,"carb_per_100g":20,"fat_per_100g":1,"is_custom":false,"has_image":true},
            {"id":1,"name":"My rice","calories_per_100g":120,"protein_per_100g":6,"carb_per_100g":22,"fat_per_100g":2,"is_custom":true,"has_image":false}
        ]""", Array<FoodDto>::class.java)
        assertEquals(2, foods.size)
        assertTrue(foods[0].has_image)
        assertFalse(foods[0].is_custom)
        assertTrue(foods[1].is_custom)
        assertFalse(foods[1].has_image)
        assertNotEquals(foods[0].is_custom, foods[1].is_custom)
    }
}
