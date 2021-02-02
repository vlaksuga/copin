package com.example.copinwebapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
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
import com.google.firebase.auth.*
import io.branch.referral.Branch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class AccountWebViewActivity : BaseWebViewActivity() {

    companion object {
        const val TAG = "TAG : AccountWebViewActivity"
        const val GOOGLE_SIGN_IN = 9001 // account
    }

    // Firebase Auth
    private lateinit var googleSignInClient: GoogleSignInClient // account
    private lateinit var callbackManager: CallbackManager // account


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callbackManager = CallbackManager.Factory.create() // account
        currentUrl = "$BASE_URL + ?c=login" // Each
        webView.loadUrl(currentUrl) // Each
    }

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
                            Log.d(TAG, "signInWithCredential: ${user.providerData[1].providerId} ")
                            for(provider in user.providerData) {
                                Log.d(TAG, "signInWithCredential: ${provider.providerId}")
                            }
                            /*Branch.getInstance().userCompletedAction("create_account")*/
                        }
                    }
                }
            }
    } // account

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
                                        Log.d(TAG, "onResponse: ret = ${response.body()!!.body}")

                                        // Set Identity For Branch
                                        accountPKey =
                                            response.body()?.body?.userinfo?.accountpkey ?: ""
                                        if(accountPKey != "") {
                                            val branch = Branch.getInstance(applicationContext)
                                            branch.setIdentity(accountPKey)
                                            Log.d(TAG, "onResponse: accountPKey = $accountPKey")
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

}