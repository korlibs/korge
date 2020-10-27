package com.soywiz.korim.vector.rasterizer

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class RasterizerTest {
    @Test
    fun test() = suspendTest {
        //Bitmap32(100, 100).context2d {
        //    fill(Colors.RED) {
        //        circle(50, 50, 40)
        //    }
        //}.writeTo("/tmp/1/b.png".uniVfs, PNG)

        val rast = Rasterizer()
        rast.quality = 1
        rast.path.reset()
        rast.path.moveTo(0.0, 10.0)
        rast.path.lineTo(2.0, 0.0)
        rast.path.lineTo(10.0, 0.0)
        rast.path.lineTo(10.0, 10.0)
        rast.path.close()
        val log = arrayListOf<String>()
        val stats = Rasterizer.Stats()
        rast.rasterizeFill(Rectangle(0, 0, 10, 10), quality = 8, stats = stats) { a, b, y ->
            log += "rast(${(a.toDouble() / RastScale.RAST_FIXED_SCALE).niceStr}, ${(a.toDouble() / RastScale.RAST_FIXED_SCALE).niceStr}, ${(a.toDouble() / RastScale.RAST_FIXED_SCALE).niceStr})"
            //println(log.last())
        }
        //assertEquals(Rasterizer.Stats(edgesChecked=380, edgesEmitted=80, yCount=95), stats)
        assertEquals(Rasterizer.Stats(edgesChecked = 190, edgesEmitted = 80, yCount = 88), stats)
    }

    @Test
    fun test2() = suspendTest {
        val bmp1 = Bitmap32(100, 100).context2d {
            //debug = true
            fill(
                createLinearGradient(0, 0, 0, 100) {
                    add(0.0, Colors.BLUE)
                    add(1.0, Colors.GREEN)
                }
            ) {
                moveTo(0, 25)
                lineTo(100, 0)
                lineToV(100)
                lineToH(-100)
                close()
            }
        }
        val shipSize = 24
        val bmp2 = Bitmap32(shipSize, shipSize).context2d {
            stroke(Colors.RED, lineWidth = shipSize * 0.05, lineCap = LineCap.ROUND) {
                moveTo(shipSize * 0.5, 0.0)
                lineTo(shipSize, shipSize)
                lineTo(shipSize * 0.5, shipSize * 0.8)
                lineTo(0, shipSize)
                close()
            }
        }
        val bmp3 = Bitmap32(3, (shipSize * 0.3).toInt()).context2d {
            lineWidth = 1.0
            lineCap = LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(width / 2, 0)
                lineToV(height)
            }
        }
        //bmp1.showImageAndWait()
        //bmp2.showImageAndWait()
        //bmp3.showImageAndWait()
    }

    @Test
    fun testLineJoin() = suspendTest {
        // https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/lineJoin
        val bmp = NativeImageOrBitmap32(500, 500, native = false).context2d {
            //val bmp = NativeImageOrBitmap32(150, 150, native = true).context2d {
            lineWidth = 10.0
            for ((i, lineJoin) in listOf(LineJoin.ROUND, LineJoin.BEVEL, LineJoin.MITER).withIndex()) {
                this.lineJoin = lineJoin
                keep {
                    beginPath()
                    moveTo(-5, 5 + i * 40)
                    lineTo(35, 45 + i * 40)
                    lineTo(75, 5 + i * 40)
                    lineTo(115, 45 + i * 40)
                    lineTo(155, 5 + i * 40)
                    stroke()
                }

                keep {
                    translate(0, 350)
                    beginPath()
                    moveTo(-5, 5 + i * 40)
                    quadTo(35, 45 + i * 40, 75, 5 + i * 40)
                    quadTo(115, 45 + i * 40, 155, 5 + i * 40)
                    stroke()
                }
            }
            beginPath()
            //lineJoin = LineJoin.MITER
            lineJoin = LineJoin.BEVEL
            circle(250, 200, 30)
            stroke()

            beginPath()
            moveTo(150, 200)
            lineTo(200, 250)
            lineTo(150, 300)
            lineTo(100, 250)
            //lineTo(150, 200)
            close()
            stroke()

            for (n in listOf(LineCap.BUTT, LineCap.ROUND, LineCap.SQUARE)) {
                keep {
                    translate(250, 25)
                    beginPath()
                    lineCap = n
                    moveTo(n.ordinal * 20, 0)
                    lineTo(n.ordinal * 20, 50)
                    stroke()
                }
            }

            for (n in listOf(LineCap.BUTT, LineCap.ROUND, LineCap.SQUARE)) {
                keep {
                    translate(25, 250)
                    beginPath()
                    lineCap = n
                    moveTo(n.ordinal * 20, 0)
                    lineTo(n.ordinal * 20, -50)
                    stroke()
                }
            }

            keep {
                val radius = 50
                translate(400, 100)
                beginPath()
                lineCap = LineCap.ROUND
                val colorA = Colors.RED
                val colorB = Colors.BLUE
                for (n in 0 until 360 step 30) {
                    stroke(RGBA.interpolate(colorA, colorB, n.toDouble() / 360)) {
                        beginPath()
                        moveTo(0, 0)
                        lineTo(n.degrees.cosine * radius, n.degrees.sine * radius)
                    }
                }
            }

            // https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/strokeStyle
            keep {
                translate(300, 300)
                lineWidth = 1.0
                for (i in 0 until 6) {
                    for (j in 0 until 6) {
                        strokeStyle = com.soywiz.korim.paint.ColorPaint(RGBA(0, (255 - 42.5 * i).toInt(), (255 - 42.5 * j).toInt(), 255))
                        beginPath();
                        //arc(12.5 + j * 25, 12.5 + i * 25, 10.0, 0.0, PI * 2, true)
                        arc(12.5 + j * 25, 12.5 + i * 25, 10.0, 0.degrees, 360.degrees)
                        stroke();
                    }
                }
            }
        }
        //bmp.showImageAndWait()
    }

    @Test
    fun testLineJoin2() = suspendTest {
        val bmp = NativeImageOrBitmap32(500, 500, native = false).context2d {
            beginPath()
            lineCap = LineCap.SQUARE
            lineWidth = 30.0
            moveTo(200, 200)
            lineTo(300, 100)
            stroke()
        }
        //bmp.showImageAndWait()
    }
}
