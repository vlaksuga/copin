package com.copincomics.copinapp

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class App : Application() {

    companion object {
        lateinit var preferences: AppSharedPreferences
        lateinit var config: AppConfig
        lateinit var firebaseAnalytics: FirebaseAnalytics
        lateinit var networkCallback: NetworkCallback
        const val currentVersion = 11
    }


    override fun onCreate() {
        preferences = AppSharedPreferences(applicationContext)
        config = AppConfig.shared()
        firebaseAnalytics = Firebase.analytics
        networkCallback = object : NetworkCallback(applicationContext) {}
        super.onCreate()
    }

}