package com.app_health.app.feature.integrated

import com.app_health.app.core.network.ApiFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppDataControllerTest {
    private class Backend(scope: CoroutineScope) {
        var user: Long? = 1
        var calories = 0.0
        val calls = mutableListOf<Pair<String, RequestContext>>()
        lateinit var data: AppDataController
        var onRead: suspend (String, RequestContext) -> Unit = { route, ctx -> respond(route, ctx) }

        init {
            data = AppDataController(scope, { user }) { route, ctx ->
                calls += route to ctx
                onRead(route, ctx)
            }
            data.activate()
        }

        fun respond(route: String, ctx: RequestContext) {
            if (route == "water") data.publish(ctx) {
                it.copy(water = WaterSettingsDto(null, 200, 2500))
            }
            if (route == "meals") data.publish(ctx) {
                it.copy(meals = listOf(SavedMealDto(1, "Bữa đã lưu", emptyList())))
            }
            if (route == "home") data.publish(ctx) {
                it.copy(dashboard = DashboardDto(ctx.date, calories, 0.0, 2000.0, 2000.0 - calories,
                    calories, 0, MacroDto(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), emptyList(), emptyList()))
            }
        }
    }

    @Test fun navigatingLoadsOnlyNeededDataAndDoesNotCarryErrorsToOtherPages() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("home")
        advanceUntilIdle()
        assertEquals(listOf("profile", "home", "water"), backend.calls.map { it.first })
        backend.onRead = { route, ctx ->
            if (route == "meals") throw IOException("Disconnected")
            backend.respond(route, ctx)
        }
        data.enter("meals")
        advanceUntilIdle()
        assertNotNull(data.state.value.error)
        data.enter("profile")
        advanceUntilIdle()
        assertNull(data.state.value.error)
        assertFalse(data.state.value.loading)
        // Returning to the failed page retries it automatically, with no Reload click.
        backend.onRead = backend::respond
        data.enter("meals")
        advanceUntilIdle()
        assertNull(data.state.value.error)
        assertEquals("Bữa đã lưu", data.state.value.meals.single().name)
        assertEquals(2, backend.calls.count { it.first == "meals" })
        assertFalse(backend.calls.any { it.first in listOf("chat", "weight", "activity") })
        val count = backend.calls.size
        data.enter("home")
        advanceUntilIdle()
        assertEquals(count, backend.calls.size)
    }

    @Test fun cancelledRequestCannotPublishDataOrConnectionErrorsAfterNavigation() = runTest {
        val backend = Backend(this)
        val data = backend.data
        backend.onRead = { route, ctx ->
            if (route == "weight") {
                try { awaitCancellation() } catch (_: CancellationException) {
                    data.publish(ctx) { it.copy(weights = listOf(WeightDto(1, ctx.date, 70.0))) }
                    throw IOException("Canceled socket")
                }
            }
            backend.respond(route, ctx)
        }
        data.enter("weight")
        runCurrent()
        assertTrue(data.state.value.loading)
        data.enter("profile")
        advanceUntilIdle()
        assertEquals("profile", data.state.value.route)
        assertNull(data.state.value.error)
        assertTrue(data.state.value.weights.isEmpty())
        assertFalse(data.state.value.loading)
    }

    @Test fun oldScreenResponsesAreRejectedEvenAfterReturningToTheSameScreen() = runTest {
        val data = Backend(this).data
        data.enter("food")
        val old = data.context()
        data.enter("profile")
        advanceUntilIdle()
        data.enter("food")
        data.showError("Old failed search", old)
        data.publish(old) { it.copy(photoConfirmed = true) }
        assertNull(data.state.value.error)
        assertFalse(data.state.value.photoConfirmed)
        val revision = data.state.value.readRevision
        data.refresh()
        advanceUntilIdle()
        // Parameterized search/calendar reads are retriggered even when no shared API is needed.
        assertTrue(data.state.value.readRevision > revision)
    }

    @Test fun dateAndAccountChangesInvalidateCachedDataAndPendingResponses() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("meals")
        advanceUntilIdle()
        val old = data.context()
        val yesterday = LocalDate.now().minusDays(1)
        data.chooseDate(yesterday)
        advanceUntilIdle()
        assertEquals(yesterday, data.state.value.date)
        assertEquals(yesterday.toString(), backend.calls.last().second.date)
        assertEquals(2, backend.calls.count { it.first == "home" })
        data.showError("Wrong day", old)
        assertNull(data.state.value.error)
        val previousUser = data.context()
        backend.user = 2
        data.activate()
        data.publish(previousUser) { it.copy(error = "Previous user", meals = listOf(SavedMealDto(7, "Private", emptyList()))) }
        assertNull(data.state.value.error)
        assertTrue(data.state.value.meals.isEmpty())
        data.enter("profile")
        advanceUntilIdle()
        assertEquals(2L, backend.calls.last().second.owner.userId)
    }

    @Test fun successfulSaveCanNavigateAndRefreshTotalsWithoutReplayingTheWrite() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("food")
        var writes = 0
        data.write(success = { data.enter("home") }) {
            writes++
            backend.calories = 350.0
        }
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals("home", data.state.value.route)
        assertEquals(350.0, data.state.value.dashboard!!.calories_consumed, 0.001)
        assertFalse(data.state.value.busy)
        assertFalse(data.state.value.loading)
        assertNull(data.state.value.error)
        data.enter("meals")
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(1, backend.calls.count { it.first == "home" })
    }

    @Test fun uncertainWriteStaysBlockedAcrossNavigationUntilReadReconciliationSucceeds() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("food")
        var writes = 0
        data.write { writes++; throw IOException("Response lost after sending") }
        advanceUntilIdle()
        assertTrue(data.state.value.uncertain)
        data.enter("profile")
        data.write { writes++ }
        advanceUntilIdle()
        assertEquals(1, writes)
        assertNotNull(data.state.value.error)
        backend.onRead = { _, _ -> throw IOException("Still offline") }
        data.refresh()
        advanceUntilIdle()
        assertTrue(data.state.value.uncertain)
        backend.onRead = backend::respond
        data.refresh()
        advanceUntilIdle()
        assertTrue(backend.calls.any { it.first == "diary" })
        assertFalse(data.state.value.uncertain)
        assertNull(data.state.value.error)
        assertEquals(1, writes)
    }

    @Test fun refreshFailureAfterSuccessfulWriteDoesNotReportSaveFailureOrResubmit() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("profile")
        advanceUntilIdle()
        var writes = 0
        backend.onRead = { _, _ -> throw IOException("Connection lost while reloading") }
        data.write { writes++ }
        advanceUntilIdle()
        assertEquals(1, writes)
        assertTrue(data.state.value.error!!.startsWith("Đã lưu."))
        assertFalse(data.state.value.uncertain)
        assertFalse(data.state.value.busy)
        backend.onRead = backend::respond
        data.refresh()
        advanceUntilIdle()
        assertEquals(1, writes)
        assertNull(data.state.value.error)
    }

    @Test fun successfulReadClearsItsOldFailureWithoutRequiringManualReload() = runTest {
        val data = Backend(this).data
        data.enter("food")
        val ctx = data.context()
        data.showError("A previous search failed", ctx)
        data.readSucceeded(ctx) { it.copy(foods = listOf(FoodDto(1, "Apple", 52.0, 0.3, 14.0, 0.2))) }
        assertNull(data.state.value.error)
        assertEquals("Apple", data.state.value.foods.single().name)
    }

    @Test fun staleReadSuccessCannotClearANewerScreenFailure() = runTest {
        val data = Backend(this).data
        data.enter("food")
        val old = data.context()
        data.enter("profile")
        advanceUntilIdle()
        data.enter("food")
        val current = data.context()
        data.showError("Current failed search", current)
        data.readSucceeded(old) { it.copy(foods = listOf(FoodDto(1, "Wrong result", 1.0, 0.0, 0.0, 0.0))) }
        assertEquals("Current failed search", data.state.value.error)
        assertTrue(data.state.value.foods.isEmpty())
        data.readSucceeded(current)
        assertNull(data.state.value.error)
    }

    @Test fun readSuccessDoesNotHideAValidationErrorOrAnUncertainWrite() = runTest {
        val data = Backend(this).data
        data.enter("food")
        data.write { throw ApiFailure(422, "Invalid portion") }
        advanceUntilIdle()
        data.readSucceeded(data.context())
        assertEquals("Invalid portion", data.state.value.error)
        data.write { throw IOException("The response was lost") }
        advanceUntilIdle()
        val warning = data.state.value.error
        data.readSucceeded(data.context())
        data.showError("Background search failed", data.context())
        assertTrue(data.state.value.uncertain)
        assertEquals(warning, data.state.value.error)
    }

    @Test fun malformedWriteResponseRequiresReconciliationBeforeAnotherWrite() = runTest {
        val backend = Backend(this)
        val data = backend.data
        data.enter("food")
        var writes = 0
        data.write {
            writes++
            // The server may have committed even when its response cannot be decoded.
            throw com.google.gson.JsonSyntaxException("Expected object but got array")
        }
        advanceUntilIdle()
        assertTrue(data.state.value.uncertain)
        assertFalse(data.state.value.busy)
        val warning = data.state.value.error
        data.readSucceeded(data.context())
        assertEquals(warning, data.state.value.error)
        data.write { writes++ }
        advanceUntilIdle()
        assertEquals(1, writes)
        data.refresh()
        advanceUntilIdle()
        assertTrue(backend.calls.any { it.first == "diary" })
        assertFalse(data.state.value.uncertain)
        assertNull(data.state.value.error)
        assertEquals(1, writes)
    }

    @Test fun diaryLoadsWaterTargetAlongsideTheSelectedDay() = runTest {
        val backend = Backend(this)
        backend.data.enter("diary")
        advanceUntilIdle()
        assertEquals(listOf("diary", "water"), backend.calls.map { it.first })
        assertEquals(2500, backend.data.state.value.water!!.effective_target_ml)
    }
}
