package com.copincomics.copinapp

import android.app.Application

class App : Application() {

    companion object {
        lateinit var preferences: AppSharedPreferences
        val currentVersion = 11
    }

    override fun onCreate() {
        preferences = AppSharedPreferences(applicationContext)
        super.onCreate()
    }
}