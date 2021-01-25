package com.example.copinwebapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.android.billingclient.api.*
import com.example.copinwebapp.data.Confirm
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PayActivity : BaseActivity () {

    companion object {
        const val TAG = "TAG : Pay"
    }

    private val billingAgent = InAppBilling()
    private var productDetailList: MutableList<SkuDetails>? = null
    private val productIdsList = arrayListOf("c10", "c30", "c100", "c500", "c1000")
    private val bonusList = arrayListOf("0", "10", "35", "200", "440")
    private val bestTagList = arrayListOf(false, false, false, true, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)

        init()

        findViewById<TextView>(R.id.title).text = getString(R.string.titie_pay_activity)
        findViewById<ImageView>(R.id.btn_finish).setOnClickListener { finish() }

        updateUI()
    }

    private fun updateUI() {
        try {
            billingAgent
        } catch (e: Exception) {

        }
    }

    inner class InAppBilling : PurchasesUpdatedListener {

        private var billingClient: BillingClient = BillingClient.newBuilder(this@PayActivity)
            .enablePendingPurchases()
            .setListener(this)
            .build()

        override fun onPurchasesUpdated(result: BillingResult, purchaseList: MutableList<Purchase>?) {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchaseList?.let { list ->
                        for (purchase in list) {
                            sendBackEnd(purchase.purchaseToken, purchase.sku)
                            Log.d(TAG, "onPurchasesUpdated: List = $list")
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {}
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {}
                else -> {}
            }
        }

        private fun sendBackEnd(purchaseToken: String, sku: String) {
            repo.payDAO.confirm(purchaseToken, sku).enqueue(object : Callback<Confirm> {
                override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                    response.body()?.let { res ->
                        if (res.body.result == "OK") {
                            consumePurchase(purchaseToken)
                        }
                    }
                }

                override fun onFailure(call: Call<Confirm>, t: Throwable) {
                    Log.e(TAG, "onFailure: Confirm from backend fail", t)
                }
            })
        }

        private fun consumePurchase(purchaseToken: String) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { res, _ ->
                if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "consumePurchase: consume ok")
                }
            }
        }

        private fun getInventory() {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryInventoryAsync()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "onBillingServiceDisconnected: disconnected")
                }
            })
        }

        private fun queryInventoryAsync() {
            val params = SkuDetailsParams.newBuilder()
                params.setSkusList(productIdsList)
                .setType(BillingClient.SkuType.INAPP)

            billingClient.querySkuDetailsAsync(params.build()
            ) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    list?.let { skuDetails ->
                        productDetailList = skuDetails
                        val dataSorted = productDetailList!!.sortedBy { it.priceAmountMicros }
                    }

                }
            }
        }

    }
}