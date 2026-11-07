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
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class ReadRecoveryInterceptorTest {
    data class JsonCount(val value: Int)
    interface JsonApi {
        @GET("api/v1/count") fun count(): Call<JsonCount>
        @POST("api/v1/count") fun save(): Call<JsonCount>
    }
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

    private fun json(body: String = "{\"value\":7}") = MockResponse()
        .setHeader("Content-Type", "application/json; charset=utf-8").setBody(body)

    private fun api() = Retrofit.Builder().baseUrl(server.url("/")).client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(JsonApi::class.java)

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

    @Test fun retrofitGetRecoversWhenTheConnectionDropsDuringJsonBody() {
        server.enqueue(json().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        server.enqueue(json())
        assertEquals(7, api().count().execute().body()!!.value)
        assertEquals(2, server.requestCount)
    }

    @Test fun repeatedTruncatedJsonBodiesDoNotCauseAThirdAttempt() {
        server.enqueue(json().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        server.enqueue(json().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        server.enqueue(json())
        assertThrows(IOException::class.java) { api().count().execute() }
        assertEquals(2, server.requestCount)
    }

    @Test fun validJsonWithWrongStructureIsNotRetried() {
        server.enqueue(json("{\"value\":{\"not_an_integer\":7}}"))
        assertThrows(Exception::class.java) { api().count().execute() }
        assertEquals(1, server.requestCount)
    }

    @Test fun malformedJsonWithACompleteTransportIsNotRetried() {
        server.enqueue(json("{\"value\":"))
        assertThrows(Exception::class.java) { api().count().execute() }
        assertEquals(1, server.requestCount)
    }

    @Test fun truncatedMutationResponseIsNotRetried() {
        server.enqueue(json().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertThrows(IOException::class.java) { api().save().execute() }
        assertEquals(1, server.requestCount)
    }

    @Test fun errorStatusWithATruncatedJsonBodyIsNotRetried() {
        server.enqueue(json().setResponseCode(500).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertThrows(IOException::class.java) {
            client.newCall(Request.Builder().url(server.url("/api/v1/count")).build()).execute().use {
                assertEquals(500, it.code)
                it.body!!.string()
            }
        }
        assertEquals(1, server.requestCount)
    }

    @Test fun declaredLargeJsonBodyKeepsStreamingWithoutPrefetchingIt() {
        val builder = client.newBuilder()
        builder.interceptors().clear()
        val limited = builder.addInterceptor(ReadRecoveryInterceptor(maxJsonPrefetchBytes = 32)).build()
        server.enqueue(json("{\"value\":\"" + "x".repeat(64 * 1024) + "\"}"))
        limited.newCall(Request.Builder().url(server.url("/api/v1/count")).build()).execute().use {
            assertEquals(200, it.code)
            assertEquals(0L, it.body!!.source().buffer.size)
        }
        assertEquals(1, server.requestCount)
    }

    @Test fun unknownLengthJsonPrefetchIsBoundedAndDoesNotReadTheEntireBody() {
        val builder = client.newBuilder()
        builder.interceptors().clear()
        val limited = builder.addInterceptor(ReadRecoveryInterceptor(maxJsonPrefetchBytes = 32)).build()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setChunkedBody("{\"value\":\"" + "x".repeat(64 * 1024) + "\"}", 8192))
        limited.newCall(Request.Builder().url(server.url("/api/v1/count")).build()).execute().use {
            assertEquals(-1L, it.body!!.contentLength())
            assertTrue(it.body!!.source().buffer.size in 33L..(32L + 8192L))
        }
        assertEquals(1, server.requestCount)
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

    @Test fun storageFailureAfterSuccessfulRefreshCannotReplayTheGetOrRefreshPost() {
        var refreshes = 0
        val authenticated = client.newBuilder().authenticator { _, _ ->
            protectRefreshSideEffects {
                refreshes++
                val request = Request.Builder().url(server.url("/api/v1/auth/refresh"))
                    .post("{}".toRequestBody("application/json".toMediaType())).build()
                client.newCall(request).execute().use { assertEquals(200, it.code) }
                // Same production guard covers SessionStore.replaceTokens/persist throwing here.
                throw IOException("Unable to persist rotated tokens")
            }
        }.build()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("{}"))
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
