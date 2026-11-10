package com.aistudymentor

import android.app.Application
import com.aistudymentor.data.AppContainer
import com.aistudymentor.data.telemetry.FirebaseTelemetry

class StudyMentorApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        FirebaseTelemetry.initialize(this)
        appContainer.offlineSyncCoordinator.start()
    }
}