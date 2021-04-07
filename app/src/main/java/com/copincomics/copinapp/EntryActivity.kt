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

    private var networkConnect = false
    private val fm = FirebaseMessaging.getInstance()
    private var checkVersion = false
    private var checkLogin = false
    private var updateDeviceId = false
    private var subTopic = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: start")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        var at = CopinPref.Shared().getAc()
        init()
        putAppPref("a", "https://sapi.copincomics.com")


        /* INTENT EXTRA */
        val intent = intent
        if (intent.data != null && intent.data.toString().contains("toon://open/")) {
            // Deep link : scheme toon://
            toon = intent.data.toString().replace("toon://open/", "")
            Log.d(TAG, "intent extra toon = $toon")
        }

        intent.extras?.let {
            link = it.getString("link") // intent from FCM
            Log.d(TAG, "intent extra link = $link")
        }

        Log.d(TAG, "onCreate: a = ${intent.action}, d = ${intent.data} ")


        /* CHECK NETWORK CONNECTION */
        if (networkConnection(this)) {
            networkConnect = true
            checkVersion()
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
        Log.d(TAG, "networkConnection: start")
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
        Log.d(TAG, "networkConnection: end")
        return result
    }

    private fun checkVersion() {
        Log.d(TAG, "checkVersion: start")
        repo.accountDAO.requestCheckVersion().enqueue(object : Callback<CheckVersion> {
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
                    Log.d(TAG, "onResponse: curVersion : $curVersion")
                    Log.d(TAG, "onResponse: minVersion : $minVersion")
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
                        if (curVersion > recentVersion) {
                            Log.d(TAG, "onResponse: apiURL11 : $apiURL11")
                            Log.d(TAG, "onResponse: entryURL11 : $entryURL11")
                            putAppPref("e", defaultEntryURL)
                            putAppPref("a", defaultApiURL)

                            if (entryURL11.isNotBlank() or entryURL11.isNotEmpty()) {
                                putAppPref("e", entryURL11)
                            }

                            if (apiURL11.isNotBlank() or apiURL11.isNotEmpty()) {
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
                    checkVersion = true
                    Log.d(TAG, "checkVersion: end")
                    checkLogin()
                } ?: {
                    Log.d(TAG, "onResponse: here??")
                    putAppPref("e", "https://copincomics.com")
                    putAppPref("a", "https://api.copincomics.com")
                    checkVersion = true
                    Log.d(TAG, "checkVersion: end")
                    checkLogin()
                }()
            }

            override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                Log.w(TAG, "onFailure: check version error", t)
                Log.d(TAG, "onFailure: start Default version mode")
                putAppPref("e", "https://copincomics.com")
                putAppPref("a", "https://api.copincomics.com")
                checkVersion = true
                Log.d(TAG, "checkVersion: end")
                checkLogin()
            }
        })
    }

    private fun checkLogin() {
        Log.d(TAG, "checkLogin: start")
        val lt = getAppPref("lt")
        if (lt != "") {
            Log.d(TAG, "checkLogin: lt is not empty")
            repo.accountDAO.processLoginByToken(lt = lt).enqueue(object : Callback<RetLogin> {
                override fun onResponse(call: Call<RetLogin>, response: Response<RetLogin>) {
                    response.body()?.let { res ->
                        val head = res.head
                        if (head.status != "error") {
                            val ret = res.body
                            putAppPref("lt", ret.t2)
                            putAppPref("t", ret.token)
                            putAppPref("nick", ret.userinfo.nick)
                            putAppPref("accountPKey", ret.userinfo.accountpkey)
                            Log.d(TAG, "onResponse: Auth Login Token = ${ret.t2}")
                            Log.d(TAG, "onResponse: Access Token = ${ret.token}")
                            Log.d(
                                TAG,
                                "onResponse: AccountPKey = ${ret.userinfo.accountpkey}"
                            )
                        } else {
                            emptyAccountPreference()
                            Log.d(TAG, "onResponse: login fail message (${head.msg})")
                        }
                        checkLogin = true
                        Log.d(TAG, "checkLogin: end")
                        updateDeviceId()
                    }
                }

                override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                    Log.w(TAG, "onFailure: error", t)
                    emptyAccountPreference()
                    checkLogin = true
                    Log.d(TAG, "checkLogin: end")
                    updateDeviceId()
                }
            })
        } else {
            emptyAccountPreference()
            checkLogin = true
            Log.d(TAG, "checkLogin: end")
            updateDeviceId()
        }
        Log.d(TAG, "checkLogin: $checkLogin")
    }

    private fun updateDeviceId() {
        Log.d(TAG, "updateDeviceId: start")
        val fcm = FirebaseMessaging.getInstance()
        fcm.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let {
                    putAppPref("deviceId", it)
                    Log.d(TAG, "updateDeviceId: firebase instance id : $it")
                    Log.d(TAG, "updateDeviceId: in pref ${getAppPref("deviceId")}")
                    updateDeviceId = true
                    Log.d(TAG, "updateDeviceId: end")
                    subscribeInit()
                }
            } else {
                putAppPref("deviceId", "fail_to_get_FCM_instance_token")
                Log.d(TAG, "updateDeviceId: firebase instance id : Fail")
                updateDeviceId = true
                Log.d(TAG, "updateDeviceId: end")
                subscribeInit()
            }
        }
    }

    private fun subscribeInit() {
        Log.d(TAG, "subscribeInit: start")
        val topicList = arrayListOf("Notice", "Event", "Series")
        if (getAppPref("subInit") != "Y") {
            Log.d(TAG, "subscribeInit: invoked")
            fm.subscribeToTopic(topicList[0]).addOnCompleteListener { task0 ->
                if(task0.isSuccessful) {
                    Log.d(TAG, "subscribeInit: ${topicList[0]} subscribed")
                    putAppPref(topicList[0], "Y")
                    Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[0])}")
                }
                fm.subscribeToTopic(topicList[1]).addOnCompleteListener { task1 ->
                    if(task1.isSuccessful) {
                        Log.d(TAG, "subscribeInit: ${topicList[1]} subscribed")
                        putAppPref(topicList[1], "Y")
                        Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[1])}")
                    }
                    fm.subscribeToTopic(topicList[2]).addOnCompleteListener { task2 ->
                        if(task2.isSuccessful) {
                            Log.d(TAG, "subscribeInit: ${topicList[2]} subscribed")
                            putAppPref(topicList[2], "Y")
                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref(topicList[2])}")
                        }
                        putAppPref("subInit", "Y")
                        subTopic = true
                        Log.d(TAG, "subscribeInit: end")
                        startMainActivity()
                    }
                }
            }
        } else {
            subTopic = true
            Log.d(TAG, "subscribeInit: end")
            startMainActivity()
        }
    }

    private fun emptyAccountPreference() {
        Log.d(TAG, "emptyAccountPreference: empty")
        putAppPref("lt", "")
        putAppPref("t", "")
        putAppPref("nick", "")
        putAppPref("accountPKey", "")
    }

    private fun startMainActivity() {
        Log.d(TAG, "startMainActivity: invoked")
        Log.d(
            TAG,
            "startMainActivity: n : $networkConnect, v : $checkVersion, l : $checkLogin, d : $updateDeviceId, s: $subTopic"
        )
        if (networkConnect and checkVersion and checkLogin and updateDeviceId and subTopic) {

            // Branch Set Identity
            if (getAppPref("accountPKey") != "") {
                val branch = Branch.getInstance(applicationContext)
                branch.setIdentity(getAppPref("accountPKey"))
            }

            val intent = Intent(this, MainWebViewActivity::class.java)
            when {
                link != null -> {
                    Log.d(TAG, "startMainActivity: Start intent from FCM")
                    intent.putExtra("link", link)
                    Log.d(TAG, "startMainActivity: link = $link")
                }
                toon != null -> {
                    Log.d(TAG, "startMainActivity: Start intent from deep link")
                    intent.putExtra("toon", toon)
                    Log.d(TAG, "startMainActivity: toon = $toon")
                }
                else -> {
                }
            }

            startActivity(intent)
            finish()
        }
    }

}