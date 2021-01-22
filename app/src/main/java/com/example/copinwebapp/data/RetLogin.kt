package com.example.copinwebapp.data

data class RetLogin(
    val head: HeaderContext,
    val body: BodyRetLogin

)

data class BodyRetLogin(
    val t2: String,
    val userinfo: UserInfoForm,
    val token: String
)

data class UserInfoForm(
    val nick: String,
    val kind: String,
    val accesstoken: String,
    val deviceid: String,
    val newlogintoken: String,
    val status: String,
    val accountpkey : String
)

