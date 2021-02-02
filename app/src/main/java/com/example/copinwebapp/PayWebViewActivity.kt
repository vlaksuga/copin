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
import com.example.copinwebapp.data.Confirm
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
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class PayWebViewActivity : BaseWebViewActivity() {

    companion object {
        const val TAG = "TAG : PayWebViewActivity"
    }

    // Billing Service // pay
    private val billingAgent = WebBillingAgent(this)
    var selectedRevenue: Double? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUrl = "$BASE_URL + ?c=tx&m=payment" // Each
        webView.loadUrl(currentUrl) // Each
    }

    fun payInit() {
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

    fun startWebPayActivity() {
        /*webView.loadUrl("https://stage.copincomics.com/?c=tx&m=payment")*/
        val webPayIntent = Intent(this, PayActivity2::class.java)
        startActivity(webPayIntent)
    } // pay

    fun startPayActivity() {
            val payIntent = Intent(this, PayActivity::class.java)
            startActivity(payIntent)
    } // pay

    fun sendBackEnd(purchaseToken: String, sku: String) {
        repo.payDAO.confirm(purchaseToken, sku).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    if (res.body.result == "OK") {
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

    fun onSelectProduct(id: String) {
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
            Toast.makeText(this, "Product Id Invalid", Toast.LENGTH_SHORT).show()
            Log.d(BaseWebViewActivity.TAG, "selectProduct: Invalid Product Index")
        }
    }

}