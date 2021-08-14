package com.soywiz.korge.box2d

import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*
import org.jbox2d.dynamics.*
import kotlin.test.*

class Box2dTest {
	@Test
	fun test() = viewsLog { log ->
        /*
		lateinit var body: Body

		val view = worldView {
			createBody {
				setPosition(0, -10)
			}.fixture {
				shape = BoxShape(100, 20)
				density = 0f
			}.setView(graphics {
				fill(Colors.RED) {
					rect(-50f, -10f, 100f, 20f)
					//anchor(0.5, 0.5)
				}
			})

			// Dynamic Body
			body = createBody {
				type = BodyType.DYNAMIC
				setPosition(0, 10)
			}.fixture {
				shape = BoxShape(2f, 2f)
				density = 1f
				friction = .2f
			}.setView(solidRect(2f, 2f, Colors.GREEN).anchor(.5, .5))
		}

		assertEquals(10f, body.position.y)
		for (n in 0 until 40) view.updateSingleView(16.milliseconds)
		assertEquals(true, body.position.y < 8f)
         */
	}
}
