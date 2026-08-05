package com.alexandria.app

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlexandriaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        initAppCheck()
    }

    private fun initAppCheck() {
        try {
            FirebaseApp.initializeApp(this)
            setupAppCheck()
        } catch (e: Exception) {
            // No google-services.json or emulator without play integrity — resolution still falls back locally.
        }
    }
}
