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
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainWebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : BaseWebView"
        const val BASE_URL = "https://stage.copincomics.com/"
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

    val billingAgent = WebBillingAgent(this)

    lateinit var webView: WebView
    var currentUrl: String = BASE_URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Log.d(BaseWebViewActivity.TAG, "onCreate: currentUrl = $link")
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

        fabGoogle.setOnClickListener { startPayActivity() }


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
                Log.d(BaseWebViewActivity.TAG, "onPageStarted: invoked")
                currentUrl = url.toString()
                Log.d(BaseWebViewActivity.TAG, "urlHistory: currentUrl = $currentUrl")
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
                    Log.w(BaseWebViewActivity.TAG, "shouldOverrideUrlLoading: error", e)
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
                Log.d(BaseWebViewActivity.TAG, "touch url : ${url.toString()} ")
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                Log.d(BaseWebViewActivity.TAG, "create : ${webView.url.toString()} ")
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d(BaseWebViewActivity.TAG, "onProgressChanged : ${webView.url.toString()} $newProgress")

            }

            override fun onJsBeforeUnload(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                Log.d(
                    BaseWebViewActivity.TAG,
                    "onJsBeforeUnload : ${webView.url.toString()} ${url.toString()} ${result.toString()}"
                )
                return super.onJsBeforeUnload(view, url, message, result)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                Log.d(BaseWebViewActivity.TAG, "onReceivedTitle : ${webView.url.toString()} ${title.toString()} ")
            }

        }

        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun showToast(msg: String) {
                    Toast.makeText(this@MainWebViewActivity, msg, Toast.LENGTH_SHORT).show()
                }

                @JavascriptInterface
                fun googleLogin() {
                    Log.d(BaseWebViewActivity.TAG, "googleLogin: invoked")
                    googleSignIn()
                }

                @JavascriptInterface
                fun facebookLogin() {
                    Log.d(BaseWebViewActivity.TAG, "facebookLogin: invoked")
                    facebookLoginInApp()
                }

                @JavascriptInterface
                fun twitterLogin() {
                    Log.d(BaseWebViewActivity.TAG, "twitterLogin: invoked")
                    signInWithProvider("twitter.com")
                }

                @JavascriptInterface
                fun appleLogin() {
                    Log.d(BaseWebViewActivity.TAG, "appleLogin: invoked")
                    signInWithProvider("apple.com")
                }

                @JavascriptInterface
                fun androidLogout() {
                    Log.d(BaseWebViewActivity.TAG, "androidLogout: invoked")
                    logout()
                }

                @JavascriptInterface
                fun setLTokens(t: String, lt: String) {
                    Log.d(BaseWebViewActivity.TAG, "setLTokens: invoked")
                    putAppPref("lt", lt)
                    putAppPref("t", t)
                    setCookie()
                }

                @JavascriptInterface
                fun goCoinShop() {
                    Log.d(BaseWebViewActivity.TAG, "goCoinShop: invoked")
                    startPayActivity()
                }

                @JavascriptInterface
                fun goWebCoinShop() {
                    Log.d(BaseWebViewActivity.TAG, "goWebCoinShop: invoked")
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
                    Log.d(BaseWebViewActivity.TAG, "branchEvent: $mapConvertibleJsonString")
                    Log.d(BaseWebViewActivity.TAG, "branchEvent: $eventName")
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
                    Log.d(BaseWebViewActivity.TAG, "goNotificationSetting: invoked")
                    notificationSetting(channelId)
                }

                @JavascriptInterface
                fun goSharePage(msg: String) {
                    Log.d(BaseWebViewActivity.TAG, "shareContent: invoked")
                    sharePage(msg)
                }

                @JavascriptInterface
                fun initCoin() {
                    Log.d(TAG, "initCoin: invoked")
                    payInit()
                }

                @JavascriptInterface
                fun selectProduct(id: String) {
                    Log.d(TAG, "selectProduct: xxx")
                    Toast.makeText(this@MainWebViewActivity, id, Toast.LENGTH_SHORT).show()
                }
            }, "AndroidCopin")



        setCookie()
        webView.loadUrl(currentUrl)
    }

    private fun payInit () {
        Log.d(TAG, "setProductListView: invoked")
        billingAgent.init()
        startBilling()

        val productString = "[{pid:'c10', a:'1.99', b:'',off:'', c:'10'}, {pid:'c30', a:'3.99', b:'10',off:'', c:'30'}, {pid:'c100', a:'5.99', b:'35', off:'', c:'100'}, {pid:'coin500', a:'5.99', b:'200',off:'', c:'500', best:'Y'}, {pid:'c1000', a:'5.99', b:'440',off:'', c:'1000'}]"
        val jsonData = JSONArray(productString)
        webView.post { webView.loadUrl("javascript:setData('$jsonData')")  }
        Log.d(TAG, "setDataTt: jsonData = $jsonData")

    }


    private fun startWebPayActivity() {
        /*webView.loadUrl("https://stage.copincomics.com/?c=tx&m=payment")*/
        val webPayIntent = Intent(this, PayActivity2::class.java)
        startActivity(webPayIntent)
    }

    private fun startPayActivity() {
            val payIntent = Intent(this, PayActivity::class.java)
            startActivity(payIntent)
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
                    Log.w(BaseWebViewActivity.TAG, "signInWithProvider: Pending Result Fail", it)
                }
        } else {
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener { authResult ->
                    Log.d(BaseWebViewActivity.TAG, "signInWithProvider: success")
                    authResult.user?.let { loginAuthServerWithFirebaseUser(it) }
                    Toast.makeText(this, "Auth Success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show()
                    Log.e(BaseWebViewActivity.TAG, "signInWithProvider: Fail", e)
                }
        }
    }

    private fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            setCookie("copincomics.com", "copinandroid=${getAppPref("t")}")
            setCookie("live.copincomics.com", "copinandroid=${getAppPref("t")}")
        }
        Log.d(BaseWebViewActivity.TAG, "setCookie: cookie = copinandroid=${getAppPref("t")}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN && resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                Log.d(BaseWebViewActivity.TAG, "onActivityResult: credential = $credential")
                Log.d(BaseWebViewActivity.TAG, "onActivityResult: requestCode = $requestCode")
                Log.d(BaseWebViewActivity.TAG, "onActivityResult: resultCode = $resultCode ")
                Log.d(BaseWebViewActivity.TAG, "onActivityResult: $data")
                Log.d(BaseWebViewActivity.TAG, "onActivityResult: success")
                signInWithCredential(credential)
            } catch (e: Exception) {
                Log.w(BaseWebViewActivity.TAG, "onActivityResult: Google Sign In Failed", e)
            }
        } else {
            Log.d(BaseWebViewActivity.TAG, "onActivityResult: requestCode = $requestCode")
            Log.d(BaseWebViewActivity.TAG, "onActivityResult: resultCode = $resultCode ")
            Log.d(BaseWebViewActivity.TAG, "onActivityResult: $data")
            Log.d(BaseWebViewActivity.TAG, "onActivityResult: else")
        }
    }

    private fun signInWithCredential(credential: AuthCredential) {
        loadingDialog.show()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(BaseWebViewActivity.TAG, "signInWithCredential: success")
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
                        Log.d(BaseWebViewActivity.TAG, "loginAuthServerWithFirebaseUser: Firebase Id Token : $idToken")
                        if (idToken != null) {
                            repo.accountDAO.processLoginFirebase(idToken).enqueue(object :
                                Callback<RetLogin> {
                                override fun onResponse(
                                    call: Call<RetLogin>,
                                    response: Response<RetLogin>
                                ) {
                                    Log.d(BaseWebViewActivity.TAG, "onResponse: success")

                                    // Set Identity For Branch
                                    val accountPKey =
                                        response.body()?.body?.userinfo?.accountpkey ?: ""
                                    val branch = Branch.getInstance(applicationContext)
                                    branch.setIdentity(accountPKey)
                                    Log.d(BaseWebViewActivity.TAG, "onResponse: accountPKey = $accountPKey")

                                    loadingDialog.dismiss()
                                    webView.loadUrl("javascript:loginWithFirebase('$idToken')")
                                }

                                override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                                    Log.w(BaseWebViewActivity.TAG, "onFailure: Auth Server Respond Fail", t)
                                    loadingDialog.dismiss()
                                }
                            })
                        } else {
                            Log.d(BaseWebViewActivity.TAG, "updateUserInfo: Firebase Id Token Null")
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e(BaseWebViewActivity.TAG, "loginAuthServerWithFirebaseUser: Fail", e)
        }
    }

    private fun restartToBaseUrl() { webView.loadUrl(BaseWebViewActivity.BASE_URL) }

    private fun googleSignIn() {
        Log.d(BaseWebViewActivity.TAG, "googleSignIn: invoked")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        /*webView.settings.userAgentString = System.getProperty("http.agent")*/
        val signInIntent = googleSignInClient.signInIntent
        Log.d(BaseWebViewActivity.TAG, "googleSignIn: invoked2")
        startActivityForResult(signInIntent, BaseWebViewActivity.GOOGLE_SIGN_IN)
        Log.d(BaseWebViewActivity.TAG, "googleSignIn: invoked3")
    }

    private fun facebookLoginInApp() {
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(this, arrayListOf("email", "public_profile"))
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(BaseWebViewActivity.TAG, "facebook: onSuccess")
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                signInWithCredential(credential)
            }

            override fun onCancel() {
                Log.d(BaseWebViewActivity.TAG, "facebook: onCancel")
                Toast.makeText(applicationContext, "Authentication Failed.", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(error: FacebookException) {
                Log.d(BaseWebViewActivity.TAG, "facebook: onError", error)
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
        Log.d(BaseWebViewActivity.TAG, "notificationSetting: channelId = $channelId")
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

    private fun startBilling() {
        try {
            billingAgent.startBillingConnection()
            billingAgent.checkProductUnconsumed()
        } catch (e: Exception) {
            Log.w(PayActivity.TAG, "startBilling: error", e)
            Toast.makeText(this, "Error! Please Try Again!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    override fun onBackPressed() {
        when {
            currentUrl == BaseWebViewActivity.BASE_URL -> {
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

    override fun onStop() {
        super.onStop()
        loadingDialog.dismiss()
    }

}