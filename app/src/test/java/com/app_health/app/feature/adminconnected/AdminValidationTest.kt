package com.app_health.app.feature.adminconnected

import org.junit.Assert.*
import org.junit.Test

class AdminValidationTest {
    @Test fun catalogLimitsAndFractionalMetMatchBackend() {
        assertNull(catalogError(CatalogDraft(" Walk ", listOf("0,01")), true))
        assertEquals(0.01, catalogPayload(CatalogDraft(" Walk ", listOf("0,01")), true)["met"] as Double, 0.0)
        assertEquals("Walk", catalogPayload(CatalogDraft(" Walk ", listOf("0,01")), true)["name"])
        for (met in listOf("0", "30.01", "NaN", "Infinity")) assertNotNull(catalogError(CatalogDraft("Walk", listOf(met)), true))
        assertNull(catalogError(CatalogDraft("Food", listOf("20000", "2000", "3000", "2000")), false))
        assertNotNull(catalogError(CatalogDraft("Food", listOf("1", "2001", "3", "4")), false))
        assertNotNull(catalogError(CatalogDraft("", listOf("1", "2", "3", "4")), false))
    }
    @Test fun filtersPreserveUnicodeAndPaginationAndSeparateResourceFields() {
        val query = AdminQuery(tab = AdminTab.USERS, page = 2, search = "Ngọc Lý", status = "active", active = false)
        assertEquals("Ngọc Lý", query.parameters()["q"])
        assertEquals("2", query.parameters()["page"])
        assertEquals("20", query.parameters()["per_page"])
        assertEquals("active", query.parameters()["status"])
        assertFalse(query.parameters().containsKey("is_active"))
        val logs = AdminQuery(tab = AdminTab.LOGS, from = "2026-11-01", to = "2026-11-08", userId = "42", kind = "photo")
        assertEquals("42", logs.parameters()["user_id"])
        assertEquals("photo", logs.parameters()["kind"])
    }
    @Test fun invalidDateRangesAndUserIdsAreRejectedBeforeCalls() {
        assertNull(adminQueryError(AdminQuery(from = "2024-01-01", to = "2024-12-31")))
        assertNotNull(adminQueryError(AdminQuery(from = "2024-01-01", to = "2025-01-01")))
        assertNotNull(adminQueryError(AdminQuery(from = "2024-02-30", to = "2024-03-01")))
        assertNotNull(adminQueryError(AdminQuery(tab = AdminTab.LOGS, userId = "0")))
    }
    @Test fun userStatusEditorCannotDisableAdminOrSelf() {
        assertFalse(canChangeUserStatus(AdminUserRow(1,"a@x.test","A","admin","active"),2))
        assertFalse(canChangeUserStatus(AdminUserRow(2,"b@x.test","B","user","active"),2))
        assertTrue(canChangeUserStatus(AdminUserRow(3,"c@x.test","C","user","disabled"),2))
    }
}
