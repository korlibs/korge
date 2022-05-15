package com.soywiz.korge.view

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korev.Event
import com.soywiz.korev.dispatch
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.component.EventComponent
import com.soywiz.korge.component.docking.sortChildrenByY
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korio.lang.portableSimpleName
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewsTest : ViewsForTesting() {
    val tex = Bitmap32(10, 10)

    @Test
    fun testFixedUpdater() {
        run {
            val view = DummyView()
            var ticks = 0
            view.addFixedUpdater(60.timesPerSecond, initial = false) { // Each 16.6666667 milliseconds
                ticks++
            }
            assertEquals(0, ticks)
            view.updateSingleView(50.milliseconds)
            assertEquals(3, ticks)
            view.updateSingleView(16.milliseconds)
            assertEquals(4, ticks) // This is 4 instead of 3 (16 < 16.6666) since the fixedUpdater approximates it to prevent micro-stuttering
            view.updateSingleView(1.milliseconds)
            assertEquals(4, ticks)
        }
        run {
            val view = DummyView()
            var ticks = 0
            view.addFixedUpdater(60.timesPerSecond, initial = true) {
                ticks++
            }
            assertEquals(1, ticks)
            view.updateSingleView(50.milliseconds)
            assertEquals(4, ticks)
        }
    }

    @Test
    fun testFixedUpdaterLimit() {
        val view = DummyView()
        var ticks = 0
        view.addFixedUpdater(1.seconds / 60, initial = true, limitCallsPerFrame = 6) {
            ticks++
        }
        assertEquals(1, ticks)
        view.updateSingleView(1000.milliseconds)
        assertEquals(7, ticks)
        view.updateSingleView(1000.milliseconds)
        assertEquals(13, ticks)
    }

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
        assertNotNull(s1["s2"].firstOrNull)
        assertNotNull(s1["s3"].firstOrNull)

        s1 -= s3
        assertNotNull(s1["s2"].firstOrNull)
        assertNull(s1["s3"].firstOrNull)
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
        assertEquals(null, View.commonAncestor(null, a))
        assertEquals(null, View.commonAncestor(a, b))
        assertEquals(null, View.commonAncestor(b, a))
    }

    @Test
    fun alignTest() = viewsTest {
        fixedSizeContainer(1280, 720) {
            scale(0.5)
            val rootView = this
            x = 32.0
            y = -100.0
            val zoomOut = solidRect(100, 100, Colors.RED) {
                anchor(0.5, 1.0)
                alignBottomToBottomOf(rootView, 150.0)
                alignRightToRightOf(rootView, 60.0)
            }
            val zoomIn = solidRect(100, 100, Colors.RED) {
                anchor(0.5, 1.0)
                println("addZoomMap")
                println(zoomOut.x)
                println(zoomOut.getBounds(this))
                alignLeftToLeftOf(zoomOut)
                alignBottomToTopOf(zoomOut, 10.0)
            }
        }
    }

    @Test
    fun alignTest2a() = viewsTest {
        val rect1 = solidRect(400, 100, Colors.RED).xy(200, 50)
        val rect2 = solidRect(83, 65, Colors.RED)
        rect2.alignTopToTopOf(rect1, 3.0).also { assertEquals(53.0, rect2.y) }
        rect2.alignBottomToTopOf(rect1, 3.0).also { assertEquals(-18.0, rect2.y) }
        rect2.alignBottomToBottomOf(rect1, 3.0).also { assertEquals(82.0, rect2.y) }
        rect2.alignTopToBottomOf(rect1, 3.0).also { assertEquals(153.0, rect2.y) }
    }

    @Test
    fun alignTest2b() = viewsTest {
        val rect1 = solidRect(400, 100, Colors.RED).xy(200, 50)
        val rect2 = solidRect(83, 65, Colors.RED)
        rect2.alignLeftToLeftOf(rect1, 3.0).also { assertEquals(203.0, rect2.x) }
        rect2.alignRightToLeftOf(rect1, 3.0).also { assertEquals(114.0, rect2.x) }
        rect2.alignRightToRightOf(rect1, 3.0).also { assertEquals(514.0, rect2.x) }
        rect2.alignLeftToRightOf(rect1, 3.0).also { assertEquals(603.0, rect2.x) }
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
    fun commonAncestorSiblings() = viewsTest {
        val a = Container()
        val b = Container()
        val c = Container()
        a += b
        a += c
        assertEquals(a, View.commonAncestor(b, c))
        assertEquals(a, View.commonAncestor(c, b))
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
        c.scaledWidth = 400.0
        assertEquals(400, c.scaledWidth.toInt())
        assertEquals(100, c.scaledHeight.toInt())
        assertEquals(2.0, c.scaleX)
    }

    @Test
    // This test is verified against AS3
    fun testSize2() = viewsTest {
        val container = Container();
        container.x = 100.0
        container.y = 100.0
        this.addChild(container)
        val contents = Graphics().apply {
            fill(Colors.RED) { circle(0.0,0.0,100.0) }
        }
        container.addChild(contents)
        assertEquals(Rectangle(-100, -100, 200, 200), contents.getBounds(container), "bounds1") // (x=-100, y=-100, w=200, h=200)
        assertEquals(Rectangle(0, 0, 200, 200), contents.getBounds(this), "bounds2") // (x=0, y=0, w=200, h=200)
    }

    @Test
    fun testTween() = viewsTest {
        val image = solidRect(100, 100, Colors.RED).position(0, 0)
        image.tween(image::x[-101], time = 4.seconds)
        assertEquals(false, image.isVisibleToUser())
    }

    private fun assertEquals(a: Rectangle, b: Rectangle) = assertEquals(a.toString(), b.toString())

    @Test
    fun testRect() = viewsTest {
        assertEquals(Rectangle(0, 0, 1280, 720), this.stage.globalBounds, "rect0")

        RectBase().also { addChild(it) }.also { rect1 ->
            assertEquals(Rectangle(0, 0, 0, 0), rect1.globalBounds, "rect1")
        }
        Image(Bitmap32(16, 16, Colors.RED)).also { addChild(it) }.also { rect2 ->
            assertEquals(Rectangle(0, 0, 16, 16), rect2.globalBounds, "rect2")
        }

        SolidRect(32, 32, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 32, 32), rect3.globalBounds, "rect3")
        }

        RoundRect(32.0, 24.0, 5.0, 5.0, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 32, 24), rect3.globalBounds, "rect4")
        }

        Circle(32.0, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 64, 64).toString(), rect3.globalBounds.toString(), "rect5")
        }

        Graphics().also { addChild(it) }.apply { fill(Colors.RED) { rect(0, 0, 100, 100) } }.also { rect4 ->
            assertEquals(Rectangle(0, 0, 100, 100), rect4.globalBounds, "rect6")
            rect4.render(views.renderContext)
            assertEquals(Rectangle(0, 0, 100, 100), rect4.globalBounds, "rect7")
        }
    }

    @Test
    fun testRoundRect() = viewsTest {
        RoundRect(32.0, 24.0, 5.0, 5.0, Colors.RED).also { addChild(it) }.also { rect3 ->
            assertEquals(Rectangle(0, 0, 32, 24), rect3.getLocalBounds(), message = "local")
            assertEquals(Rectangle(0, 0, 32, 24), rect3.globalBounds, message = "global")
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
        container.updateSingleView(0.milliseconds)
        container.updateSingleView(0.milliseconds)
        assertEquals(0.0, container.x)
        assertEquals(3.0, rect.x) // It is 3 instead of 2 since when the addUpdater is called the body is called with time as 0.secs once
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
        rect.alignLeftToLeftOf(other, padding = 5)
        assertEquals(rect.x, 55.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignLeftToRightOf(other, padding = 5)
        assertEquals(rect.x, 105.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignRightToLeftOf(other, padding = 5)
        assertEquals(rect.x, -55.0)
        assertEquals(rect.y, 0.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignRightToRightOf(other, padding = 5)
        assertEquals(rect.x, -5.0)
        assertEquals(rect.y, 0.0)


        rect.position(0, 0)
        other.position(50, 50)
        rect.alignTopToTopOf(other, padding = 5)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 55.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignTopToBottomOf(other, padding = 5)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, 105.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignBottomToTopOf(other, padding = 5)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, -55.0)

        rect.position(0, 0)
        other.position(50, 50)
        rect.alignBottomToBottomOf(other, padding = 5)
        assertEquals(rect.x, 0.0)
        assertEquals(rect.y, -5.0)
    }

    @Test
    fun testPropagateAlpha() = viewsTest {
        lateinit var container1: Container
        lateinit var container2: Container
        lateinit var container3: Container
        container1 = container {
            container2 = container {
                container3 = container {
                }
            }
        }
        container3.alpha = 0.5
        container1.alpha = 0.5
        assertEquals(0.25, container3.renderColorMul.ad, 0.03)
        container3.alpha = 0.10
        container1.alpha = 1.0
        assertEquals(0.10, container3.renderColorMul.ad, 0.03)
    }

    @Test
    fun testDoubleDispatch() = viewsTest {
        class MyEvent : Event()
        class MyOtherEvent : Event()

        fun BaseView.addEventComponent(block: (Event) -> Unit) {
            addComponent(object : EventComponent {
                override fun onEvent(event: Event) = block(event)
                override val view: BaseView get() = this@addEventComponent
            })
        }

        val log = arrayListOf<String>()

        container {
            this.addEventComponent {
                log.add("Container:${it::class.portableSimpleName}")
            }
            solidRect(100, 100) {
                this.addEventComponent {
                    log.add("SolidRect1:${it::class.portableSimpleName}")
                    if (it is MyEvent) {
                        addChildAt(SolidRect(200, 200).apply {
                            this.addEventComponent {
                                log.add("SolidRect2:${it::class.portableSimpleName}")
                            }
                        }, 0)
                        views.dispatch(MyOtherEvent())
                    }
                }
            }
        }

        views.dispatch(MyEvent())

        assertEquals(
            """
                SolidRect1:MyEvent
                SolidRect1:MyOtherEvent
                SolidRect2:MyOtherEvent
                Container:MyOtherEvent
                Container:MyEvent
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
