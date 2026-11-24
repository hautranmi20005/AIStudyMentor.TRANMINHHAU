package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.AccountReauthenticationRequestDto
import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.remote.LoginRequestDto
import com.aistudymentor.data.remote.LogoutRequestDto
import com.aistudymentor.data.remote.MfaVerifyRequestDto
import com.aistudymentor.data.remote.PasswordRecoveryRequestDto
import com.aistudymentor.data.remote.PasswordResetRequestDto
import com.aistudymentor.data.remote.RegisterRequestDto
import com.aistudymentor.data.remote.RegisterResponseDto
import com.aistudymentor.data.remote.StudentProfileDto
import com.aistudymentor.data.remote.StudentProfileUpdateRequestDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.FirebaseTelemetry
import com.aistudymentor.data.telemetry.monitorRetrofitCall
import java.time.LocalDate

sealed interface AuthResult {
    data object MfaRequired : AuthResult
    data class Authenticated(val guardianConsentRequired: Boolean) : AuthResult
}

class AuthRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage
) : AuthDataSource {
    override suspend fun login(email: String, password: String): AuthResult {
        val response = monitorRetrofitCall("auth.login") {
            api.login(LoginRequestDto(email.trim(), password)).requireBody()
        }
        if (response.mfaRequired == true) {
            val mfaToken = response.mfaSessionToken
                ?: throw ApiFailure(null, null, "The server did not start MFA verification.", retryable = false)
            tokenStorage.saveMfaSessionToken(mfaToken)
            return AuthResult.MfaRequired
        }
        return establishSession(response.accessToken, response.refreshToken, response.userId)
    }

    override suspend fun verifyMfa(code: String, type: String): AuthResult.Authenticated {
        val sessionToken = tokenStorage.mfaSessionToken()
            ?: throw ApiFailure(401, "MFA_SESSION_MISSING", "Please sign in again to verify your code.", retryable = false)
        val response = monitorRetrofitCall("auth.mfa_verify") {
            api.verifyMfa(
                MfaVerifyRequestDto(mfaSessionToken = sessionToken, code = code.trim(), type = type)
            ).requireBody()
        }
        return establishSession(response.accessToken, response.refreshToken, response.userId)
    }

    override suspend fun register(
        email: String,
        password: String,
        age: Int,
        educationLevel: String,
        guardianEmail: String?
    ): RegisterResponseDto {
        val dob = LocalDate.now().minusYears(age.toLong()).toString()
        return monitorRetrofitCall("auth.register") {
            api.register(
                RegisterRequestDto(
                    email = email.trim(),
                    password = password,
                    dateOfBirth = dob,
                    educationLevel = educationLevel.trim(),
                    guardianEmail = guardianEmail?.trim()?.takeIf(String::isNotEmpty)
                )
            ).requireBody()
        }
    }

    override suspend fun guardianConsentStatus(): GuardianConsentResponseDto {
        val status = monitorRetrofitCall("guardian.status") {
            api.getGuardianConsentStatus(bearerToken()).requireBody()
        }
        FirebaseTelemetry.updateGuardianConsent(status.status.equals("APPROVED", ignoreCase = true))
        return status
    }

    override suspend fun resendGuardianConsent() {
        monitorRetrofitCall("guardian.resend") {
            api.resendGuardianConsent(bearerToken()).requireSuccess()
        }
    }

    override suspend fun myProfile(): StudentProfileDto =
        monitorRetrofitCall("profile.load") {
            api.getMyProfile(bearerToken()).requireBody()
        }

    override suspend fun updateProfile(request: StudentProfileUpdateRequestDto): StudentProfileDto =
        monitorRetrofitCall("profile.update") {
            api.updateMyProfile(bearerToken(), request).requireBody()
        }

    override suspend fun requestPasswordReset(email: String) {
        monitorRetrofitCall("auth.password_forgot") {
            api.requestPasswordReset(PasswordRecoveryRequestDto(email.trim())).requireSuccess()
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        monitorRetrofitCall("auth.password_reset") {
            api.resetPassword(PasswordResetRequestDto(token.trim(), newPassword)).requireSuccess()
        }
    }

    override suspend fun logoutFromServer() {
        val refresh = tokenStorage.refreshToken()
        if (refresh != null) {
            monitorRetrofitCall("auth.logout") {
                api.logout(LogoutRequestDto(refresh)).requireSuccess()
            }
        }
        logout()
    }

    override suspend fun deleteAccount(password: String, code: String) {
        monitorRetrofitCall("account.delete") {
            api.deleteAccount(
                bearerToken(),
                AccountReauthenticationRequestDto(password, code)
            ).requireSuccess()
        }
        logout()
    }

    override suspend fun logoutAllDevices(password: String, code: String) {
        monitorRetrofitCall("account.logout_all") {
            api.logoutAllDevices(
                bearerToken(),
                AccountReauthenticationRequestDto(password, code)
            ).requireSuccess()
        }
        logout()
    }

    override fun logout() {
        FirebaseTelemetry.endSession()
        tokenStorage.clear()
    }

    private suspend fun establishSession(
        accessToken: String?,
        refreshToken: String?,
        userId: String?
    ): AuthResult.Authenticated {
        val access = accessToken?.takeIf(String::isNotBlank)
            ?: throw ApiFailure(null, null, "The server did not return an access token.", retryable = false)
        val accountId = userId?.takeIf(String::isNotBlank)
            ?: throw ApiFailure(null, null, "The server did not return an account identifier.", retryable = false)
        tokenStorage.saveSession(access, refreshToken, accountId)

        // A 404 means this account has no guardian-consent record; other failures surface.
        val consentResponse = monitorRetrofitCall("guardian.status_after_login") {
            api.getGuardianConsentStatus("Bearer $access")
        }
        val pendingConsent = when {
            consentResponse.isSuccessful -> consentResponse.body()?.status.equals("PENDING", ignoreCase = true)
            consentResponse.code() == 404 -> false
            else -> throw consentResponse.toApiFailure()
        }
        FirebaseTelemetry.startSession(guardianConsentRequired = pendingConsent)
        return AuthResult.Authenticated(guardianConsentRequired = pendingConsent)
    }

    private fun bearerToken(): String {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in again.", retryable = false)
        return "Bearer $token"
    }
}