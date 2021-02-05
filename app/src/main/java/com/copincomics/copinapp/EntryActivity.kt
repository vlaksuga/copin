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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: start")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)

        init()

        /* INTENT EXTRA */
        val intent = intent
        intent.extras?.let {
            link = it.getString("link") // intent from FCM
            toon = it.getString("toon") // intent from deep link
            Log.d(TAG, "intent extra link = $link")
        }

        /* CHECK NETWORK CONNECTION */
        if (networkConnection(this)) {
            networkConnect = true
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Network Error")
            builder.setPositiveButton("Confirm") { _, _ -> finish() }
            builder.setCancelable(false)
            builder.show()
        }

        /* UPDATE DEVICE ID */
        updateDeviceId = updateDeviceId()

        /* CREATE NOTIFICATION CHANNELS */
        createNotificationChannels()

        /* SUBSCRIBE TO TOPIC */
        if(getAppPref("subInit") != "Y") {
            subscribeInit()
            Log.d(TAG, "onCreate: subscribeInit invoked")
        }


    }

    private fun subscribeInit() {
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
    }

    override fun onResume() {
        Log.d(TAG, "onResume: start")
        super.onResume()

        /* CHECK LOGIN */
        checkLogin = checkLogin()

        /* CHECK APP VERSION */
        checkVersion = checkVersion()
    }

    override fun onStart() {
        super.onStart()
        try {
            Branch.enableLogging()
            /*Branch.enableTestMode()*/ // For Test Mode
            IntegrationValidator.validate(this)
            Branch.sessionBuilder(this)
                    .withCallback { referringParams, _ -> Log.d(TAG, "Branch Session Builder: $referringParams")}
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
            Branch.sessionBuilder(this).withCallback { _, _ ->  startActivity(intent)}.reInit()
        } catch (e: Exception) {
            Log.w(TAG, "onNewIntent: Branch ReInit Failed", e)
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val commonChannelId = applicationContext.getString(R.string.default_notification_channel_id)
        val commonChannel = NotificationChannel(commonChannelId, commonChannelId, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(commonChannel)
        Log.d(TAG, "createNotificationChannel: Created")
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

    private fun updateDeviceId(): Boolean {
        val fcm = FirebaseMessaging.getInstance()
        fcm.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let {
                    putAppPref("deviceId", it)
                    Log.d(TAG, "updateDeviceId: firebase instance id : $it")
                    Log.d(TAG, "updateDeviceId: ${getAppPref("deviceId")}")
                }
            } else {
                putAppPref("deviceId", "fail_to_get_FCM_instance_token")
                Log.d(TAG, "updateDeviceId: firebase instance id : Fail")
            }
        }
        fcm.isAutoInitEnabled = false
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        return true
    }

    private fun checkVersion(): Boolean {

        try {
            repo.accountDAO.requestCheckVersion().enqueue(object : Callback<CheckVersion> {
                override fun onResponse(
                    call: Call<CheckVersion>,
                    response: Response<CheckVersion>
                ) {
                    response.body()?.let { res ->
                        val minVersion = res.body.ANDROIDMIN.toInt()
                        val curVersion = currentVersion()
                        if (curVersion >= minVersion) {
                            Log.d(TAG, "onResponse: minVersion : $minVersion")
                            Log.d(TAG, "onResponse: curVersion : $curVersion")
                            executeNext()
                        } else {
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
                                .setNegativeButton("Ignore") { dialog , _ ->
                                    dialog.dismiss()
                                    executeNext()
                                }
                                .show()
                        }
                    }
                }

                override fun onFailure(call: Call<CheckVersion>, t: Throwable) {
                    Log.w(TAG, "onFailure: check version error", t)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "checkVersion: check version error", e)
        }
        return true
    }

    private fun currentVersion(): Int {
        var version = 99
        var packageInfo: PackageInfo? = null
        try {
            packageInfo =
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            packageInfo?.let {
                val versionName = it.versionName
                version = it.versionCode
                putAppPref("appVersion_view", versionName)
                putAppPref("appVersion", version.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "packageInfo error", e)
        }
        return version
    }

    private fun checkLogin(): Boolean {
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

                        } catch (e: Exception) {
                            emptyUserPreference()
                            Log.w(TAG, "onResponse: error", e)
                        }
                        executeNext()
                    }

                    override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                        Log.w(TAG, "onFailure: error", t)
                        emptyUserPreference()
                        executeNext()
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "checkLogin: check login error", e)
                emptyUserPreference()
                executeNext()
            }
        } else {
            emptyUserPreference()
            executeNext()
        }
        Log.d(TAG, "checkLogin: $checkLogin")
        return true
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
        Log.d(TAG, "executeNext: n : $networkConnect, v : $checkVersion, l : $checkLogin, d : $updateDeviceId ")
        if(networkConnect and checkVersion and checkLogin and updateDeviceId) {

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