package com.example.copinwebapp.dao

import com.example.copinwebapp.data.Confirm
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PayDAO {
    @GET("paymentAndroid/confirm.json")
    fun confirm(
        @Query("purchasetoken") purchasetoken: String,
        @Query("productid") productid: String
    ): Call<Confirm>
}