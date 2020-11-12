import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(width = 1280, height = 720, bgcolor = Colors["#2b2b2b"]) {
    val native = true
    //val native = false
    val image = resourcesVfs["korge.png"].readBitmap()
    val bmpResult = measureTimeWithResult {
        NativeImageOrBitmap32(1280, 720, premultiplied = false, native = native).context2d {
            for ((index, lineCap) in listOf(LineCap.ROUND, LineCap.SQUARE, LineCap.BUTT).withIndex()) {
                keep {
                    translate(128 + 256 * index, 128)
                    val NSTEPS = 32
                    for (n in 0 until NSTEPS) {
                        val ratio = n.toDouble() / NSTEPS
                        val angle = 360.degrees * ratio
                        val radius = 96 - ratio * 16
                        //clip({ circle(0.0, 0.0, 64.0) }) {
                            stroke(RGBA.interpolate(Colors.GREEN, Colors.BLUE, ratio), StrokeInfo(thickness = 1.0 + ratio * 6, startCap = lineCap, endCap = lineCap)) {
                                moveTo(0, 0)
                                lineTo(angle.cosine * radius, angle.sine * radius)
                            }
                        //}
                    }
                }
            }
            keep {
                translate(32, 320)
                fill(LinearGradientPaint(0, 0, 128, 128).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)) {
                    rect(0, 0, 128, 128)
                }
            }
            keep {
                translate(192, 320)
                fill(RadialGradientPaint(64, 64, 16, 64, 64, 64).add(0.0, Colors.BLUE).add(1.0, Colors.PURPLE)) {
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
    println("Time to render: ${bmpResult.time}")
    image(bmpResult.result)
}
