package com.soywiz.korge.view

import com.soywiz.korge.render.Texture
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ViewsTest : ViewsForTesting() {
	val tex = Texture(views.ag.createTexture(), 10, 10)

	@Test
	fun testBounds() = syncTest {
		val image = views.image(tex)
		image.x = 100.0
		image.y = 100.0
		views.stage += image
		assertEquals(Rectangle(100, 100, 10, 10), image.getGlobalBounds())
	}

	@Test
	fun removeFromParent() = viewsTest {
		val s1 = views.container().apply { name = "s1" }
		val s2 = views.container().apply { name = "s2" }
		val s3 = views.container().apply { name = "s3" }
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
	fun commonAncestorSimple() = viewsTest {
		val a = views.container()
		val b = views.container()
		assertEquals(a, View.commonAncestor(a, a))
		assertEquals(null, View.commonAncestor(a, null))
		assertEquals(null, View.commonAncestor(a, b))
	}

	@Test
	fun commonAncestor2() = viewsTest {
		val a = views.container()
		val b = views.container()
		a += b
		assertEquals(a, View.commonAncestor(a, b))
		assertEquals(a, View.commonAncestor(b, a))
	}

	@Test
	fun size() = viewsTest {
		val c = views.container()
		val s1 = views.solidRect(100, 100, Colors.RED)
		val s2 = views.solidRect(100, 100, Colors.RED)
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
