package com.copincomics.copinapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.util.EventLog
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.copincomics.copinapp.data.Confirm
import com.copincomics.copinapp.util.EventTracker
import com.copincomics.copinapp.util.SocialLoginHandler
import com.facebook.CallbackManager
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.*
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

    }

    lateinit var facebookCallbackManager: CallbackManager // account

    // Billing Service // pay
    val billingAgent = WebBillingAgent(this)
    var currentUrl: String = App.config.entryURL // base

    lateinit var loginHandler: SocialLoginHandler
    lateinit var eventTracker: EventTracker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_web_view) // base
        webView = findViewById(R.id.webView) // base

        setDialog()
        showLoader()

        loginHandler = SocialLoginHandler(this)

        facebookCallbackManager = CallbackManager.Factory.create()

        // Get Entry URL
        entryURL = App.config.entryURL
        currentUrl = entryURL


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

    fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setCookie("copincomics.com", "copinandroid=${App.config.accessToken}")
            setCookie("live.copincomics.com", "copinandroid=${App.config.accessToken}")
        }
    } // base

    private fun restartToBaseUrl() {
        webView.loadUrl(entryURL)
    } // base

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
        Retrofit().buildPaymentService().confirmReal(purchaseToken, sku).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    when (res.body.result) {
                        "OK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : OK")
                            billingAgent.consumePurchase(purchaseToken)
//                            check(purchaseToken, sku, "1")
                        }
                        "CONSUMED" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Token already consumed")
                            billingAgent.consumePurchase(purchaseToken)
//                            check(purchaseToken, sku, "3")
                        }
                        "OLDOK" -> {
                            Log.d(TAG, "onResponse: BackEnd Says : Already OK")
                            billingAgent.consumePurchase(purchaseToken)
//                            check(purchaseToken, sku, "2")

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
        Retrofit().buildPaymentService().confirm(purchaseToken, sku).enqueue(object : Callback<Confirm> {
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
    } // tracker

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
    } // tracker

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
    } // tracker

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
    } // tracker

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
    } // tracker

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SocialLoginHandler.GOOGLE_SIGN_IN && resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                Log.d(TAG, "SEQ 2. Credential Received From : Google")
                loginHandler.firebaseSignInWithCredential(credential)
            } catch (e: Exception) {
                Log.w(TAG, "onActivityResult: Google Sign In Failed", e)
            }
        } else {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

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