package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.remote.RegisterResponseDto
import com.aistudymentor.data.remote.StudentProfileDto
import com.aistudymentor.data.remote.StudentProfileUpdateRequestDto

interface AuthDataSource {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun verifyMfa(code: String, type: String = "TOTP"): AuthResult.Authenticated
    suspend fun register(
        email: String,
        password: String,
        age: Int,
        educationLevel: String,
        guardianEmail: String?
    ): RegisterResponseDto
    suspend fun guardianConsentStatus(): GuardianConsentResponseDto
    suspend fun resendGuardianConsent()
    suspend fun requestPasswordReset(email: String)
    suspend fun resetPassword(token: String, password: String)
    suspend fun myProfile(): StudentProfileDto
    suspend fun updateProfile(request: StudentProfileUpdateRequestDto): StudentProfileDto
    suspend fun logoutFromServer()
    suspend fun deleteAccount(password: String, code: String)
    suspend fun logoutAllDevices(password: String, code: String)
    fun logout()
}
