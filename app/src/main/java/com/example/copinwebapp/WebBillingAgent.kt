package com.example.copinwebapp


import android.util.Log
import com.android.billingclient.api.*
import com.example.copinwebapp.data.Confirm
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class WebBillingAgent(private val activity: MainWebViewActivity) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "TAG : WebBillingAgent"
    }

    private var productDetailList: MutableList<SkuDetails>? = null
    private val productIdsList = arrayListOf("c10", "c30", "c100", "c500", "c1000")
    private val bonusList = arrayListOf("0", "10", "35", "200", "440")
    private val coinList = arrayListOf("10", "30", "100", "500", "1000")
    private val bestTagList = arrayListOf("", "", "", "Y", "")
    var dataSorted = listOf<SkuDetails>()

    private lateinit var billingClient: BillingClient

    fun buildBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
            .enablePendingPurchases()
            .setListener(this)
            .build()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchaseList: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // TODO : SEND SERVER OK RESPONSE FOR COMPLETE COVER VIEW
/*                activity.successfulBlocker.visibility = View.VISIBLE
                activity.processBlocker.visibility = View.GONE
                activity.productLayout.visibility = View.GONE*/
                purchaseList?.let { list ->
                    for (purchase in list) {
                        activity.sendBackEnd(purchase.purchaseToken, purchase.sku)
                        Log.d(TAG, "onPurchasesUpdated: List = $list")
                    }
                }

            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "onPurchasesUpdated: USER CANCELED")
            }
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                Log.d(TAG, "onPurchasesUpdated: SERVICE TIMEOUT")
            }
            else -> {
            }
        }
    }



    fun consumePurchase(purchaseToken: String) {
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

                    // DRAW LIST WITH SKU DETAILS
                    val theList = arrayListOf<HashMap<String, String>>()
                    for((index, data) in dataSorted.withIndex()) {
                        val map : HashMap<String, String> = hashMapOf()
                        map["pid"] = data.sku
                        map["a"] = data.price
                        map["b"] = bonusList[index]
                        map["off"] = ""
                        map["c"] = coinList[index]
                        map["best"] = bestTagList[index]
                        theList.add(map)
                        Log.d(TAG, "queryInventoryAsync: map $map added")
                    }

                    val productString = "[{pid:'c10', a:'1.99', b:'', off:'', c:'10'}, {pid:'c30', a:'3.99', b:'10',off:'', c:'30'}, {pid:'c100', a:'5.99', b:'35', off:'', c:'100'}, {pid:'coin500', a:'5.99', b:'200',off:'', c:'500', best:'Y'}, {pid:'c1000', a:'5.99', b:'440',off:'', c:'1000'}]"
                    val jsonData = JSONArray(theList)
                    activity.webView.post { activity.webView.loadUrl("javascript:setData('$jsonData')")  }
                    Log.d(TAG, "setDataTt: setData = $jsonData")
                    Log.d(TAG, "queryInventoryAsync: Completed")
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
        Log.d(TAG, "launchBillingFlow: pkey = ${activity.accountPKey}, pid = $productId")
    }

    fun checkProductUnconsumed() {
        val purchaseList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList

        if (purchaseList != null) {
            Log.d(PayActivity.TAG, "checkProductUnconsumed: ${purchaseList.size} unconsumed items")
            for (purchase in purchaseList) {
                activity.sendBackEnd(purchase.purchaseToken, purchase.sku)
            }
        } else {
            Log.d(PayActivity.TAG, "checkProductUnconsumed: No unconsumed item")
        }
    }

}