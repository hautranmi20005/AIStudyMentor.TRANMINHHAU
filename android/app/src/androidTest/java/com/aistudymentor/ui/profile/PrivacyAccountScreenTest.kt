package com.aistudymentor.ui.profile

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudymentor.data.remote.AccountReauthenticationRequestDto
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.repository.AuthRepository
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.ui.auth.AuthViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PrivacyAccountScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var tokenStorage: SecureTokenStorage

    @Before
    fun setUp() {
        tokenStorage = SecureTokenStorage(ApplicationProvider.getApplicationContext<Context>())
        tokenStorage.saveSession("account-access", "account-refresh", "student-id")
    }

    @After
    fun tearDown() {
        tokenStorage.clear()
    }

    @Test
    fun deleteRequiresTypedConfirmationAndReauthentication() {
        val deletedRequest = AtomicReference<AccountReauthenticationRequestDto?>()
        val authorization = AtomicReference<String?>()
        val api = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            if (method.name != "deleteAccount") error("Unexpected API call: ${method.name}")
            authorization.set(args[0] as String)
            deletedRequest.set(args[1] as AccountReauthenticationRequestDto)
            Response.success<Void>(204, null)
        } as ApiService
        val viewModel = AuthViewModel(AuthRepository(api, tokenStorage))
        var deleted = false
        composeRule.setContent {
            MaterialTheme {
                PrivacyAccountScreen(viewModel, {}, { deleted = true }, {})
            }
        }

        composeRule.onNodeWithTag("privacy-delete-button").assertIsNotEnabled()
        composeRule.onNodeWithTag("privacy-password").performTextInput("Strong-password123")
        composeRule.onNodeWithTag("privacy-mfa-code").performTextInput("123456")
        composeRule.onNodeWithTag("privacy-delete-confirmation").performTextInput("DELETE MY ACCOUNT")
        composeRule.onNodeWithTag("privacy-delete-button").performClick()
        composeRule.waitUntil(5_000) { deletedRequest.get() != null }
        composeRule.waitUntil(5_000) { deleted }

        assertEquals("Bearer account-access", authorization.get())
        assertEquals("Strong-password123", deletedRequest.get()?.password)
        assertEquals("123456", deletedRequest.get()?.code)
        assertTrue(deleted)
    }
}
