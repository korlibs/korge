package com.soywiz.korge.render

import com.soywiz.korio.lang.*
import kotlin.test.*

class AgAutoFreeManagerTest {
    @Test
    fun test() {
        var log = ""
        val free = AgAutoFreeManager()
        val a = CloseableCancellable { log += "a" }
        val b = CloseableCancellable { log += "b" }
        free.reference(a)
        free.reference(b)
        free.gc()
        assertEquals("", log)
        free.reference(a)
        free.gc()
        assertEquals("b", log)
        free.gc()
        assertEquals("ba", log)
    }
}
