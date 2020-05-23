package com.soywiz.korge.view

import com.soywiz.korim.color.Colors
import kotlin.test.*

class ViewCollisionTest {
	@Test
    @Ignore
	fun test() {
		val container = Container().apply {
			solidRect(16, 16, Colors.RED).position(0, 0).name("view1")
			solidRect(16, 16, Colors.RED).position(14, 0).name("view2")
		}
		val log = arrayListOf<String>()
		container["view1"]!!.onCollision {
			log += "collision[${this.name}, ${it.name}]"
		}
		container["view1"]!!.updateSingleView(0.0)
		assertEquals(listOf("collision[view1, view1]"), log)
	}
}
