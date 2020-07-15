package com.soywiz.klogger.test

import com.soywiz.klogger.Logger
import kotlin.test.Test

class Logger2Test {
    companion object {
        val mylogger = Logger("demo")
    }

    @Test
    fun test() {
        mylogger.level = Logger.Level.INFO
    }
}