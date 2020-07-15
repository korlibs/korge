package com.soywiz.korio.ds

import com.soywiz.kmem.*
import com.soywiz.korio.util.encoding.*
import kotlin.test.*

class ByteArrayBuilderTest {
	@Test
	fun name() {
		val bb = ByteArrayBuilder()
		bb.append(byteArrayOf(1))
		bb.append(byteArrayOf(2, 3))
		bb.append(4)
		bb.append(byteArrayOf(5))
		assertEquals("0102030405", bb.toByteArray().hex)
	}
}