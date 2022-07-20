package samples

import com.soywiz.klock.measureTimeWithResult
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.ui.uiCheckBox
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.image
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.paint.BitmapPaint
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korim.paint.RadialGradientPaint
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.times
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.rect

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
                NativeImageOrBitmap32(1280, 720, premultiplied = false, native = native).context2d {
                    listOf(LineCap.ROUND, LineCap.SQUARE, LineCap.BUTT).forEachIndexed { index, lineCap ->
                        keep {
                            translate(128 + 256 * index, 128)

                            for (n in 0 until N_STEPS) {
                                val ratio = n.toDouble() / N_STEPS
                                val angle = 360.degrees * ratio
                                val radius = 96 - ratio * 16
                                //clip({ circle(0.0, 0.0, 64.0) }) {
                                stroke(
                                    RGBA.interpolate(Colors.GREEN, Colors.BLUE, ratio),
                                    StrokeInfo(thickness = 1.0 + ratio * 6, startCap = lineCap, endCap = lineCap)
                                ) {
                                    moveTo(0, 0)
                                    lineTo(angle.cosine * radius, angle.sine * radius)
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
                        fill(BitmapPaint(image, Matrix().scale(0.25, 0.25))) {
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
