package com.app_health.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

/** Prevents an outer GET recovery from repeating a failed refresh-token POST. */
class NonReplayableIOException(cause: IOException) : IOException(cause.message, cause)

private class IncompleteJsonBodyIOException(cause: IOException) : IOException(cause.message, cause)

/** Recover one transport failure, including small JSON bodies, without parsing or replaying writes. */
class ReadRecoveryInterceptor(
    private val maxJsonPrefetchBytes: Long = 1024L * 1024L,
    private val diagnostic: (method: String, path: String, failureType: String) -> Unit = { _, _, _ -> }
) : Interceptor {
    init { require(maxJsonPrefetchBytes in 1L..(1024L * 1024L)) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" && request.method != "HEAD") return chain.proceed(request)
        try {
            return readResponse(chain)
        } catch (failure: IOException) {
            if (chain.call().isCanceled()) throw failure
            diagnostic(request.method, request.url.encodedPath, failure.javaClass.simpleName)
            if (failure is NonReplayableIOException || failure is ApiFailure || failure is SSLException ||
                failure is ProtocolException || failure is InterruptedIOException && failure !is SocketTimeoutException) throw failure
            // readResponse closes a failed body before this final recovery attempt.
            return readResponse(chain)
        }
    }

    private fun readResponse(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        try {
            val body = response.body ?: return response
            val subtype = body.contentType()?.subtype
            if (chain.request().method != "GET" || !response.isSuccessful ||
                subtype == null || subtype != "json" && !subtype.endsWith("+json")) return response
            val length = body.contentLength()
            // Large responses keep streaming. Unknown-length bodies prefetch only a bounded prefix.
            // Okio may additionally fill one segment; no copy of the body is allocated here.
            if (length > maxJsonPrefetchBytes) return response
            val source = body.source()
            val required = if (length >= 0) length else maxJsonPrefetchBytes + 1
            try {
                val available = source.request(required)
                if (!available && length >= 0 && source.buffer.size < length) {
                    throw EOFException("Incomplete JSON response body")
                }
            } catch (failure: ProtocolException) {
                // OkHttp reports a prematurely closed fixed-length body as ProtocolException.
                // Wrap only this bounded body-transfer case, not arbitrary HTTP protocol errors.
                if (length >= 0 && source.buffer.size < length) throw IncompleteJsonBodyIOException(failure)
                throw failure
            }
            return response
        } catch (failure: Throwable) {
            runCatching { response.close() }
            throw failure
        }
    }
}
