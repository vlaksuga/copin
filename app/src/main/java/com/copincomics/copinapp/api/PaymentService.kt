package com.copincomics.copinapp.api

import com.copincomics.copinapp.data.Confirm
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PaymentService {
    @GET("paymentAndroid/confirm.json")
    fun confirm(
        @Query("purchasetoken") purchasetoken: String,
        @Query("productid") productid: String
    ): Call<Confirm>

    @GET("paymentAndroid/confirmReal.json")
    fun confirmReal(
        @Query("purchasetoken") purchasetoken: String,
        @Query("productid") productid: String
    ): Call<Confirm>
}