package com.copincomics.copinapp

import android.content.Context
import android.content.SharedPreferences

class AppSharedPreferences(context: Context) {

    companion object {
        const val PREFERENCE_NAME = "preferences"
        const val API_URL = "KEY.COPIN.API_URL"
        const val ENTRY_URL = "KEY.COPIN.ENTRY_URL"
        const val ACCESS_TOKEN = "KEY.COPIN.ACCESS_TOKEN"
        const val REFRESH_TOKEN = "KEY.COPIN.REFRESH_TOKEN"
        const val ACCOUNT_PRIMARY_KEY = "KEY.COPIN.ACCOUNT_PRIMARY_KEY"
        const val DEVICE_ID = "KEY.COPIN.DEVICE_ID"
        const val curVersion = 11
    }
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    var apiURL: String
    get() = preferences.getString(API_URL, "https://api.copincomics.com")!!
    set(value) {
        preferences.edit().putString(API_URL, value).apply()
    }

    var entryURL: String
    get() = preferences.getString(ENTRY_URL, "https://copincomics.com")!!
    set(value) {
        preferences.edit().putString(ENTRY_URL, value).apply()
    }

    var accessToken: String
    get() = preferences.getString(ACCESS_TOKEN, "")!!
    set(value) {
        preferences.edit().putString(ACCESS_TOKEN, value).apply()
    }

    var refreshToken: String
    get() = preferences.getString(REFRESH_TOKEN, "")!!
    set(value) {
        preferences.edit().putString(REFRESH_TOKEN, value).apply()
    }

    var accountPKey: String?
    get() = preferences.getString(ACCOUNT_PRIMARY_KEY, null)
    set(value) {
        preferences.edit().putString(ACCOUNT_PRIMARY_KEY, value).apply()
    }

    var deviceID: String
    get() = preferences.getString(DEVICE_ID, "")!!
    set(value) {
        preferences.edit().putString(DEVICE_ID, value).apply()
    }
}