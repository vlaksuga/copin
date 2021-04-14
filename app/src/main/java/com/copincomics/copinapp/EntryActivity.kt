package com.copincomics.copinapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.copincomics.copinapp.data.CheckVersion
import com.copincomics.copinapp.data.RetLogin
import com.google.firebase.messaging.FirebaseMessaging
import io.branch.referral.Branch
import io.branch.referral.validators.IntegrationValidator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EntryActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : Entry"
        const val DEFAULT_API_URL = "https://api.copincomics.com"
        const val DEFAULT_ENTRY_URL = "https://copincomics.com"
    }

    private var link: String? = null
    private var toon: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)

        /* CHECK NETWORK CONNECTION */
        if (!checkNetworkConnection(this)) {
            showCustomAlert("net")
            return
        }
        checkVersion()
    }

    private fun showCustomAlert(case: String) {
        var message = ""
        var buttonText = ""
        var action: () -> Unit = {}
        when (case) {
            "net" -> {
                message = "Network Error"
                buttonText = "Confirm"
                action = { finish() }
            }
            "update" -> {
                message = "Confirm to upgrade version?"
                buttonText = "Confirm"
                action = { startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.copincomics.copinapp")))
                    finish() }
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(message)
            setPositiveButton(buttonText) { _, _-> action() }
            setCancelable(false)
            show()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            Branch.enableLogging()
            IntegrationValidator.validate(this)
            Branch.sessionBuilder(this)
                    .withCallback { referringParams, _ ->
                        Log.d(
                                TAG,
                                "Branch Session Builder: $referringParams"
                        )
                    }
                    .withData(this.intent.data)
                    .init()
        } catch (e: Exception) {
            Log.w(TAG, "onStart: Branch Init Fail", e)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try {
            setIntent(intent)
            Branch.sessionBuilder(this).withCallback { _, _ -> startActivity(intent) }.reInit()
        } catch (e: Exception) {
            Log.w(TAG, "onNewIntent: Branch ReInit Failed", e)
        }
    }

    private fun checkNetworkConnection(activity: AppCompatActivity): Boolean {
        val result: Boolean
        val connectivityManager =
                activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result

    }

    private fun checkVersion() {
        // TODO : TO MAP
        Log.d(TAG, "checkVersion: start")
        Retrofit().accountDAO.requestCheckVersion().enqueue(object : Callback<CheckVersion> {
            override fun onResponse(
                    call: Call<CheckVersion>,
                    response: Response<CheckVersion>
            ) {
                // validate response
                if (response.body() == null) {
                    adaptHardCodedVersion()
                    loginWithRefreshToken()
                    return
                }

                val res = response.body()!!

                // put data
                val minVersion = res.body.ANDROIDMIN.toIntOrNull() ?: 1
                val recentVersion = res.body.ANDROIDRECENT.toIntOrNull() ?: 99
                val apiURL11: String = res.body.APIURL11.ifEmpty { DEFAULT_API_URL }
                val defaultApiURL = res.body.DEFAULTAPIURL.ifEmpty { DEFAULT_API_URL }
                val entryURL11: String = res.body.ENTRYURL11.ifEmpty { DEFAULT_ENTRY_URL }
                val defaultEntryURL = res.body.DEFAULTENTRYURL.ifEmpty { DEFAULT_ENTRY_URL }

                // validateMinimumVersion
                // we only need minVersion because currentVersion is already hard-coded

                if (App.currentVersion < minVersion) {
                    showCustomAlert("update")
                    loginWithRefreshToken()
                    return
                }

                // validateRecentVersion
                App.config.entryURL = if (App.currentVersion > recentVersion) entryURL11 else defaultEntryURL
                App.config.apiURL = if (App.currentVersion > recentVersion) apiURL11  else defaultApiURL
                Log.d(TAG, "checkVersion: end")
                loginWithRefreshToken()
            }

            override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                adaptHardCodedVersion()
                loginWithRefreshToken()
            }
        })
    }

    private fun adaptHardCodedVersion() {
        App.config.entryURL = DEFAULT_ENTRY_URL
        App.config.apiURL = DEFAULT_API_URL
    }

    private fun loginWithRefreshToken() {
        Log.d(TAG, "loginWithRefreshToken: start ")
        val refreshToken = App.preferences.refreshToken

        // validate refreshToken
        if (refreshToken == "") {
            emptyAccountData()
            updateDeviceId()
            Log.d(TAG, "loginWithRefreshToken: end")
            return
        }

        Retrofit().accountDAO.processLoginByToken(lt = refreshToken).enqueue(object : Callback<RetLogin> {
            override fun onResponse(call: Call<RetLogin>, response: Response<RetLogin>) {

                if(response.body() == null) {
                    emptyAccountData()
                    updateDeviceId()
                    return
                }

                val res = response.body()!!
                val head = res.head
                val body = res.body

                if (head.status == "error") {
                    emptyAccountData()
                    updateDeviceId()
                    return
                }

                App.preferences.refreshToken = body.t2
                App.config.accessToken = body.token
                App.config.accountPKey = body.userinfo.accountpkey
                setBranchIdentity()
                updateDeviceId()
                Log.d(TAG, "loginWithRefreshToken: end ")
            }

            override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                emptyAccountData()
                updateDeviceId()
                Log.d(TAG, "loginWithRefreshToken: end ")
            }
        })

    }



    private fun updateDeviceId() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            App.config.deviceID = ""
            if (task.isSuccessful) {
                task.result?.let { token ->
                    App.config.deviceID = token
                }
            }
            startMainActivity()
        }
    }

//    private fun subscribeInit() {
//        Log.d(TAG, "subscribeInit: start")
//        val topicList = arrayListOf("Notice", "Event", "Series")
//        if (getAppPref("subInit") != "Y") {
//            Log.d(TAG, "subscribeInit: invoked")
//            fm.subscribeToTopic(topicList[0]).addOnCompleteListener { task0 ->
//                if(task0.isSuccessful) {
//                    Log.d(TAG, "subscribeInit: ${topicList[0]} subscribed")
//                    putAppPref(topicList[0], "Y")
//                    Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[0])}")
//                }
//                fm.subscribeToTopic(topicList[1]).addOnCompleteListener { task1 ->
//                    if(task1.isSuccessful) {
//                        Log.d(TAG, "subscribeInit: ${topicList[1]} subscribed")
//                        putAppPref(topicList[1], "Y")
//                        Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[1])}")
//                    }
//                    fm.subscribeToTopic(topicList[2]).addOnCompleteListener { task2 ->
//                        if(task2.isSuccessful) {
//                            Log.d(TAG, "subscribeInit: ${topicList[2]} subscribed")
//                            putAppPref(topicList[2], "Y")
//                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[2])}")
//                        }
//                        putAppPref("subInit", "Y")
//                        subTopic = true
//                        Log.d(TAG, "subscribeInit: end")
//                        startMainActivity()
//                    }
//                }
//            }
//        } else {
//            subTopic = true
//            Log.d(TAG, "subscribeInit: end")
//            startMainActivity()
//        }
//    }

    private fun emptyAccountData() {
        App.preferences.refreshToken = ""
        App.config.accessToken = ""
        App.config.accountPKey = ""
    }

    private fun startMainActivity() {
        val mainActivityIntent = Intent(this, WebViewActivity::class.java)
        if (link != null) {
            intent.extras?.let { bundle ->
                link = bundle.getString("link")
            }
            mainActivityIntent.putExtra("link", link)
        }
        startActivity(mainActivityIntent)
    }

}