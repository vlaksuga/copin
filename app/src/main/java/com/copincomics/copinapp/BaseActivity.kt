package com.copincomics.copinapp

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Base"
    }

    lateinit var auth: FirebaseAuth // base
    lateinit var loadingDialog: AlertDialog
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var webView: WebView // base

    var entryURL: String = "https://stage.copincomics.com/"
    lateinit var cm: ConnectivityManager
    // Network Callback

    fun setDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.dialog_loading)
        loadingDialog = builder.create()
    }

    fun showLoader() {
        loadingDialog.show()
    }

    fun dismissLoader() {
        loadingDialog.dismiss()
    }

    fun setBranchIdentity() {
        if (AppConfig.shared().accountPKey != "") {
            val branch = Branch.getInstance(applicationContext)
            branch.setIdentity(AppConfig.shared().accountPKey)
        }
    }

    fun registerNetworkCallback(networkCallback: NetworkCallback) {
        cm = getSystemService(ConnectivityManager::class.java)
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
        cm = getSystemService(ConnectivityManager::class.java)
        cm.unregisterNetworkCallback(networkCallback)
        Log.d(TAG, "unRegisterNetworkCallback: unregistered")
    }



}