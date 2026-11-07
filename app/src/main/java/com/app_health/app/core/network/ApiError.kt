package com.app_health.app.core.network

import com.google.gson.JsonParser
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class ApiFailure(val statusCode: Int, message: String) : IOException(message)

/** Handles FastAPI detail as a string or as a list of validation errors. */
fun apiErrorMessage(statusCode: Int, errorBody: String?): String {
    val detail = runCatching {
        val value = JsonParser.parseString(errorBody ?: "{}").asJsonObject.get("detail")
        when {
            value == null || value.isJsonNull -> null
            value.isJsonPrimitive -> value.asString
            value.isJsonArray -> value.asJsonArray.mapNotNull { entry ->
                val error = entry.takeIf { it.isJsonObject }?.asJsonObject
                error?.get("msg")?.takeIf { it.isJsonPrimitive }?.asString
            }.distinct().joinToString("\n").takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrNull()
    return detail?.takeIf { it.isNotBlank() } ?: when (statusCode) {
        401 -> "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."
        403 -> "Bạn không có quyền thực hiện thao tác này."
        404 -> "Không tìm thấy dữ liệu."
        409 -> "Dữ liệu đã thay đổi hoặc bị trùng. Vui lòng tải lại."
        422 -> "Thông tin gửi lên chưa hợp lệ."
        429 -> "Có quá nhiều yêu cầu. Vui lòng thử lại sau."
        in 500..599 -> "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."
        else -> "Không thể hoàn thành yêu cầu (HTTP $statusCode)."
    }
}

fun Throwable.toUserMessage(): String = when (this) {
    is CancellationException -> throw this
    is ApiFailure -> message ?: "Yêu cầu thất bại."
    is HttpException -> apiErrorMessage(code(), runCatching { response()?.errorBody()?.string() }.getOrNull())
    is JsonParseException, is MalformedJsonException -> "Dữ liệu máy chủ trả về chưa hợp lệ. Vui lòng thử tải lại."
    is IOException -> "Không kết nối được máy chủ. Kiểm tra mạng và địa chỉ backend rồi thử lại."
    else -> "Không thể xử lý dữ liệu. Vui lòng thử lại."
}
