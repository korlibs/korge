package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*

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
                                val ratio = n.toDouble() / N_STEPS
                                val angle = 360.degrees * ratio
                                val radius = 96 - ratio * 16
                                //clip({ circle(0.0, 0.0, 64.0) }) {
                                stroke(
                                    RGBA.interpolate(Colors.GREEN, Colors.BLUE, ratio.toRatio()),
                                    StrokeInfo(thickness = 1.0 + ratio * 6, startCap = lineCap, endCap = lineCap)
                                ) {
                                    moveTo(0, 0)
                                    lineTo(angle.cosineD * radius, angle.sineD * radius)
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
                        fill(BitmapPaint(image, MMatrix().scale(0.25, 0.25))) {
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
