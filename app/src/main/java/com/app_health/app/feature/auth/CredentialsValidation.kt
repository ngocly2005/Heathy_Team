package com.app_health.app.feature.auth

fun validAccountEmail(email: String): Boolean = email.trim().length <= 320 &&
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())

fun validEmailLogin(email: String, password: String): Boolean = validAccountEmail(email) && password.length in 1..128

fun validRegistration(email: String, name: String, password: String, confirmation: String): Boolean =
    validAccountEmail(email) && name.trim().length in 1..120 && password.length in 8..128 && password == confirmation

fun validPasswordUpdate(hasPassword: Boolean, current: String, next: String, confirmation: String): Boolean =
    (!hasPassword || current.length in 1..128) && next.length in 8..128 && next == confirmation
