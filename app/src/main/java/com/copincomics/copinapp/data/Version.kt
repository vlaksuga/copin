package com.copincomics.copinapp.data

data class Version(
    val head: HeaderContext,
    val body: VersionBody
)

data class VersionBody(
    val ANDROIDMIN: String,
    val ANDROIDWARNING: String,
    val ANDROIDRECENT: String,
    val APIURL11: String,
    val ENTRYURL11: String,
    val DEFAULTAPIURL: String,
    val DEFAULTENTRYURL: String
)