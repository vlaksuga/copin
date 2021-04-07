package com.copincomics.copinapp



class CopinPref private constructor() {

    companion object {
        @Volatile private var instance: CopinPref? = null

        @JvmStatic fun Shared(): CopinPref =
                instance ?: synchronized(this) {
                    instance ?: CopinPref().also {
                        instance = it
                    }
                }
    }
    var abc : String = ""
    var acccessToken : String = ""


    fun setAll(a,b,c,de,f,g,h){

    }

    fun setA(v: String){
        acccessToken = v
    }
    fun getApiUrl() : String{
        return this.abc
    }

    fun getAc(): String {
        return this.acccessToken
    }
}