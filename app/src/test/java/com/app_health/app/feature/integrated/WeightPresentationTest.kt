package com.app_health.app.feature.integrated

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WeightPresentationTest {
    @Test fun monthFiltersBoundaryDatesAndKeepsChronologicalMeasurements() {
        val values = listOf(WeightDto(1,"2026-10-31",62.0), WeightDto(2,"2026-11-30",60.05),
            WeightDto(3,"2026-12-01",59.0), WeightDto(4,"2026-11-01",61.25))
        val rows = monthlyWeightPoints(values,YearMonth.of(2026,11))
        assertEquals(listOf("2026-11-01","2026-11-30"),rows.map{it.date.toString()})
        assertEquals(60.05,rows.last().entry.weight_kg,0.00001)
    }

    @Test fun emptyMonthHasNoInventedMeasurements() {
        val rows = monthlyWeightPoints(listOf(WeightDto(1,"2026-10-31",62.0)),YearMonth.of(2026,11))
        assertTrue(rows.isEmpty())
        assertTrue(weightChartRange(rows).maxKg > weightChartRange(rows).minKg)
    }

    @Test fun leapMonthHasFullCalendarXAxis() {
        val month = YearMonth.of(2024,2)
        assertEquals(0f,weightDayFraction(month.atDay(1),month),0.00001f)
        assertEquals(1f,weightDayFraction(month.atDay(29),month),0.00001f)
        assertEquals(0.5f,weightDayFraction(month.atDay(15),month),0.00001f)
    }

    @Test fun singlePointKeepsItsActualDayRatherThanMovingToChartCenter() {
        val month = YearMonth.of(2026,11)
        val rows = monthlyWeightPoints(listOf(WeightDto(1,"2026-11-03",60.05)),month)
        assertEquals(2f/29f,weightDayFraction(rows.single().date,month),0.00001f)
        val range = weightChartRange(rows)
        assertTrue(range.minKg < 60.05 && range.maxKg > 60.05)
    }

    @Test fun unchangedMeasurementsStillHaveANonzeroYAxisRange() {
        val month = YearMonth.of(2026,11)
        val rows = monthlyWeightPoints(listOf(WeightDto(1,"2026-11-03",60.0),WeightDto(2,"2026-11-07",60.0)),month)
        val range = weightChartRange(rows)
        assertEquals(1.0,range.maxKg-range.minKg,0.00001)
    }

    @Test fun editingPreservesHundredthsAndAcceptsVietnameseDecimalInput() {
        assertEquals("60.05",weightInputText(60.05))
        assertEquals("60",weightInputText(60.0))
        assertEquals("60,05",weightLabel(60.05))
        assertEquals(60.05,weightInputAmount("60,05")!!,0.00001)
        assertEquals(60.05,weightInputAmount("60.05")!!,0.00001)
        assertEquals(400.0,weightInputAmount("400,00")!!,0.00001)
        assertEquals(20.01,weightInputAmount("20.01")!!,0.00001)
    }

    @Test fun invalidOrOverPreciseWeightsHaveAnExplicitValidationReason() {
        for (value in listOf("", "20", "400.01", "NaN", "Infinity", "6e1", "60.005", "not a weight")) {
            assertNotNull("Expected validation for $value",weightInputError(value))
            assertNull(weightInputAmount(value))
        }
    }

    @Test fun pastMeasurementDatesAreAllowedAndOnlyFutureDatesAreBlocked() {
        val today = LocalDate.of(2026,11,7)
        assertTrue(allowedWeightDate(LocalDate.of(2020,1,5),today))
        assertTrue(allowedWeightDate(today,today))
        assertFalse(allowedWeightDate(today.plusDays(1),today))
    }

    @Test fun datePickerUtcConversionKeepsTheSelectedCalendarDate() {
        for (date in listOf(LocalDate.of(2026,11,7),LocalDate.of(2024,2,29),LocalDate.of(2025,12,31))) {
            assertEquals(date,weightPickerDate(weightPickerMillis(date)))
        }
    }
}
