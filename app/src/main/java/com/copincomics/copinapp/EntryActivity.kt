package com.copincomics.copinapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.copincomics.copinapp.data.CheckVersion
import com.copincomics.copinapp.data.RetLogin
import com.google.firebase.analytics.FirebaseAnalytics
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

    var link: String? = null
    var toon: String? = null

    private var networkConnect = false
    private val fm = FirebaseMessaging.getInstance()
    var checkVersion = false
    var checkLogin = false
    var updateDeviceId = false
    var subTopic = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: start")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        init()
        putAppPref("a", "https://sapi.copincomics.com")


        /* INTENT EXTRA */
        val intent = intent
        intent.extras?.let {
            link = it.getString("link") // intent from FCM
            toon = it.getString("toon") // intent from deep link
            Log.d(TAG, "intent extra link = $link")
            Log.d(TAG, "intent extra toon = $toon")
        }


        /* CHECK NETWORK CONNECTION */
        if (networkConnection(this)) {
            networkConnect = true
            checkVersion()
            checkLogin()
            updateDeviceId()
            subscribeInit()
        } else {
            Log.d(TAG, "onCreate: network error ")
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Network Error")
            builder.setPositiveButton("Confirm") { _, _ -> finish() }
            builder.setCancelable(false)
            builder.show()
        }

    }

    override fun onStart() {
        super.onStart()
        try {
            Branch.enableLogging()
            IntegrationValidator.validate(this)
            Branch.sessionBuilder(this)
                    .withCallback { referringParams, _ -> Log.d(TAG, "Branch Session Builder: $referringParams")}
                    .withData(this.intent.data)
                    .init()
        } catch (e: Exception) {
            Log.w(TAG, "onStart: Branch Init Fail", e)
        }
    }

    private fun subscribeInit() {
        Log.d(TAG, "subscribeInit: invoked")
        if(getAppPref("subInit") != "Y") {
            Log.d(TAG, "subscribeInit: invoked")
            fm.subscribeToTopic("Notice")
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            Log.d(TAG, "subscribeInit: notice")
                            putAppPref("Notice", "Y")
                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Notice")} ")
                        }
                    }

            fm.subscribeToTopic("Event")
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            Log.d(TAG, "subscribeInit: event")
                            putAppPref("Event", "Y")
                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Event")} ")
                        }
                    }

            fm.subscribeToTopic("Series")
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            Log.d(TAG, "subscribeInit: series")
                            putAppPref("Series", "Y")
                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Series")} ")
                        }
                    }
            putAppPref("subInit", "Y")
            subTopic = true
            executeNext()
        }
        subTopic = true
        executeNext()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try {
            setIntent(intent)
            Branch.sessionBuilder(this).withCallback { _, _ ->  startActivity(intent)}.reInit()
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

    private fun updateDeviceId() {
        val fcm = FirebaseMessaging.getInstance()
        fcm.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let {
                    putAppPref("deviceId", it)
                    Log.d(TAG, "updateDeviceId: firebase instance id : $it")
                    Log.d(TAG, "updateDeviceId: in pref ${getAppPref("deviceId")}")
                }
            } else {
                putAppPref("deviceId", "fail_to_get_FCM_instance_token")
                Log.d(TAG, "updateDeviceId: firebase instance id : Fail")

            }
            updateDeviceId = true
            executeNext()
        }
    }

    private fun checkVersion() {
        val body = repo.accountDAO.requestCheckVersion().execute().body()
        Log.d(TAG, "execute: body = $body")
/*        try {
            repo.accountDAO.requestCheckVersion().enqueue(object : Callback<CheckVersion> {
                override fun onResponse(
                        call: Call<CheckVersion>,
                        response: Response<CheckVersion>
                ) {
                    response.body()?.let { res ->
                        val minVersion = res.body.ANDROIDMIN.toInt()
                        val recentVersion = res.body.ANDROIDRECENT.toInt()
                        val apiURL11: String? = res.body.APIURL11
                        val entryURL11: String? = res.body.ENTRYURL11
                        val defaultEntryURL = res.body.DEFAULTENTRYURL
                        val defaultApiURL = res.body.DEFAULTAPIURL
                        Log.d(TAG, "onResponse: curVersion : $curVersion")
                        Log.d(TAG, "onResponse: recentVersion : $recentVersion")
                        if (curVersion < minVersion) {
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
                        } else {
                            Log.d(TAG, "onResponse: No need to update app")
                            if (curVersion >= recentVersion) {
                                Log.d(TAG, "onResponse: apiURL11 : $apiURL11")
                                Log.d(TAG, "onResponse: entryURL11 : $entryURL11")
                                putAppPref("e", defaultEntryURL)
                                putAppPref("a", defaultApiURL)

                                if (entryURL11 != null) {
                                    putAppPref("e", entryURL11)
                                }

                                if (apiURL11 != null) {
                                    putAppPref("a", apiURL11)
                                } else {
                                    Log.d(TAG, "onResponse: defaultEntry")
                                }

                            } else {
                                putAppPref("e", defaultEntryURL)
                                putAppPref("a", defaultApiURL)
                                Log.d(TAG, "onResponse: defaultEntryURL = $defaultEntryURL")
                                Log.d(TAG, "onResponse: defaultApiURL = $defaultApiURL")
                                Log.d(TAG, "onResponse: recentVersion adapted")

                            }
                        }

                    }
                }

                override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                    Log.w(TAG, "onFailure: check version error", t)
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
                }
            })

        } catch (e: Exception) {
            Log.w(TAG, "checkVersion: check version error", e)
        }*/
    }

    private fun checkLogin() {
        val lt = getAppPref("lt")
        if (lt != "") {
            Log.d(TAG, "checkLogin: lt is not empty")
            try {
                repo.accountDAO.processLoginByToken(lt = lt).enqueue(object : Callback<RetLogin> {
                    override fun onResponse(call: Call<RetLogin>, response: Response<RetLogin>) {
                        try {
                            response.body()?.let { res ->
                                val head = res.head
                                if (head.status != "error") {
                                    val ret = res.body
                                    putAppPref("lt", ret.t2)
                                    putAppPref("t", ret.token)
                                    putAppPref("nick", ret.userinfo.nick)
                                    putAppPref("accountPKey", ret.userinfo.accountpkey)
                                    Log.d(TAG, "onResponse: Login Token = ${ret.t2}")
                                    Log.d(TAG, "onResponse: Access Token = ${ret.token}")
                                    Log.d(TAG, "onResponse: AccountPKey = ${ret.userinfo.accountpkey}")
                                } else {
                                    emptyUserPreference()
                                    Log.d(TAG, "onResponse: login fail message (${head.msg})")
                                }
                            }
                            checkLogin = true
                            executeNext()
                        } catch (e: Exception) {
                            emptyUserPreference()
                            Log.w(TAG, "onResponse: error", e)
                            checkLogin = true
                            executeNext()
                        }
                    }

                    override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                        Log.w(TAG, "onFailure: error", t)
                        emptyUserPreference()
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "checkLogin: check login error", e)
                emptyUserPreference()
                checkLogin = true
                executeNext()
            }
        } else {
            emptyUserPreference()
            checkLogin = true
            executeNext()
        }
        Log.d(TAG, "checkLogin: $checkLogin")
    }

    private fun emptyUserPreference() {
        Log.d(TAG, "emptyUserPreference: empty")
        putAppPref("lt", "")
        putAppPref("lt", "")
        putAppPref("t", "")
        putAppPref("nick", "")
        putAppPref("accountPKey", "")
    }

    private fun executeNext() {
        Log.d(TAG, "executeNext: invoked")
        Log.d(TAG, "executeNext: n : $networkConnect, v : $checkVersion, l : $checkLogin, d : $updateDeviceId, s: $subTopic")
        if(networkConnect and checkVersion and checkLogin and updateDeviceId and subTopic) {

            // Branch Set Identity
            if(getAppPref("accountPKey") != "") {
                val branch = Branch.getInstance(applicationContext)
                branch.setIdentity(getAppPref("accountPKey"))
            }

            val intent = Intent(this, MainWebViewActivity::class.java)
            when {
                link != null -> {
                    Log.d(TAG, "executeNext: Start intent from FCM")
                    intent.putExtra("link", link)
                    Log.d(TAG, "executeNext: link = $link")
                }
                toon != null -> {
                    Log.d(TAG, "executeNext: Start intent from deep link")
                    intent.putExtra("toon", toon)
                    Log.d(TAG, "executeNext: toon = $toon")
                }
                else -> {}
            }

            startActivity(intent)
            finish()
        }
    }

}