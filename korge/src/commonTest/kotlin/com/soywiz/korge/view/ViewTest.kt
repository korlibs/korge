package com.soywiz.korge.view

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.SizeInt
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewTest {
    @Test
    fun testAncestorCount() {
        val v0: View? = null
        assertEquals(0, v0.ancestorCount)
        val c2 = Container()
        val c = Container()
        val v = DummyView()
        assertEquals(0, v.ancestorCount)
        c.addChild(v)
        assertEquals(1, v.ancestorCount)
        c2.addChild(c)
        assertEquals(2, v.ancestorCount)
    }

    @Test
    fun testPositionRelativeTo() {
        lateinit var rect: SolidRect
        lateinit var rectParent: Container
        val container = Container().apply {
            scale = 2.0
            position(10, 10)
            rectParent = container {
                scale = 3.0
                rect = solidRect(100, 100).position(30, 30)
            }
        }
        assertEquals("(30, 30), (90, 90)", "${rect.pos}, ${rect.getPositionRelativeTo(container)}")
        rect.setPositionRelativeTo(container, Point(240, 240))
        assertEquals("(80, 80), (240, 240)", "${rect.pos}, ${rect.getPositionRelativeTo(container)}")
    }

    @Test
    fun testConcatMatrix() {
        val viewsForTesting = ViewsForTesting(
            windowSize = SizeInt(200, 200),
            virtualSize = SizeInt(100, 100),
        )
        lateinit var root: Container
        lateinit var middle: Container
        lateinit var leaf: Image
        root = Container().apply {
            scale(4, 2).position(20, 10)
            middle = container {
                scale(7, 3).position(50, 30)
                leaf = image(Bitmap32(32, 32)) {
                    //anchor(Anchor.MIDDLE_CENTER)
                    scale(2, 5).position(70, 90)
                }
            }
        }

        val log = arrayListOf<String>()
        log.add("[1]:${leaf.getLocalBoundsOptimizedAnchored().toStringCompat()}")
        log.add("[3]:${leaf.getBounds().toStringCompat()}")
        log.add("[2]:${leaf.getBounds(leaf).toStringCompat()}")
        log.add("[4]:${leaf.getBounds(middle).toStringCompat()}")
        log.add("[5]:${leaf.getBounds(root).toStringCompat()}")
        log.add("[6]:${leaf.getGlobalBounds().toStringCompat()}")
        log.add("[7]:${leaf.getBounds(root, inclusive = true).toStringCompat()}")
        log.add("[8]:${leaf.getWindowBoundsOrNull()?.toStringCompat()}")
        log.add("[9]:${leaf.getWindowBounds().toStringCompat()}")

        root.addTo(viewsForTesting.stage)

        log.add("[b1]:${leaf.getBounds(root, inclusive = true).toStringCompat()}")
        log.add("[b2]:${leaf.getWindowBoundsOrNull()?.toStringCompat()}")
        log.add("[b3]:${leaf.getWindowBounds().toStringCompat()}")

        assertEquals(
            """
                [1]:Rectangle(x=0, y=0, w=32, h=32)
                [3]:Rectangle(x=0, y=0, w=32, h=32)
                [2]:Rectangle(x=0, y=0, w=32, h=32)
                [4]:Rectangle(x=70, y=90, w=64, h=160)
                [5]:Rectangle(x=540, y=300, w=448, h=480)
                [6]:Rectangle(x=2180, y=610, w=1792, h=960)
                [7]:Rectangle(x=2180, y=610, w=1792, h=960)
                [8]:null
                [9]:Rectangle(x=2180, y=610, w=1792, h=960)
                [b1]:Rectangle(x=2180, y=610, w=1792, h=960)
                [b2]:Rectangle(x=4360, y=1220, w=3584, h=1920)
                [b3]:Rectangle(x=4360, y=1220, w=3584, h=1920)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun scaleWhileMaintainingAspect_byWidthAndHeight_scalesToWidthToFit() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByWidthAndHeight(
            160.0, 100000.0
        ))

        assertEquals(rect.scaledWidth, 160.0)
        assertEquals(rect.scaledHeight, 120.0)
    }

    @Test
    fun scaleWhileMaintainingAspect_byWidthAndHeight_scalesToHeightToFit() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByWidthAndHeight(
            1000000.0, 120.0
        ))

        assertEquals(rect.scaledWidth, 160.0)
        assertEquals(rect.scaledHeight, 120.0)
    }

    @Test
    fun scaleWhileMaintainingAspect_byWidth_scalesUpCorrectly() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByWidth(240.0))

        assertEquals(rect.scaledWidth, 240.0)
        assertEquals(rect.scaledHeight, 180.0)
    }

    @Test
    fun scaleWhileMaintainingAspect_byWidth_scalesDownCorrectly() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByWidth(40.0))

        assertEquals(rect.scaledWidth, 40.0)
        assertEquals(rect.scaledHeight, 30.0)
    }

    @Test
    fun scaleWhileMaintainingAspect_byHeight_scalesUpCorrectly() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByHeight(240.0))

        assertEquals(rect.scaledWidth, 320.0)
        assertEquals(rect.scaledHeight, 240.0)
    }

    @Test
    fun scaleWhileMaintainingAspect_byHeight_scalesDownCorrectly() {
        val rect = SolidRect(80.0, 60.0)

        rect.scaleWhileMaintainingAspect(ScalingOption.ByHeight(15.0))

        assertEquals(rect.scaledWidth, 20.0)
        assertEquals(rect.scaledHeight, 15.0)
    }

    @Test
    fun testZIndexTest() {
        val c = Container()
        val rect1 = c.solidRect(1, 1)
        val rect2 = c.solidRect(1, 1)
        rect1.zIndex = 0.0
        rect2.zIndex = 1.0
        fun getRenderViews(): List<View> = ArrayList<View>().also { array -> c.fastForEachChildRender { array.add(it) } }
        assertEquals(listOf(rect1, rect2), getRenderViews())
        rect1.zIndex = 1.0
        rect2.zIndex = 0.0
        assertEquals(listOf(rect2, rect1), getRenderViews())
    }
}
