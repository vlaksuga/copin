package com.copincomics.copinapp.data

class Amu<K, V>() : HashMap<K, V>() {
    fun getS(k: K): String {
        return getS(k, "")
    }

    fun getS(k: K, a18: String): String {
        return getOrDefault(k, a18 as V).toString();
    }

    fun getI(k: K, aaa: Int): Int {
        return this.getS(k).toIntOrNull() ?: aaa
    }
}