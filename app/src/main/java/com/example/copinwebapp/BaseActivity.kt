package com.example.copinwebapp

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFERENCE_NAME = "com.example.copinwebapp"
    }

    lateinit var sharedPreferences: SharedPreferences
    lateinit var repo: ServiceRepo
    lateinit var loadingDialog: AlertDialog

    fun init() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sharedPreferences = applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        repo = ServiceRepo(sharedPreferences)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.dialog_loading)
        loadingDialog = builder.create()
    }

    fun putAppPref(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            commit()
        }
    }

    fun getAppPref(key: String): String {
        return sharedPreferences.getString(key, "")!!
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}