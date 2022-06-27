package com.soywiz.korge.view

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.cubic
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class GraphicsTest {
	@Test
	fun test() = suspendTest({ !OS.isAndroid }) {
		val g = Graphics().updateShape(redrawNow = true) {
			fill(Colors.RED) {
				rect(-50, -50, 100, 100)
			}
		}
		val bmp = g.bitmap.base.toBMP32()
		assertEquals(Size(101, 101), bmp.size)
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
        val bmp = g.bitmap.base.toBMP32()
        // This would fail in Android
        assertTrue(bmp.width > 0)
        assertTrue(bmp.height > 0)
    }

    @Test
    fun testGraphicsSize() {
        Graphics().updateShape {
            fill(Colors.RED) {
                rect(0, 0, 100, 100)
            }
        }.also { g ->
            assertEquals(100.0, g.width)
            assertEquals(100.0, g.height)
        }
        Graphics().updateShape {
            fill(Colors.RED) {
                rect(10, 10, 100, 100)
            }
        }.also { g ->
            assertEquals(100.0, g.width)
            assertEquals(100.0, g.height)
        }
    }

    //@Test
    //fun testPathPool() {
    //    val g = Graphics()
    //    g.updateShape {
    //        for (n in 0 until 10) {
    //            fill(Colors.RED) { rect(0, 0, 100, 100) }
    //            clear()
    //        }
    //    }
    //    assertEquals(1, g.vectorPathPool.itemsInPool)
    //    g.updateShape {
    //        for (n in 0 until 10) fill(Colors.RED) { rect(0, 0, 100, 100) }
    //    }
    //    assertEquals(10, g.vectorPathPool.itemsInPool)
    //    assertNotSame(g.vectorPathPool.alloc(), g.vectorPathPool.alloc())
    //}

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
        val graphics = Graphics().updateShape {
            fill(Colors.RED) {
                circle(0.0, 0.0, 32.0)
            }
            fill(Colors.BLUE) {
                circle(0.0, 0.0, 16.0)
            }
        }
        //println(graphics.hitShape2d)
        assertNotNull(graphics.hitShape2d, "hitShape2d should be defined")
        assertEquals(graphics, graphics.hitTestLocal(0, 0))
        assertEquals(graphics, graphics.hitTestLocal(20, 0))
        assertEquals(null, graphics.hitTestLocal(33, 0))
    }

    @Test
    fun testGraphicsTextureSize() {
        val bitmap = Graphics().updateShape(redrawNow = true) {
            stroke(Colors["#f0f0f0"], StrokeInfo(thickness = 2.0)) { rect(-75.0, -50.0, 150.0, 100.0) }
        }.bitmap
        assertEquals("153x103", "${bitmap.width}x${bitmap.height}")
    }

    @Test
    fun testUpdatingTheGraphicsBitmapAndRenderingRemovesThePreviousBitmapFromActiveTextures() {
        val g = Graphics()
        assertEquals(0, g.bitmapsToRemove.size)
        g.updateShape {
            stroke(Colors["#f0f0f0"], StrokeInfo(thickness = 2.0)) { rect(-75.0, -50.0, 150.0, 100.0) }
        }
        assertEquals(0, g.bitmapsToRemove.size)
        assertEquals("1x1", "${g.bitmap.width}x${g.bitmap.height}")
        g.redrawIfRequired()
        assertEquals(1, g.bitmapsToRemove.size)
        assertEquals("153x103", "${g.bitmap.width}x${g.bitmap.height}")
        val rc = TestRenderContext()
        g.render(rc)
        assertEquals(0, g.bitmapsToRemove.size)
    }

    @Test
    fun testGraphicsBoundsWithOnlyStrokes() {
        val p0 = Point(109, 135)
        val p1 = Point(25, 190)
        val p2 = Point(210, 250)
        val p3 = Point(234, 49)
        val g = Graphics()
        assertEquals(Rectangle(), g.getLocalBounds())
        g.updateShape {
            clear()
            stroke(Colors.DIMGREY, info = StrokeInfo(thickness = 1.0)) {
                moveTo(p0)
                lineTo(p1)
                lineTo(p2)
                lineTo(p3)
            }
            stroke(Colors.WHITE, info = StrokeInfo(thickness = 2.0)) {
                cubic(p0, p1, p2, p3)
            }
            val ratio = 0.3

            val cubic2 = Bezier(p0, p1, p2, p3).split(ratio).leftCurve
            val cubic3 = Bezier(p0, p1, p2, p3).split(ratio).rightCurve

            stroke(Colors.PURPLE, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic2)
            }
            stroke(Colors.YELLOW, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic3)
            }
        }
        assertEquals(Rectangle(25, 49, 209, 201), g.getLocalBounds())
    }
}

fun TestRenderContext() = RenderContext(LogAG())
