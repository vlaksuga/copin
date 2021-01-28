package com.example.copinwebapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
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
    lateinit var productLayout: LinearLayout
    lateinit var processBlocker: ConstraintLayout
    lateinit var successfulBlocker: ConstraintLayout
    lateinit var tryAgainButton: ImageView
    lateinit var closeProcess: ImageView
    lateinit var refreshPay: TextView
    lateinit var confirmButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)

        init()

        findViewById<TextView>(R.id.title).text = getString(R.string.title_pay_activity)
        findViewById<ImageView>(R.id.btn_finish).setOnClickListener { finish() }
        rv = findViewById(R.id.rv)
        productLayout = findViewById(R.id.product_list_layout)
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

        refreshPay.setOnClickListener { updateUI() }
        tryAgainButton.setOnClickListener { billingAgent.tryAgain() }

        rv.layoutManager = LinearLayoutManager(this)
        payCoin = findViewById(R.id.pay_coin)

        billingAgent.init()
        updateUI()
    }

    private fun updateUI() {
        try {
            billingAgent.startBillingConnection()
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