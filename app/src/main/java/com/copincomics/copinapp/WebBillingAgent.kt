package com.copincomics.copinapp


import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import org.json.JSONArray

open class WebBillingAgent(private val activity: MainWebViewActivity) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "TAG : WebBillingAgent"
    }

    private var productDetailList: MutableList<SkuDetails>? = null
    private val productIdsList = arrayListOf("c10", "c30", "c100", "c500", "c1000")
    private val bonusList = arrayListOf("0", "10", "35", "200", "440")
    private val coinList = arrayListOf("10", "30", "100", "500", "1000")
    private val bestTagList = arrayListOf("", "", "", "Y", "")
    private var jsonData: JSONArray? = null
    var dataSorted = listOf<SkuDetails>()

    var billingClient: BillingClient? = null

    fun buildBillingClient() {
        Log.d(TAG, "buildBillingClient: start")
        if(billingClient == null) {
            billingClient = BillingClient.newBuilder(activity)
                    .enablePendingPurchases()
                    .setListener(this)
                    .build()
            Log.d(TAG, "buildBillingClient: end")
        }
        startBillingConnection()
    }

    private fun startBillingConnection() {
        Log.d(TAG, "startBillingConnection: start")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (billingClient?.isReady == true) {
                        Log.d(TAG, "onBillingSetupFinished: ok")
                        checkProductUnconsumed()
                    }
                } else {
                    Log.d(TAG, "onBillingSetupFinished: ${result.debugMessage}")
                    Toast.makeText(activity, "Billing Service Error, Please Try Again", Toast.LENGTH_SHORT).show()
                    endBillingConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected: disconnected")
                Toast.makeText(activity, "Billing Service Disconnected, Please Try Again", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun checkProductUnconsumed() {
        Log.d(TAG, "checkProductUnconsumed: start")
        val purchaseList = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)?.purchasesList
        Log.d(TAG, "checkProductUnconsumed: purchaselist = $purchaseList")
        if (purchaseList != null && purchaseList.size != 0) {
            Log.d(TAG, "checkProductUnconsumed: ${purchaseList.size} unconsumed items")
            for (purchase in purchaseList) {
                activity.sendBackEndForCheckUnconsumed(purchase.purchaseToken, purchase.sku)
            }
        } else {
            Log.d(TAG, "checkProductUnconsumed: No unconsumed item")
            queryInventoryAsync()
        }
    }

    private fun queryInventoryAsync() {
        Log.d(TAG, "queryInventoryAsync: invoked")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(productIdsList)
                .setType(BillingClient.SkuType.INAPP)

        billingClient?.querySkuDetailsAsync(
                params.build()
        ) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryInventoryAsync: Response Code is OK")
                if (!list.isNullOrEmpty()) {
                    list.let { skuDetails ->
                        // CHECK jsonData is null
                        if (jsonData == null) {
                            Log.d(TAG, "queryInventoryAsync: jsonData is null")
                            productDetailList = skuDetails
                            dataSorted = productDetailList!!.sortedBy { it.priceAmountMicros }
                            Log.d(TAG, "queryInventoryAsync: dataSorted = $dataSorted")
                            val theList = arrayListOf<HashMap<String, String>>()
                            for ((index, data) in dataSorted.withIndex()) {
                                val map: HashMap<String, String> = hashMapOf()
                                map["pid"] = data.sku
                                map["a"] = data.price
                                map["b"] = bonusList[index]
                                map["off"] = ""
                                map["c"] = coinList[index]
                                map["best"] = bestTagList[index]
                                theList.add(map)
                                Log.d(TAG, "queryInventoryAsync: map $map added")
                            }
                            jsonData = JSONArray(theList)
                        }
                        activity.webView.post { activity.webView.loadUrl("javascript:setData('$jsonData')") }
                        Log.d(TAG, "setDataTt: setData = $jsonData")
                        Log.d(TAG, "queryInventoryAsync: Completed")
                    }
                } else {
                    Toast.makeText(activity, "Inventory Invalid, Please Try Again", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(activity, "Inventory Invalid, Please Try Again", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "queryInventoryAsync: Response Code is Not OK")
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchaseList: MutableList<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: invoked")
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchaseList != null && purchaseList.size > 0) {

                        activity.sendBackEnd(purchaseList[0].purchaseToken, purchaseList[0].sku)
                        Log.d(TAG, "onPurchasesUpdated: Purchase = $purchaseList[0]")
                } else {
                    Toast.makeText(activity, "Purchase List is Empty", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onPurchasesUpdated: Purchase List is Empty")
                }
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                Toast.makeText(activity, "Service Timeout", Toast.LENGTH_SHORT).show()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Toast.makeText(activity, "User Canceled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(activity, "Billing Unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

        billingClient?.consumeAsync(consumeParams) { res, _ ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "consumePurchase: consume ok")
            } else {
                Log.d(TAG, "consumePurchase: consume not ok")
            }
            endBillingConnection()
        }
    }

    fun consumePurchaseRetry(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

        billingClient?.consumeAsync(consumeParams) { res, _ ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "consumePurchase: consume ok")
                queryInventoryAsync()
            } else {
                Toast.makeText(activity, "Consume Purchase Failed. Please Retry", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "consumePurchase: consume not ok")
            }
        }
    }

    fun launchBillingFlow(item: SkuDetails) {
        val productId = item.sku
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(item)
                .setObfuscatedAccountId("${activity.accountPKey}:$productId")
                .build()
        billingClient?.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "launchBillingFlow: accountPKey = ${activity.accountPKey}, pid = $productId")
    }

    fun endBillingConnection() {
        if(billingClient != null) {
            billingClient!!.endConnection()
            billingClient = null
            Log.d(TAG, "endBillingConnection: done")
        }
    }


}