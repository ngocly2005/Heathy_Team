package com.app_health.app.feature.integrated

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiScreensHelpersTest {
    @Test fun openingAnEditorDoesNotRoundOrInvalidateTheSavedPortion() {
        // Display-only one-decimal rounding used to turn the valid minimum into 0.0 g.
        for (grams in listOf(0.01, 0.05, 60.25, 99.99, 10000.0)) {
            val edited = inputNumber(uiEditNumber(grams))!!
            assertEquals(grams, edited, 0.00000001)
            assertTrue(edited >= 0.01 && edited <= 10000)
        }
        assertEquals("0.01", uiEditNumber(0.01))
    }
}
