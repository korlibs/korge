package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.test.*

class ViewUpdatedTest {
    @Test
    fun testStandard() = updatePropertyTest({ solidRect(10.0, 10.0) }) {
        assertUpdatedOnce(view::x, 11.0)
        assertUpdatedOnce(view::y, 12.0)
        assertUpdatedOnce(view::scaleX, 2.0)
        assertUpdatedOnce(view::scaleY, 4.0)
        assertUpdatedOnce(view::alphaF, 0.5f)
        assertUpdatedOnce(view::colorMul, Colors.BLUE)
        assertUpdatedOnce(view::rotation, 45.degrees)
        assertUpdatedOnce(view::skewX, 90.degrees)
        assertUpdatedOnce(view::skewY, 1.degrees)
        assertUpdatedOnce(view::visible, false)
    }

    @Test
    fun testSolidRect() = updatePropertyTest({ solidRect(10.0, 10.0) }) {
        assertUpdatedOnce(view::unscaledWidth, 5.0)
        assertUpdatedOnce(view::unscaledHeight, 5.0)
        assertUpdatedOnce(view::color, Colors.RED)
        assertUpdatedOnce(view::anchor, Anchor(0.5, 0.75))
        assertUpdatedOnce(view::whiteBitmap, Bitmap32(1, 1, RGBAPremultiplied(-1)).slice())
    }

    @Test
    fun testImage() = updatePropertyTest({ image(Bitmap32(1, 1, Colors.RED.premultiplied)) }) {
        assertUpdatedOnce(view::bitmap, Bitmap32(1, 1, RGBAPremultiplied(-1)).slice())
        assertUpdatedOnce(view::anchor, Anchor(0.5, 0.75))

        // @TODO: We might want to repaint when the source has been loaded and this might be asynchronous
        //assertUpdatedOnce(view::bitmapSrc, Bitmap32(1, 1, RGBAPremultiplied(-1)).slice())
    }

    fun <R> updatePropertyTest(gen: Stage.() -> R, block: UpdatePropertyTest<R>.() -> Unit) {
        UpdatePropertyTest<R>(gen).apply(block)
    }

    class UpdatePropertyTest<R>(gen: Stage.() -> R) {
        val coroutineContext = EmptyCoroutineContext
        val viewsLog = ViewsLog(coroutineContext)
        val views = viewsLog.views
        val view = gen(views.stage)

        fun <T> assertUpdatedOnce(prop: KMutableProperty0<T>, value: T, expectedCount: Int = 1) {
            assertUpdatedOnce(name = prop.name, expectedCount = expectedCount) { prop.set(value) }
        }

        fun assertUpdatedOnce(name: String = "block()", expectedCount: Int = 1, block: () -> Unit) {
            views.updatedSinceFrame.value = 0
            block()
            assertEquals(expectedCount, views.updatedSinceFrame.value, message = "Should update view::${name}")
            views.updatedSinceFrame.value = 0
            block()
            assertEquals(0, views.updatedSinceFrame.value, message = "Shouldn't re-update view::${name}")
        }
    }
}
