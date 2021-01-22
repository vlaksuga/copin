package com.example.copinwebapp.data

data class GetMe (
    val head: HeaderContext,
    val body: BodyGetMe
)

data class BodyGetMe(
    val nick: String,
    val isadult: String,
    val c: String,
    val t: String,
    val d: String,
    val v: String,
    val kind: String,
    val type: String,
    val profileimg: String,
    val email: String,
    val coins: String,
    val apkey: String
)