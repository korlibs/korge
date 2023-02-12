package com.soywiz.korge.view

import com.soywiz.korge.testing.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import kotlin.test.*

class ViewsRetinaFilterTest {
    @Test
    fun test() = korgeScreenshotTest(
        100, 100,
        devicePixelRatio = 2.0,
    ) {
        val container = container {
            image(Bitmap32(50, 50, Colors.RED.premultiplied))
            //solidRect(512, 512, Colors.RED)
                .filters(
                    SwizzleColorsFilter("rrra"),
                    ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX),
                )
        }
        assertScreenshot()
    }
}
