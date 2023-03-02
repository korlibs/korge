package com.soywiz.korge.view

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class GpuShapeViewTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(450, 200)) {
        gpuShapeView {
            it.antialiased = true
            val paint1 = createLinearGradient(0, 0, 200, 200).add(0.0, Colors.BLUE.withAd(0.9)).add(1.0, Colors.WHITE.withAd(0.7))
            translate(60.0, 70.0) {
                fill(paint1, winding = Winding.EVEN_ODD) {
                    rect(0, 0, 100, 100)
                    rect(-50, -50, 70, 70)
                    rectHole(75, -50, 70, 70)
                }
            }
            translate(280.0, 70.0) {
                fill(paint1, winding = Winding.NON_ZERO) {
                    rect(0, 0, 100, 100)
                    rect(-50, -50, 70, 70)
                    rectHole(75, -50, 70, 70)
                }
            }
        }
        assertScreenshot(posterize = 6)
    }
}
