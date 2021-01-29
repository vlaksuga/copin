package com.example.copinwebapp

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.example.copinwebapp.data.Confirm
import com.example.copinwebapp.data.GetMe
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PayActivity2 : BaseActivity() {

    companion object {
        const val TAG = "TAG : PayActivity2"
        const val BASE_URL = "https://stage.copincomics.com/?c=tx&m=payment"
    }

    /*private val billingAgent = BillingAgent(this)*/

    private lateinit var payCoin: TextView
    lateinit var accountPKey: String
    lateinit var rv: RecyclerView
    lateinit var productLayout: LinearLayout
    lateinit var processBlocker: ConstraintLayout
    lateinit var successfulBlocker: ConstraintLayout
    lateinit var tryAgainButton: ImageView
    lateinit var closeProcess: ImageView
    lateinit var refreshPay: TextView
    lateinit var confirmButton: Button
    lateinit var webView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)
        webView = findViewById(R.id.webView_pay2)

        processBlocker = findViewById(R.id.processing_blocker_layout)
        tryAgainButton = findViewById(R.id.imageView_try_again)
        closeProcess = findViewById(R.id.imageView_close_blocker)
        successfulBlocker = findViewById(R.id.successful_blocker_layout)
        confirmButton = findViewById(R.id.button_confirm)
        refreshPay = findViewById(R.id.pay_refresh)

        closeProcess.setOnClickListener {
            productLayout.visibility = View.VISIBLE
            processBlocker.visibility = View.GONE
            successfulBlocker.visibility = View.GONE
        }

        confirmButton.setOnClickListener {
            productLayout.visibility = View.VISIBLE
            processBlocker.visibility = View.GONE
            successfulBlocker.visibility = View.GONE
        }

        refreshPay.setOnClickListener { startBilling() }
        /*tryAgainButton.setOnClickListener { billingAgent.tryAgain() }*/

        loadingDialog.show()

        /*billingAgent.init()*/
        startBilling()

        webView.loadUrl(BASE_URL)
    }

    private fun startBilling() {
        try {
            /*billingAgent.startBillingConnection()
            billingAgent.checkProductUnconsumed()*/
        } catch (e: Exception) {
            Log.w(TAG, "updateUI: error", e)
            Toast.makeText(this, "Error! Please Try Again!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    fun updateCoin() {
        try {
            repo.accountDAO.getMe().enqueue(object : Callback<GetMe> {
                override fun onResponse(call: Call<GetMe>, response: Response<GetMe>) {
                    response.body()?.let { res ->
                        val ret = res.body
                        payCoin.text = ret.coins
                        Log.d(TAG, "onResponse: coin = ${ret.coins}")
                        accountPKey = ret.apkey

                    }
                }

                override fun onFailure(call: Call<GetMe>, t: Throwable) {
                    Log.w(TAG, "onFailure: GetMe Fail", t)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "updateCoin: Error", e)
        }
    }

    override fun onBackPressed() {
        val entryIntent = Intent(this, EntryActivity::class.java)
        entryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(entryIntent)
    }


    override fun onStop() {
        super.onStop()
        productLayout.visibility = View.VISIBLE
        processBlocker.visibility = View.GONE
        successfulBlocker.visibility = View.GONE
    }

}