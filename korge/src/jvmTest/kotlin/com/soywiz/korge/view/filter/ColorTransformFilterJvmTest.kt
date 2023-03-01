package com.soywiz.korge.view.filter

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import kotlin.test.*

class ColorTransformFilterJvmTest {
    @Test
    fun test() = korgeScreenshotTest(30, 30) {
        val rect = solidRect(10, 10, Colors.DARKGRAY).xy(10, 10)
        rect.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+127, 0, +127, +255)))
        assertScreenshot()
    }
}
