package com.soywiz.korge.view

import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.annotations.*
import com.soywiz.korge.test.assertEqualsFileReference
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.fast.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test

class ReferenceGraphicsTest : ViewsForTesting(
    windowSize = SizeInt(200, 200),
    virtualSize = SizeInt(100, 100),
    log = true,
) {
    //override fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean = kind == LogBaseAG.Kind.DRAW || kind == LogBaseAG.Kind.SHADER
    //override fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean = kind != LogBaseAG.Kind.DRAW_DETAILS
    override fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean = true

    private suspend fun testFrame() {
        logAg.clearLog()
        delayFrame()
    }

    @Test
    fun testGraphics() = viewsTest {
        graphics {
            fill(Colors.RED) {
                rect(-60, -60, 70, 70)
            }
        }

        // Circle Graphics
        circle(64.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 32.0).xy(50, 50).centered.rotation(30.degrees)

        val bmp = BitmapSlice(
            Bitmap32(64, 64) { x, y -> Colors.PURPLE },
            RectangleInt(0, 0, 64, 64),
            virtFrame = RectangleInt(64, 64, 196, 196)
        )
        val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)

        testFrame()
        assertEqualsFileReference(
            "korge/render/Graphics.log",
            listOf(
                logAg.getLogAsString(),
                image.getGlobalBounds().toString(),
                image.getLocalBounds().toString()
            ).joinToString("\n")
        )
    }

    @Test
    fun testFSprites1() = testFSpritesN(1)

    @Test
    fun testFSprites2() = testFSpritesN(2)

    @Test
    fun testFSprites3() = testFSpritesN(3)

    @Test
    fun testFSprites4() = testFSpritesN(4)

    fun testFSpritesN(N: Int) = viewsTest {
        val bmp = Bitmap32(32, 32, Colors.RED).slice()
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
        val fview = FSprites.FView(sprites, Array(N) { bmp.bmpBase })
        addChild(fview)

        testFrame()
        "korge/render/FSprites.log"
        "korge/render/FSprites1.log"
        "korge/render/FSprites2.log"
        "korge/render/FSprites3.log"
        "korge/render/FSprites4.log"
        assertEqualsFileReference(
            "korge/render/FSprites$N.log",
            listOf(
                logAg.getLogAsString(),
            ).joinToString("\n")
        )
    }

    @Test
    @OptIn(KorgeExperimental::class)
    fun testGpuShapeView() = viewsTest {
        val korgeBitmap = resourcesVfs["korge.png"].readBitmap()
        val view = gpuShapeView({
            keep {
                translate(100, 200)
                fill(Colors.BLUE) {
                    rect(-10, -10, 120, 120)
                    rectHole(40, 40, 80, 80)
                }
                fill(Colors.YELLOW) {
                    this.circle(100, 100, 40)
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
                        Matrix().translate(50, 50).scale(0.125),
                        cycleX = CycleMethod.REPEAT,
                        cycleY = CycleMethod.REPEAT
                    )
                    fillRect(0.0, 0.0, 100.0, 100.0)
                }

                run {
                    globalAlpha = 0.9
                    fillStyle =
                        //createLinearGradient(150.0, 0.0, 200.0, 50.0)
                        createLinearGradient(0.0, 0.0, 100.0, 100.0, transform = Matrix().scale(0.5).pretranslate(300, 0))
                            //.addColorStop(0.0, Colors.BLACK).addColorStop(1.0, Colors.WHITE)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.GREEN).addColorStop(1.0, Colors.BLUE)
                    clip({
                        circle(150, 50, 50)
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

        testFrame()
        assertEqualsFileReference(
            "korge/render/GpuShapeView.log",
            listOf(
                logAg.getLogAsString(),
                view.getGlobalBounds().toString(),
                view.getLocalBounds().toString()
            ).joinToString("\n")
        )
    }

    @Test
    fun testBlurFilterInEmptyContainer() = viewsTest {
        val view = container {
            filter = BlurFilter(4.0)
        }
        testFrame()
        assertEqualsFileReference(
            "korge/render/BlurFilterEmptyContainer.log",
            listOf(
                logAg.getLogAsString(),
                view.getGlobalBounds().toString(),
                view.getLocalBoundsOptimized().toString()
            ).joinToString("\n")
        )
    }
}
