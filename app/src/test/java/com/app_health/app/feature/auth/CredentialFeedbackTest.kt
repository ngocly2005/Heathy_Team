package com.app_health.app.feature.auth

import com.app_health.app.core.network.ApiFailure
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CredentialFeedbackTest {
    @Test fun screenshotInputsExplainBothEmailAndSevenCharacterPassword() {
        val errors = registrationErrors("thanhthien55200gmail.com", "VO THANH THIEN", "5500000", "5500000")
        assertFalse(errors.isValid)
        assertTrue(errors.email!!.contains("@"))
        assertTrue(errors.password!!.contains("hiện có 7"))
        assertNull(errors.name); assertNull(errors.confirmation)
        assertTrue(registrationErrors("thanhthien55200@gmail.com", "VO THANH THIEN", "55000000", "55000000").isValid)
    }

    @Test fun emailRejectsMalformedAddressesAndAcceptsCommonRealForms() {
        listOf("", "name", "a@@gmail.com", "a@", "@gmail.com", ".a@gmail.com", "a.@gmail.com", "a..b@gmail.com",
            "a b@gmail.com", "a\u200bb@gmail.com", "a@gma il.com", "a@-example.com", "a@example-.com", "a@example..com",
            "a@localhost", "a@example.com.", "a@123.456", "a@"+"x".repeat(64)+".com", "a".repeat(65)+"@gmail.com")
            .forEach { assertNotNull("Must reject: $it", accountEmailError(it)) }
        listOf(" Name+tag@Example.COM ", "o'connor@example.com", "a_b-c@example.co.uk", "ngọc@example.com", "a@bücher.de")
            .forEach { assertNull("Must accept: $it", accountEmailError(it)) }
    }

    @Test fun exactEmailBoundsAndUnicodeCharactersAreHandled() {
        val domain = listOf("b".repeat(63), "c".repeat(63), "d".repeat(61)).joinToString(".")
        assertNull(accountEmailError("a".repeat(64)+"@"+domain)) // total254
        assertNotNull(accountEmailError("a".repeat(64)+"@"+domain+"e"))
        assertEquals(8, credentialLength("😀".repeat(8)))
        assertNull(newPasswordError("😀".repeat(128)))
        assertNotNull(newPasswordError("😀".repeat(129)))
        assertNull(accountNameError("😀".repeat(120)))
        assertNotNull(accountNameError("😀".repeat(121)))
    }

    @Test fun nameAndNewPasswordBoundsAndControlCharactersAreExplained() {
        assertNotNull(accountNameError(" \u00a0 "))
        assertNull(accountNameError(" Ngọc Lý "))
        listOf("A\nB", "A\tB", "A\u0000B", "A\u007fB").forEach { assertNotNull(accountNameError(it)) }
        listOf("", "5500000", "x".repeat(129), " ".repeat(8), "\u00a0".repeat(8), "abcd\nefgh", "abcd\u0000efgh", "abcd\u0085efgh")
            .forEach { assertNotNull(newPasswordError(it)) }
        listOf("55000000", "x".repeat(128), " 12345678 ").forEach { assertNull(newPasswordError(it)) }
    }

    @Test fun confirmationTracksChangesAndMessagesNeverEchoThePassword() {
        val secret = "private-secret"
        assertNotNull(confirmationError(secret, ""))
        assertNotNull(confirmationError(secret, "private-secret "))
        assertNotNull(confirmationError(secret+"x", secret))
        assertNull(confirmationError(secret, secret))
        assertTrue(registrationErrors("", "", secret, "different").messages.none { it.contains(secret) })
    }

    @Test fun existingLoginDoesNotApplyNewPasswordRules() {
        listOf("x", " ".repeat(8), "a\nb").forEach { assertTrue(validEmailLogin("user@example.com", it)) }
        assertFalse(validPasswordUpdate(false, "", " ".repeat(8), " ".repeat(8)))
        assertFalse(validPasswordUpdate(true, "", "12345678", "12345678"))
    }

    @Test fun serverCredentialFailuresAreTranslatedWithoutLeakingInput() {
        val secret = "submitted-password"
        listOf(401,403,409,422).forEach { code -> assertFalse(credentialErrorMessage(ApiFailure(code, secret)).contains(secret)) }
        assertTrue(credentialErrorMessage(ApiFailure(409, secret)).contains("đã có tài khoản"))
        assertTrue(credentialErrorMessage(ApiFailure(401, secret)).contains("mật khẩu không đúng"))
        assertTrue(credentialErrorMessage(IOException()).contains("Không kết nối"))
    }
}
