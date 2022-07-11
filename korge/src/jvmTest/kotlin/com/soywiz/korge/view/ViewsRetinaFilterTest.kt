package com.soywiz.korge.view

import com.soywiz.korge.test.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import kotlin.test.*

class ViewsRetinaFilterTest : ViewsForTesting(
    defaultDevicePixelRatio = 2.0,
    log = true,
) {
    @Test
    fun test() = viewsTest {
        stage.scale = 2.0 // @TODO: Should this be like that already by being defaultDevicePixelRatio = 2.0
        assertEquals(2.0, stage.scale, absoluteTolerance = 0.01)
        //println("stage.scale=${stage.scale}")
        //stage.scale = 2.0
        val container = container {
            image(Bitmap32(512, 512, Colors.RED))
            //solidRect(512, 512, Colors.RED)
                .filters(
                    SwizzleColorsFilter("rrra"),
                    ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX),
                )
        }
        delayFrame()
        container.renderToBitmap(views)
        logAg.getLogAsString()
        assertEqualsFileReference(
            "korge/render/ViewFilterRetina.log",
            listOf(
                logAg.getLogAsString(),
            ).joinToString("\n")
        )
    }
}
