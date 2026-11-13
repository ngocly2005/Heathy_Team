package com.app_health.app.feature.auth

import com.app_health.app.core.network.SessionRequestTag
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthWireTest {
    @Test fun emailSessionAndNullableGoogleIdentitySurviveActualRetrofitExchange() = runBlocking {
        MockWebServer().use { server ->
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
                .build().create(AuthApi::class.java)
            server.enqueue(MockResponse().setBody("""{"access_token":"access","refresh_token":"refresh"}"""))
            val tokens = api.register(RegisterRequest("user@example.com", " exact password ", "Ngọc Lý"))
            assertEquals("access", tokens.accessToken)
            val registration = server.takeRequest()
            assertEquals("/api/v1/auth/register", registration.path)
            val body = JsonParser.parseString(registration.body.readUtf8()).asJsonObject
            assertEquals(" exact password ", body["password"].asString)
            assertEquals("Ngọc Lý", body["full_name"].asString)
            server.enqueue(MockResponse().setBody("""{"id":42,"email":"user@example.com","full_name":"Ngọc Lý","role":"admin","google_sub":null,"has_password":true}"""))
            val user = api.me("Bearer access")
            assertNull(user.googleSubject)
            assertTrue(user.hasPassword)
            assertEquals("admin", user.toSessionUser().role)
            assertEquals("Bearer access", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test fun firstPasswordIsNullableWhileNewPasswordKeepsSpaces() = runBlocking {
        MockWebServer().use { server ->
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
                .build().create(AuthApi::class.java)
            server.enqueue(MockResponse().setBody("""{"message":"Password updated"}"""))
            api.password(SessionRequestTag(42), PasswordUpdateRequest(null, " password "))
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertTrue(body["current_password"].isJsonNull)
            assertEquals(" password ", body["new_password"].asString)
        }
    }
}
