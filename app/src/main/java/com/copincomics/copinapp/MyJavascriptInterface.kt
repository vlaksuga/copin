package com.copincomics.copinapp

import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.copincomics.copinapp.util.SocialLoginHandler

open class MyJavascriptInterface (private val activity: WebViewActivity) {

    private val loginHandler: SocialLoginHandler = SocialLoginHandler(activity)

    @JavascriptInterface
    fun showToast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun googleLogin() {
        Log.d(WebViewActivity.TAG, "googleLogin: invoked")
        loginHandler.googleSignIn()
    }

    @JavascriptInterface
    fun facebookLogin() {
        Log.d(WebViewActivity.TAG, "facebookLogin: invoked")
        loginHandler.facebookSignIn()
    }

    @JavascriptInterface
    fun twitterLogin() {
        Log.d(WebViewActivity.TAG, "twitterLogin: invoked")
        loginHandler.signInWithProvider("twitter.com")
    }

    @JavascriptInterface
    fun appleLogin() {
        Log.d(WebViewActivity.TAG, "appleLogin: invoked")
        loginHandler.signInWithProvider("apple.com")
    }

    @JavascriptInterface
    fun setLTokens(t: String, lt: String) {
        Log.d(WebViewActivity.TAG, "setLTokens: invoked!")
        App.preferences.refreshToken = lt
        App.config.accessToken = t
        activity.setCookie()
    }

    @JavascriptInterface
    fun allEvent(eventName: String, params: String) {
        Log.d(WebViewActivity.TAG, "allEvent: e = $eventName, p = $params invoked")
        activity.branchCustomEvent(eventName, params)
        activity.firebaseCustomEvent(eventName, params)
        activity.facebookCustomEvent(eventName, params)
    }

    @JavascriptInterface
    fun allEventWithParams(eventName: String, params: String) {
        activity.branchCustomEvent(eventName, params)
        activity.firebaseCustomEvent(eventName, params)
        activity.facebookCustomEvent(eventName, params)
    }

    @JavascriptInterface
    fun branchEvent(eventName: String, params: String) {
        Log.d(WebViewActivity.TAG, "branchEvent: eventName = $eventName")
        Log.d(WebViewActivity.TAG, "branchEvent: params = $params")
        activity.branchCustomEvent(eventName, params)
    }

    @JavascriptInterface
    fun firebaseEvent(eventName: String, params: String) {
        Log.d(WebViewActivity.TAG, "firebaseEvent: eventName = $eventName")
        Log.d(WebViewActivity.TAG, "firebaseEvent: params = $params")
        activity.firebaseCustomEvent(eventName, params)
    }

    @JavascriptInterface
    fun facebookEvent(eventName: String, params: String) {
        Log.d(WebViewActivity.TAG, "facebookEvent: eventName = $eventName")
        Log.d(WebViewActivity.TAG, "facebookEvent: params = $params")
        activity.facebookCustomEvent(eventName, params)
    }

    @JavascriptInterface
    fun branchEventPurchaseCoin(itemID: String, price: String) {
        Log.d(WebViewActivity.TAG, "branchPurchaseCoin: invoked, item = $itemID, price = $price")
        activity.branchEventPurchase(itemID, price)
    }

    @JavascriptInterface
    fun initCoin() {
        Log.d(WebViewActivity.TAG, "initCoin: invoked")
        activity.payInit()
    }

    @JavascriptInterface
    fun selectProduct(id: String) {
        Log.d(WebViewActivity.TAG, "selectProduct: id = $id")
        activity.billingAgent.dataSorted[id]?.let { activity.billingAgent.launchBillingFlow(it) } ?: {
            Toast.makeText(activity, "Product Id invalid", Toast.LENGTH_SHORT).show()
        } ()
    }
}