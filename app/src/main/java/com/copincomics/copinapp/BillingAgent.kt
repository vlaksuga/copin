package com.copincomics.copinapp


import android.util.Log
import android.view.View
import com.android.billingclient.api.*
import com.copincomics.copinapp.data.Confirm
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BillingAgent(private val activity: PayActivity) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "TAG : BillingAgent"
    }

    private var productDetailList: MutableList<SkuDetails>? = null
    private val productIdsList = arrayListOf("c10", "c30", "c100", "c500", "c1000")
    private val bonusList = arrayListOf("0", "10", "35", "200", "440")
    private val bestTagList = arrayListOf(false, false, false, true, false)
    private var currentCellPosition = 0
    private var dataSorted = listOf<SkuDetails>()

    private lateinit var billingClient: BillingClient

    fun init() {
        billingClient = BillingClient.newBuilder(activity)
            .enablePendingPurchases()
            .setListener(this)
            .build()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchaseList: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                activity.successfulBlocker.visibility = View.VISIBLE
                activity.processBlocker.visibility = View.GONE
                activity.productLayout.visibility = View.GONE
                purchaseList?.let { list ->
                    for (purchase in list) {
                        sendBackEnd(purchase.purchaseToken, purchase.sku)
                        Log.d(PayActivity.TAG, "onPurchasesUpdated: List = $list")
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
            }
            else -> {
            }
        }
    }

    private fun sendBackEnd(purchaseToken: String, sku: String) {
        BaseActivity().repo.payDAO.confirm(purchaseToken, sku).enqueue(object : Callback<Confirm> {
            override fun onResponse(call: Call<Confirm>, response: Response<Confirm>) {
                response.body()?.let { res ->
                    if (res.body.result == "OK") {
                        consumePurchase(purchaseToken)
                    } else {
                        Log.d(PayActivity.TAG, "onResponse: BackEnd Says Not OK")
                    }
                }
            }

            override fun onFailure(call: Call<Confirm>, t: Throwable) {
                Log.e(PayActivity.TAG, "onFailure: Confirm from backend fail", t)
            }
        })
    }

    private fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { res, _ ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(PayActivity.TAG, "consumePurchase: consume ok")
            }
        }
    }

    fun startBillingConnection() {
        Log.d(TAG, "getInventory: invoked")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished: ok")
                    queryInventoryAsync()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(PayActivity.TAG, "onBillingServiceDisconnected: disconnected")
            }
        })
    }

    private fun queryInventoryAsync() {
        Log.d(TAG, "queryInventoryAsync: invoked")
        val recyclerView = activity.rv
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(productIdsList)
            .setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(
            params.build()
        ) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryInventoryAsync: Response Code is OK")
                list?.let { skuDetails ->
                    productDetailList = skuDetails
                    dataSorted = productDetailList!!.sortedBy { it.priceAmountMicros }
                    Log.d(TAG, "queryInventoryAsync: dataSorted = $dataSorted")
                    recyclerView.adapter = SkuDetailAdapter(dataSorted, bonusList, bestTagList)
                    (activity.rv.adapter as SkuDetailAdapter).setProductCellClickListener(object :
                        SkuDetailAdapter.ProductCellClickListener {
                        override fun onCellClick(position: Int) {
                            currentCellPosition = position
                            activity.processBlocker.visibility = View.VISIBLE
                            activity.successfulBlocker.visibility = View.GONE
                            activity.productLayout.visibility = View.GONE
                            launchBillingFlow(dataSorted[position])
                            Log.d(TAG, "onCellClick: position = $position")
                        }
                    })
                }
            } else {
                Log.d(TAG, "queryInventoryAsync: Response Code is Not OK")
            }
        }
    }

    fun launchBillingFlow(item: SkuDetails) {
        val productId = item.sku
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(item)
            .setObfuscatedAccountId("${activity.accountPKey}:$productId")
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun checkProductUnconsumed() {
        val purchaseList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList

        if (purchaseList != null) {
            Log.d(PayActivity.TAG, "checkProductUnconsumed: ${purchaseList.size} unconsumed items")
            for (purchase in purchaseList) {
                sendBackEnd(purchase.purchaseToken, purchase.sku)
            }
            activity.updateCoin()
        } else {
            Log.d(PayActivity.TAG, "checkProductUnconsumed: No unconsumed item")
            activity.updateCoin()
        }
    }

    fun tryAgain() {
        activity.processBlocker.visibility = View.VISIBLE
        activity.successfulBlocker.visibility = View.GONE
        activity.productLayout.visibility = View.GONE
        launchBillingFlow(dataSorted[currentCellPosition])
        Log.d(TAG, "onCellClick: tryAgain position = $currentCellPosition")
    }

}