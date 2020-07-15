package com.soywiz.korio.async

import com.soywiz.klock.*
import com.soywiz.korio.*
import com.soywiz.korio.util.*
import kotlin.browser.*
import kotlin.test.*

class PromiseJsTest {
    @Test
    fun test() = suspendTest({ !OS.isJsNodeJs }) {
        val startTime = DateTime.now()
        val value = delay(100)
        assertTrue(js("(value instanceof Promise)"))
        assertTrue(js("(typeof value.then == \"function\")"))
        value.await()
        val endTime = DateTime.now()
        assertEquals(true, endTime - startTime >= 100.milliseconds)
    }

    fun delay(ms: Int): Promise<Unit> = Promise { resolve, reject -> global.setTimeout({ resolve(Unit) }, ms) }
}
