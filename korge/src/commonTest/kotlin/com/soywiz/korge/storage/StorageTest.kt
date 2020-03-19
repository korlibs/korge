package com.soywiz.korge.storage

import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.service.storage.item
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageTest {
    @BeforeTest
    fun cleanup() {
        NativeStorage.removeAll()
    }

    @Test
    fun test() {
        val demo = NativeStorage.item<Int>("hello")
        assertEquals(false, demo.isDefined)
        assertFailsWith<Throwable> { demo.value }
        demo.value = 10
        assertEquals(true, demo.isDefined)
        assertEquals(10, demo.value)
    }
}
