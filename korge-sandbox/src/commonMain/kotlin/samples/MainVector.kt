package samples

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainVector : ScaledScene(1280, 720) {
    companion object {
        const val N_STEPS = 32
    }
    override suspend fun SContainer.sceneMain() {
        suspend fun reload(native: Boolean) {
            removeChildren()
            uiCheckBox(text = "native", checked = native) {
                onChange {
                    launchImmediately { reload(it.checked) }
                }
            }
            val image = resourcesVfs["korge.png"].readBitmap()
            val bmpResult = measureTimeWithResult {
                NativeImageOrBitmap32(1280, 720, premultiplied = true, native = native).context2d {
                    listOf(LineCap.ROUND, LineCap.SQUARE, LineCap.BUTT).forEachIndexed { index, lineCap ->
                        keep {
                            translate(128 + 256 * index, 128)

                            for (n in 0 until N_STEPS) {
                                val ratio = n.toFloat() / N_STEPS
                                val angle = 360.degrees * ratio
                                val radius = 96 - ratio * 16
                                //clip({ circle(0.0, 0.0, 64.0) }) {
                                stroke(
                                    RGBA.interpolate(Colors.GREEN, Colors.BLUE, ratio.toRatio()),
                                    StrokeInfo(thickness = 1f + ratio * 6, startCap = lineCap, endCap = lineCap)
                                ) {
                                    moveTo(Point(0, 0))
                                    lineTo(Point(angle.cosineD * radius, angle.sineD * radius))
                                }
                                //}
                            }
                        }
                    }
                    keep {
                        translate(32, 320)
                        fill(
                            LinearGradientPaint(0, 0, 128, 128)
                                .add(0.0, Colors.BLUE)
                                .add(1.0, Colors.GREEN)
                        ) {
                            rect(0, 0, 128, 128)
                        }
                    }
                    keep {
                        translate(192, 320)
                        fill(
                            RadialGradientPaint(64, 64, 16, 64, 64, 64)
                                .add(0.0, Colors.BLUE)
                                .add(1.0, Colors.PURPLE)
                        ) {
                            rect(0, 0, 128, 128)
                        }
                    }
                    keep {
                        translate(356, 320)
                        fill(BitmapPaint(image, MMatrix().scale(0.25, 0.25).immutable)) {
                            rect(0, 0, 128, 128)
                        }
                    }
                }
            }
            println("Time to render: ${bmpResult.time}, native=$native")
            image(bmpResult.result)
        }
        reload(native = true)
    }
}
