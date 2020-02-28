package com.slab.marketcoco.env

import org.springframework.core.env.Environment

internal class EnvLocal(val e: Environment): Env {
    override val settingPathes = arrayOf("setting/common.json","setting/local.json")
    override val testMode = true
    override val debugMode = true
}