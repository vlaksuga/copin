package com.copincomics.copinapp.util

import android.util.Log
import android.widget.Toast
import com.copincomics.copinapp.App
import com.copincomics.copinapp.R
import com.copincomics.copinapp.Retrofit
import com.copincomics.copinapp.WebViewActivity
import com.copincomics.copinapp.data.RetLogin
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SocialLoginHandler(val activity: WebViewActivity) {
    companion object {
        const val TAG = "TAG : SocialLogin"
        const val GOOGLE_SIGN_IN = 9001 // account
    }

    lateinit var googleSignInClient: GoogleSignInClient
    private val auth: FirebaseAuth = Firebase.auth

    fun googleSignIn() {
        Log.d(TAG, "SEQ 1. Login Provider : google.com")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    fun facebookSignIn() {
        Log.d(TAG, "SEQ 1. Login Provider : facebook.com")
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(activity, arrayListOf("email", "public_profile"))
        loginManager.registerCallback(activity.facebookCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(TAG, "facebook: onSuccess")
                Log.d(TAG, "SEQ 2. Credential Received From : Facebook")
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                firebaseSignInWithCredential(credential)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook: onCancel")
                Toast.makeText(activity, "Authentication Failed.", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(error: FacebookException) {
                Log.d(WebViewActivity.TAG, "facebook: onError", error)
                Toast.makeText(
                    activity,
                    "Authentication Failed. ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun signInWithProvider(providerId: String) {
        Log.d(TAG, "SEQ 1. Login Provider : $providerId")
        val provider: OAuthProvider.Builder = OAuthProvider.newBuilder(providerId)
        val pendingResultTask: Task<AuthResult>? = auth.pendingAuthResult
        if (auth.pendingAuthResult != null) {
            pendingResultTask!!
                .addOnSuccessListener { authResult ->
                    Log.d(TAG, "SEQ 3. Firebase Sign In Success")
                    authResult.user?.let { loginWithFirebaseUser(it) }
                }
                .addOnFailureListener {
                    Log.w(TAG, "signInWithProvider: Pending Result Fail", it)
                }
        } else {
            auth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener { authResult ->
                    authResult.user?.let {
                        Log.d(TAG, "SEQ 3. Firebase Sign In Success")
                        loginWithFirebaseUser(it)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Auth Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun firebaseSignInWithCredential(credential: AuthCredential) {
        activity.showLoader()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        Log.d(TAG, "SEQ 3. Firebase Sign In Success")
                        loginWithFirebaseUser(it)
                    }
                }
            }
    }

    private fun loginWithFirebaseUser(user: FirebaseUser) {
        try {
            user.getIdToken(true)
                .addOnSuccessListener { task ->
                    val idToken = task.token
                    if (idToken.isNullOrBlank()) {
                        // TODO : TOKEN IS INVALID
                        return@addOnSuccessListener
                    }

                    Retrofit().buildApiService().processLoginFirebase(idToken).enqueue(object :
                        Callback<RetLogin> {
                        override fun onResponse(
                            call: Call<RetLogin>,
                            response: Response<RetLogin>
                        ) {
                            if (!response.isSuccessful) {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(WebViewActivity.TAG, "onResponse: error ")
                                return
                            }

                            if (response.body() == null) {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(WebViewActivity.TAG, "onResponse: error ")
                                return
                            }

                            if (response.body()?.head?.status == "error") {
                                // TODO : ERROR FOR USER FLOW
                                Log.d(WebViewActivity.TAG, "onResponse: error ")
                                return
                            }

                            Log.d(TAG, "SEQ 4. Back-End Sign In Success")
                            val ret = response.body()!!.body
                            App.preferences.refreshToken = ret.t2
                            App.config.accessToken = ret.token
                            App.config.accountPKey = ret.userinfo.accountpkey

                            // Set Identity For Branch
                            activity.setBranchIdentity(ret)
                            activity.dismissLoader()
                            Log.d(TAG, "SEQ Final. Load Url For Success Login")
                            activity.webView.loadUrl("javascript:loginWithFirebase('$idToken', '${App.config.deviceID}', 'android')")
                        }

                        override fun onFailure(call: Call<RetLogin>, t: Throwable) {
                            Log.w(
                                WebViewActivity.TAG,
                                "onFailure: Auth Server Respond Fail",
                                t
                            )
                            activity.dismissLoader()
                        }
                    })
                }

        } catch (e: Exception) {
            Log.e(WebViewActivity.TAG, "loginAuthServerWithFirebaseUser: Fail", e)
        }
    }
}