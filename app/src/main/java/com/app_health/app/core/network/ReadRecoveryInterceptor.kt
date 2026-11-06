package com.app_health.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

/** Prevents an outer GET recovery from repeating a failed refresh-token POST. */
class NonReplayableIOException(cause: IOException) : IOException(cause.message, cause)

/** Recover a broken/stale connection once for reads; never replay a mutation or an HTTP error. */
class ReadRecoveryInterceptor(
    private val diagnostic: (method: String, path: String, failureType: String) -> Unit = { _, _, _ -> }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" && request.method != "HEAD") return chain.proceed(request)
        try {
            return chain.proceed(request)
        } catch (failure: IOException) {
            if (chain.call().isCanceled()) throw failure
            diagnostic(request.method, request.url.encodedPath, failure.javaClass.simpleName)
            if (failure is NonReplayableIOException || failure is ApiFailure || failure is SSLException ||
                failure is ProtocolException || failure is InterruptedIOException && failure !is SocketTimeoutException) throw failure
            // OkHttp discards the failed exchange before this second proceed.
            return chain.proceed(request)
        }
    }
}
