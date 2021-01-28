package com.example.copinwebapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.copinwebapp.data.RetLogin
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthProvider
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainWebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : Main"
        const val BASE_URL = "https://stage.copincomics.com/?v=2021012700111"
        const val NEED_LOGIN = "https://stage.copincomics.com/?c=login&v=2021012700111"
        const val GOOGLE_SIGN_IN = 9001
    }

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    // dummy buttons
    private lateinit var fabApple: FloatingActionButton
    private lateinit var fabTwitter: FloatingActionButton
    private lateinit var fabFacebook: FloatingActionButton
    private lateinit var fabGoogle: FloatingActionButton
    private lateinit var fabEmail: FloatingActionButton
    private lateinit var fabLogout: FloatingActionButton
    private lateinit var fabPay: FloatingActionButton
    private lateinit var fabEmailSignUp: FloatingActionButton

    lateinit var webView: WebView
    var currentUrl: String = BASE_URL
    val userAgent: String = WebSettings.getDefaultUserAgent(this)

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

        // dummy listener
        fabApple.setOnClickListener { signInWithProvider("apple.com") }
        fabTwitter.setOnClickListener { signInWithProvider("twitter.com") }
        fabFacebook.setOnClickListener { facebookLoginInApp() }
        fabGoogle.setOnClickListener { googleSignIn() }
        fabEmail.setOnClickListener { signInWithEmail("tekiteki@naver.com", "password1234@") }
        fabLogout.setOnClickListener { logout() }
        fabPay.setOnClickListener { startPayActivity() }
        fabEmailSignUp.setOnClickListener { createUserWithEmailAndPassword("tekiteki@naver.com", "password1234@") }



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
                    Toast.makeText(this@MainWebViewActivity, msg, Toast.LENGTH_SHORT).show()
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

            }, "AndroidCopin"
        )
        setCookie()
        webView.loadUrl(currentUrl)
    }

    private fun startPayActivity() {
        if(getAppPref("t") != "") {
            val payIntent = Intent(this, PayActivity::class.java)
            startActivity(payIntent)
        } else {
            webView.loadUrl(NEED_LOGIN)
        }
    }


    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail: Success")
                    Log.d(TAG, "signInWithEmail: user = ${auth.currentUser} ")
                    auth.currentUser?.let { loginAuthServerWithFirebaseUser(it) }
                } else {
                    Log.e(TAG, "signInWithEmail: fail", task.exception)
                }
            }
    }

    private fun signInWithProvider(providerId: String) {
        val customScopes = ArrayList<String>()
        auth.startActivityForSignInWithProvider(this, oAuthProvider(providerId, auth) {
            scopes = customScopes
        })
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { loginAuthServerWithFirebaseUser(it) }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "signInWithProvider: Fail", e)
                }
    }

    private fun createUserWithEmailAndPassword(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmailAndPassword: success => ${auth.currentUser}")
                        val user: FirebaseUser? = auth.currentUser
                        if (user != null) {
                            loginAuthServerWithFirebaseUser(user)
                        } else {
                            Log.d(TAG, "createUserWithEmailAndPassword: Fail User is null")
                        }
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
        Log.d(TAG, "setCookieAndLoadUrl: cookie = copinandroid=${getAppPref("t")}")
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
                webView.loadUrl("javascript:window.history.back()")
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
                                        webView.loadUrl("javascript:loginWithFirebase('$idToken')")
                                    }

                                    override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                                        Log.w(TAG, "onFailure: Auth Server Respond Fail", t)
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

    private fun restartToBaseUrl() { webView.loadUrl(BASE_URL) }

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



}