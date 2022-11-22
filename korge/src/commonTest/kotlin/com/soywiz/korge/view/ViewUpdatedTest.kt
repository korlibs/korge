package com.soywiz.korge.view

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korma.geom.degrees
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KMutableProperty0
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewUpdatedTest {
    @Test
    fun testStandard() = updatePropertyTest({ solidRect(10.0, 10.0) }) {
        assertUpdatedOnce(view::x, 11.0)
        assertUpdatedOnce(view::y, 12.0)
        assertUpdatedOnce(view::scaleX, 2.0)
        assertUpdatedOnce(view::scaleY, 4.0)
        assertUpdatedOnce(view::alpha, 0.5)
        assertUpdatedOnce(view::colorMul, Colors.BLUE)
        assertUpdatedOnce(view::rotation, 45.degrees)
        assertUpdatedOnce(view::skewX, 90.degrees)
        assertUpdatedOnce(view::skewY, 1.degrees)
        assertUpdatedOnce(view::visible, false)
    }

    @Test
    fun testSolidRect() = updatePropertyTest({ solidRect(10.0, 10.0) }) {
        assertUpdatedOnce(view::width, 5.0)
        assertUpdatedOnce(view::height, 5.0)
        assertUpdatedOnce(view::color, Colors.RED)
        assertUpdatedOnce(view::anchorX, 0.5)
        assertUpdatedOnce(view::anchorY, 0.75)
        assertUpdatedOnce(view::whiteBitmap, Bitmap32(1, 1, RGBAPremultiplied(-1)).slice())
    }

    @Test
    fun testImage() = updatePropertyTest({ image(Bitmap32(1, 1, Colors.RED.premultiplied)) }) {
        assertUpdatedOnce(view::bitmap, Bitmap32(1, 1, RGBAPremultiplied(-1)).slice())
        assertUpdatedOnce(view::anchorX, 0.5)
        assertUpdatedOnce(view::anchorY, 0.75)

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
            views.startFrame()
            block()
            assertEquals(expectedCount, views.updatedSinceFrame, message = "Should update view::${name}")
            views.startFrame()
            block()
            assertEquals(0, views.updatedSinceFrame, message = "Shouldn't re-update view::${name}")
        }
    }
}
