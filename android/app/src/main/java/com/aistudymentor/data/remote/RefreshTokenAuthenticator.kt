package com.aistudymentor.data.remote

import com.aistudymentor.data.security.SecureTokenStorage
import com.google.gson.JsonParser
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class RefreshTokenAuthenticator(
    private val baseUrl: String,
    private val tokenStorage: SecureTokenStorage
) : Authenticator {
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        synchronized(tokenStorage) {
            val currentAccessToken = tokenStorage.accessToken() ?: return null
            val failedAuthorization = response.request.header("Authorization")
            if (failedAuthorization != "Bearer $currentAccessToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStorage.refreshToken() ?: run {
                tokenStorage.clear()
                return null
            }
            val payload = """{"refreshToken":"${escapeJson(refreshToken)}"}"""
            val refreshRequest = Request.Builder()
                .url("${baseUrl}auth/refresh")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val refreshResponse = try {
                refreshClient.newCall(refreshRequest).execute()
            } catch (_: java.io.IOException) {
                return null
            }

            refreshResponse.use { result ->
                if (!result.isSuccessful) {
                    tokenStorage.clear()
                    return null
                }
                val json = result.body?.string()?.let(JsonParser::parseString)?.asJsonObject
                    ?: return null
                val accessToken = json.get("accessToken")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.takeIf(String::isNotBlank) ?: return null
                val rotatedRefreshToken = json.get("refreshToken")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.takeIf(String::isNotBlank) ?: return null
                val userId = json.get("userId")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.takeIf(String::isNotBlank) ?: tokenStorage.userId() ?: return null
                tokenStorage.saveSession(accessToken, rotatedRefreshToken, userId)
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
