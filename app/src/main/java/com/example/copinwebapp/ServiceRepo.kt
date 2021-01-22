package com.example.copinwebapp

import android.content.SharedPreferences
import com.example.copinwebapp.dao.AccountDAO
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ServiceRepo (private val sharedPreferences: SharedPreferences) {

    companion object {
        const val BASE_URL = "https://sapi.copincomics.com/"
    }

    private class AuthInterceptor(private val t: String, private val c: String, val d: String, val v: String) :
        Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val url: HttpUrl = chain.request().url()
                .newBuilder()
                .addQueryParameter("t", t)
                .addQueryParameter("c", c)
                .addQueryParameter("d", d)
                .addQueryParameter("v", v)
                .build()
            val request: Request = chain.request().newBuilder().url(url).build()
            return chain.proceed(request)
        }
    }

    private fun okHttpclient(): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
        httpClient.apply {
            connectTimeout(5, TimeUnit.MINUTES)
            readTimeout(5, TimeUnit.MINUTES)
            val t = sharedPreferences.getString("t", " ")?:""
            val c = sharedPreferences.getString("deviceId", "")?:""
            val d = "android"
            val v = sharedPreferences.getString("appVersion", "1.0")?:"1.0"
            addInterceptor(AuthInterceptor(t, c, d, v))
        }
        return httpClient.build()
    }

    private val okHttpclient = okHttpclient()

    val accountDAO: AccountDAO = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpclient)
        .build().create(AccountDAO::class.java)
}