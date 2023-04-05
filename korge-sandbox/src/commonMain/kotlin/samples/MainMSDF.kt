package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.image.vector.*
import korlibs.image.vector.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.text.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

class MainMSDF : Scene() {
    override suspend fun SContainer.sceneMain() {
        //solidRect(width, height, Colors.WHITE)

        val Apath = resourcesVfs["A.svg"].readSVG().toShape().getPath()
        println(Apath.toSvgString())

        val msdfBitmap = Apath.msdfBmp(64, 64)

        //val outputPng = resourcesVfs["output.png"].readBitmap().mipmaps()
        val outputPng = resourcesVfs["output.png"].readBitmap()

        image(outputPng) {
            xy(196 * 0, 0)
            scaleAvg = 3.0f
        }

        image(outputPng) {
            xy(196 * 1, 0)
            scaleAvg = 3.0f
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 50)
            scaleAvg = 1.5f
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 100)
            scaleAvg = 0.5f
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 150)
            scaleAvg = 0.25f
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 175)
            scaleAvg = 0.15f
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 200)
            scaleAvg = 0.1f
            program = MsdfRender.PROGRAM_MSDF
        }

        image(msdfBitmap) {
            xy(196 * 2, 0)
            scaleAvg = 3.0f
        }

        image(msdfBitmap) {
            xy(196 * 3, 0)
            scaleAvg = 3.0f
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 0, 256)
            scaleAvg = 2f
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 2, 256)
            scaleAvg = 0.5f
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 3, 256)
            scaleAvg = 0.25f
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 4, 256)
            scaleAvg = 2f
        }

        //val font1 = resourcesVfs["msdf/SaniTrixieSans.fnt"].readBitmapFont()
        val font2 = resourcesVfs["msdf/SaniTrixieSans.json"].readBitmapFont()
        val font1 = DefaultTtfFontAsBitmap

        val shape = buildShape {
            fill(Colors.WHITE) {
                write(DefaultTtfFont.get('A')?.path?.path!!)
            }
        }

        println(shape.toSvg())

        font1.get('A')
        font1.get('a')
        font1.get('0')

        for (n in 0 until 2) {
            container {
                xy(400, 200 + 150 * n)
                solidRect(300, 100, Colors.DARKGREY)
                if (n == 0) {
                    text("HELLO WORLD áéúóúñ cooool", textSize = 32.0, font = font1).also {
                        it.setTextBounds(Rectangle(0, 0, 300, 100))
                        it.alignment = TextAlignment.MIDDLE_CENTER
                    }
                } else {
                    //textBlock(RichTextData("HELLO WORLD aeioun coooool", font = font2, textSize = 32.0)).also {
                    val tb = textBlock(RichTextData("HELLO WORLD áéúóúñ cooool", font = font2, textSize = 32.0)).also {
                        it.setSize(300.0, 100.0)
                        it.align = TextAlignment.MIDDLE_CENTER
                    }

                    keys {
                        var toggle = true
                        down(Key.RETURN) {
                            tb.text = if (toggle) {
                                RichTextData("HELLO WORLD", font = font1, textSize = 32.0)
                            } else {
                                RichTextData("HELLO WORLD", font = font2, textSize = 32.0)
                            }
                            toggle = !toggle
                        }
                    }
                }
            }
        }

        image(font1.baseBmp).xy(200, 200)

        renderableView(viewRenderer = ViewRenderer {
            ctx2d.rect(0.0, 0.0, 100.0, 100.0, Colors.RED)
            //ctx2d.drawText("HELLO WORLD!", font1, textSize = 128.0)
            ctx2d.drawText(
                RichTextData.fromHTML(
                    "HELLO, World! <b>this</b> is a test!",
                    RichTextData.Style(textSize = 64.0, font = font2, color = Colors.WHITE)
                )
            )
        })

        //val image = text("HELLO, World!\nthis is test!", font = font2, color = Colors.WHITE)
//
        //image.simpleAnimator.sequence(looped = true) {
        //    tween(image::scale[15.0], time = 3.seconds)
        //    tween(image::scale[1.0], time = 3.seconds)
        //}

        //textBlock(RichTextData("hello world!", 128.0, font1, color = Colors.WHITE), width = 1024.0, height = 1024.0)
        return
    }
}
