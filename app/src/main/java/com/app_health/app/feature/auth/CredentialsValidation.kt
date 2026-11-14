package com.app_health.app.feature.auth

import java.net.IDN

// Count Unicode characters as the BE does, rather than UTF-16 code units.
fun credentialLength(value: String): Int = value.codePointCount(0, value.length)
private fun String.hasControls() = any { Character.isISOControl(it) }

fun accountEmailError(email: String): String? {
    val value = email.trim()
    if (value.isEmpty()) return "Vui lòng nhập email."
    if (credentialLength(value) > 254) return "Email tối đa 254 ký tự."
    val parts = value.split('@')
    if (parts.size != 2 || parts.any { it.isEmpty() }) return "Email cần đúng dạng ten@gmail.com (có dấu @)."
    val (local, domain) = parts
    val forbidden = "()<>[]:;\\,\""
    if (credentialLength(local) > 64 || local.startsWith('.') || local.endsWith('.') || ".." in local ||
        local.any { it.isWhitespace() || Character.isSpaceChar(it) || Character.isISOControl(it) || it in forbidden ||
            Character.getType(it) == Character.FORMAT.toInt() }) return "Phần email trước dấu @ chưa hợp lệ."
    val ascii = runCatching { IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES) }.getOrNull()
        ?: return "Tên miền email chưa hợp lệ, ví dụ gmail.com."
    val labels = ascii.split('.')
    if (ascii.length > 253 || labels.size < 2 || labels.any { it.length !in 1..63 || it.startsWith('-') || it.endsWith('-') ||
            !it.all { ch -> ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-' } } ||
        labels.last().all { it.isDigit() }) return "Tên miền email chưa hợp lệ, ví dụ gmail.com."
    return null
}

fun accountNameError(name: String): String? = when {
    name.trim().isEmpty() -> "Vui lòng nhập họ và tên."
    credentialLength(name.trim()) > 120 -> "Họ và tên tối đa 120 ký tự."
    name.hasControls() -> "Họ và tên không được chứa ký tự xuống dòng hoặc điều khiển."
    else -> null
}

fun newPasswordError(password: String): String? = when {
    password.isEmpty() -> "Vui lòng nhập mật khẩu mới."
    credentialLength(password) < 8 -> "Mật khẩu cần ít nhất 8 ký tự (hiện có ${credentialLength(password)})."
    credentialLength(password) > 128 -> "Mật khẩu tối đa 128 ký tự."
    password.isBlank() -> "Mật khẩu không được chỉ gồm khoảng trắng."
    password.hasControls() -> "Mật khẩu không được chứa ký tự xuống dòng hoặc điều khiển."
    else -> null
}

fun loginPasswordError(password: String): String? = when {
    password.isEmpty() -> "Vui lòng nhập mật khẩu."
    credentialLength(password) > 128 -> "Mật khẩu tối đa 128 ký tự."
    else -> null // Do not apply new-password rules to existing credentials or trim passwords.
}

fun confirmationError(password: String, confirmation: String): String? = when {
    confirmation.isEmpty() -> "Vui lòng nhập lại mật khẩu để xác nhận."
    password != confirmation -> "Xác nhận mật khẩu chưa khớp."
    else -> null
}

data class CredentialErrors(val name: String? = null, val email: String? = null,
    val password: String? = null, val confirmation: String? = null) {
    val messages: List<String> get() = listOfNotNull(name, email, password, confirmation)
    val isValid: Boolean get() = messages.isEmpty()
}

fun registrationErrors(email: String, name: String, password: String, confirmation: String) = CredentialErrors(
    accountNameError(name), accountEmailError(email), newPasswordError(password), confirmationError(password, confirmation))
fun emailLoginErrors(email: String, password: String) = CredentialErrors(email = accountEmailError(email), password = loginPasswordError(password))
fun validAccountEmail(email: String) = accountEmailError(email) == null
fun validEmailLogin(email: String, password: String) = emailLoginErrors(email, password).isValid
fun validRegistration(email: String, name: String, password: String, confirmation: String) = registrationErrors(email, name, password, confirmation).isValid
fun validPasswordUpdate(hasPassword: Boolean, current: String, next: String, confirmation: String) =
    (!hasPassword || loginPasswordError(current) == null) && newPasswordError(next) == null && confirmationError(next, confirmation) == null
