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
    var dataSorted = listOf<SkuDetails>()

    private lateinit var billingClient: BillingClient

    fun buildBillingClient() {
        Log.d(TAG, "buildBillingClient: start")
        billingClient = BillingClient.newBuilder(activity)  // TODO : Billing 클라이언트 하나만 만들게 로직 바꾸기 메뉴얼보기
            .enablePendingPurchases()
            .setListener(this)
            .build()
        Log.d(TAG, "buildBillingClient: end")
        startBillingConnection()
        checkProductUnconsumed() // Todo : 컨슘체크 끝나면 리스트 불러오기
    }

    fun startBillingConnection() {
        Log.d(TAG, "startBillingConnection: start")
        billingClient.startConnection(object : BillingClientStateListener { // todo : connection option들 체크하기
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished: ok")
                    queryInventoryAsync()
                } else {
                    Log.d(TAG, "onBillingSetupFinished: ${result.debugMessage}")
                    Toast.makeText(activity, "Billing Service Error, Please Try Again", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected: disconnected")
                Toast.makeText(activity, "Billing Service Disconnected, Please Try Again", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun checkProductUnconsumed() {
        val purchaseList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList

        if (purchaseList != null) {
            Log.d(TAG, "checkProductUnconsumed: ${purchaseList.size} unconsumed items")
            for (purchase in purchaseList) {
                activity.sendBackEndForCheckUnconsumed(purchase.purchaseToken, purchase.sku)
            }
        } else {
            Log.d(TAG, "checkProductUnconsumed: No unconsumed item")
        }
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
                if(!list.isNullOrEmpty()) {
                    list.let { skuDetails ->
                        productDetailList = skuDetails
                        dataSorted = productDetailList!!.sortedBy { it.priceAmountMicros }
                        Log.d(TAG, "queryInventoryAsync: dataSorted = $dataSorted")

                        // DRAW LIST WITH SKU DETAILS // todo : 여러번 안걸리게 하기
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

                        val jsonData = JSONArray(theList)
                        activity.webView.post { activity.webView.loadUrl("javascript:setData('$jsonData')")  }
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
        Log.d(TAG, "onPurchasesUpdated: invoked") // todo : 컨슘될때 타는지 체크
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchaseList != null) {
                    if (purchaseList.size > 0) {
                        activity.sendBackEnd(purchaseList[0].purchaseToken, purchaseList[0].sku)
                        Log.d(TAG, "onPurchasesUpdated: Purchase = $purchaseList[0]")
                    } else {
                        Log.d(TAG, "onPurchasesUpdated: Purchase List is Empty")
                    }
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

        billingClient.consumeAsync(consumeParams) { res, _ ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "consumePurchase: consume ok")
            } else {
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
        billingClient.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "launchBillingFlow: accountPKey = ${activity.accountPKey}, pid = $productId")
    }



}