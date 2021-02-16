package com.soywiz.korag.shader

import com.soywiz.kds.*
import kotlin.test.*

class VertexLayoutTest {
	@Test
	fun testLayout1() {
		val a1 = Attribute("a1", VarType.Byte4, normalized = false)
		val a2 = Attribute("a2", VarType.Short3, normalized = false)
		val layout = VertexLayout(a1, a2)
		assertEquals(intArrayListOf(0, 4), layout.attributePositions)
		assertEquals(10, layout.totalSize)
	}

	@Test
	fun testLayout2() {
		val a1 = Attribute("a1", VarType.Short3, normalized = false)
		val a2 = Attribute("a2", VarType.INT(1), normalized = false)
		val layout = VertexLayout(a1, a2)
		assertEquals(intArrayListOf(0, 8), layout.attributePositions)
		assertEquals(12, layout.totalSize)
	}
}
