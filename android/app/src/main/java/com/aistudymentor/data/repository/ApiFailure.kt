package com.aistudymentor.data.repository

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException

class ApiFailure(
    val statusCode: Int?,
    val code: String?,
    override val message: String,
    val retryable: Boolean
) : Exception(message)

internal fun <T : Any> Response<T>.requireBody(): T {
    if (!isSuccessful) throw toApiFailure()
    return body() ?: throw ApiFailure(code(), null, "The server returned an empty response.", retryable = code() >= 500)
}

internal fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw toApiFailure()
}

internal fun Response<*>.toApiFailure(): ApiFailure {
    val payload = try {
        errorBody()?.string()?.let(JsonParser::parseString)?.asJsonObject
    } catch (_: Exception) {
        null
    }
    val status = code()
    val errorCode = payload?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
    val message = payload?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
        ?: when (status) {
            401 -> "Your session has expired. Please sign in again."
            403 -> "You do not have permission to perform this action."
            404 -> "The requested information was not found."
            429 -> "Too many requests. Please wait and try again."
            else -> "The request could not be completed."
        }
    return ApiFailure(
        statusCode = status,
        code = errorCode,
        message = message,
        retryable = status == 408 || status == 429 || status >= 500
    )
}

internal fun Throwable.isRetryableNetworkFailure(): Boolean =
    this is IOException || (this as? ApiFailure)?.retryable == true