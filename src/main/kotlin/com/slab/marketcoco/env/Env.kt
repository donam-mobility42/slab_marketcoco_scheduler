package com.slab.marketcoco.env

import ein.spring.core.s
import org.springframework.core.env.Environment

interface Env{
    companion object{
        fun env(e: Environment):Env{
            val active = e.s("spring.profiles.active")
            return EnvLocal(e)
            /*
            return when(active){
                "real"->EnvReal(e)
                "test"->EnvTest(e)
                else->EnvLocal(e)
            }*/
        }
    }
    val settingPathes:Array<String>
    val appName:String get() = "MarketCOCO"
    val appVersion:String get() = "1.0.0"
    val debugMode:Boolean get() = false
    val testMode:Boolean get() = false
}