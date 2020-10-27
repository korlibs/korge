package com.soywiz.korio.stream

import com.soywiz.korio.lang.*
import kotlin.test.*

class FastByteArrayInputStreamTest {
	@kotlin.test.Test
	fun name() {
		val v = FastByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
		assertEquals(4, v.available)
		assertEquals("01020304", "%08X".format(v.readS32BE()))
		assertEquals(0, v.available)
		assertEquals(4, v.position)
		assertEquals(4, v.length)
	}
}
