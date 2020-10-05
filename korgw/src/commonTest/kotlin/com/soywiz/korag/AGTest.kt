package com.soywiz.korag

import com.soywiz.korag.software.*
import com.soywiz.korio.async.*
import kotlin.test.*

class AGTest {
	@Test
	fun testOnReady() = suspendTest {
		val ag = AGFactorySoftware().create(null, AGConfig())
		val buffer = ag.createIndexBuffer()
		buffer.upload(intArrayOf(1, 2, 3, 4))
	}
}
