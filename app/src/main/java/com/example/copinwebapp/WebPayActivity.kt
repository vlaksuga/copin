package com.example.copinwebapp

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.Toast
import io.branch.referral.Branch
import org.json.JSONObject

class WebPayActivity : BaseActivity() {

    companion object {
        const val TAG = "TAG : WebPayActivity"
        const val BASE_URL = "https://stage.copincomics.com/?c=tx&m=payment"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_pay)
        webView = findViewById(R.id.webView_pay)

        init()
        loadingDialog.show()

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
                    TAG, "onJsBeforeUnload : ${webView.url.toString()} ${url.toString()} ${result.toString()}"
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
                    Toast.makeText(this@WebPayActivity, msg, Toast.LENGTH_SHORT).show()
                }

                @JavascriptInterface
                fun setLTokens(t: String, lt: String) {
                    Log.d(TAG, "setLTokens: invoked")
                    putAppPref("lt", lt)
                    putAppPref("t", t)
                    setCookie()
                }

                @JavascriptInterface
                fun branchEvent(eventName: String, mapConvertibleJsonString: String?) {
                    var obj = ""
                    if (mapConvertibleJsonString == null || "" == mapConvertibleJsonString) {
                        obj = "{}"
                    } else {
                        obj = mapConvertibleJsonString
                    }
                    Log.d(MainWebViewActivity.TAG, "branchEvent: $mapConvertibleJsonString")
                    Log.d(MainWebViewActivity.TAG, "branchEvent: $eventName")
                    var jsonObj = JSONObject("{}")
                    try {
                        jsonObj = JSONObject(obj)
                    } catch (e: Exception) {

                    }
                    val branch = Branch.getInstance()
                    branch.userCompletedAction(eventName, jsonObj)
                    /* ONLY {} TYPE, NOT [] TYPE JSON_OBJECT */
                }

            }, "AndroidCopin"
        )
        setCookie()
        webView.loadUrl(BASE_URL)
    }

    private fun setCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            setCookie("copincomics.com", "copinandroid=${getAppPref("t")}")
            setCookie("live.copincomics.com", "copinandroid=${getAppPref("t")}")
        }
        Log.d(TAG, "setCookie: cookie = copinandroid=${getAppPref("t")}")
    }
}