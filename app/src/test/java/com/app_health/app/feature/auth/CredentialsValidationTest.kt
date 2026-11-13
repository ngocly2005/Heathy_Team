package com.app_health.app.feature.auth

import org.junit.Assert.*
import org.junit.Test

class CredentialsValidationTest {
    @Test fun registrationRequiresMatchingBoundedCredentials() {
        assertTrue(validRegistration(" user@example.com ", " Ngọc Lý ", " 12345678 ", " 12345678 "))
        assertFalse(validRegistration("user@example.com", " ", "12345678", "12345678"))
        assertFalse(validRegistration("user@example.com", "Name", "1234567", "1234567"))
        assertFalse(validRegistration("user@example.com", "Name", "x".repeat(129), "x".repeat(129)))
        assertFalse(validRegistration("user@example.com", "Name", "12345678 ", "12345678"))
    }

    @Test fun loginAllowsExistingPasswordWithoutRegistrationRulesOrTrimming() {
        assertTrue(validEmailLogin("user@example.com", " "))
        assertFalse(validEmailLogin("user@example.com", ""))
        assertFalse(validEmailLogin("invalid email", "12345678"))
        assertFalse(validEmailLogin("user@example.com", "x".repeat(129)))
    }

    @Test fun changingExistingPasswordRequiresProofAndConfirmation() {
        assertTrue(validPasswordUpdate(false, "", "12345678", "12345678"))
        assertFalse(validPasswordUpdate(true, "", "12345678", "12345678"))
        assertTrue(validPasswordUpdate(true, "old", "12345678", "12345678"))
        assertFalse(validPasswordUpdate(true, "old", "12345678", "87654321"))
    }
}
