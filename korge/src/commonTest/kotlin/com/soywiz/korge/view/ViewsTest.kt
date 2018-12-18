package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewsTest : ViewsForTesting() {
	val tex = Bitmap32(10, 10)

	@Test
	fun testBounds() = viewsTest {
		val image = Image(tex)
		image.x = 100.0
		image.y = 100.0
		views.stage += image
		assertEquals(Rectangle(100, 100, 10, 10), image.getGlobalBounds())
	}

	@Test
	fun removeFromParent() = viewsTest {
		val s1 = Container().apply { name = "s1" }
		val s2 = Container().apply { name = "s2" }
		val s3 = Container().apply { name = "s3" }
		views.stage += s1
		s1 += s2
		s1 += s3
		assertNotNull(s1["s2"])
		assertNotNull(s1["s3"])

		s1 -= s3
		assertNotNull(s1["s2"])
		assertNull(s1["s3"])
	}

	@Test
	fun sortChildren() = viewsTest {
		lateinit var a: View
		lateinit var b: View
		lateinit var c: View
		val s1 = Container().apply {
			this += SolidRect(100, 100, Colors.RED).apply { a = this; y = 100.0 }
			this += SolidRect(100, 100, Colors.RED).apply { b = this; y = 50.0 }
			this += SolidRect(100, 100, Colors.RED).apply { c = this; y = 0.0 }
		}

		fun View.toStr() = "($index,${y.niceStr})"
		fun dump() = "${a.toStr()},${b.toStr()},${c.toStr()}::${s1.children.map { it.toStr() }}"
		assertEquals("(0,100),(1,50),(2,0)::[(0,100), (1,50), (2,0)]", dump())
		s1.sortChildrenByY()
		assertEquals("(2,100),(1,50),(0,0)::[(0,0), (1,50), (2,100)]", dump())
	}

	@Test
	fun commonAncestorSimple() = viewsTest {
		val a = Container()
		val b = Container()
		assertEquals(a, View.commonAncestor(a, a))
		assertEquals(null, View.commonAncestor(a, null))
		assertEquals(null, View.commonAncestor(a, b))
	}

	@Test
	fun commonAncestor2() = viewsTest {
		val a = Container()
		val b = Container()
		a += b
		assertEquals(a, View.commonAncestor(a, b))
		assertEquals(a, View.commonAncestor(b, a))
	}

	@Test
	fun size() = viewsTest {
		val c = Container()
		val s1 = SolidRect(100, 100, Colors.RED)
		val s2 = SolidRect(100, 100, Colors.RED)
		c += s1.apply { x = 0.0 }
		c += s2.apply { x = 100.0 }
		assertEquals(200, c.width.toInt())
		assertEquals(100, c.height.toInt())
		assertEquals(1.0, c.scaleX)
		c.width = 400.0
		assertEquals(400, c.width.toInt())
		assertEquals(100, c.height.toInt())
		assertEquals(2.0, c.scaleX)
	}
}
