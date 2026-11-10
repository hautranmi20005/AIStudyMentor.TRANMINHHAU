package com.aistudymentor.data.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.UUID

/**
 * Privacy-conscious Firebase instrumentation. No email, prompt text, tokens, image data,
 * or backend response bodies are recorded. Collection stays disabled until consent status
 * is known; pending guardian consent keeps both products disabled.
 */
object FirebaseTelemetry {
    @Volatile
    private var initialized = false

    @Volatile
    private var collectionAllowed = false

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    @Volatile
    private var crashlytics: FirebaseCrashlytics? = null

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        runCatching {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
            analytics = FirebaseAnalytics.getInstance(context.applicationContext).also {
                it.setAnalyticsCollectionEnabled(false)
            }
            crashlytics = FirebaseCrashlytics.getInstance().also {
                it.setCrashlyticsCollectionEnabled(false)
            }
        }.onFailure {
            // Firebase is optional for local builds without google-services.json.
            analytics = null
            crashlytics = null
        }
    }

    @Synchronized
    fun startSession(guardianConsentRequired: Boolean) {
        val analyticsClient = analytics ?: return
        val crashClient = crashlytics ?: return
        val allowed = !guardianConsentRequired
        collectionAllowed = allowed
        crashClient.setCustomKey("auth_state", if (allowed) "authenticated" else "guardian_consent_pending")
        crashClient.setCustomKey("guardian_consent_pending", guardianConsentRequired)
        if (!allowed) {
            crashClient.setCrashlyticsCollectionEnabled(false)
            analyticsClient.setAnalyticsCollectionEnabled(false)
            return
        }

        val sessionId = UUID.randomUUID().toString()
        crashClient.setCustomKey("session_id", sessionId)
        crashClient.setCustomKey("app_version", com.aistudymentor.BuildConfig.VERSION_NAME)
        crashClient.setCrashlyticsCollectionEnabled(true)
        analyticsClient.setAnalyticsCollectionEnabled(true)
        analyticsClient.logEvent("session_started", Bundle().apply {
            putString("session_id", sessionId)
        })
    }

    @Synchronized
    fun updateGuardianConsent(approved: Boolean) {
        if (approved) {
            startSession(guardianConsentRequired = false)
        } else {
            collectionAllowed = false
            crashlytics?.setCustomKey("auth_state", "guardian_consent_pending")
            crashlytics?.setCustomKey("guardian_consent_pending", true)
            crashlytics?.setCrashlyticsCollectionEnabled(false)
            analytics?.setAnalyticsCollectionEnabled(false)
        }
    }

    @Synchronized
    fun endSession() {
        if (collectionAllowed) {
            analytics?.logEvent("session_ended", null)
        }
        collectionAllowed = false
        crashlytics?.setCustomKey("auth_state", "signed_out")
        crashlytics?.setCustomKey("session_id", "")
        crashlytics?.setCrashlyticsCollectionEnabled(false)
        analytics?.setUserId(null)
        analytics?.setAnalyticsCollectionEnabled(false)
    }

    fun recordNetworkOrParsingFailure(operation: String, exception: Exception) {
        if (!collectionAllowed) return
        val crashClient = crashlytics ?: return
        crashClient.setCustomKey("last_operation", operation)
        crashClient.setCustomKey("last_exception_type", exception.javaClass.simpleName.take(64))
        crashClient.recordException(exception)
        analytics?.logEvent("non_fatal_client_error", Bundle().apply {
            putString("operation", operation)
            putString("exception_type", exception.javaClass.simpleName.take(64))
        })
    }
}
