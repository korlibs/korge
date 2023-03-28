package korlibs.korge.view

import korlibs.korge.testing.*
import korlibs.korge.view.vector.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlin.test.*

class GpuShapeViewTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(450, 200)) {
        gpuShapeView {
            it.antialiased = true
            it.alpha = 0.75
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

    @Test
    fun test2() = korgeScreenshotTest(SizeInt(20, 20)) {
        gpuShapeView {
            fillStroke(fill = Colors.TRANSPARENT_WHITE, stroke = Stroke(Colors.GREEN, thickness = 2.0)) {
                rect(.0, .0, 10.0, 10.0)
            }
        }

        gpuShapeView {
            it.xy(10.0, 0.0)
            stroke(Colors.GREEN, lineWidth = 2.0) {
                rect(0.0, 0.0, 10.0, 10.0)
            }
        }
        assertScreenshot(posterize = 6)
    }

    @Test
    fun testClipping() = korgeScreenshotTest(SizeInt(30, 20)) {
        val from = Point(0, 0)
        val to = Point(-10, -10)
        val shift = Point(5, 15)

        container {
            this.xy(shift + Point(10, 0))
            gpuShapeView(antialiased = false) {
                stroke(Colors.RED, StrokeInfo(thickness = 3.0)) {
                    line(from, to)
                }
            }
        }

        gpuShapeView(antialiased = false) {
            stroke(Colors.GREEN, StrokeInfo(thickness = 3.0)) {
                val gap = Point(20, 0)
                line(from + shift + gap, to + shift + gap)
            }
        }

        assertScreenshot(posterize = 6)
    }
}
