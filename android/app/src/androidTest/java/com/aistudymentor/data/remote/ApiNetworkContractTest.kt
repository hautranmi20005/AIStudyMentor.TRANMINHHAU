package com.aistudymentor.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudymentor.data.security.SecureTokenStorage
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class ApiNetworkContractTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun retrofitSerializesQuestionRequestAndParsesResponse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"questionId":"question-1","subject":"Physics","questionText":"Explain gravity","status":"ANSWERED","isBookmarked":false,"imageStoragePaths":[],"answer":null,"createdAt":null}"""
            )
        )
        val service = retrofit().create(ApiService::class.java)

        val response = service.submitQuestion(
            "Bearer access-token",
            QuestionCreateRequestDto("Physics", "Explain gravity")
        )
        val request = server.takeRequest()
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject

        assertTrue(response.isSuccessful)
        assertEquals("question-1", response.body()?.questionId)
        assertEquals("Bearer access-token", request.getHeader("Authorization"))
        assertEquals("Physics", body.get("subject").asString)
        assertEquals("Explain gravity", body.get("questionText").asString)
    }

    @Test
    fun authenticatorRefreshesTokensAndRetriesUnauthorizedRequest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStorage = SecureTokenStorage(context)
        tokenStorage.clear()
        tokenStorage.saveSession("old-access-token", "old-refresh-token", "student-id")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/v1/auth/refresh" -> MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """{"accessToken":"new-access-token","refreshToken":"new-refresh-token","userId":"student-id"}"""
                    )
                request.getHeader("Authorization") == "Bearer old-access-token" ->
                    MockResponse().setResponseCode(401)
                request.getHeader("Authorization") == "Bearer new-access-token" ->
                    MockResponse().setResponseCode(200).setBody("""{"content":[],"last":true,"totalPages":0}""")
                else -> MockResponse().setResponseCode(401)
            }
        }
        val baseUrl = server.url("/api/v1/").toString()
        val client = OkHttpClient.Builder()
            .authenticator(RefreshTokenAuthenticator(baseUrl, tokenStorage))
            .build()
        val service = retrofit(client).create(ApiService::class.java)

        val response = service.listQuestions("Bearer old-access-token")
        val requests = List(3) { server.takeRequest() }

        assertTrue(response.isSuccessful)
        assertEquals("/api/v1/questions?page=0&size=100", requests[0].path)
        assertEquals("/api/v1/auth/refresh", requests[1].path)
        assertEquals("/api/v1/questions?page=0&size=100", requests[2].path)
        assertEquals("new-access-token", tokenStorage.accessToken())
        assertEquals("new-refresh-token", tokenStorage.refreshToken())
    }

    private fun retrofit(client: OkHttpClient = OkHttpClient()): Retrofit =
        Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
