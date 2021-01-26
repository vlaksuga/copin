package com.example.copinwebapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.example.copinwebapp.data.Confirm
import com.example.copinwebapp.data.GetMe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PayActivity : BaseActivity () {

    companion object {
        const val TAG = "TAG : Pay"
    }

    private val billingAgent = BillingAgent(this)

    private lateinit var payCoin: TextView
    lateinit var accountPKey: String
    lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)

        init()

        findViewById<TextView>(R.id.title).text = getString(R.string.title_pay_activity)
        findViewById<ImageView>(R.id.btn_finish).setOnClickListener { finish() }
        rv = findViewById(R.id.rv)
        payCoin = findViewById(R.id.pay_coin)

        billingAgent.init()

        updateUI()
    }

    private fun updateUI() {
        try {
            billingAgent.getInventory()
            billingAgent.checkProductUnconsumed()
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


}