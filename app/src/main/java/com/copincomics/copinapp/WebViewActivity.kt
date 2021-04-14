package com.copincomics.copinapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.copincomics.copinapp.data.Confirm
import com.copincomics.copinapp.data.RetLogin
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : WebViewActivity"
        const val GOOGLE_SIGN_IN = 9001 // account
    }

    // Subscribe Topic State //
//    lateinit var subTopicEvent: String
//    lateinit var subTopicSeries: String

    // Firebase Auth
    lateinit var googleSignInClient: GoogleSignInClient // account
    lateinit var callbackManager: CallbackManager // account

    // Billing Service // pay
    val billingAgent = WebBillingAgent(this)
    var currentUrl: String = AppConfig.shared().entryURL // base




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_web_view) // base
        webView = findViewById(R.id.webView) // base

        setDialog()
        showLoader()

        auth = Firebase.auth // base
        callbackManager = CallbackManager.Factory.create() // account

        // Get Entry URL
        entryURL = AppConfig.shared().entryURL
        currentUrl = entryURL

        // Get Subscribe Topic
//        subTopicEvent = getAppPref("Event")
//        subTopicSeries = getAppPref("Series")
//        Log.d(TAG, "onCreate: subTopicEvent = $subTopicEvent")
//        Log.d(TAG, "onCreate: subTopicSeries = $subTopicSeries")

        val entryIntent = intent // main

        // From Notification
        entryIntent.getStringExtra("link")?.let { link ->
            currentUrl = link
            Log.d(TAG, "onCreate: currentUrl = $link")
        } // main

        // From toon:// URI SCHEME
//        entryIntent.getStringExtra("toon")?.let { toon ->
//            currentUrl = "$entryURL?c=toon&k=$toon"
//            Log.d(TAG, "onCreate: currentUrl = $entryURL?c=toon&k=$toon")
//        } // main

        WebView.setWebContentsDebuggingEnabled(true)

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
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (currentUrl.contains("m=payment") && url?.contains("m_ps.html") == false) {
                    billingAgent.endBillingConnection()
                }

                if (url?.contains("m=setting") == true) {
                    Log.d(TAG, "onPageStarted: this is setting page")
//                    onSettingPageStart()
                }

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
        } // base
        webView.addJavascriptInterface(MyJavascriptInterface(this), "AndroidCopin") // base

        setCookie() // main
    }

    override fun onStart() {
        super.onStart()
        webView.loadUrl(currentUrl) // Each*/

    }

    fun payInit() {
        if(App.config.accountPKey != "") {
            try {
                billingAgent.buildBillingClient()
            } catch (e: Exception) {
                Log.w(TAG, "payInit: error", e)
                Toast.makeText(this, "Error! Please Try Again!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // TODO : PROCESS WHEN "ACCOUNTPKEY IS EMPTY"
            Toast.makeText(this, "Error! Please Try Again!", Toast.LENGTH_SHORT).show()
            finish()
        }
    } // pay

    fun signInWithProvider(providerId: String) {
        val provider: OAuthProvider.Builder = OAuthProvider.newBuilder(providerId)
        val pendingResultTask: Task<AuthResult>? = auth.pendingAuthResult
        if (auth.pendingAuthResult != null) {
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
                    authResult.user?.let { loginAuthServerWithFirebaseUser(it) }
                    Toast.makeText(this, "Auth Success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show()
                }
        }
    } // account

    fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setCookie("copincomics.com", "copinandroid=${App.config.accessToken}")
            setCookie("live.copincomics.com", "copinandroid=${App.config.accessToken}")
        }
    } // account

    private fun signInWithCredential(credential: AuthCredential) {
        showLoader()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        loginAuthServerWithFirebaseUser(it)
                    }
                }
            }
    } // account

    private fun loginAuthServerWithFirebaseUser(user: FirebaseUser) {
        try {
            user.getIdToken(true)
                .addOnSuccessListener { task ->
                    val idToken = task.token
                    if (idToken.isNullOrBlank()) {
                        // TODO : TOKEN IS INVALID
                        return@addOnSuccessListener
                    }

                    Retrofit().accountDAO.processLoginFirebase(idToken).enqueue(object :
                        Callback<RetLogin> {
                        override fun onResponse(
                            call: Call<RetLogin>,
                            response: Response<RetLogin>
                        ) {
                            if (!response.isSuccessful) {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(TAG, "onResponse: error ")
                                return
                            }

                            if (response.body() == null) {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(TAG, "onResponse: error ")
                                return
                            }

                            if (response.body()?.head?.status == "error") {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(TAG, "onResponse: error ")
                                return
                            }

                            val ret = response.body()!!.body
                            App.preferences.refreshToken = ret.t2
                            App.config.accessToken = ret.token
                            App.config.accountPKey = ret.userinfo.accountpkey

                            // Set Identity For Branch
                            setBranchIdentity()
                            dismissLoader()
                            webView.loadUrl("javascript:loginWithFirebase('$idToken', '${App.config.deviceID}', 'android')")
                        }

                        override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                            Log.w(
                                TAG,
                                "onFailure: Auth Server Respond Fail",
                                t
                            )
                            dismissLoader()
                        }
                    })
                }

        } catch (e: Exception) {
            Log.e(TAG, "loginAuthServerWithFirebaseUser: Fail", e)
        }
    } // account

    private fun restartToBaseUrl() {
        webView.loadUrl(entryURL)
    } // base

    fun googleSignIn() {
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

    fun check(purchaseToken: String, sku: String,a:String) {

        when (a) {
            "1" -> { sendBackEnd(purchaseToken, sku) }
            "2" -> {
                billingAgent.consumePurchase(purchaseToken)
                sendBackEnd(purchaseToken, sku)
            }
            else -> {
                sendBackEnd("FERFFER", sku)
            }
        }



    }

    fun sendBackEnd(purchaseToken: String, sku: String) {
        Log.d(TAG, "sendBackEnd: invoked")
        Retrofit().payDAO.confirmReal(purchaseToken, sku).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    when (res.body.result) {
                        "OK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : OK")
                            // billingAgent.consumePurchase(purchaseToken)
                            check(purchaseToken, sku, "1")
                        }
                        "CONSUMED" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Token already consumed")
                            check(purchaseToken, sku, "3")
                        }
                        "OLDOK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Already OK")
                            check(purchaseToken, sku, "2")

                        }
                        else -> {
                            Log.d(TAG, "onResponse: result = ${res.body.result} ")
                            Log.d(TAG, "onResponse: BackEnd Says Not OK")
                            billingAgent.endBillingConnection()
                        }
                    }
                    webView.loadUrl("javascript:payDone()")
                }
            }

            override fun onFailure(call: Call<Confirm>, t: Throwable) {
                Log.e(TAG, "onFailure: Confirm from backend fail", t)

            }
        })
    } // pay

    fun sendBackEndForCheckUnconsumed(purchaseToken: String, sku: String) {
        Log.d(TAG, "sendBackEndForCheckUnconsumed: invoked")
        Retrofit().payDAO.confirm(purchaseToken, sku).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    when (res.body.result) {
                        "OK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : OK")
                            billingAgent.consumePurchaseRetry(purchaseToken)
                        }
                        "CONSUMED" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Token already consumed")
                            billingAgent.consumePurchaseRetry(purchaseToken)
                        }
                        "OLDOK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Already OK")
                            billingAgent.consumePurchaseRetry(purchaseToken)
                        }
                        else -> {
                            Log.d(TAG, "onResponse: BackEnd Says Not OK")
                            billingAgent.endBillingConnection()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Confirm>, t: Throwable) {
                Log.e(TAG, "onFailure: Confirm from backend fail", t)
            }
        })
    } // pay

//    fun onSettingPageStart() {
//        Log.d(TAG, "onSettingPageStart: invoked")
//        val event = getAppPref("Event")
//        val series = getAppPref("Series")
//        Log.d(TAG, "onSettingPageStart: Event = $event, Series = $series")
//        Toast.makeText(this, "Setting Page", Toast.LENGTH_SHORT).show()
//        // TODO : webView.loadUrl("javascript:sendSubState('$event', '$series')")
//    }

//    private fun toggleSubTopic(topic: String) {
//        Log.d(TAG, "toggleSubTopic: invoked")
//        if (getAppPref(topic) == "Y") {
//            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
//                .addOnSuccessListener {
//                    putAppPref(topic, "")
//                    Toast.makeText(this, "$topic notification unsubscribed", Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, "toggleSubTopic: $topic = ${getAppPref(topic)}")
//                    // TODO : CALL JAVASCRIPT subTopicStateChange(topic: String, state: String), topic = topic, state = "N"
//                }
//                .addOnFailureListener {
//                    Log.e(TAG, "toggleSubTopic: error", it)
//                }
//
//        } else {
//            FirebaseMessaging.getInstance().subscribeToTopic(topic)
//                .addOnSuccessListener {
//                    putAppPref(topic, "Y")
//                    Toast.makeText(this, "$topic notification subscribed", Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, "toggleSubTopic: $topic = ${getAppPref(topic)}")
//                    // TODO : CALL JAVASCRIPT subTopicStateChange(topic: String, state: String), topic = topic, state = "Y"
//                }
//                .addOnFailureListener {
//                    Log.e(TAG, "toggleSubTopic: error", it)
//                }
//        }
//    } // base

    fun branchCustomEvent(eventName: String, params: String?) {
        if (params == null || params == "" || params == "undefined" || params == "{}") {
            val branch = Branch.getInstance()
            branch.userCompletedAction(eventName)
            Log.d(TAG, "branchCustomEvent: e = $eventName")
        } else {
            try {
                val jsonObj = JSONObject(params)
                val branch = Branch.getInstance()
                branch.userCompletedAction(eventName, jsonObj)
                Log.d(TAG, "branchCustomEvent: e = $eventName")
                Log.d(TAG, "branchCustomEvent: p = $params")

            } catch (e: Exception) {
                Log.e(TAG, "branchEvent: error", e)
            }
        }

        /* ONLY {} TYPE, NOT [] TYPE JSON_OBJECT */
    }

    fun firebaseCustomEvent(eventName: String, params: String?) {
        try {
            Log.d(TAG, "firebaseCustomEvent: invoked")
            if (params == null || params == "" || params == "undefined" || params == "{}") {
                firebaseAnalytics.logEvent(eventName) {}
                Log.d(TAG, "firebaseCustomEvent: e = $eventName")
            } else {
                val j = JSONObject(params)
                firebaseAnalytics.logEvent(eventName, bundleParams(j))
                Log.d(TAG, "firebaseCustomEvent: e = $eventName")
                Log.d(TAG, "firebaseCustomEvent: p = $params")
            }
        } catch (e: Exception) {}
    } // log

    private fun bundleParams(jsonObject: JSONObject): Bundle {
        val bundle = Bundle()
        val iterator: Iterator<String> = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = jsonObject.getString(key)
            bundle.putString(key, value)
        }
        Log.d(TAG, "bundleParams: bundle = $bundle")
        return bundle
    } // log

    fun facebookCustomEvent(eventName: String, params: String?) {
        val logger: AppEventsLogger = AppEventsLogger.newLogger(this)
        if (params == null || params == "" || params == "undefined" || params == "{}") {
            logger.logEvent(eventName)
            Log.d(TAG, "facebookCustomEvent: e = $eventName")
        } else {
            val j = JSONObject(params)
            logger.logEvent(eventName, bundleParams(j))
            Log.d(TAG, "facebookCustomEvent: e = $eventName")
            Log.d(TAG, "facebookCustomEvent: p = $params")
        }
    }

    fun branchEventPurchase(itemID: String, price: String) {
        Log.d(TAG, "branchEventPurchaseCoin: invoked")
        try {

            val rr = BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
                .setCurrency(CurrencyType.USD)
                .setRevenue(price.toDouble())
                .setDescription(itemID)

            val pp = price.toDoubleOrNull()
            if (pp != null) {
                rr.setRevenue(pp)
            }

            rr.logEvent(this)
        } catch (e: Exception) {
            Log.e(TAG, "branchEventPurchaseCoin", e)
        }
    } // log

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
            currentUrl == entryURL || currentUrl == "$entryURL/" -> {
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
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    restartToBaseUrl()
                }

            }
        }
    } // base

    override fun onResume() {
        registerNetworkCallback(App.networkCallback)
        super.onResume()
    }

    override fun onStop() {
        dismissLoader()
        unregisterNetworkCallback(App.networkCallback)
        super.onStop()
    } // base

    override fun onDestroy() {
        if (currentUrl.contains("m=payment") && billingAgent.billingClient != null) {
            billingAgent.endBillingConnection()
        }
        super.onDestroy()
    }

}