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
    }

    private var link: String? = null
    private var toon: String? = null

    private var subTopic = false

    lateinit var config: AppConfig


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        config = AppConfig.shared()

        /* CHECK NETWORK CONNECTION */
        if (networkConnection(this)) {
            checkVersion()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Network Error")
            builder.setPositiveButton("Confirm") { _, _ -> finish() }
            builder.setCancelable(false)
            builder.show()
        }


        /* INTENT EXTRA */
//        val intent = intent
//        if (intent.data != null && intent.data.toString().contains("toon://open/")) {
//            // Deep link : scheme toon://
//            toon = intent.data.toString().replace("toon://open/", "")
//            Log.d(TAG, "intent extra toon = $toon")
//        }
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

    private fun networkConnection(activity: AppCompatActivity): Boolean {
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
                response.body()?.let { res ->
                    var minVersion = res.body.ANDROIDMIN.toIntOrNull()
                    var recentVersion = res.body.ANDROIDRECENT.toIntOrNull()

                    // if min or recent can't convert to int
                    if (minVersion == null) {
                        minVersion = 1
                    }
                    if (recentVersion == null) {
                        recentVersion = 99
                    }

                    val apiURL11: String = res.body.APIURL11
                    val entryURL11: String = res.body.ENTRYURL11
                    val defaultEntryURL = res.body.DEFAULTENTRYURL
                    val defaultApiURL = res.body.DEFAULTAPIURL

                    if (App.currentVersion < minVersion) {
                        val builder = AlertDialog.Builder(this@EntryActivity)
                        builder.setMessage("Confirm to upgrade version?")
                            .setPositiveButton("Confirm") { _, _ ->
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=com.copincomics.copinapp")
                                    )
                                )
                                finish()
                            }
                            .show()

                        // ENTRY FLOW STOPS HERE
                    } else {
                        if (App.currentVersion > recentVersion) {
                            config.entryURL = defaultEntryURL
                            config.apiURL = defaultEntryURL

                            if (entryURL11.isNotBlank() or entryURL11.isNotEmpty()) {
                                config.entryURL = entryURL11
                            }

                            if (apiURL11.isNotBlank() or apiURL11.isNotEmpty()) {
                                config.apiURL = apiURL11
                            }

                        } else {
                            config.entryURL = defaultEntryURL
                            config.apiURL = defaultApiURL
                        }
                    }
                    loginWithRefreshToken()
                } ?: {
                    adaptHardCodedVersion()
                }()
            }

            override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                adaptHardCodedVersion()
            }
        })
    }

    private fun adaptHardCodedVersion() {
        config.entryURL = "https://copincomics.com"
        config.apiURL = "https://api.copincomics.com"
        loginWithRefreshToken()
    }

    private fun loginWithRefreshToken() {
        val refreshToken = App.preferences.refreshToken
        if (refreshToken != "") {
            Retrofit().accountDAO.processLoginByToken(lt = refreshToken).enqueue(object : Callback<RetLogin> {
                override fun onResponse(call: Call<RetLogin>, response: Response<RetLogin>) {
                    response.body()?.let { res ->
                        val head = res.head
                        if (head.status != "error") {
                            val ret = res.body
                            App.preferences.refreshToken = ret.t2
                            config.acccessToken = ret.token
                            config.accountPKey = ret.userinfo.accountpkey
                            if(config.accountPKey != "") {
                                val branch = Branch.getInstance(applicationContext)
                                branch.setIdentity(config.accountPKey)
                            }
                            updateDeviceId()
                        } else {
                            emptyAccountPreference()
                        }
                    }?:{
                        emptyAccountPreference()
                    }()
                }

                override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                    emptyAccountPreference()
                }
            })
        } else {
            emptyAccountPreference()
        }
    }

    private fun updateDeviceId() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token ->
                    config.deviceID = token
                    startMainActivity()
                }
            } else {
                // TODO : CHECK EMPTY DEVICE ID FLOW
                config.deviceID = ""
                startMainActivity()
            }
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

    private fun emptyAccountPreference() {
        App.preferences.refreshToken = ""
        config.acccessToken = ""
        config.accountPKey = ""
        updateDeviceId()
    }

    private fun startMainActivity() {
        val mainActivityIntent = Intent(this, WebViewActivity::class.java)
        if(link != null) {
            intent.extras?.let { bundle ->
                link = bundle.getString("link")
            }
            mainActivityIntent.putExtra("link", link)
        }
        startActivity(mainActivityIntent)
    }

}