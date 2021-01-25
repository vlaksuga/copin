package com.example.copinwebapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.SkuDetails

class SkuDetailAdapter(
    val data: List<SkuDetails>,
    private val bonusList: ArrayList<String>,
    private val bestTagList: ArrayList<Boolean>
) : RecyclerView.Adapter<SkuDetailAdapter.SkuViewHolder> (){

    companion object {
        const val TAG = "TAG : SkuDetailAdapter"
    }

    private lateinit var mCellClickListener : ProductCellClickListener

    inner class SkuViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val view = itemView
        val amount: TextView = view.findViewById(R.id.product_amount)
        val price: TextView = view.findViewById(R.id.product_price)
        val bonus: TextView = view.findViewById(R.id.product_bonus)
        val tagBest: ImageView = view.findViewById(R.id.tag_best)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkuViewHolder {
        val context = parent.context
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflater.inflate(R.layout.section_product, parent, false)
        return SkuViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SkuViewHolder, position: Int) {
        try {
            val ret = data[position]
            holder.apply {
                amount.text = ret.description
                price.text = ret.price
                if (bonusList[position] != "0") {
                    val bonusText = "(+ including Bonus ${bonusList[position]} Coins)"
                    bonus.text = bonusText
                } else {
                    bonus.visibility = View.INVISIBLE
                }
                if (bestTagList[position]) {
                    tagBest.visibility = View.VISIBLE
                }
                itemView.setOnClickListener {
                    mCellClickListener.onCellClick(position)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "onBindViewHolder: Error", e)
        }
    }

    override fun getItemCount(): Int = data.size

    interface ProductCellClickListener {
        fun onCellClick (position: Int)
    }

    fun setProductCellClickListener(listener: ProductCellClickListener) {
        this.mCellClickListener = listener
    }
}