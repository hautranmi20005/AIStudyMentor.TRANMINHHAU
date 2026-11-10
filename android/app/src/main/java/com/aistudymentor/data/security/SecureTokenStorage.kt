package com.aistudymentor.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStorage(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFERENCES_FILE,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun mfaSessionToken(): String? = preferences.getString(KEY_MFA_SESSION_TOKEN, null)

    fun userId(): String? = preferences.getString(KEY_USER_ID, null)

    fun saveMfaSessionToken(token: String) {
        preferences.edit()
            .putString(KEY_MFA_SESSION_TOKEN, token)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun saveSession(accessToken: String, refreshToken: String?, userId: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_USER_ID, userId)
            .apply {
                if (refreshToken == null) remove(KEY_REFRESH_TOKEN) else putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            .remove(KEY_MFA_SESSION_TOKEN)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_FILE = "encrypted_auth_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_MFA_SESSION_TOKEN = "mfa_session_token"
        const val KEY_USER_ID = "user_id"
    }
}