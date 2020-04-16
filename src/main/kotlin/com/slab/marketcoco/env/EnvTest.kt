package com.slab.marketcoco.env

import org.springframework.core.env.Environment

internal class EnvTest(val e: Environment): Env {
    override val settingPathes = arrayOf("setting/common.json","setting/test.json")
    override val testMode = true
    override val debugMode = true
}