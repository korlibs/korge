package samples

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.image.vector.*
import korlibs.image.vector.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.time.*

class MainGpuVectorRendering : Scene() {
    private val logger = Logger("MainGpuVectorRendering")

    override suspend fun SContainer.sceneMain() {
        gpuShapeView {  }

        graphics(GraphicsRenderer.GPU) {
            it.antialiased = false
            //it.debugDrawOnlyAntialiasedBorder = false
            fill(Colors.GREEN, winding = Winding.EVEN_ODD) {
                //circle(100, 100, 100)
                //circleHole(100, 100, 50)
                rect(100, 100, 100, 100)
                rectHole(120, 120, 30, 30)
            }
        }//.filters(BlurFilter())
        //return

        //return

        /*
        gpuShapeView({
            //val paint = createLinearGradient(200, 200, 400, 400).add(0.0, Colors.BLUE.withAd(0.9)).add(1.0, Colors.WHITE.withAd(0.7))
            val paint = Colors.WHITE.withAd(0.7)
            //stroke(paint, lineWidth = 10.0, lineCap = LineCap.BUTT, lineJoin = LineJoin.ROUND) {
            //stroke(paint, lineWidth = 10.0, lineCap = LineCap.SQUARE, lineJoin = LineJoin.ROUND) {
            //stroke(paint, lineWidth = 10.0, lineCap = LineCap.ROUND, lineJoin = LineJoin.ROUND) {
            //stroke(paint, lineWidth = 10.0, lineCap = LineCap.ROUND, lineJoin = LineJoin.BEVEL) {
            stroke(paint, lineWidth = 10.0, lineCap = LineCap.ROUND, lineJoin = LineJoin.ROUND) {
            //stroke(paint, lineWidth = 10.0, lineCap = LineCap.ROUND, lineJoin = LineJoin.BEVEL) {
                moveTo(100, 100)
                //quadTo(400, 200, 400, 400)
                lineTo(400, 400)
                lineTo(200, 500)
                lineTo(500, 500)
                lineTo(200, 700)
                //lineTo(100, 140)
                //lineTo(100, 100)
                close()

                moveTo(800, 600)
                //quadTo(400, 200, 400, 400)
                lineTo(900, 600)
                lineTo(900, 400)
                //lineTo(100, 140)
                //lineTo(100, 100)
                close()

                moveTo(800, 100)
                lineTo(800, 110)

                moveTo(750, 100)
                lineTo(750, 110)
            }
        }) {
            keys {
                down(Key.N0) { antialiased = !antialiased }
                down(Key.A) { antialiased = !antialiased }
            }
        }
        */

        //circle(6.0, Colors.RED).anchor(Anchor.CENTER).xy(100, 100)
        //.xy(40, 0)
        //.scale(1.1)
        //.rotation(15.degrees)
        //return

        //return

        logger.info { "[1]" }
        val korgeBitmap = resourcesVfs["korge.png"].readBitmap()//.mipmaps()
        logger.info { "[2]" }
        val tigerSvg = measureTime({ resourcesVfs["Ghostscript_Tiger.svg"].readSVG() }) {
            logger.info { "Elapsed $it" }
        }
        logger.info { "[3]" }
        //AudioData(44100, AudioSamples(1, 1024)).toSound().play()

        val PAINT_TIGER = true
        val PAINT_SHAPES = true
        val PAINT_BITMAP = true
        //val PAINT_BITMAP = false
        val PAINT_TEXT = true
        val PAINT_LINEAR_GRADIENT = true
        val PAINT_RADIAL_GRADIENT = true

        val tigerShape = tigerSvg.toShape()
        val tigerRender = tigerSvg
        //val tigerRender = tigerShape

        fun Context2d.buildGraphics(kind: String) {
            if (PAINT_TIGER) {
                keep {
                    scale(0.5)
                    draw(tigerRender)
                }
            }
            if (PAINT_SHAPES) {
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
                    stroke(Colors.GREEN, StrokeInfo(thickness = 5f, startCap = LineCap.ROUND, endCap = LineCap.ROUND, dash = floatArrayListOf(15f, 10f), dashOffset = 8f)) {
                        regularPolygon(6, 30.0, x = 100.0, y = 100.0)
                    }
                }
            }
            keep {
                translate(100, 20)
                scale(2.0)
                if (PAINT_BITMAP) {
                    globalAlpha = 0.75f
                    fillStyle = BitmapPaint(
                        korgeBitmap,
                        MMatrix().translate(50, 50).scale(0.125).immutable,
                        cycleX = CycleMethod.REPEAT,
                        cycleY = CycleMethod.REPEAT
                    )
                    fillRect(0.0, 0.0, 100.0, 100.0)
                }

                if (PAINT_LINEAR_GRADIENT) {
                    globalAlpha = 0.9f
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
                if (PAINT_RADIAL_GRADIENT) {
                    globalAlpha = 0.9f
                    fillStyle =
                        createRadialGradient(150,150,30, 130,180,70)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.GREEN).addColorStop(1.0, Colors.BLUE)
                    fillRect(100.0, 100.0, 100.0, 100.0)
                }
                if (PAINT_RADIAL_GRADIENT) {
                    globalAlpha = 0.9f
                    fillStyle =
                        createSweepGradient(175, 100)
                            .addColorStop(0.0, Colors.RED).addColorStop(0.5, Colors.PURPLE).addColorStop(1.0, Colors.YELLOW)
                    fillRect(150.0, 75.0, 50.0, 50.0)
                }
            }
            if (PAINT_TEXT) {
                keep {
                    font = DefaultTtfFont
                    fontSize = 16f
                    fillStyle = Colors.WHITE
                    alignment = TextAlignment.TOP_LEFT
                    fillText("HELLO WORLD ($kind)", Point(0, 16))
                }
            }
        }

        buildShape { buildGraphics("only shape") }
        //for (n in 0 until 2) {
        //    NativeImage(512, 512).context2d { buildGraphics("KOTLIN") }
        //    Bitmap32(512, 512).context2d { buildGraphics("KOTLIN") }
        //}

        measureTime({
            buildShape { buildGraphics("only shape") }
        }) {
            println("BUILD SHAPE: $it")
        }

        val gpuTigger = measureTime({
            gpuShapeView({ buildGraphics("GPU") }) {
                xy(40, 0)
                scale(1.1)
                rotation(15.degrees)
                keys {
                    down(Key.N0) { antialiased = !antialiased }
                    down(Key.A) { antialiased = !antialiased }
                    down(Key.N9) { debugDrawOnlyAntialiasedBorder = !debugDrawOnlyAntialiasedBorder }
                }
            }
        }) {
            println("GPU SHAPE: $it")
        }
        measureTime({
            image(NativeImageContext2d(512, 512) { buildGraphics("NATIVE") }).xy(550, 0)
        }) {
            println("CONTEXT2D NATIVE: $it")
        }

        measureTime({
            image(Bitmap32Context2d(512, 512) { buildGraphics("KOTLIN") }).xy(550, 370)
        }) {
            println("CONTEXT2D BITMAP: $it")
        }

        gamepad {
            connected { println("CONNECTED gamepad=${it}") }
            disconnected { println("DISCONNECTED gamepad=${it}") }
            button { playerId, pressed, button, value ->
                if (pressed && button == GameButton.START) {
                    //shape.antialiased = !shape.antialiased
                    gpuTigger.antialiased = !gpuTigger.antialiased
                    //println("shape.antialiased=${shape.antialiased}")
                }
                println("BUTTON: $playerId, $pressed, button=$button, value=$value")
            }
            stick { playerId, stick, x, y ->
                println("STICK: $playerId, stick=$stick, x=$x, y=$y")
                if (stick == GameStick.LEFT) {
                    rotation += x.degrees
                }
            }
            updatedGamepad {
                //println("updatedGamepad: $it")
                rotation += it.lx.degrees
                //shape.rotation += it.ly.degrees
            }
        }

        uiButton("HELLO").xy(400, 400).scale(4.0)

        //while (true) Bitmap32(512, 512).context2d { buildGraphics("KOTLIN") }
    }
}
