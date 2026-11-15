package com.app_health.app.core.network

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import retrofit2.HttpException
import java.io.IOException
import javax.net.ssl.SSLException

/** GET-only caller helper. Writes must never be replayed after an uncertain response. */
internal suspend fun <T> retryTransientRead(checkCurrent: () -> Unit = {}, operation: suspend () -> T): T {
    var failures = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        checkCurrent()
        try {
            return operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            currentCoroutineContext().ensureActive()
            checkCurrent()
            val transient = when (failure) {
                is NonReplayableIOException, is ApiFailure, is SSLException -> false
                is HttpException -> failure.code() == 408 || failure.code() in 500..599
                is JsonParseException -> true
                is IOException -> true
                else -> false
            }
            if (!transient || failures >= 2) throw failure
            failures++
            delay(if (failures == 1) 300L else 900L)
        }
    }
}
