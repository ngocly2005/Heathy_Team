package com.app_health.app.feature.integrated

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DashboardPresentationTest {
    private fun day(food: Double, burned: Double = 400.0, target: Double? = 2000.0) = DashboardDto(
        "2026-11-07", food, burned, target, target?.let { it - food + burned }, food - burned, 0,
        MacroDto(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), emptyList(), emptyList())

    @Test fun foodProgressChangesEvenWhileExerciseExceedsFood() {
        val before = caloriePresentation(day(100.0))
        val after = caloriePresentation(day(200.0))
        assertEquals(0.05f, before.progress!!, 0.00001f)
        assertEquals(0.10f, after.progress!!, 0.00001f)
        assertEquals(2300.0, before.remaining!!, 0.0001)
        assertEquals(2200.0, after.remaining!!, 0.0001)
    }

    @Test fun fractionalFoodCaloriesAreNotTruncatedToAnInteger() {
        val model = caloriePresentation(day(0.5, 0.0))
        assertTrue(model.progress!! > 0f)
        assertEquals(1999.5, model.remaining!!, 0.0001)
    }

    @Test fun overTargetAndMissingTargetsAreExplicit() {
        val over = caloriePresentation(day(2300.0, 0.0))
        assertEquals(1f, over.progress!!, 0f)
        assertEquals(115.0, over.percent!!, 0.0001)
        assertEquals(-300.0, over.remaining!!, 0.0001)
        assertTrue(over.aboveFoodTarget)
        assertNull(caloriePresentation(day(100.0, target = null)).progress)
        assertNull(caloriePresentation(day(100.0, target = 0.0)).percent)
    }

    @Test fun referenceNumbersFollowBackendBudgetIncludingExercise() {
        val model = caloriePresentation(day(1180.0, 120.0, 1800.0))
        assertEquals(740.0, model.remaining!!, 0.0001)
        assertEquals(1180.0 / 1800.0, model.progress!!.toDouble(), 0.00001)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun savingAndDeletingFoodRefreshesDashboardWithoutManualReload() = runTest {
        var food = 100.0
        lateinit var controller: AppDataController
        controller = AppDataController(this, { 1L }) { route, ctx ->
            if (route == "home") controller.publish(ctx) { it.copy(dashboard = day(food)) }
        }
        controller.activate()
        controller.enter("home")
        advanceUntilIdle()
        val old = caloriePresentation(controller.state.value.dashboard!!)
        controller.enter("food")
        controller.write(success = { controller.enter("home") }) { food += 52.5 }
        advanceUntilIdle()
        val added = caloriePresentation(controller.state.value.dashboard!!)
        assertTrue(added.progress!! > old.progress!!)
        assertEquals(52.5, old.remaining!! - added.remaining!!, 0.0001)
        controller.enter("diary")
        advanceUntilIdle()
        controller.write { food -= 52.5 }
        advanceUntilIdle()
        controller.enter("home")
        advanceUntilIdle()
        assertEquals(old.progress!!, caloriePresentation(controller.state.value.dashboard!!).progress!!, 0.00001f)
    }

    @Test fun browsedWeekDoesNotShiftAgainWhenSelectingAnotherDayInIt() {
        val anchor = weekBeginning(LocalDate.parse("2026-11-07"))
        val browsed = anchor.minusWeeks(2)
        assertEquals(browsed, weekBeginning(browsed.plusDays(4)))
        assertEquals(browsed, weekBeginning(browsed.plusDays(6)))
        assertEquals(LocalDate.parse("2026-12-28"), weekBeginning(LocalDate.parse("2027-01-01")))
    }
}
