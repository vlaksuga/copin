package com.copincomics.copinapp.api

import com.copincomics.copinapp.data.Version
import com.copincomics.copinapp.data.GetMe
import com.copincomics.copinapp.data.RetLogin
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("a/checkVersion.json")
    fun getVersion(
    ): Call<Version>

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