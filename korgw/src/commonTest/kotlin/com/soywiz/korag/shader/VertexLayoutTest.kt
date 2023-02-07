package com.soywiz.korag.shader

import com.soywiz.kds.intArrayListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class VertexLayoutTest {
	@Test
	fun testLayout1() {
		val a1 = Attribute("a1", VarType.Byte4, normalized = false, fixedLocation = 0)
		val a2 = Attribute("a2", VarType.Short3, normalized = false, fixedLocation = 1)
		val layout = VertexLayout(a1, a2)
		assertEquals(intArrayListOf(0, 4), layout.attributePositions)
		assertEquals(10, layout.totalSize)
	}

	@Test
	fun testLayout2() {
		val a1 = Attribute("a1", VarType.Short3, normalized = false, fixedLocation = 0)
		val a2 = Attribute("a2", VarType.INT(1), normalized = false, fixedLocation = 1)
		val layout = VertexLayout(a1, a2)
		assertEquals(intArrayListOf(0, 8), layout.attributePositions)
		assertEquals(12, layout.totalSize)
	}
}
