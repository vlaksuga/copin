package com.copincomics.copinapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.billingclient.api.*
import com.copincomics.copinapp.data.Confirm
import com.copincomics.copinapp.data.RetLogin
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.branch.referral.Branch
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class MainWebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : MainWebView"
        const val GOOGLE_SIGN_IN = 9001 // account
    }

    // Subscribe Topic State //
    lateinit var subTopicEvent: String
    lateinit var subTopicSeries: String

    // Firebase Auth
    lateinit var auth: FirebaseAuth // base
    lateinit var googleSignInClient: GoogleSignInClient // account
    lateinit var callbackManager: CallbackManager // account

    // dummy buttons // main
    lateinit var fabApple: FloatingActionButton
    lateinit var fabTwitter: FloatingActionButton
    lateinit var fabFacebook: FloatingActionButton
    lateinit var fabGoogle: FloatingActionButton
    lateinit var fabEmail: FloatingActionButton
    lateinit var fabLogout: FloatingActionButton
    lateinit var fabPay: FloatingActionButton
    lateinit var fabEmailSignUp: FloatingActionButton

    // Billing Service // pay
    private val billingAgent = WebBillingAgent(this)
    lateinit var accountPKey: String
    var selectedRevenue: Double? = null

    lateinit var webView: WebView // base
    var currentUrl: String = entryURL // base
    var t: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_web_view) // base
        webView = findViewById(R.id.webView) // base

        init() // base
        loadingDialog.show() // base
        auth = Firebase.auth // base
        callbackManager = CallbackManager.Factory.create() // account

        // Get Token
        t = getAppPref("t")
        Log.d(TAG, "onCreate: t = $t")

        // Get Entry URL
        entryURL = getAppPref("e")
        currentUrl = entryURL
        Log.d(TAG, "onCreate: entryURl = $entryURL")
        Log.d(TAG, "onCreate: currentUrl = $currentUrl")

        // Get Subscribe Topic
        subTopicEvent = getAppPref("Event")
        subTopicSeries = getAppPref("Series")
        Log.d(TAG, "onCreate: subTopicEvent = $subTopicEvent")
        Log.d(TAG, "onCreate: subTopicSeries = $subTopicSeries")

        val entryIntent = intent // main

        // From Notification
        entryIntent.getStringExtra("link")?.let { link ->
            currentUrl = link
            Log.d(TAG, "onCreate: currentUrl = $link")
        } // main

        // From Toon:// URI SCHEME
        entryIntent.getStringExtra("toon")?.let { toon ->
            currentUrl = "$entryURL?c=toon&k=$toon"
            Log.d(TAG, "onCreate: currentUrl = $entryURL?c=toon&k=$toon")
        } // main

        // dummy buttons
        fabApple = findViewById(R.id.apple_login_btn) // main
        fabTwitter = findViewById(R.id.twitter_login_btn) // main
        fabFacebook = findViewById(R.id.facebook_login_btn) // main
        fabGoogle = findViewById(R.id.google_login_btn) // main
        fabEmail = findViewById(R.id.email_login_btn) // main
        fabLogout = findViewById(R.id.logout_btn) // main
        fabPay = findViewById(R.id.purchase_btn) // main
        fabEmailSignUp = findViewById(R.id.email_sign_up_btn) // main

        // test listeners for test
        fabGoogle.setOnClickListener { branchEventPurchaseCoin(1.5) } // main
        fabTwitter.setOnClickListener {
            firebaseEventPurchaseCoin(1.99)
        }
        fabFacebook.setOnClickListener {
            toggleSubTopic("Series")
            if(getAppPref("Series") == "Y") {
                fabFacebook.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8800"))
            } else {
                fabFacebook.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#333333"))
            }
        }
        fabEmail.setOnClickListener { firebaseEventSpendCoin("111", "coin", 2.toDouble()) }



        // webView settings // base
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
                    view?.loadUrl(request?.url.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "shouldOverrideUrlLoading: error", e)
                }
                return false
            }
        } // base
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

        } // base
        webView.addJavascriptInterface(object : WebViewJavascriptInterface(){}, "AndroidCopin") // base

        setCookie() // main
        accountPKey = getAppPref("accountPKey") // base
        Log.d(TAG, "onCreate: accountPKey = $accountPKey")
    }

    override fun onStart() {
        super.onStart()
        webView.loadUrl(currentUrl) // Each
    }

    private fun payInit() {
        Log.d(TAG, "payInit: invoked")
        try {
            billingAgent.buildBillingClient()
            billingAgent.startBillingConnection()
            billingAgent.checkProductUnconsumed()
        } catch (e: Exception) {
            Log.w(TAG, "payInit: error", e)
            Toast.makeText(this, "Error! Please Try Again!", Toast.LENGTH_SHORT).show()
            finish()
        }
    } // pay

    private fun startWebPayActivity() {
        /*webView.loadUrl("https://stage.copincomics.com/?c=tx&m=payment")*/
        val webPayIntent = Intent(this, PayActivity2::class.java)
        startActivity(webPayIntent)
    } // pay

    fun startPayActivity() {
            val payIntent = Intent(this, PayActivity::class.java)
            startActivity(payIntent)
    } // pay

    fun signInWithProvider(providerId: String) {
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
    } // account

    fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            setCookie("copincomics.com", "copinandroid=${getAppPref("t")}")
            setCookie("live.copincomics.com", "copinandroid=${getAppPref("t")}")
        }
        Log.d(TAG, "setCookie: cookie = copinandroid=${getAppPref("t")}")
    } // account

    private fun signInWithCredential(credential: AuthCredential) {
        loadingDialog.show()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential: success")
                    val user = auth.currentUser
                    user?.let {
                        loginAuthServerWithFirebaseUser(it)

                        if(it.metadata?.creationTimestamp != it.metadata?.lastSignInTimestamp) {
                            val provider = user.providerData[1].providerId
                            Log.d(TAG, "signInWithCredential: provider = $provider")
                            branchEventCreateAccount(provider)
                            firebaseEventCreateAccount(provider)
                        }
                    }
                }
            }
    } // account

    private fun branchEventCreateAccount(providerId: String) {
        Log.d(TAG, "branchEventCreateAccount: invoked")
        BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)
                .setDescription("create account")
                .addCustomDataProperty("auth provider", "")
                .addCustomDataProperty("accountPKey", accountPKey)
                .logEvent(this)
    }

    private fun firebaseEventCreateAccount(providerId: String) {
        Log.d(TAG, "firebaseEventCreateAccount: invoked, method = $providerId")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, providerId)
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
                                    if (response.body()?.head?.status != "error") {
                                        Log.d(TAG, "onResponse: success")
                                        response.body()?.let {
                                            val ret = it.body
                                            putAppPref("lt", ret.t2)
                                            putAppPref("t", ret.token)
                                            putAppPref("accountPKey", ret.userinfo.accountpkey)

                                            // Set Identity For Branch
                                            accountPKey = ret.userinfo.accountpkey
                                            if(accountPKey != "") {
                                                val branch = Branch.getInstance(applicationContext)
                                                branch.setIdentity(accountPKey)
                                                Log.d(TAG, "onResponse: accountPKey = $accountPKey")
                                            }
                                        }
                                        loadingDialog.dismiss()
                                        webView.loadUrl("javascript:loginWithFirebase('$idToken')")
                                    } else {
                                        Log.d(TAG, "onResponse: error : , ${response.body()!!.head.msg}")
                                    }

                                }

                                override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                                    Log.w(
                                        TAG,
                                        "onFailure: Auth Server Respond Fail",
                                        t
                                    )
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
    } // account

    private fun restartToBaseUrl() { webView.loadUrl(entryURL!!) } // base

    fun googleSignIn() {
        Log.d(TAG, "googleSignIn: invoked")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    } // account

    fun facebookLoginInApp() {
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
    } // account

    fun logout() {
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
    } // account

    private fun sharePage(msg: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$msg \n $currentUrl")
        }
        val shareIntent = Intent.createChooser(intent, "Copin Comics")
        startActivity(shareIntent)
    } // base

    fun sendBackEnd(purchaseToken: String, sku: String) {
        Log.d(TAG, "sendBackEnd: invoked")

        repo.payDAO.confirm(purchaseToken, sku, t!!).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    if (res.body.result == "OK") {
                        Log.d(TAG, "onResponse: BackEnd Says OK")
                        billingAgent.consumePurchase(purchaseToken)
                    } else {
                        Log.d(TAG, "onResponse: BackEnd Says Not OK")
                    }
                    webView.loadUrl("javascript:payDone()")
                    selectedRevenue?.let { rev ->
                        branchEventPurchaseCoin(rev)
                        firebaseEventPurchaseCoin(rev)
                    }
                }
            }

            override fun onFailure(call: Call<Confirm>, t: Throwable) {
                selectedRevenue?.let { rev ->
                    branchEventPurchaseCoin(rev)
                    firebaseEventPurchaseCoin(rev)
                }
                Log.e(TAG, "onFailure: Confirm from backend fail", t)
            }
        })
    } // pay

    fun sendBackEndForCheckUnconsumed(purchaseToken: String, sku: String) {
        Log.d(TAG, "sendBackEndForCheckUnconsumed: invoked")
        repo.payDAO.confirm(purchaseToken, sku, t!!).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    if (res.body.result == "OK") {
                        Log.d(TAG, "onResponse: BackEnd Says OK")
                        billingAgent.consumePurchase(purchaseToken)
                    } else {
                        Log.d(TAG, "onResponse: BackEnd Says Not OK")
                    }
                }
            }

            override fun onFailure(call: Call<Confirm>, t: Throwable) {
                Log.e(TAG, "onFailure: Confirm from backend fail", t)
            }
        })
    } // pay

    fun branchEventPurchaseCoin(revenue: Double) {
        Log.d(TAG, "branchEventPurchaseCoin: invoked")
        BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
                .setCurrency(CurrencyType.USD)
                .setRevenue(revenue)
                .setDescription("Purchase Coin")

                .logEvent(this)
    } // pay

    fun firebaseCustomEvent(eventName: String, params: String) {
        Log.d(TAG, "firebaseCustomEvent: invoked")
        val j = JSONObject(params)
        firebaseAnalytics.logEvent(eventName, bundleParams(j))
    } // log

    private fun firebaseEventSpendCoin(episodeId: String, kind: String, value: Double) {
        Log.d(TAG, "firebaseEventSpendCoin: invoked")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY) {
            param(FirebaseAnalytics.Param.ITEM_NAME, episodeId)
            param(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, kind)
            param(FirebaseAnalytics.Param.VALUE, value)
        }
    } // log

    fun firebaseEventPurchaseCoin(revenue: Double) {
        Log.d(TAG, "firebaseEventPurchase: invoked")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
            param(FirebaseAnalytics.Param.CURRENCY, "USD")
            param(FirebaseAnalytics.Param.AFFILIATION, "Google Store")
            param(FirebaseAnalytics.Param.VALUE, revenue)
            param(FirebaseAnalytics.Param.ITEMS, "coinTest")
            param(FirebaseAnalytics.Param.PRICE, revenue)
        }
    }

    private fun bundleParams(jsonObject: JSONObject) : Bundle {
        val bundle = Bundle()
        val iterator: Iterator<String> = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = jsonObject.getString(key)
            bundle.putString(key, value)
        }
        Log.d(TAG, "bundleParams: bundle = $bundle")
        return bundle
    }

    fun onSettingPageStart() {
        Log.d(TAG, "onSettingPageStart: invoked")
        val event = getAppPref("Event")
        val series = getAppPref("series")
        Log.d(TAG, "onSettingPageStart: Event = $event, Series = $series")
        webView.loadUrl("javascript:sendSubState('$event', '$series')")
    }

    private fun toggleSubTopic(topic: String) {
        Log.d(TAG, "toggleSubTopic: invoked")
        if(getAppPref(topic) == "Y") {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnSuccessListener {
                        putAppPref(topic, "N")
                        Toast.makeText(this, "$topic notification unsubscribed", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "toggleSubTopic: $topic = ${getAppPref(topic)}")
                        // TODO : CALL WEB VIEW SCRIPT subTopicStateChange(topic: String, state: String), topic = topic, state = "N"
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "toggleSubTopic: error", it)
                    }

        } else {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnSuccessListener {
                        putAppPref(topic, "Y")
                        Toast.makeText(this, "$topic notification subscribed", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "toggleSubTopic: $topic = ${getAppPref(topic)}")
                        // TODO : CALL WEB VIEW SCRIPT subTopicStateChange(topic: String, state: String), topic = topic, state = "Y"
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "toggleSubTopic: error", it)
                    }
        }
    } // base

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
    } // account

    override fun onBackPressed() {
        when {
            currentUrl == entryURL -> {
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
                if(webView.canGoBack()) {
                    webView.goBack()
                } else {
                    val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    builder.apply {
                        setMessage("Do you really want to quit?")
                        setPositiveButton("Yes") { _, _ -> super.onBackPressed() }
                        setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        show()
                    }
                }

            }
        }
    } // base

    override fun onStop() {
        super.onStop()
        loadingDialog.dismiss()
    } // base


    open inner class WebViewJavascriptInterface {
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

        @JavascriptInterface
        fun goWebCoinShop() {
            Log.d(TAG, "goWebCoinShop: invoked")
            startWebPayActivity()
        }

        @JavascriptInterface
        fun branchEvent(eventName: String, mapConvertibleJsonString: String?) {
            var obj = ""
            if (mapConvertibleJsonString == null || "" == mapConvertibleJsonString) {
                obj = ""
            } else {
                obj = mapConvertibleJsonString
            }
            Log.d(TAG, "branchEvent: $mapConvertibleJsonString")
            Log.d(TAG, "branchEvent: $eventName")
            var jsonObj = JSONObject("")
            try {
                jsonObj = JSONObject(obj)
                val branch = Branch.getInstance()
                branch.userCompletedAction(eventName, jsonObj)
            } catch (e: Exception) {
                Log.e(TAG, "branchEvent: error", e)
            }
            val branch = Branch.getInstance()
            branch.userCompletedAction(eventName, jsonObj)
            /* ONLY {} TYPE, NOT [] TYPE JSON_OBJECT */
        }

        @JavascriptInterface
        fun gaEvent(eventName: String, params: String) {
            Log.d(TAG, "gaEvent: eventName = $eventName, params: $params")
            firebaseCustomEvent(eventName, params)
        }

        @JavascriptInterface
        fun goSharePage(msg: String) {
            Log.d(TAG, "shareContent: invoked")
            sharePage(msg)
        }

        @JavascriptInterface
        fun initCoin() {
            Log.d(TAG, "initCoin: invoked")
            payInit()
        }

        @JavascriptInterface
        fun selectProduct(id: String) {
            Log.d(TAG, "selectProduct: id = $id")
            val revenueList = arrayListOf(1.99, 3.99, 12.99, 59.99, 109.99)
            val productIndex: Int? = when(id) {
                "c10" -> 0
                "c30" -> 1
                "c100" -> 2
                "c500" -> 3
                "c1000" -> 4
                else -> null
            }
            if (productIndex != null) {
                billingAgent.launchBillingFlow(billingAgent.dataSorted[productIndex])
                selectedRevenue = revenueList[productIndex]
            } else {
                Toast.makeText(this@MainWebViewActivity, "Product Id Invalid", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "selectProduct: Invalid Product Index")
            }
        }

        @JavascriptInterface
        fun toggleSubTopicState(topic: String) {
            Log.d(TAG, "toggleSubTopicState: invoked -> $topic")
            toggleSubTopicState(topic)
        }

        @JavascriptInterface
        fun settingInit() {
            Log.d(TAG, "settingInit: invoked")
            onSettingPageStart()
        }
    } // pay

}