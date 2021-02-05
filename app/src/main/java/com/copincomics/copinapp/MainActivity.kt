package com.copincomics.copinapp

import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton

open class MainActivity : BaseWebViewActivity() {

    companion object {
        const val TAG = "TAG : MainActivity"
    }

    // dummy buttons // main
    lateinit var fabApple: FloatingActionButton
    lateinit var fabTwitter: FloatingActionButton
    lateinit var fabFacebook: FloatingActionButton
    lateinit var fabGoogle: FloatingActionButton
    lateinit var fabEmail: FloatingActionButton
    lateinit var fabLogout: FloatingActionButton
    lateinit var fabPay: FloatingActionButton
    lateinit var fabEmailSignUp: FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        fabGoogle.setOnClickListener { PayWebViewActivity().branchEventPurchaseCoin(1.5) } // main

        val entryIntent = intent // main

        // From Notification
        entryIntent.getStringExtra("link")?.let { link ->
            currentUrl = link
            Log.d(MainWebViewActivity.TAG, "onCreate: currentUrl = $link")
        } // main

        // From Toon:// URI SCHEME
        entryIntent.getStringExtra("toon")?.let { toon ->
            currentUrl = "${MainWebViewActivity.BASE_URL}?c=toon&k=$toon"
            Log.d(MainWebViewActivity.TAG, "onCreate: currentUrl = ${MainWebViewActivity.BASE_URL}?c=toon&k=$toon")
        } // main

        setCookie() // main
        webView.loadUrl(currentUrl) // Each
    }

}