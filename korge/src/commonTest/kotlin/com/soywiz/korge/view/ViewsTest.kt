package com.soywiz.korge.view

import com.soywiz.klock.seconds
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class ViewsTest : ViewsForTesting() {
    val tex = Bitmap32(10, 10)

    @Test
    fun testBounds() = viewsTest {
        val image = Image(tex).position(100, 100)
        views.stage += image
        assertEquals(Rectangle(100, 100, 10, 10), image.getGlobalBounds())
    }

    @Test
    fun testBounds2() = viewsTest {
        val image = Image(tex).position(-100, 100)
        views.stage += image
        assertEquals(Rectangle(-100, 100, 10, 10), image.getGlobalBounds())
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
    fun testSize() = viewsTest {
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

    @Test
    fun testTween() = viewsTest {
        val image = solidRect(100, 100, Colors.RED).position(0, 0)
        image.tween(image::x[-101], time = 4.seconds)
        assertEquals(false, image.isVisibleToUser())
    }

    @Test
    fun testRect() = viewsTest {
        assertEquals(Rectangle(0, 0, 1280, 720), this.stage.globalBounds)

        RectBase().also { addChild(it) }.also { rect1 ->
            assertEquals(Rectangle(0, 0, 0, 0), rect1.globalBounds)
        }
        Image(Bitmap32(16, 16, Colors.RED)).also { addChild(it) }.also { rect2 ->
            assertEquals(Rectangle(0, 0, 16, 16), rect2.globalBounds)
        }

        SolidRect(32, 32, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 32, 32), rect3.globalBounds)
        }

        Circle(32.0, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 64, 64).toString(), rect3.globalBounds.toString())
        }

        Graphics().also { addChild(it) }.apply { fill(Colors.RED) { rect(0, 0, 100, 100) } }.also { rect4 ->
            assertEquals(Rectangle(0, 0, 100, 100), rect4.globalBounds)
            rect4.render(views.renderContext)
            assertEquals(Rectangle(0, 0, 100, 100), rect4.globalBounds)
        }
    }

    @Test
    fun testAddUpdatable() {
        lateinit var rect: SolidRect
        val container = Container().apply {
            rect = solidRect(100, 100, Colors.RED)
            rect.addUpdater { time ->
                x++
                assertEquals(0.0.seconds, time)
                @Suppress("USELESS_IS_CHECK")
                assertTrue { this is SolidRect }
            }
        }
        container.updateSingleView(0.0)
        container.updateSingleView(0.0)
        assertEquals(0.0, container.x)
        assertEquals(3.0, rect.x) // It is 3 instead of 2 since when the addUpdater is called the body is called with time as 0.seconds once
    }

    @Test
    fun testCentering() = viewsTest {
        val rect = solidRect(100, 100, Colors.WHITE)

        rect.position(0, 0)
        rect.centerXBetween(100, 200)
        assertEquals(rect.x, 100.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        rect.centerYBetween(100, 200)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 100.0)

        rect.position(0, 0)
        rect.centerBetween(0, 0, 200, 200)
        assertEquals(rect.x, 50.0)
        assertEquals(rect.y, 50.0)

        val other = solidRect(50, 50, Colors.WHITE)

        rect.position(0, 0)
        other.position(50, 50)
        rect.centerXOn(other)
        assertEquals(rect.x, 25.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.centerYOn(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 25.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.centerOn(other)
        assertEquals(rect.x, 25.0)
        assertEquals(rect.y, 25.0)
    }

    @Test
    fun testRelativePositioning() = viewsTest {
        val rect = solidRect(100, 100, Colors.WHITE)
        val other = solidRect(50, 50, Colors.WHITE)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignLeftToLeft(other)
        assertEquals(rect.x, 50.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignLeftToRight(other)
        assertEquals(rect.x, 100.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignRightToLeft(other)
        assertEquals(rect.x, -50.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignRightToRight(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 0.0)


        rect.position(0, 0)
        other.position(50, 50)
        rect.alignTopToTop(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 50.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignTopToBottom(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 100.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignBottomToTop(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, -50.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignBottomToBottom(other)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 0.0)
    }
}
