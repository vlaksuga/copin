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
        Log.d(TAG, "networkConnection: start ")
        if (networkConnection(this)) {
            Log.d(TAG, "networkConnection: end")
            networkConnect = true
            checkVersion()
        } else {
            Log.d(TAG, "onCreate: network error")
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
                    .withCallback { referringParams, _ -> Log.d(TAG, "Branch Session Builder: $referringParams") }
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
        Log.d(TAG, "checkVersion: start")
        repo.accountDAO.requestCheckVersion().enqueue(object : Callback<CheckVersion> {
            override fun onResponse(
                    call: Call<CheckVersion>,
                    response: Response<CheckVersion>
            ) {
                response.body()?.let { res ->
                    val minVersion = res.body.ANDROIDMIN.toInt()
                    val recentVersion = res.body.ANDROIDRECENT.toInt()
                    val apiURL11: String = res.body.APIURL11
                    val entryURL11: String = res.body.ENTRYURL11
                    val defaultEntryURL = res.body.DEFAULTENTRYURL
                    val defaultApiURL = res.body.DEFAULTAPIURL
                    Log.d(TAG, "onResponse: curVersion : $curVersion")
                    Log.d(TAG, "onResponse: recentVersion : $recentVersion")
                    if (curVersion < minVersion) {
                        Log.d(TAG, "onResponse: need to update")
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
                        putAppPref("e", defaultEntryURL)
                        putAppPref("a", defaultApiURL)
                        if (curVersion >= recentVersion) {
                            Log.d(TAG, "onResponse: apiURL11 : $apiURL11")
                            Log.d(TAG, "onResponse: entryURL11 : $entryURL11")
                            if (entryURL11.isNotBlank() and entryURL11.isNotEmpty()) {
                                putAppPref("e", entryURL11)
                            }
                            if (apiURL11.isNotBlank() and entryURL11.isNotEmpty()) {
                                putAppPref("a", apiURL11)
                            } else {
                                Log.d(TAG, "onResponse: apiurl is empty or blank ")
                            }
                        } else {
                            Log.d(TAG, "onResponse: defaultEntryURL = $defaultEntryURL")
                            Log.d(TAG, "onResponse: defaultApiURL = $defaultApiURL")
                            Log.d(TAG, "onResponse: recentVersion adapted")
                        }
                        checkVersion = true
                        Log.d(TAG, "checkVersion: end")
                        checkLogin()
                    }

                }
            }

            override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                putAppPref("e", entryURL)
                Log.w(TAG, "onFailure: check version error", t)
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
                            Log.d(TAG, "onResponse: Login Token = ${ret.t2}")
                            Log.d(TAG, "onResponse: Access Token = ${ret.token}")
                            Log.d(TAG, "onResponse: AccountPKey = ${ret.userinfo.accountpkey}")
                        } else {
                            emptyUserPreference()
                            Log.d(TAG, "onResponse: login fail message (${head.msg})")
                        }
                        checkLogin = true
                        Log.d(TAG, "checkLogin: end")
                        updateDeviceId()
                    }
                }

                override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                    Log.w(TAG, "onFailure: error", t)
                    emptyUserPreference()
                    checkLogin = true
                    Log.d(TAG, "checkLogin: end")
                    updateDeviceId()
                }
            })
        } else {
            emptyUserPreference()
            checkLogin = true
            Log.d(TAG, "checkLogin: end")
            updateDeviceId()
        }
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
                Log.d(TAG, "updateDeviceId: firebase instance id : Fail")
                updateDeviceId = true
                Log.d(TAG, "updateDeviceId: end")
                subscribeInit()
            }
        }
    }

    private fun subscribeInit() {
        Log.d(TAG, "subscribeInit: start")
        if (getAppPref("subInit") != "Y") {
            Log.d(TAG, "subscribeInit: invoked")
            fm.subscribeToTopic("Notice")
                    .addOnCompleteListener { task0 ->
                        if (task0.isSuccessful) {
                            Log.d(TAG, "subscribeInit: notice")
                            putAppPref("Notice", "Y")
                            Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Notice")} ")
                        }
                        fm.subscribeToTopic("Event")
                                .addOnCompleteListener { task1 ->
                                    if (task1.isSuccessful) {
                                        Log.d(TAG, "subscribeInit: event")
                                        putAppPref("Event", "Y")
                                        Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Event")} ")
                                    }
                                    fm.subscribeToTopic("Series")
                                            .addOnCompleteListener { task2 ->
                                                if (task2.isSuccessful) {
                                                    Log.d(TAG, "subscribeInit: series")
                                                    putAppPref("Series", "Y")
                                                    Log.d(TAG, "subscribeInit: prefs = ${getAppPref("Series")} ")
                                                }
                                                putAppPref("subInit", "Y")
                                                Log.d(TAG, "subscribeInit: end")
                                                subTopic = true
                                                startMainActivity()
                                            }
                                }
                    }
        } else {
            Log.d(TAG, "subscribeInit: end")
            subTopic = true
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        Log.d(TAG, "startMainActivity: start")
        Log.d(TAG, "startMainActivity: n : $networkConnect, v : $checkVersion, l : $checkLogin, d : $updateDeviceId, s: $subTopic")
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
        }
        startActivity(intent)
        finish()
    }

    private fun emptyUserPreference() {
        Log.d(TAG, "emptyUserPreference: empty")
        putAppPref("lt", "")
        putAppPref("lt", "")
        putAppPref("t", "")
        putAppPref("nick", "")
        putAppPref("accountPKey", "")
    }

}