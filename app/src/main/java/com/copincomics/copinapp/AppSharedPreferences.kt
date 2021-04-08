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
}

class AppConfig private constructor() {

    companion object {

        @Volatile private var instance: AppConfig? = null

        @JvmStatic fun shared(): AppConfig =
            instance ?: synchronized(this) {
                instance ?: AppConfig().also {
                    instance = it
                }
            }
    }

    var apiURL : String = "https://sapi.copincomics.com/"
    var entryURL: String = ""
    var acccessToken : String = ""
    var accountPKey: String = ""
    var deviceID: String = ""

}