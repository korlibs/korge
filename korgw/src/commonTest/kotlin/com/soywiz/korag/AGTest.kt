package com.soywiz.korag

import com.soywiz.korag.software.AGFactorySoftware
import com.soywiz.korio.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AGTest {
	@Test
	fun testOnReady() = suspendTest {
		val ag = AGFactorySoftware().create(null, AGConfig())
		val buffer = ag.createIndexBuffer()
		buffer.upload(intArrayOf(1, 2, 3, 4))
	}

    @Test
    fun testCombineScissor() {
        assertEquals(null, AG.Scissor.combine(null, null))
        assertEquals(AG.Scissor(0.0, 0.0, 100.0, 100.0), AG.Scissor.combine(AG.Scissor(0.0, 0.0, 100.0, 100.0), null))
        assertEquals(AG.Scissor(50.0, 50.0, 50.0, 50.0), AG.Scissor.combine(AG.Scissor(0.0, 0.0, 100.0, 100.0), AG.Scissor(50.0, 50.0, 100.0, 100.0)))
        assertEquals(AG.Scissor(50.0, 50.0, 100.0, 100.0), AG.Scissor.combine(null, AG.Scissor(50.0, 50.0, 100.0, 100.0)))
        assertEquals(AG.Scissor(0.0, 0.0, 0.0, 0.0), AG.Scissor.combine(AG.Scissor(2000.0, 2000.0, 100.0, 100.0), AG.Scissor(50.0, 50.0, 100.0, 100.0)))
    }
}
