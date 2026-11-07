package com.app_health.app.core.network

import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class ApiErrorMessageTest {
    @Test fun invalidServerJsonIsNotReportedAsAnUnreachableBackend() {
        for (failure in listOf(JsonSyntaxException("wrong schema"), MalformedJsonException("invalid JSON"))) {
            assertTrue(failure.toUserMessage().contains("Dữ liệu máy chủ"))
            assertFalse(failure.toUserMessage().contains("Kiểm tra mạng"))
        }
    }

    @Test fun transportAndServerValidationErrorsRemainDistinct() {
        assertTrue(SocketTimeoutException().toUserMessage().contains("Kiểm tra mạng"))
        assertEquals("Thông tin ngày chưa đúng", ApiFailure(422, "Thông tin ngày chưa đúng").toUserMessage())
    }
}
