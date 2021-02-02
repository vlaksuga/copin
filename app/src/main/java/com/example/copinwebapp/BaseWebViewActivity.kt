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
import com.android.billingclient.api.*
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch
import org.json.JSONObject

open class BaseWebViewActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : BaseWebViewActivity"
        const val BASE_URL = "https://stage.copincomics.com/" // base
    }

    // Firebase Auth
    lateinit var auth: FirebaseAuth // base
    lateinit var webView: WebView // base
    var currentUrl: String = BASE_URL // base
    lateinit var accountPKey: String // base
    val account = AccountWebViewActivity() // base
    val pay = PayWebViewActivity() // base


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_web_view) // base
        webView = findViewById(R.id.webView) // base

        init() // base
        loadingDialog.show() // base
        auth = Firebase.auth // base


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

        accountPKey = getAppPref("accountPKey") // base
        Log.d(MainWebViewActivity.TAG, "onCreate: accountPKey = $accountPKey")
        webView.addJavascriptInterface(object : WebViewJavascriptInterface(){}, "AndroidCopin") // base
    }

    private fun restartToBaseUrl() { webView.loadUrl(BASE_URL) } // base

    fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            setCookie("copincomics.com", "copinandroid=${getAppPref("t")}")
            setCookie("live.copincomics.com", "copinandroid=${getAppPref("t")}")
        }
        Log.d(MainActivity.TAG, "setCookie: cookie = copinandroid=${getAppPref("t")}")
    } // base

    fun notificationSetting(channelId: String) {
        Log.d(TAG, "notificationSetting: channelId = $channelId")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mChannel = notificationManager.getNotificationChannel(channelId).id
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, applicationContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, mChannel)
        }
        startActivity(intent)
    } // base

    fun sharePage(msg: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$msg \n $currentUrl")
        }
        val shareIntent = Intent.createChooser(intent, "Copin Comics")
        startActivity(shareIntent)
    } // base

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
            Toast.makeText(this@BaseWebViewActivity, msg, Toast.LENGTH_SHORT).show()
        } // base

        @JavascriptInterface
        fun googleLogin() {
            Log.d(TAG, "googleLogin: invoked")
            account.googleSignIn()
        }

        @JavascriptInterface
        fun facebookLogin() {
            Log.d(TAG, "facebookLogin: invoked")
            account.facebookLoginInApp()
        }

        @JavascriptInterface
        fun twitterLogin() {
            Log.d(TAG, "twitterLogin: invoked")
            account.signInWithProvider("twitter.com")
        }

        @JavascriptInterface
        fun appleLogin() {
            Log.d(TAG, "appleLogin: invoked")
            account.signInWithProvider("apple.com")
        }

        @JavascriptInterface
        fun androidLogout() {
            Log.d(TAG, "androidLogout: invoked")
            account.logout()
        }

        @JavascriptInterface
        fun setLTokens(t: String, lt: String) {
            Log.d(TAG, "setLTokens: invoked")
            putAppPref("lt", lt)
            putAppPref("t", t)
            account.setCookie()
        }

        @JavascriptInterface
        fun goCoinShop() {
            Log.d(TAG, "goCoinShop: invoked")
            pay.startPayActivity()
        }

        @JavascriptInterface
        fun goWebCoinShop() {
            Log.d(TAG, "goWebCoinShop: invoked")
            pay.startWebPayActivity()
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
        fun goNotificationSetting(channelId: String) {
            Log.d(TAG, "goNotificationSetting: invoked")
            notificationSetting(channelId)
        }

        @JavascriptInterface
        fun goSharePage(msg: String) {
            Log.d(TAG, "shareContent: invoked")
            sharePage(msg)
        }

        @JavascriptInterface
        fun initCoin() {
            Log.d(TAG, "initCoin: invoked")
            pay.payInit()
        } // pay

        @JavascriptInterface
        fun selectProduct(id: String) {
            pay.onSelectProduct(id)
        } // pay
    } // common

}