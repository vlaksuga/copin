package com.copincomics.copinapp

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Base"
        const val PREFERENCE_NAME = "copincomics"
        const val curVersion = 11
    }

    lateinit var sharedPreferences: SharedPreferences
    lateinit var repo: ServiceRepo
    lateinit var loadingDialog: AlertDialog
    lateinit var firebaseAnalytics: FirebaseAnalytics
    var entryURL: String = "https://stage.copincomics.com/"

    fun init() {
        sharedPreferences = applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        repo = ServiceRepo(sharedPreferences)
        firebaseAnalytics = Firebase.analytics
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

    fun getCurVersion() : Int {
        return curVersion
    }

    fun registerNetworkCallback(networkCallback: NetworkCallback) {
        val cm = getSystemService(ConnectivityManager::class.java)
        val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()
        cm.registerNetworkCallback(networkRequest, networkCallback)
        Log.d(TAG, "registerNetworkCallback: registered")
    }

    fun unregisterNetworkCallback(networkCallback: NetworkCallback) {
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.unregisterNetworkCallback(networkCallback)
        Log.d(TAG, "unRegisterNetworkCallback: unregistered")
    }



}