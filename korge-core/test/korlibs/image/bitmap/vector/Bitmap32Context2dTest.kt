@file:Suppress("UnusedImport")

package korlibs.image.bitmap.vector

import korlibs.datastructure.*
import korlibs.memory.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.io.async.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.encoding.*
import korlibs.image.vector.*
import korlibs.platform.*
import kotlin.test.*

class Bitmap32Context2dTest {
    @Test
    fun testVisualRendered() = suspendTest {
        if (Platform.isNative) return@suspendTest
        //if (OS.isMac) return@suspendTest // Ignore on MAC since this fails on travis on K/N?
        //if (OS.isTvos) return@suspendTest // Ignore on MAC since this fails on travis on K/N?

        val bitmaps = listOf(Bitmap32(128, 128, premultiplied = false), NativeImage(128, 128))
        for (bmp in bitmaps) {
            bmp.context2d {
                //bmp.getContext2d().apply {
                //fill(ColorPaint(Colors.BLUE))
                keep {
                    scale(2.0, 1.0)
                    rotate(15.degrees)
                    fill(
                        GradientPaint(
                            GradientKind.LINEAR,
                            8.0, 8.0, 0.0,
                            32.0, 32.0, 1.0,
                            //32.0, 8.0, 1.0,
                            stops = DoubleArrayList(0.0, 1.0),
                            colors = IntArrayList(Colors.BLUE.value, Colors.RED.value),
                            transform = Matrix.IDENTITY.scaled(2.0, 0.75)
                        )
                    )
                    if (true) {
                        keep {
                            beginPath()
                            moveTo(Point(8, 8))
                            quadTo(Point(40, 0), Point(64, 32))
                            lineTo(Point(8, 64))
                            close()

                            //fillRect(8, 8, 32, 64)
                            rect(8, 8, 32, 64)
                            rectHole(16, 16, 16, 32)

                            fill()
                        }
                    } else {
                    }
                }
            }
        }
        val out = Bitmap32(256, 128, premultiplied = false)
        out.put(bitmaps[0].toBMP32(), 0, 0)
        out.put(bitmaps[1].toBMP32(), 128, 0)

        //runBlocking {
        //showImageAndWait(out)
        //}
    }

    @Test
    fun renderContext2dWithImage() =
        suspendTest({ !Platform.isJsNodeJs }) { // @TODO: Check why this is not working on Node.JS
            val pngBytes =
                "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgAQMAAABJtOi3AAAAA1BMVEVrVPMZmyLtAAAAC0lEQVR4AWMY5AAAAKAAAVQqnscAAAAASUVORK5CYII=".fromBase64()
            PNG.decode(pngBytes)

            val img = nativeImageFormatProvider.decode(pngBytes)

            val rendered = NativeImage(128, 128).context2d {
                rect(0, 0, 100, 100)
                fill(BitmapPaint(img, Matrix.IDENTITY))
            }
            val bmp = rendered.toBMP32()

            // @TODO: This should work on native too!
            if (!Platform.isNative) {
                assertEquals("#6b54f3ff", bmp[0, 0].hexString)
                assertEquals("#6b54f3ff", bmp[31, 31].hexString)
                //assertEquals("#6b54f3ff", bmp[99, 99].hexString) // @TODO: This should work too on Node.JS or should not work on JVM?
                assertEquals("#00000000", bmp[101, 101].hexString)
            }
        }

    @Test
    fun testBug1() = suspendTest(timeout = null) {
        val color = Colors.RED
        run {
            //val bmp = NativeImage(20, 200).context2d {
            val bmp = Bitmap32(20, 200, premultiplied = false).context2d {
                fillStyle = ColorPaint(color)
                beginPath()
                //rect(0, 20, 20, 180)
                rect(0, 20, 20, 160)
                triangleLeftUp(20.0, 0.0, 180.0)
                triangleLeftDown(20.0, 0.0, 20.0)
                fill()
            }
            //bmp.showImageAndWait()
            assertEquals(color, bmp.toBMP32()[10, 100])
        }
        run {
            //val bmp = NativeImage(200, 20).context2d {
            val bmp = Bitmap32(200, 20, premultiplied = false).context2d {
                fillStyle = ColorPaint(color)
                beginPath()
                rect(20, 0, 160, 20)
                triangleUpLeft(20.0, 180.0, 0.0)
                triangleUpRight(20.0, 20.0, 0.0)
                fill()
            }
            //bmp.showImageAndWait()
            assertEquals(color, bmp.toBMP32()[100, 10])
        }
    }

    @Test
    fun testLineDash() = suspendTest {
        val shape: Shape = buildShape {
            stroke(Colors.WHITE, 4.0, lineDash = doubleArrayListOf(20.0, 10.0)) {
                line(Point(0, 0), Point(100, 100))
            }
        }
        Bitmap32(100, 100).context2d {
            assertEquals(null, this.lineDash)
            //this.draw(shape)
            (shape as PolylineShape).setState(this)
            assertEquals(4.0, this.lineWidth)
            assertEquals(doubleArrayListOf(20.0, 10.0), this.lineDash)

        }
            //.showImageAndWait()
    }
}


fun VectorBuilder.triangleLeftUp(size: Double, x: Double, y: Double) = this.triangle(x, y, x, y + size, x + size, y)
fun VectorBuilder.triangleLeftDown(size: Double, x: Double, y: Double) = this.triangle(x, y, x, y - size, x + size, y)

fun VectorBuilder.triangleUpLeft(size: Double, x: Double, y: Double) = this.triangle(x, y, x, y + size, x - size, y)
fun VectorBuilder.triangleUpRight(size: Double, x: Double, y: Double) = this.triangle(x, y, x, y + size, x + size, y)

fun VectorBuilder.triangle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) {
    //this.rect()
    this.moveTo(Point(x1, y1))
    this.lineTo(Point(x2, y2))
    this.lineTo(Point(x3, y3))
    this.lineTo(Point(x1, y1))
    //this.close() // @TODO: Is this a Bug? But still we have to handle strange cases like this one to be consistent with other rasterizers.
    //println("TRIANGLE: ($x1,$y1)-($x2,$y2)-($x3,$y3)")
}
