package com.soywiz.korge.view

import com.soywiz.korge.testing.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import org.junit.*

class ReferenceViewsTest {
    @Test
    fun testClippedContainerInFlippedContainerInTexture() = korgeScreenshotTest(512, 512) {
        container {
            y = views.virtualHeightDouble; scaleY = -1.0
            clipContainer(150, 100) {
                xy(75, 50)
                image(Bitmap32(300, 400) { x, y -> if (y <= 25) Colors.BLUE else Colors.RED }.premultiplied())
            }
        }
        assertScreenshot()
    }
}
