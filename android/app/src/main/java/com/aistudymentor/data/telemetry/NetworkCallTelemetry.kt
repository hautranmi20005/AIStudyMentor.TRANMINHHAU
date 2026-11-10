package com.aistudymentor.data.telemetry

import com.aistudymentor.data.repository.ApiFailure
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Wrap Retrofit calls, reporting only transport/parser failures and server errors. */
suspend fun <T> monitorRetrofitCall(operation: String, request: suspend () -> T): T {
    try {
        return request()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        val isServerFailure = (exception as? ApiFailure)?.statusCode?.let { it >= 500 } == true
        if (exception is IOException || exception is JsonParseException || isServerFailure) {
            FirebaseTelemetry.recordNetworkOrParsingFailure(operation, exception)
        }
        throw exception
    }
}
