package com.copincomics.copinapp.data

data class Confirm (
    val head: HeaderContext,
    val body: BodyConfirm
)

data class BodyConfirm (
    val result: String
)
