package com.app_health.app.feature.auth

import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.toUserMessage
import retrofit2.HttpException

/** Credential-specific messages; never include submitted passwords or echo validation input. */
fun credentialErrorMessage(error: Exception): String {
    val status = when (error) { is HttpException -> error.code(); is ApiFailure -> error.statusCode; else -> null }
    return when (status) {
        401 -> "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại."
        403 -> "Tài khoản chưa được phép đăng nhập. Vui lòng liên hệ quản trị viên."
        409 -> "Email này đã có tài khoản. Hãy đăng nhập bằng phương thức đã đăng ký."
        422 -> "Thông tin chưa được máy chủ chấp nhận. Kiểm tra email, họ tên và quy tắc mật khẩu."
        else -> error.toUserMessage()
    }
}
