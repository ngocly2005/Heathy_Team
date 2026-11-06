package com.app_health.app.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class ReadRecoveryInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().retryOnConnectionFailure(false)
            .readTimeout(2, TimeUnit.SECONDS).addInterceptor(ReadRecoveryInterceptor()).build()
    }

    @After fun cleanup() {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
        server.shutdown()
    }

    private fun disconnect() = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)

    @Test fun getRecoversOnceWhenTheFirstConnectionDrops() {
        server.enqueue(disconnect())
        server.enqueue(MockResponse().setBody("saved meals"))
        client.newCall(Request.Builder().url(server.url("/api/v1/saved-meals")).build()).execute().use {
            assertEquals(200, it.code)
            assertEquals("saved meals", it.body!!.string())
        }
        assertEquals(2, server.requestCount)
    }

    @Test fun secondReadFailureIsSurfacedInsteadOfRetryingForever() {
        server.enqueue(disconnect())
        server.enqueue(disconnect())
        server.enqueue(MockResponse().setBody("unexpected third attempt"))
        assertThrows(IOException::class.java) {
            client.newCall(Request.Builder().url(server.url("/api/v1/saved-meals")).build()).execute().close()
        }
        assertEquals(2, server.requestCount)
    }

    @Test fun mutationsAreNeverReplayedAfterTheServerReceivesTheRequest() {
        for (method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            val before = server.requestCount
            server.enqueue(disconnect())
            val request = Request.Builder().url(server.url("/api/v1/food-logs"))
                .method(method, "{}".toRequestBody("application/json".toMediaType())).build()
            assertThrows(IOException::class.java) { client.newCall(request).execute().close() }
            assertEquals(before + 1, server.requestCount)
        }
    }

    @Test fun httpErrorsAreReturnedWithoutTransportRetries() {
        server.enqueue(MockResponse().setResponseCode(401))
        client.newCall(Request.Builder().url(server.url("/api/v1/profile")).build()).execute().use {
            assertEquals(401, it.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test fun getRecoveryCannotIndirectlyRepeatARefreshTokenPost() {
        var refreshes = 0
        val authenticated = client.newBuilder().authenticator { _, _ ->
            refreshes++
            val request = Request.Builder().url(server.url("/api/v1/auth/refresh"))
                .post("{}".toRequestBody("application/json".toMediaType())).build()
            try { client.newCall(request).execute().close() }
            catch (error: IOException) { throw NonReplayableIOException(error) }
            null
        }.build()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(disconnect())
        assertThrows(NonReplayableIOException::class.java) {
            authenticated.newCall(Request.Builder().url(server.url("/api/v1/profile")).build()).execute().close()
        }
        assertEquals(1, refreshes)
        assertEquals(2, server.requestCount)
    }

    @Test fun certificateFailuresAreNotRetried() {
        val authenticated = client.newBuilder().authenticator { _, _ -> throw SSLHandshakeException("Invalid certificate") }.build()
        server.enqueue(MockResponse().setResponseCode(401))
        assertThrows(SSLHandshakeException::class.java) {
            authenticated.newCall(Request.Builder().url(server.url("/api/v1/profile")).build()).execute().close()
        }
        assertEquals(1, server.requestCount)
    }
}
