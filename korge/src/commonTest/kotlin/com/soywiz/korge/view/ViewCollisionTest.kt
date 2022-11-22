package com.soywiz.korge.view

import com.soywiz.korim.color.*
import kotlin.test.*

class ViewCollisionTest {
    /*
	@Test
	fun test() {
		val container = Container().apply {
			solidRect(16, 16, Colors.RED).position(0, 0).name("view1")
			solidRect(16, 16, Colors.RED).position(14, 0).name("view2")
		}
		val log = arrayListOf<String>()
		container["view1"].onCollision {
			log += "collision[${this.name}, ${it.name}]"
		}
		container["view1"].first.updateSingleView(0.0.milliseconds)
		assertEquals(listOf("collision[view1, view1]"), log)
	}
     */

    @Test
    fun test() {
        lateinit var circle1: Circle
        lateinit var circle2: Circle
        val container = Container().apply {
            circle1 = circle(50.0, fill = Colors.RED).xy(100.0, 100.0).centered
            circle2 = circle(50.0, fill = Colors.GREEN).xy(0, 0)
        }
        assertNotNull(circle1.hitTestView(circle2))
        circle1.xy(130.0, 130.0)
        assertNull(circle1.hitTestView(circle2))
    }
}
