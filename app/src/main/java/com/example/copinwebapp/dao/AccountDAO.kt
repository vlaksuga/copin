package com.example.copinwebapp.dao

import com.example.copinwebapp.data.CheckVersion
import com.example.copinwebapp.data.GetMe
import com.example.copinwebapp.data.RetLogin
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AccountDAO {
    @GET("a/checkVersion.*")
    fun requestCheckVersion(
    ): Call<CheckVersion>

    @GET("a/processLoginByToken.json")
    fun processLoginByToken(
        @Query("lt") lt: String
    ): Call<RetLogin>

    @GET("a/processLoginFirebase.json")
    fun processLoginFirebase(
        @Query("idtoken") idtoken: String
    ): Call<RetLogin>


    @GET("a/getMe.json")
    fun getMe(
    ): Call<GetMe>
}