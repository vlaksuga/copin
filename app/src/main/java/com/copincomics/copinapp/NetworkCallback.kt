package com.copincomics.copinapp

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.appcompat.app.AlertDialog

open class NetworkCallback(val context: Context) : ConnectivityManager.NetworkCallback() {

    companion object {
        const val TAG = "NetworkCallback"
    }

    override fun onAvailable(network: Network) {
        Log.d(TAG, "onAvailable: network available")
    }

    override fun onLost(network: Network) {
        Log.d(TAG, "onLost: network lost")
        checkNetwork()
    }

    private fun checkNetwork() {
        val dialog = AlertDialog.Builder(context)
                .setMessage("Network is now unavailable. Please Check Network Connection")
                .setPositiveButton("Finish This App") { _, _ -> (context as Activity).finish() }
                .setNegativeButton("Retry") { dialog, _ ->
                    dialog.dismiss()
                    checkNetwork()
                }
                .setCancelable(false)
                .create()
        if (!networkConnection()) {
            dialog.show()
        } else {
            dialog.dismiss()
        }
    }

    private fun networkConnection(): Boolean {
        val result: Boolean
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities)
                ?: return false
        result = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }
}