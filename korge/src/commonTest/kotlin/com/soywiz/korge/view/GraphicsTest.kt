package com.soywiz.korge.view

import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class GraphicsTest {
	@Test
	fun test() = suspendTest({ !OS.isAndroid }) {
		val g = Graphics().apply {
			fill(Colors.RED) {
				rect(-50, -50, 100, 100)
			}
		}
		val rc = TestRenderContext()
		g.render(rc)
		val bmp = g.bitmap.bmp.toBMP32()
		assertEquals(Size(100, 100), bmp.size)
		//assertEquals("#ff0000ff", bmp[0, 0].hexString)
		//assertEquals("#ff0000ff", bmp[99, 99].hexString)
        assertEquals("#ff0000ff", bmp[1, 1].hexString)
        assertEquals("#ff0000ff", bmp[98, 98].hexString)
		assertEquals(-50.0, g._sLeft)
		assertEquals(-50.0, g._sTop)
	}

    @Test
    fun testEmptyGraphics() = suspendTest({ !OS.isAndroid }) {
        val g = Graphics().apply {
        }
        val rc = TestRenderContext()
        g.render(rc)
        val bmp = g.bitmap.bmp.toBMP32()
        // This would fail in Android
        assertTrue(bmp.width > 0)
        assertTrue(bmp.height > 0)
    }

    @Test
    fun testGraphicsSize() {
        Graphics().apply {
            fill(Colors.RED) {
                rect(0, 0, 100, 100)
            }
        }.also { g ->
            assertEquals(100.0, g.width)
            assertEquals(100.0, g.height)
        }
        Graphics().apply {
            fill(Colors.RED) {
                rect(10, 10, 100, 100)
            }
        }.also { g ->
            assertEquals(100.0, g.width)
            assertEquals(100.0, g.height)
        }
    }

    @Test
    fun testPathPool() {
        val g = Graphics()
        g.clear()
        for (n in 0 until 10) {
            g.fill(Colors.RED) { g.rect(0, 0, 100, 100) }
            g.clear()
        }
        assertEquals(1, g.graphicsPathPool.itemsInPool)
        for (n in 0 until 10) g.fill(Colors.RED) { g.rect(0, 0, 100, 100) }
        g.clear()
        assertEquals(10, g.graphicsPathPool.itemsInPool)
        assertNotSame(g.graphicsPathPool.alloc(), g.graphicsPathPool.alloc())
    }

    @Test
    fun testCollidesWithShape() {
        lateinit var circle1: Circle
        lateinit var circle2: Circle
        val container = Container().apply {
            scale(1.2)
            circle1 = circle(128.0, Colors.RED)
                .scale(0.75)
                .anchor(Anchor.MIDDLE_CENTER)
            circle2 = circle(64.0, Colors.BLUE)
                .scale(1.5)
                .anchor(Anchor.MIDDLE_CENTER)
        }
        assertEquals(true, circle1.xy(0, 0).collidesWithShape(circle2.xy(0, 0)))
        assertEquals(true, circle1.xy(180, 0).collidesWithShape(circle2.xy(0, 0)))
        assertEquals(false, circle1.xy(200, 0).collidesWithShape(circle2.xy(0, 0)))
        assertEquals(true, circle1.xy(160, 100).collidesWithShape(circle2.xy(0, 0)))
        assertEquals(false, circle1.xy(167, 100).collidesWithShape(circle2.xy(0, 0)))
    }

    @Test
    fun testMultiShapeHitTesting() {
        val graphics = Graphics().apply {
            fill(Colors.RED) {
                circle(0.0, 0.0, 32.0)
            }
            fill(Colors.BLUE) {
                circle(0.0, 0.0, 16.0)
            }
        }
        assertEquals(graphics, graphics.hitTest(0, 0))
        assertEquals(graphics, graphics.hitTest(20, 0))
        assertEquals(null, graphics.hitTest(33, 0))
    }
}

fun TestRenderContext() = RenderContext(LogAG())
