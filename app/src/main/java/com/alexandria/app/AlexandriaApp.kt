package com.alexandria.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
            val factory = if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
        } catch (e: Exception) {
            // No google-services.json or emulator without play integrity — resolution still falls back locally.
        }
    }
}
