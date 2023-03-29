package korlibs.korge.view

import assertEqualsFloat
import korlibs.korge.tests.ViewsForTesting
import korlibs.image.bitmap.Bitmap32
import korlibs.math.geom.*
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
        assertEqualsFloat(listOf(Point(80, 80), Point(240, 240)), listOf(rect.pos, rect.getPositionRelativeTo(container)), 0.1)
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
                leaf = image(Bitmap32(32, 32, premultiplied = false)) {
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
        log.add("[9]:${leaf.windowBounds.toStringCompat()}")

        root.addTo(viewsForTesting.stage)

        log.add("[b1]:${leaf.getBounds(root, inclusive = true).toStringCompat()}")
        log.add("[b2]:${leaf.getWindowBoundsOrNull()?.toStringCompat()}")
        log.add("[b3]:${leaf.windowBounds.toStringCompat()}")

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

    @Test
    fun testRemoveFromParentUpdatesZIndex() {
        val c = Container()
        val rect1 = c.solidRect(1, 1).name("a")
        val rect2 = c.solidRect(1, 1).name("b")
        val rect3 = c.solidRect(1, 1).name("c")
        val ctx = TestRenderContext()
        fun render() = c.render(ctx)

        fun assertEqualsOperation(block: () -> Unit, expected: List<View>) {
            block()
            render()
            assertEquals(expected, c.children.toList())
            assertEquals(expected, c.__childrenZIndex.toList())
            for (n in 0 until c.numChildren) {
                assertEquals(n, c.getChildAt(n).index)
                assertEquals(c, c.getChildAt(n).parent)
            }
        }

        assertEqualsOperation({ }, listOf(rect1, rect2, rect3))
        assertEqualsOperation({ rect2.removeFromParent() }, listOf(rect1, rect3))
        assertEqualsOperation({ c.addChild(rect2) }, listOf(rect1, rect3, rect2))
        assertEqualsOperation({ c.swapChildrenAt(1, 2) }, listOf(rect1, rect2, rect3))
        assertEqualsOperation({ c.removeChildrenIf { _, child -> child == rect1 || child == rect3 } }, listOf(rect2))
        assertEqualsOperation({ c.addChildAt(rect3, 0) }, listOf(rect3, rect2))
        assertEqualsOperation({ c.addChildAt(rect1, c.numChildren) }, listOf(rect3, rect2, rect1))
    }

    @Test
    fun testBoundsInOtherView() {
        lateinit var container1: Container
        lateinit var container2: Container
        lateinit var view: View
        val container0 = Container().apply {
            scale(6)
            container1 = container { scale(8.0); view = solidRect(100, 100).xy(100, 50).scale(2) }
            container2 = container { scale(4.0); xy(200, 200) }
        }
        assertEquals(
            """
                null:Rectangle(x=4800, y=2400, width=9600, height=9600)
                root:Rectangle(x=800, y=400, width=1600, height=1600)
                parent:Rectangle(x=100, y=50, width=200, height=200)
                other:Rectangle(x=150, y=50, width=400, height=400)
                self:Rectangle(x=0, y=0, width=100, height=100)
            """.trimIndent(),
            """
                null:${view.getBoundsInSpace(null)}
                root:${view.getBoundsInSpace(container0)}
                parent:${view.getBoundsInSpace(container1)}
                other:${view.getBoundsInSpace(container2)}
                self:${view.getBoundsInSpace(view)}
            """.trimIndent()
        )
    }

    @Test
    fun testBounds() {
        lateinit var container1: Container
        lateinit var container2: Container
        lateinit var container3: Container
        lateinit var container4: Container
        lateinit var container5: Container
        lateinit var view1: View
        lateinit var view2: View

        container1 = Container().apply {
            container2 = container {
                view1 = solidRect(100, 100).xy(200, 200)
            }
            container3 = container {
                container4 = container {
                    container5 = container {
                        scale(2.0)
                        view2 = solidRect(100, 100)
                    }
                }
            }
        }

        val logs = arrayListOf<String>()

        fun act(name: String, block: () -> Unit = {}) {
            block()
            logs += "$name:" + container1.getBounds().toStringCompat()
        }

        act("initial")
        act("moveView") { view1.xy(300, 0) }
        view1.width = 400.0
        view1.x = 100.0
        logs += "view_bounds:" + view1.getBounds()
        act("reinsertView1") { container5.addChild(view1) }

        assertEquals(
            """
                initial:Rectangle(x=0, y=0, w=300, h=300)
                moveView:Rectangle(x=0, y=0, w=400, h=200)
                view_bounds:Rectangle(x=0, y=0, width=400, height=100)
                reinsertView1:Rectangle(x=0, y=0, w=1000, h=200)
            """.trimIndent(),
            logs.joinToString("\n")
        )
    }
}
