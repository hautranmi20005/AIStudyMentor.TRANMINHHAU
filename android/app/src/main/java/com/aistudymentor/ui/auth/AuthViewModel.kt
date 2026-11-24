package com.aistudymentor.ui.auth

import com.aistudymentor.R
import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.remote.StudentProfileDto
import com.aistudymentor.data.remote.StudentProfileUpdateRequestDto
import com.aistudymentor.data.repository.AuthDataSource
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
    val mfaSetupRequired: Boolean = false,
    val mfaSecret: String? = null,
    val recoveryCodes: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val mfaRequired: Boolean = false,
    val isAuthenticated: Boolean = false,
    val guardianConsentRequired: Boolean = false,
    val registrationSucceeded: Boolean = false,
    val registrationNoticeRes: Int? = null,
    val registrationNoticeArgs: List<Any> = emptyList(),
    val consentStatus: GuardianConsentResponseDto? = null,
    val profile: StudentProfileDto? = null,
    val explanationStyle: String = "step-by-step",
    val forgotPasswordEmail: String = "",
    val resetToken: String = "",
    val newPassword: String = ""
)

class AuthViewModel(private val repository: AuthDataSource) : NetworkViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState = mutableUiState.asStateFlow()

    fun updateEmail(value: String) = mutableUiState.update { it.copy(email = value) }
    fun updatePassword(value: String) = mutableUiState.update { it.copy(password = value) }
    fun updateAge(value: String) = mutableUiState.update { it.copy(ageText = value.filter(Char::isDigit).take(3)) }
    fun updateGuardianEmail(value: String) = mutableUiState.update { it.copy(guardianEmail = value) }
    fun updateEducationLevel(value: String) = mutableUiState.update { it.copy(educationLevel = value) }
    fun updateMfaCode(value: String) = mutableUiState.update { it.copy(mfaCode = value.filter(Char::isDigit).take(6)) }
    fun updateExplanationStyle(value: String) = mutableUiState.update { it.copy(explanationStyle = value) }
    fun updateForgotPasswordEmail(value: String) = mutableUiState.update { it.copy(forgotPasswordEmail = value) }
    fun updateResetToken(value: String) = mutableUiState.update { it.copy(resetToken = value) }
    fun updateNewPassword(value: String) = mutableUiState.update { it.copy(newPassword = value) }

    fun login() {
        val snapshot = mutableUiState.value
        if (snapshot.email.isBlank() || snapshot.password.isBlank()) {
            emitSnackbar(R.string.validation_login)
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
            emitSnackbar(R.string.validation_mfa)
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
            emitSnackbar(R.string.validation_register)
            return
        }
        if (age == null || age !in 1..120) {
            emitSnackbar(R.string.validation_age)
            return
        }
        if (age < 13 && (snapshot.guardianEmail.isBlank() || !snapshot.guardianEmail.contains('@'))) {
            emitSnackbar(R.string.validation_guardian_email)
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
                        registrationNoticeRes = if (requiresConsent) {
                            R.string.registration_pending
                        } else {
                            R.string.registration_complete
                        },
                        registrationNoticeArgs = if (requiresConsent) listOf(guardianEmail.trim()) else emptyList(),
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

    fun loadProfile() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { loadProfile() },
            action = {
                val profile = repository.myProfile()
                mutableUiState.update {
                    it.copy(
                        profile = profile,
                        educationLevel = profile.educationLevel ?: it.educationLevel,
                        explanationStyle = profile.learningStyle ?: it.explanationStyle
                    )
                }
            }
        )
    }

    fun saveProfile() {
        val state = mutableUiState.value
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { saveProfile() },
            action = {
                val profile = repository.updateProfile(
                    StudentProfileUpdateRequestDto(
                        educationLevel = state.educationLevel,
                        learningStyle = state.explanationStyle.trim()
                    )
                )
                mutableUiState.update { it.copy(profile = profile) }
            }
        )
    }

    fun requestPasswordReset() {
        val email = mutableUiState.value.forgotPasswordEmail.trim()
        if (email.isBlank() || !email.contains('@')) {
            emitSnackbar(R.string.validation_account_email)
            return
        }
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { requestPasswordReset() },
            action = {
                repository.requestPasswordReset(email)
                emitSnackbar(R.string.password_reset_sent)
            }
        )
    }

    fun resetPassword() {
        val state = mutableUiState.value
        if (state.resetToken.isBlank() || state.newPassword.isBlank()) {
            emitSnackbar(R.string.validation_reset)
            return
        }
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { resetPassword() },
            action = {
                repository.resetPassword(state.resetToken, state.newPassword)
                mutableUiState.update { it.copy(resetToken = "", newPassword = "") }
                emitSnackbar(R.string.password_updated)
            }
        )
    }

    fun logoutAllDevices(password: String, code: String, onComplete: () -> Unit) {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { logoutAllDevices(password, code, onComplete) },
            action = {
                repository.logoutAllDevices(password, code)
                mutableUiState.value = AuthUiState()
                onComplete()
            }
        )
    }

    fun deleteAccount(password: String, code: String, onComplete: () -> Unit) {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { deleteAccount(password, code, onComplete) },
            action = {
                repository.deleteAccount(password, code)
                mutableUiState.value = AuthUiState()
                onComplete()
            }
        )
    }

    fun consumeRecoveryCodes() {
        mutableUiState.update { it.copy(recoveryCodes = emptyList()) }
    }

    fun resendGuardianConsent() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { resendGuardianConsent() },
            action = {
                repository.resendGuardianConsent()
                val status = repository.guardianConsentStatus()
                mutableUiState.update { it.copy(consentStatus = status) }
                emitSnackbar(R.string.guardian_email_resent)
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
        mutableUiState.update { it.copy(registrationNoticeRes = null, registrationNoticeArgs = emptyList()) }
    }

    fun logout() {
        repository.logout()
        mutableUiState.value = AuthUiState()
    }
}