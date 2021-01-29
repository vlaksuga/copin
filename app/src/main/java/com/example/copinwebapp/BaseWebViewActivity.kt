package com.example.copinwebapp

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.copinwebapp.data.RetLogin
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class BaseWebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : BaseWebView"
        const val BASE_URL = "https://stage.copincomics.com/"
        const val NEED_LOGIN = "https://stage.copincomics.com/?c=login"
        const val GOOGLE_SIGN_IN = 9001
    }

    // Firebase Auth
    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var callbackManager: CallbackManager

    // dummy buttons
    lateinit var fabApple: FloatingActionButton
    lateinit var fabTwitter: FloatingActionButton
    lateinit var fabFacebook: FloatingActionButton
    lateinit var fabGoogle: FloatingActionButton
    lateinit var fabEmail: FloatingActionButton
    lateinit var fabLogout: FloatingActionButton
    lateinit var fabPay: FloatingActionButton
    lateinit var fabEmailSignUp: FloatingActionButton

    lateinit var webView: WebView
    var currentUrl: String = BASE_URL

    fun baseWebViewInit() {
        setContentView(R.layout.activity_main_web_view)
        webView = findViewById(R.id.webView)

        init()
        loadingDialog.show()
        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()

        // From Notification
        val entryIntent = intent
        entryIntent.getStringExtra("link")?.let { link ->
            currentUrl = link
            Log.d(TAG, "onCreate: currentUrl = $link")
        }

        // dummy buttons
        fabApple = findViewById(R.id.apple_login_btn)
        fabTwitter = findViewById(R.id.twitter_login_btn)
        fabFacebook = findViewById(R.id.facebook_login_btn)
        fabGoogle = findViewById(R.id.google_login_btn)
        fabEmail = findViewById(R.id.email_login_btn)
        fabLogout = findViewById(R.id.logout_btn)
        fabPay = findViewById(R.id.purchase_btn)
        fabEmailSignUp = findViewById(R.id.email_sign_up_btn)


        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.setSupportZoom(false)
        webView.settings.useWideViewPort = true
        webView.settings.setSupportMultipleWindows(false)
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webViewClient = object : WebViewClient() {
            var flag = true

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                flag = true

                loadingDialog.show()
                Log.d(TAG, "onPageStarted: invoked")
                currentUrl = url.toString()
                Log.d(TAG, "urlHistory: currentUrl = $currentUrl")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingDialog.dismiss()
            }

            override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
            ): Boolean {
                super.shouldOverrideUrlLoading(view, request)
                try {
                    view?.let {
                        it.loadUrl(request?.url.toString())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "shouldOverrideUrlLoading: error", e)
                }
                return false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTouchIconUrl(
                    view: WebView?,
                    url: String?,
                    precomposed: Boolean
            ) {
                super.onReceivedTouchIconUrl(view, url, precomposed)
                Log.d(TAG, "touch url : ${url.toString()} ")
            }

            override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
            ): Boolean {
                Log.d(TAG, "create : ${webView.url.toString()} ")
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d(TAG, "onProgressChanged : ${webView.url.toString()} $newProgress")

            }

            override fun onJsBeforeUnload(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
            ): Boolean {
                Log.d(
                        TAG,
                        "onJsBeforeUnload : ${webView.url.toString()} ${url.toString()} ${result.toString()}"
                )
                return super.onJsBeforeUnload(view, url, message, result)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                Log.d(TAG, "onReceivedTitle : ${webView.url.toString()} ${title.toString()} ")
            }

        }

        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun showToast(msg: String) {
                    Toast.makeText(this@BaseWebViewActivity, msg, Toast.LENGTH_SHORT).show()
                }

                @JavascriptInterface
                fun googleLogin() {
                    Log.d(TAG, "googleLogin: invoked")
                    googleSignIn()
                }

                @JavascriptInterface
                fun facebookLogin() {
                    Log.d(TAG, "facebookLogin: invoked")
                    facebookLoginInApp()
                }

                @JavascriptInterface
                fun twitterLogin() {
                    Log.d(TAG, "twitterLogin: invoked")
                    signInWithProvider("twitter.com")
                }

                @JavascriptInterface
                fun appleLogin() {
                    Log.d(TAG, "appleLogin: invoked")
                    signInWithProvider("apple.com")
                }

                @JavascriptInterface
                fun androidLogout() {
                    Log.d(TAG, "androidLogout: invoked")
                    logout()
                }

                @JavascriptInterface
                fun setLTokens(t: String, lt: String) {
                    Log.d(TAG, "setLTokens: invoked")
                    putAppPref("lt", lt)
                    putAppPref("t", t)
                    setCookie()
                }

                @JavascriptInterface
                fun goCoinShop() {
                    Log.d(TAG, "goCoinShop: invoked")
                    startPayActivity()
                }

                @JavascriptInterface
                fun goWebCoinShop() {
                    Log.d(TAG, "goWebCoinShop: invoked")
                    startWebPayActivity()
                }

                @JavascriptInterface
                fun branchEvent(eventName: String, mapConvertibleJsonString: String?) {
                    var obj = ""
                    if (mapConvertibleJsonString == null || "" == mapConvertibleJsonString) {
                        obj = "{}"
                    } else {
                        obj = mapConvertibleJsonString
                    }
                    Log.d(TAG, "branchEvent: $mapConvertibleJsonString")
                    Log.d(TAG, "branchEvent: $eventName")
                    var jsonObj = JSONObject("{}")
                    try {
                        jsonObj = JSONObject(obj)
                    } catch (e: Exception) {

                    }
                    val branch = Branch.getInstance()
                    branch.userCompletedAction(eventName, jsonObj)
                    /* ONLY {} TYPE, NOT [] TYPE JSON_OBJECT */
                }

                @JavascriptInterface
                fun goNotificationSetting(channelId: String) {
                    Log.d(TAG, "goNotificationSetting: invoked")
                    notificationSetting(channelId)
                }

                @JavascriptInterface
                fun goSharePage(msg: String) {
                    Log.d(TAG, "shareContent: invoked")
                    sharePage(msg)
                }

            }, "AndroidCopin"
        )
        setCookie()
    }

    private fun startWebPayActivity() {
        webView.loadUrl("https://stage.copincomics.com/?c=tx&m=payment")
        /*val webPayIntent = Intent(this, WebPayActivity::class.java)
        startActivity(webPayIntent)*/
    }

    private fun startPayActivity() {
        if(getAppPref("t") == "") {
            webView.loadUrl(NEED_LOGIN)
        } else {
            val payIntent = Intent(this, PayActivity::class.java)
            startActivity(payIntent)
        }
    }


    private fun signInWithProvider(providerId: String) {
        val provider: OAuthProvider.Builder = OAuthProvider.newBuilder(providerId)
        val pendingResultTask: Task<AuthResult>? = auth.pendingAuthResult
        if(auth.pendingAuthResult != null) {
            pendingResultTask!!
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { loginAuthServerWithFirebaseUser(it) }
                    Toast.makeText(this, "Auth Success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.w(TAG, "signInWithProvider: Pending Result Fail", it)
                }
        } else {
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener { authResult ->
                    Log.d(TAG, "signInWithProvider: success")
                    authResult.user?.let { loginAuthServerWithFirebaseUser(it) }
                    Toast.makeText(this, "Auth Success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "signInWithProvider: Fail", e)
                }
        }
    }




    fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            setCookie("copincomics.com", "copinandroid=${getAppPref("t")}")
            setCookie("live.copincomics.com", "copinandroid=${getAppPref("t")}")
        }
        Log.d(TAG, "setCookie: cookie = copinandroid=${getAppPref("t")}")
    }

    override fun onBackPressed() {
        when {
            currentUrl == BASE_URL -> {
                val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                builder.apply {
                    setMessage("Do you really want to quit?")
                    setPositiveButton("Yes") { _, _ -> super.onBackPressed() }
                    setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    show()
                }
            }

            // escape from auth api
            currentUrl.contains("facebook.com") -> {
                restartToBaseUrl()
            }
            currentUrl.contains("accounts.google.com") -> {
                restartToBaseUrl()
            }
            currentUrl.contains("api.twitter.com") -> {
                restartToBaseUrl()
            }
            currentUrl.contains("appleid.apple.com") -> {
                restartToBaseUrl()
            }

            else -> {
                webView.goBack()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN && resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                Log.d(TAG, "onActivityResult: credential = $credential")
                Log.d(TAG, "onActivityResult: requestCode = $requestCode")
                Log.d(TAG, "onActivityResult: resultCode = $resultCode ")
                Log.d(TAG, "onActivityResult: $data")
                Log.d(TAG, "onActivityResult: success")
                signInWithCredential(credential)
            } catch (e: Exception) {
                Log.w(TAG, "onActivityResult: Google Sign In Failed", e)
            }
        } else {
            Log.d(TAG, "onActivityResult: requestCode = $requestCode")
            Log.d(TAG, "onActivityResult: resultCode = $resultCode ")
            Log.d(TAG, "onActivityResult: $data")
            Log.d(TAG, "onActivityResult: else")
        }
    }

    private fun signInWithCredential(credential: AuthCredential) {
        loadingDialog.show()
        auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential: success")
                        val user = auth.currentUser
                        user?.let { loginAuthServerWithFirebaseUser(it) }
                    }
                }
    }

    private fun loginAuthServerWithFirebaseUser(user: FirebaseUser) {
        try {
            user.getIdToken(true)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val idToken = task.result.token
                            Log.d(TAG, "loginAuthServerWithFirebaseUser: Firebase Id Token : $idToken")
                            if (idToken != null) {
                                repo.accountDAO.processLoginFirebase(idToken).enqueue(object :
                                    Callback<RetLogin> {
                                    override fun onResponse(
                                        call: Call<RetLogin>,
                                        response: Response<RetLogin>
                                    ) {
                                        Log.d(TAG, "onResponse: success")

                                        // Set Identity For Branch
                                        val accountPKey =
                                            response.body()?.body?.userinfo?.accountpkey ?: ""
                                        val branch = Branch.getInstance(applicationContext)
                                        branch.setIdentity(accountPKey)
                                        Log.d(TAG, "onResponse: accountPKey = $accountPKey")

                                        loadingDialog.dismiss()
                                        webView.loadUrl("javascript:loginWithFirebase('$idToken')")
                                    }

                                    override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                                        Log.w(TAG, "onFailure: Auth Server Respond Fail", t)
                                        loadingDialog.dismiss()
                                    }
                                })
                            } else {
                                Log.d(TAG, "updateUserInfo: Firebase Id Token Null")
                            }
                        }
                    }

        } catch (e: Exception) {
            Log.e(TAG, "loginAuthServerWithFirebaseUser: Fail", e)
        }
    }

    fun restartToBaseUrl() { webView.loadUrl(BASE_URL) }

    private fun googleSignIn() {
        Log.d(TAG, "googleSignIn: invoked")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        /*webView.settings.userAgentString = System.getProperty("http.agent")*/
        val signInIntent = googleSignInClient.signInIntent
        Log.d(TAG, "googleSignIn: invoked2")
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
        Log.d(TAG, "googleSignIn: invoked3")
    }

    private fun facebookLoginInApp() {
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(this, arrayListOf("email", "public_profile"))
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(TAG, "facebook: onSuccess")
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                signInWithCredential(credential)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook: onCancel")
                Toast.makeText(applicationContext, "Authentication Failed.", Toast.LENGTH_SHORT)
                        .show()
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook: onError", error)
                Toast.makeText(
                        applicationContext,
                        "Authentication Failed. ${error.message}",
                        Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun logout() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener {
            putAppPref("lt", "")
            putAppPref("l", "")
            val intent = Intent(applicationContext, EntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun notificationSetting(channelId: String) {
        Log.d(TAG, "notificationSetting: channelId = $channelId")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mChannel = notificationManager.getNotificationChannel(channelId).id
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, applicationContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, mChannel)
        }
        startActivity(intent)
    }


    private fun sharePage(msg: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$msg \n $currentUrl")
        }
        val shareIntent = Intent.createChooser(intent, "Copin Comics")
        startActivity(shareIntent)
    }

    override fun onStop() {
        super.onStop()
        loadingDialog.dismiss()
    }

}