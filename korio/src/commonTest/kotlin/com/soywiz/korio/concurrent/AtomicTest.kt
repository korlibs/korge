package com.soywiz.korio.concurrent

import com.soywiz.korio.concurrent.atomic.KorAtomicInt
import com.soywiz.korio.concurrent.atomic.getAndAdd
import com.soywiz.korio.concurrent.atomic.getAndIncrement
import com.soywiz.korio.concurrent.atomic.incrementAndGet
import kotlin.test.Test
import kotlin.test.assertEquals

class AtomicTest {
	@Test
	fun test() {
		val value = KorAtomicInt(0)
		assertEquals(1, value.incrementAndGet())
		assertEquals(1, value.value++)
		assertEquals(2, value.value)
		assertEquals(3, ++value.value)
	}

    @Test
    fun testSingleton() {
        val value = singleton
        value.value = 0
        assertEquals(1, value.incrementAndGet())
        assertEquals(1, value.value++)
        assertEquals(2, value.value)
        assertEquals(3, ++value.value)
    }

    @Test
    fun testSingleton2() {
        val value = singleton
        value.value = 0
        assertEquals(0, value.getAndIncrement())
        assertEquals(1, value.getAndAdd(10))
        assertEquals(11, value.value)
    }

    companion object {
        val singleton = KorAtomicInt(0)
    }
}
