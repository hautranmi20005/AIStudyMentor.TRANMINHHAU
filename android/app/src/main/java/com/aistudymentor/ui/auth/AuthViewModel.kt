package com.aistudymentor.ui.auth

import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.repository.AuthRepository
import com.aistudymentor.data.repository.AuthResult
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val ageText: String = "16",
    val guardianEmail: String = "",
    val educationLevel: String = "middle-school",
    val mfaCode: String = "",
    val isLoading: Boolean = false,
    val mfaRequired: Boolean = false,
    val isAuthenticated: Boolean = false,
    val guardianConsentRequired: Boolean = false,
    val registrationSucceeded: Boolean = false,
    val registrationNotice: String? = null,
    val consentStatus: GuardianConsentResponseDto? = null
)

class AuthViewModel(private val repository: AuthRepository) : NetworkViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState = mutableUiState.asStateFlow()

    fun updateEmail(value: String) = mutableUiState.update { it.copy(email = value) }
    fun updatePassword(value: String) = mutableUiState.update { it.copy(password = value) }
    fun updateAge(value: String) = mutableUiState.update { it.copy(ageText = value.filter(Char::isDigit).take(3)) }
    fun updateGuardianEmail(value: String) = mutableUiState.update { it.copy(guardianEmail = value) }
    fun updateEducationLevel(value: String) = mutableUiState.update { it.copy(educationLevel = value) }
    fun updateMfaCode(value: String) = mutableUiState.update { it.copy(mfaCode = value.filter(Char::isDigit).take(6)) }

    fun login() {
        val snapshot = mutableUiState.value
        if (snapshot.email.isBlank() || snapshot.password.isBlank()) {
            emitSnackbar("Enter your email and password to continue.")
            return
        }
        requestLogin(snapshot.email, snapshot.password)
    }

    private fun requestLogin(email: String, password: String) {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { requestLogin(email, password) },
            action = {
                when (val result = repository.login(email, password)) {
                    AuthResult.MfaRequired -> mutableUiState.update { it.copy(mfaRequired = true, mfaCode = "") }
                    is AuthResult.Authenticated -> mutableUiState.update {
                        it.copy(
                            mfaRequired = false,
                            isAuthenticated = true,
                            guardianConsentRequired = result.guardianConsentRequired
                        )
                    }
                }
            }
        )
    }

    fun verifyMfa() {
        val code = mutableUiState.value.mfaCode
        if (code.length != 6) {
            emitSnackbar("Enter the 6-digit authenticator code.")
            return
        }
        requestMfaVerification(code)
    }

    private fun requestMfaVerification(code: String) {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { requestMfaVerification(code) },
            action = {
                val result = repository.verifyMfa(code)
                mutableUiState.update {
                    it.copy(
                        mfaRequired = false,
                        isAuthenticated = true,
                        guardianConsentRequired = result.guardianConsentRequired
                    )
                }
            }
        )
    }

    fun register() {
        val snapshot = mutableUiState.value
        val age = snapshot.ageText.toIntOrNull()
        if (snapshot.email.isBlank() || !snapshot.email.contains('@') || snapshot.password.length < 8) {
            emitSnackbar("Enter a valid email and a password of at least 8 characters.")
            return
        }
        if (age == null || age !in 1..120) {
            emitSnackbar("Enter a valid age.")
            return
        }
        if (age < 13 && (snapshot.guardianEmail.isBlank() || !snapshot.guardianEmail.contains('@'))) {
            emitSnackbar("A valid parent or guardian email is required for users under 13.")
            return
        }
        requestRegistration(snapshot.email, snapshot.password, age, snapshot.educationLevel, snapshot.guardianEmail)
    }

    private fun requestRegistration(email: String, password: String, age: Int, education: String, guardianEmail: String) {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { requestRegistration(email, password, age, education, guardianEmail) },
            action = {
                val response = repository.register(email, password, age, education, guardianEmail)
                val requiresConsent = response.guardianConsentRequired == true
                mutableUiState.update {
                    it.copy(
                        registrationSucceeded = true,
                        registrationNotice = if (requiresConsent) {
                            "Registration is pending guardian approval. A verification email was sent to ${guardianEmail.trim()}. Sign in after completing verification."
                        } else {
                            "Registration complete. Sign in to continue."
                        },
                        guardianConsentRequired = requiresConsent
                    )
                }
            }
        )
    }

    fun loadGuardianConsentStatus() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { loadGuardianConsentStatus() },
            action = {
                val status = repository.guardianConsentStatus()
                mutableUiState.update { it.copy(consentStatus = status) }
            }
        )
    }

    fun resendGuardianConsent() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { resendGuardianConsent() },
            action = {
                repository.resendGuardianConsent()
                val status = repository.guardianConsentStatus()
                mutableUiState.update { it.copy(consentStatus = status) }
                emitSnackbar("Consent email resent. Please check the guardian's inbox.")
            }
        )
    }

    fun consumeAuthNavigation() {
        mutableUiState.update { it.copy(isAuthenticated = false) }
    }

    fun consumeRegistrationSuccess() {
        mutableUiState.update { it.copy(registrationSucceeded = false) }
    }

    fun clearRegistrationNotice() {
        mutableUiState.update { it.copy(registrationNotice = null) }
    }

    fun logout() {
        repository.logout()
        mutableUiState.value = AuthUiState()
    }
}