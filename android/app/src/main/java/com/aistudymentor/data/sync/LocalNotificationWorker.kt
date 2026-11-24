package com.aistudymentor.data.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudymentor.MainActivity
import com.aistudymentor.R
import com.aistudymentor.StudyMentorApplication
import com.aistudymentor.data.AppContainer
import com.aistudymentor.data.local.NotificationPreferencesEntity
import com.aistudymentor.data.repository.isRetryableNetworkFailure
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId

class LocalNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val accountId = inputData.getString(ACCOUNT_ID) ?: return Result.failure()
        val container = (applicationContext as StudyMentorApplication).appContainer
        if (container.currentAccountId() != accountId) return Result.success()
        val now = LocalDateTime.now()
        val preferences = container.offlineRepository.notificationPreferences(accountId)
            ?: NotificationPreferencesEntity(accountId = accountId)
        val zone = try {
            ZoneId.of(preferences.timezone)
        } catch (failure: DateTimeException) {
            Log.e(TAG, "Invalid timezone in local notification preferences.", failure)
            return Result.failure()
        }
        val localZonedNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone)
        val localNow = localZonedNow.toLocalDateTime()
        val periodKey = LocalNotificationRules.periodKey(localZonedNow)
        val bookmarks = container.offlineRepository.notificationBookmarks(accountId)
        val questions = container.offlineRepository.notificationQuestions(accountId)
        val quizResults = container.offlineRepository.notificationQuizResults(accountId)
        val permissionsAvailable = notificationsAllowed()
        if (permissionsAvailable && !LocalNotificationRules.isQuietHours(
                localNow, preferences.quietHoursStart, preferences.quietHoursEnd)) {
            if (preferences.bookmarkReviewEnabled &&
                LocalNotificationRules.shouldRemindAboutBookmarks(bookmarks, localNow)) {
                postIfNew(container, accountId, LocalNotificationRules.BOOKMARK_REVIEW, periodKey)
            }
            if (preferences.repeatedMistakeEnabled &&
                LocalNotificationRules.hasRepeatedMistakes(quizResults, localNow)) {
                postIfNew(container, accountId, LocalNotificationRules.REPEATED_MISTAKE, periodKey)
            }
            if (preferences.weeklySummaryEnabled &&
                LocalNotificationRules.hasRecentActivity(questions, quizResults, localNow)) {
                postIfNew(container, accountId, LocalNotificationRules.WEEKLY_SUMMARY, periodKey)
            }
        }
        return try {
            container.notificationSettingsRepository.syncPendingPreferences()
            Result.success()
        } catch (failure: Exception) {
            Log.w(TAG, "Notification preference sync failed; WorkManager will retry.")
            if (failure.isRetryableNetworkFailure()) Result.retry() else Result.failure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun postIfNew(
        container: AppContainer,
        accountId: String,
        type: String,
        periodKey: String
    ) {
        if (container.offlineRepository.hasLocalNotificationBeenSent(accountId, type, periodKey)) return
        val (title, body, route) = when (type) {
            LocalNotificationRules.BOOKMARK_REVIEW -> Triple(
                "Study reminder", "Your saved study materials are ready to review.", "studymentor://bookmarks"
            )
            LocalNotificationRules.REPEATED_MISTAKE -> Triple(
                "Practice reminder", "A short practice session may help reinforce your learning.", "studymentor://quizzes"
            )
            else -> Triple(
                "Weekly study summary", "Your weekly learning summary is ready.", "studymentor://dashboard"
            )
        }
        createChannels()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(route)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, type.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId(type))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(type.hashCode(), notification)
        container.offlineRepository.recordLocalNotification(accountId, type, periodKey)
    }

    private fun notificationsAllowed(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        channelTitles.forEach { (type, title) ->
            manager.createNotificationChannel(
                NotificationChannel(channelId(type), title, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun channelId(type: String) = "studymentor-$type".lowercase()

    companion object {
        const val ACCOUNT_ID = "account_id"
        private const val TAG = "P13Notifications"
        private val channelTitles = mapOf(
            LocalNotificationRules.BOOKMARK_REVIEW to "Bookmark review reminders",
            LocalNotificationRules.REPEATED_MISTAKE to "Practice reminders",
            LocalNotificationRules.WEEKLY_SUMMARY to "Weekly study summaries"
        )
    }
}
