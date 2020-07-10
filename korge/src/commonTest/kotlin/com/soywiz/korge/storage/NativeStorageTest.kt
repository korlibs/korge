package com.soywiz.korge.storage

import com.soywiz.korge.service.storage.*
import com.soywiz.korge.tests.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeStorageTest : ViewsForTesting() {
    @BeforeTest
    fun cleanup() {
        views.storage.removeAll()
    }

    @Test
    fun test() = viewsTest {
        val demo = views.storage.item<Int>("hello")
        assertEquals(false, demo.isDefined)
        assertFailsWith<Throwable> { demo.value }
        demo.value = 10
        assertEquals(true, demo.isDefined)
        assertEquals(10, demo.value)
    }
}
