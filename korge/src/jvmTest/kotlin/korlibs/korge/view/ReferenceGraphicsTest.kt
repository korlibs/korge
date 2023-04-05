package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.io.file.std.*
import korlibs.korge.annotations.*
import korlibs.korge.testing.*
import korlibs.korge.view.fast.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import kotlin.test.*

class ReferenceGraphicsTest {
    @Test
    fun testGraphics() = korgeScreenshotTest(SizeInt(300, 300)) {
        cpuGraphics {
            fill(Colors.RED) {
                rect(-60, -60, 70, 70)
            }
        }

        // Circle Graphics
        circle(64.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 32.0).xy(50, 50).centered.rotation(30.degrees)

        val bmp = BitmapSlice(
            Bitmap32(64, 64) { x, y -> Colors.PURPLE }.premultipliedIfRequired(),
            RectangleInt(0, 0, 64, 64),
            name = null,
        ).virtFrame(RectangleInt(64, 64, 196, 196))
        val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)

        assertScreenshot(posterize = 6)
    }

    @Test
    fun testFSprites1() = testFSpritesN(1)

    @Test
    fun testFSprites2() = testFSpritesN(2)

    @Test
    fun testFSprites3() = testFSpritesN(3)

    @Test
    fun testFSprites4() = testFSpritesN(4)

    fun testFSpritesN(N: Int) = korgeScreenshotTest(SizeInt(512, 512)) {
        val bmp = Bitmap32(32, 32, Colors.RED.premultiplied).slice()
        val sprites = FSprites(16)
        val anchorsX = listOf(.5f, .5f, .5f, .0f)
        val anchorsY = listOf(.5f, 1f, .0f, 1f)
        sprites.apply {
            run {
                for (n in 0 until 4) {
                    alloc().also { sprite ->
                        sprite.xy(100f + 100f * n, 100f)
                        sprite.setAnchor(anchorsX[n], anchorsY[n])
                        sprite.scale(2f, 1f)
                        sprite.colorMul = Colors.WHITE.withAf(.5f)
                        sprite.setTex(bmp)
                        sprite.setTexIndex(n % N)
                    }
                }
            }
        }
        val fview = FSprites.FView(sprites, Array(N) { bmp.bmp })
        addChild(fview)

        assertScreenshot(this, "N$N", includeBackground = true)
    }

    @Test
    @OptIn(KorgeExperimental::class)
    @Ignore
    fun testGpuShapeView() = korgeScreenshotTest(SizeInt(512, 512)) {
        val korgeBitmap = resourcesVfs["korge.png"].readBitmap()
        val view = gpuShapeView({
            keep {
                translate(100, 200)
                fill(Colors.BLUE) {
                    rect(-10, -10, 120, 120)
                    rectHole(40, 40, 80, 80)
                }
                fill(Colors.YELLOW) {
                    this.circle(Point(100, 100), 40f)
                    //rect(-100, -100, 500, 500)
                    //rectHole(40, 40, 320, 320)
                }
                fill(Colors.RED) {
                    regularPolygon(6, 30.0, x = 100.0, y = 100.0)
                    //rect(-100, -100, 500, 500)
                    //rectHole(40, 40, 320, 320)
                }
            }
            keep {
                translate(100, 20)
                scale(2.0)
                run {
                    globalAlpha = 0.75
                    fillStyle = BitmapPaint(
                        korgeBitmap,
                        MMatrix().translate(50, 50).scale(0.125).immutable,
                        cycleX = CycleMethod.REPEAT,
                        cycleY = CycleMethod.REPEAT
                    )
                    fillRect(0.0, 0.0, 100.0, 100.0)
                }

                run {
                    globalAlpha = 0.9
                    fillStyle =
                        //createLinearGradient(150.0, 0.0, 200.0, 50.0)
                        createLinearGradient(0.0, 0.0, 100.0, 100.0, transform = Matrix().scaled(0.5).pretranslated(300, 0))
                            //.addColorStop(0.0, Colors.BLACK).addColorStop(1.0, Colors.WHITE)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.GREEN).addColorStop(1.0, Colors.BLUE)
                    clip({
                        circle(Point(150, 50), 50f)
                    }, {
                        fillRect(100.0, 0.0, 100.0, 100.0)
                    })
                }
                run {
                    globalAlpha = 0.9
                    fillStyle =
                        createRadialGradient(150,150,30, 130,180,70)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.GREEN).addColorStop(1.0, Colors.BLUE)
                    fillRect(100.0, 100.0, 100.0, 100.0)
                }
                run {
                    globalAlpha = 0.9
                    fillStyle =
                        createSweepGradient(175, 100)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.PURPLE).addColorStop(1.0, Colors.YELLOW)
                    fillRect(150.0, 75.0, 50.0, 50.0)
                }
            }
        }) {
            xy(50, 50)
            scale(2, 2)
            rotation = 180.degrees
        }

        assertScreenshot(posterize = 6)
    }

    @Test
    fun testGpuShapeViewFilter() = korgeScreenshotTest(SizeInt(400, 400)) {
        container {
            scale = 1.2
            circle(100.0).xy(100, 100).filters(DropshadowFilter())
        }
        assertScreenshot(posterize = 6)
    }

    @Test
    //@Ignore
    fun testBlurFilterInEmptyContainer() = korgeScreenshotTest(SizeInt(512, 512)) {
        val view = solidRect(100, 100) {
            filter = BlurFilter(4f)
        }
        assertScreenshot(this, "blur", includeBackground = true)
    }
}
