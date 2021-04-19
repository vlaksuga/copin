package com.copincomics.copinapp

import android.content.Context
import android.content.SharedPreferences

class AppSharedPreferences(context: Context) {

    companion object {
        const val PREFERENCE_NAME = "preferences"
        const val REFRESH_TOKEN = "KEY.COPIN.REFRESH_TOKEN"
    }
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    var refreshToken: String
    get() = preferences.getString(REFRESH_TOKEN, "")!!
    set(value) {
        preferences.edit().putString(REFRESH_TOKEN, value).commit()
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
    var accessToken : String = ""
    var accountPKey: String = ""
    var deviceID: String = ""

}