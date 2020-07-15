package com.soywiz.korio.util

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class NumberExtTest {
	@Test
	fun testNextAligned() {
		assertEquals(0L, 0L.nextAlignedTo(15L))
		assertEquals(15L, 1L.nextAlignedTo(15L))
		assertEquals(15L, 14L.nextAlignedTo(15L))
		assertEquals(15L, 15L.nextAlignedTo(15L))
		assertEquals(30L, 16L.nextAlignedTo(15L))

		assertEquals(3L, 3L.nextAlignedTo(0L))
		assertEquals(3L, 3L.nextAlignedTo(1L))
	}

	@Test
	fun testPrevAligned() {
		assertEquals(0L, 0L.prevAlignedTo(15L))
		assertEquals(0L, 1L.prevAlignedTo(15L))
		assertEquals(0L, 14L.prevAlignedTo(15L))
		assertEquals(15L, 15L.prevAlignedTo(15L))
		assertEquals(15L, 16L.prevAlignedTo(15L))

		assertEquals(3L, 3L.prevAlignedTo(0L))
		assertEquals(3L, 3L.prevAlignedTo(1L))
	}

	@Test
	fun insert() {
		val v = 0x12345678
		assertEquals("FF345678", "%08X".format(v.insert(0xFF, 24, 8)))
		assertEquals("1F345678", "%08X".format(v.insert(0xFF, 24, 4)))
		assertEquals("12345FF8", "%08X".format(v.insert(0xFF, 4, 8)))
	}

	@Test
	fun testToString() {
		assertEquals("10", 0x10.toString(16))
		assertEquals("-10", (-0x10).toString(16))
	}
}