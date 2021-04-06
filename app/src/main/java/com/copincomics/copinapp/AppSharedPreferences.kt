package com.copincomics.copinapp

import android.content.Context
import android.content.SharedPreferences

class AppSharedPreferences(context: Context) {

    companion object {
        const val TAG = "Base"
        const val PREFERENCE_NAME = "preferences"
        const val API_URL = "KEY.COPIN.APIURL"
        const val ENTRY_URL = "KEY.COPIN.ENTRYURL"
        const val curVersion = 11
    }
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    var apiURL: String?
    get() = preferences.getString(API_URL, "https://api.copincomics.com")
    set(value) {
        preferences.edit().putString(API_URL, value).apply()
    }

    var entryURL: String?
    get() = preferences.getString(ENTRY_URL, "https://copincomics.com")
    set(value) {
        preferences.edit().putString(ENTRY_URL, "").apply()
    }

    var accessToken: String?
    get() = preferences.g
}