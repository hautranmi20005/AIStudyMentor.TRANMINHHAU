package com.aistudymentor.ui.auth

import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.R
import com.aistudymentor.data.remote.RegisterResponseDto
import com.aistudymentor.data.remote.StudentProfileDto
import com.aistudymentor.data.remote.StudentProfileUpdateRequestDto
import com.aistudymentor.data.repository.AuthDataSource
import com.aistudymentor.data.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthDataSource
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthDataSource()
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginRequiresMfaThenCompletesSession() = runTest(dispatcher) {
        viewModel.updateEmail("student@example.test")
        viewModel.updatePassword("Strong-password123")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.mfaRequired)
        viewModel.updateMfaCode("123456")
        viewModel.verifyMfa()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals("123456", repository.lastMfaCode)
    }

    @Test
    fun registrationPassesAgeAndEducationLevel() = runTest(dispatcher) {
        viewModel.updateEmail("student@example.test")
        viewModel.updatePassword("Strong-password123")
        viewModel.updateAge("21")
        viewModel.updateEducationLevel("high-school")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(21, repository.registeredAge)
        assertTrue(viewModel.uiState.value.registrationSucceeded)
        assertEquals(R.string.registration_complete, viewModel.uiState.value.registrationNoticeRes)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun accountDeletionPassesPasswordAndMfaThenCompletesNavigation() = runTest(dispatcher) {
        var completed = false

        viewModel.deleteAccount("Strong-password123", "123456") { completed = true }
        advanceUntilIdle()

        assertEquals("Strong-password123", repository.deletedPassword)
        assertEquals("123456", repository.deletedMfaCode)
        assertTrue(completed)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    private class FakeAuthDataSource : AuthDataSource {
        var registeredAge: Int? = null
        var lastMfaCode: String? = null
        var deletedPassword: String? = null
        var deletedMfaCode: String? = null

        override suspend fun login(email: String, password: String): AuthResult =
            AuthResult.MfaRequired

        override suspend fun verifyMfa(code: String, type: String): AuthResult.Authenticated {
            lastMfaCode = code
            return AuthResult.Authenticated(guardianConsentRequired = false)
        }

        override suspend fun register(
            email: String,
            password: String,
            age: Int,
            educationLevel: String,
            guardianEmail: String?
        ): RegisterResponseDto {
            registeredAge = age
            return RegisterResponseDto(
                userId = "student-id",
                email = email,
                accountStatus = "ACTIVE",
                consentStatus = null,
                guardianConsentRequired = false,
                mfaRequired = true,
                message = "Registration complete."
            )
        }

        override suspend fun guardianConsentStatus() =
            GuardianConsentResponseDto(null, null, "APPROVED", null, null, true)

        override suspend fun resendGuardianConsent() = Unit
        override suspend fun requestPasswordReset(email: String) = Unit
        override suspend fun resetPassword(token: String, password: String) = Unit

        override suspend fun myProfile() = StudentProfileDto(null, null, null, null)

        override suspend fun updateProfile(request: StudentProfileUpdateRequestDto) = StudentProfileDto(
            null, null, request.educationLevel, request.learningStyle
        )

        override suspend fun logoutFromServer() = Unit
        override suspend fun deleteAccount(password: String, code: String) {
            deletedPassword = password
            deletedMfaCode = code
        }
        override suspend fun logoutAllDevices(password: String, code: String) = Unit
        override fun logout() = Unit
    }
}
