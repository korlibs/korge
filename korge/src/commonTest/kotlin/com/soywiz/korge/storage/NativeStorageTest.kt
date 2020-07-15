package com.soywiz.korge.storage

import com.soywiz.korge.service.storage.*
import com.soywiz.korge.tests.*
import kotlin.test.*

class NativeStorageTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        views.storage.removeAll()
        val demo = views.storage.item<Int>("hello")
        assertEquals(false, demo.isDefined)
        assertEquals(0, demo.value)
        //assertFailsWith<Throwable> { demo.value }
        demo.value = 10
        assertEquals(true, demo.isDefined)
        assertEquals(10, demo.value)
    }
}
