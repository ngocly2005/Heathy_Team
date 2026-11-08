package com.app_health.app.feature.adminconnected

import com.app_health.app.core.network.SessionRequestTag
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminControllerTest {
    private class Backend : AdminApi {
        var identity = AdminIdentity(1, "teacher@example.com", "Teacher", "admin", "active")
        var meFailure: Exception? = null
        val calls = mutableListOf<String>()
        var userLoader: suspend (Map<String, String>) -> AdminPage<AdminUserRow> = { AdminPage(emptyList(), 0) }
        override suspend fun me(owner: SessionRequestTag): AdminIdentity { calls += "me"; meFailure?.let { throw it }; return identity }
        override suspend fun analytics(owner: SessionRequestTag, from: String, to: String): AdminReport {
            calls += "analytics"; return AdminReport(from, to, mapOf("total_users" to 4L), emptyList())
        }
        override suspend fun users(owner: SessionRequestTag, query: Map<String, String>): AdminPage<AdminUserRow> { calls += "users:${query["q"]}"; return userLoader(query) }
        override suspend fun foods(owner: SessionRequestTag, query: Map<String, String>): AdminPage<AdminFood> { calls += "foods"; return AdminPage(emptyList(), 0) }
        override suspend fun activities(owner: SessionRequestTag, query: Map<String, String>): AdminPage<AdminActivity> = AdminPage(emptyList(), 0)
        override suspend fun logs(owner: SessionRequestTag, query: Map<String, String>): AdminPage<AdminAiLog> = AdminPage(emptyList(), 0)
        override suspend fun status(owner: SessionRequestTag, id: Long, value: Map<String, String>): AdminUserRow = error("Not used")
        override suspend fun createFood(owner: SessionRequestTag, value: Map<String, Any?>): AdminFood = error("Not used")
        override suspend fun editFood(owner: SessionRequestTag, id: Long, value: Map<String, Any?>): AdminFood = error("Not used")
        override suspend fun createActivity(owner: SessionRequestTag, value: Map<String, Any?>): AdminActivity = error("Not used")
        override suspend fun editActivity(owner: SessionRequestTag, id: Long, value: Map<String, Any?>): AdminActivity = error("Not used")
        override suspend fun deactivate(owner: SessionRequestTag, resource: String, id: Long) = error("Not used")
    }

    @Test fun liveUserRoleCannotBeOverriddenByCachedAdminUi() = runTest {
        val api = Backend().apply { identity = identity.copy(role = "user") }
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        assertTrue(controller.state.value.denied)
        assertFalse(controller.state.value.authorized)
        assertEquals(listOf("me"), api.calls)
        var writes = 0
        controller.write({ writes++ }); advanceUntilIdle()
        assertEquals(0, writes)
    }

    @Test fun demotionRemovesAlreadyLoadedPrivilegedData() = runTest {
        val api = Backend()
        val identities = mutableListOf<String>()
        val controller = AdminController(this, api, { 1L }, { identities += it.role })
        controller.activate(); advanceUntilIdle()
        assertNotNull(controller.state.value.report)
        api.identity = api.identity.copy(role = "user")
        controller.refresh(); advanceUntilIdle()
        assertTrue(controller.state.value.denied)
        assertNull(controller.state.value.report)
        assertTrue(controller.state.value.users.isEmpty())
        assertEquals(listOf("admin", "user"), identities)
        assertEquals(1, api.calls.count { it == "analytics" })
    }

    @Test fun revocationClearsDataEvenWhenIdentityPersistenceFails() = runTest {
        val api = Backend()
        val controller = AdminController(this, api, { 1L }, { identity ->
            if (identity.role != "admin") throw IOException("Keystore persistence failed")
        })
        controller.activate(); advanceUntilIdle()
        assertNotNull(controller.state.value.report)
        api.identity = api.identity.copy(role = "user")
        controller.refresh(); advanceUntilIdle()
        assertTrue(controller.state.value.denied)
        assertFalse(controller.state.value.authorized)
        assertNull(controller.state.value.report)
        var writes = 0
        controller.write({ writes++ }); advanceUntilIdle()
        assertEquals(0, writes)
    }

    @Test fun disabledOrMismatchingIdentityDoesNotLoadAdminData() = runTest {
        for (identity in listOf(AdminIdentity(1,"a@x.test","A","admin","disabled"), AdminIdentity(2,"b@x.test","B","admin","active"))) {
            val api = Backend().apply { this.identity = identity }
            val controller = AdminController(this, api, { 1L })
            controller.activate(); advanceUntilIdle()
            assertTrue(controller.state.value.denied)
            assertNull(controller.state.value.report)
        }
    }

    @Test fun lateSearchResultsCannotReplaceANewerFilter() = runTest {
        val api = Backend()
        val old = CompletableDeferred<AdminPage<AdminUserRow>>()
        api.userLoader = { query -> if (query["q"] == "old") withContext(NonCancellable) { old.await() }
            else AdminPage(listOf(AdminUserRow(2,"new@x.test","New","user","active")),1) }
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        controller.select(AdminQuery(tab = AdminTab.USERS, search = "old")); runCurrent()
        controller.select(AdminQuery(tab = AdminTab.USERS, search = "new")); runCurrent()
        old.complete(AdminPage(listOf(AdminUserRow(3,"old@x.test","Old","user","active")),1))
        advanceUntilIdle()
        assertEquals("new", controller.state.value.query.search)
        assertEquals("New", controller.state.value.users.single().full_name)
    }

    @Test fun unknownMutationIsNotReplayedAndMustReconcileBeforeNextWrite() = runTest {
        val api = Backend()
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        controller.select(AdminQuery(tab = AdminTab.FOODS)); advanceUntilIdle()
        var writes = 0
        controller.write({ writes++; throw IOException("Reply lost after commit") }); advanceUntilIdle()
        assertTrue(controller.state.value.uncertain)
        assertFalse(controller.state.value.busy)
        controller.write({ writes++ })
        controller.select(AdminQuery(tab = AdminTab.USERS))
        advanceUntilIdle()
        assertEquals(AdminTab.FOODS, controller.state.value.query.tab)
        assertEquals(1, writes)
        controller.refresh(); advanceUntilIdle()
        assertFalse(controller.state.value.uncertain)
        assertEquals(2, api.calls.count { it == "foods" })
        assertEquals(1, writes)
    }

    @Test fun malformedMutationReplyAlsoRequiresReconciliation() = runTest {
        val api = Backend()
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        controller.write({ throw JsonSyntaxException("Expected object") }); advanceUntilIdle()
        assertTrue(controller.state.value.uncertain)
    }

    @Test fun preflightConnectionFailureDoesNotClaimAnUncertainMutation() = runTest {
        val api = Backend()
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        api.meFailure = IOException("Offline before sending mutation")
        var writes = 0
        controller.write({ writes++ }); advanceUntilIdle()
        assertEquals(0, writes)
        assertFalse(controller.state.value.uncertain)
        assertNotNull(controller.state.value.error)
    }

    @Test fun forbiddenMutationClearsDataAndReleaseClearsAllCachedState() = runTest {
        val api = Backend()
        val controller = AdminController(this, api, { 1L })
        controller.activate(); advanceUntilIdle()
        controller.write({ throw HttpException(Response.error<Unit>(403, okhttp3.ResponseBody.create(null, "Forbidden"))) })
        advanceUntilIdle()
        assertTrue(controller.state.value.denied)
        assertNull(controller.state.value.report)
        controller.release()
        assertFalse(controller.state.value.authorized)
        assertFalse(controller.state.value.denied)
        controller.activate(); advanceUntilIdle()
        assertNotNull(controller.state.value.report)
    }

    @Test fun changedAccountCannotReceiveResultsFromOldRequest() = runTest {
        val api = Backend()
        var owner: Long? = 1
        val pending = CompletableDeferred<AdminPage<AdminUserRow>>()
        api.userLoader = { withContext(NonCancellable) { pending.await() } }
        val controller = AdminController(this, api, { owner })
        controller.activate(); advanceUntilIdle()
        controller.select(AdminQuery(tab = AdminTab.USERS)); runCurrent()
        owner = 2
        api.identity = api.identity.copy(id = 2, email = "new@x.test")
        controller.activate(); runCurrent()
        pending.complete(AdminPage(listOf(AdminUserRow(3,"private@x.test","Old account data","user","active")),1))
        advanceUntilIdle()
        assertEquals(2L, controller.state.value.identity!!.id)
        assertTrue(controller.state.value.users.isEmpty())
        assertEquals(AdminTab.DASHBOARD, controller.state.value.query.tab)
    }
}
