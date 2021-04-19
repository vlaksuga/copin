package com.copincomics.copinapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.copincomics.copinapp.data.*
import com.google.firebase.messaging.FirebaseMessaging
import io.branch.referral.Branch
import io.branch.referral.validators.IntegrationValidator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body

class EntryActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : Entry"
    }

    private var link: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)

        if (!checkNetworkConnection(this)) {
            showCustomAlert("net")
            return
        }

        getVersionFromApi { version ->
            setVersionToApp(version)
            getFCMToken { token ->
                App.config.deviceID = token
                autoLoginWithRefreshToken { user ->
                    setUserConfig(user)
                    setBranchIdentity(user)
                    startMainActivity()
                    user
                }
                token
            }
            version
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
        Log.d(TAG, "SEQ 1. checkNetworkConnection...")
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
        Log.d(TAG, "SEQ 1. OK")
        return result
    }

    private fun getVersionFromApi(callback: (result: VersionBody) -> VersionBody) {
        // TODO : TO MAP
        Log.d(TAG, "SEQ 2. getVersionFromApi...")
        Retrofit().buildApiService().getVersion().enqueue(object : Callback<Version> {
            override fun onResponse(
                call: Call<Version>,
                response: Response<Version>
            ) {
                if (response.body() == null) {
                    callback.invoke(getHardCodedVersion())
                    return
                }
                Log.d(TAG, "SEQ 2. OK")
                callback.invoke(response.body()!!.body)
            }

            override fun onFailure(call: Call<Version>, t: Throwable) {
                callback.invoke(getHardCodedVersion())
            }
        })
    }

    private fun setVersionToApp(versionBody: VersionBody) {
        Log.d(TAG, "SEQ 3. setVersionToApp...")
        val minVersion = versionBody.ANDROIDMIN.toInt()
        val recentVersion = versionBody.ANDROIDRECENT.toInt()
        val apiURL11: String = versionBody.APIURL11
        val entryURL11: String = versionBody.ENTRYURL11
        val defaultApiURL = versionBody.DEFAULTAPIURL
        val defaultEntryURL = versionBody.DEFAULTENTRYURL

        if (App.currentVersion < minVersion) {
            showCustomAlert("update")
            return
        }

        App.config.entryURL = if (App.currentVersion > recentVersion) entryURL11 else defaultEntryURL
        App.config.apiURL = if (App.currentVersion > recentVersion) apiURL11  else defaultApiURL
        Log.d(TAG, "SEQ 3. OK")
    }

    private fun getFCMToken(callback: (token: String) -> String) {
        Log.d(TAG, "SEQ 4. getFCMToken...")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token ->
                    Log.d(TAG, "SEQ 4. OK")
                    callback.invoke(token)
                }
            }
            if(task.isCanceled) {
                callback.invoke(App.config.deviceID)
            }
        }
    }

    private fun autoLoginWithRefreshToken(callback: (result: BodyRetLogin) -> BodyRetLogin) {
        Log.d(TAG, "SEQ 5. autoLoginWithRefreshToken...")
        val refreshToken = App.preferences.refreshToken

        if (refreshToken == "") {
            callback.invoke(getHardCodedUser())
            return
        }

        Retrofit().buildApiService().processLoginByToken(lt = refreshToken).enqueue(object :
            Callback<RetLogin> {
            override fun onResponse(call: Call<RetLogin>, response: Response<RetLogin>) {
                if (response.body() == null) {
                    callback.invoke(getHardCodedUser())
                    return
                }
                Log.d(TAG, "SEQ 5. OK")
                callback.invoke(response.body()!!.body)
            }

            override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                callback.invoke(getHardCodedUser())
            }
        })
    }

    private fun setUserConfig(user: BodyRetLogin) {
        Log.d(TAG, "SEQ 6. setUserConfig...")
        App.preferences.refreshToken = user.t2
        App.config.accessToken = user.token
        App.config.accountPKey = user.userinfo.accountpkey
        Log.d(TAG, "SEQ 6. OK")
    }

    fun getHardCodedVersion() : VersionBody {
        return VersionBody(
            "1",
            "5",
            "99",
            "https://api.copincomics.com/",
            "https://copincomics.com/",
            "https://api.copincomics.com/",
            "https://copincomics.com/"
        )
    }

    fun getHardCodedUser() : BodyRetLogin {
        return BodyRetLogin(
            "",
            UserInfoForm(
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            ),
            ""
        )
    }

    private fun startMainActivity() {
        Log.d(TAG, "SEQ Final. startMainActivity...")
        val mainActivityIntent = Intent(this, WebViewActivity::class.java)
        if (link != null) {
            intent.extras?.let { bundle ->
                link = bundle.getString("link")
            }
            mainActivityIntent.putExtra("link", link)
        }
        Log.d(TAG, "SEQ Final. OK")
        startActivity(mainActivityIntent)
    }

}