package com.app_health.app.core.network

import android.content.Context
import android.util.Log
import com.app_health.app.BuildConfig
import com.app_health.app.core.session.SessionStore
import com.app_health.app.feature.auth.AuthApi
import com.app_health.app.feature.auth.AuthRepository
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** One instance per process; no tokens, keys or bodies are logged. */
class AppContainer private constructor(context: Context) {
    val apiBaseUrl: String = BuildConfig.HEALTH_API_BASE_URL.trim().let { value ->
        val url = (value.trimEnd('/') + "/").toHttpUrl()
        require(url.username.isEmpty() && url.password.isEmpty()) { "API URL cannot contain credentials" }
        require(url.query == null && url.fragment == null && url.encodedPath == "/") {
            "Use the backend origin, for example http://10.0.2.2:8000/ (without api/v1)"
        }
        require(BuildConfig.DEBUG || url.isHttps) { "Release API must use HTTPS" }
        url.toString()
    }

    private val rawClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // A timed out POST may already have reached the server. Let the user reload first.
        .retryOnConnectionFailure(false)
        .addInterceptor(ReadRecoveryInterceptor { method, path, failure ->
            if (BuildConfig.DEBUG) Log.w("HealthRead", "$method $path: $failure")
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    // Explicit nulls are required when a feature clears nullable values with PUT.
    private val converter = GsonConverterFactory.create(GsonBuilder().serializeNulls().create())
    private val rawRetrofit = Retrofit.Builder().baseUrl(apiBaseUrl).client(rawClient)
        .addConverterFactory(converter).build()
    val sessionStore = SessionStore(context, apiBaseUrl)
    private val rawAuthApi = rawRetrofit.create(AuthApi::class.java)
    private val authenticatedClient = rawClient.newBuilder()
        .addInterceptor(BearerInterceptor(sessionStore, apiBaseUrl.toHttpUrl()))
        .authenticator(SessionAuthenticator(sessionStore, rawAuthApi, apiBaseUrl.toHttpUrl()))
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .client(authenticatedClient)
        .addConverterFactory(converter)
        .build()
    val healthApi: HealthApi = rawRetrofit.create(HealthApi::class.java)
    val authRepository = AuthRepository(context, sessionStore, rawAuthApi,
        retrofit.create(AuthApi::class.java), apiBaseUrl)

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer = instance ?: synchronized(this) {
            instance ?: AppContainer(context.applicationContext).also { instance = it }
        }
    }
}
