package com.soywiz.korge.view.filter

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.mask.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korma.geom.*
import org.junit.*

class MaskAndBackdropFilterScreenshotJvmTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(100, 100)) {
        solidRect(width, height, Colors.GREEN)

        //val fill1 = LinearGradientPaint(0, 0, 100, 100).add(0.0, Colors.RED).add(1.0, Colors.BLUE)
        var maskView = circle(50.0, renderer = GraphicsRenderer.GPU).xy(25, 25).visible(false)
        val circle1 = circle(50.0, renderer = GraphicsRenderer.GPU, fill = Colors.RED)
            //val circle1 = solidRect(200, 200, Colors.PURPLE)
            //.filters(DropshadowFilter())
            //.filters(BlurFilter())
            //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            .mask(maskView)
            //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
        //.mask(solidRect(100, 100, Colors.WHITE).xy(50, 50).visible(false))

        roundRect(40, 40, 16, 16).xy(15, 15)
            .also { it.renderer = GraphicsRenderer.GPU }
            .backdropFilters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            //.backdropFilters(BlurFilter())

        assertScreenshot(posterize = 4)
    }
}
