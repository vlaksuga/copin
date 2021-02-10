package com.copincomics.copinapp.data

data class CheckVersion(
    val head: HeaderContext,
    val body: BodyCheckVersion
)

data class BodyCheckVersion(
    val ANDROIDMIN: String,
    val ANDROIDWARNING: String,
    val IOSMIN: String,
    val ANDROIDRECENT: String,
    val IOSWEB: String,
    val IOSRECENT: String,
    val ANDROIDWEB: String,
    val IOSWARNING: String,
    val APIURL11: String,
    val ENTRYURL11: String,
    val DEFAULTAPIURL: String,
    val DEFAULTENTRYURL: String

)