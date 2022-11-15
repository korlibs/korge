package samples

import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.text.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

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
            scale = 3.0
        }

        image(outputPng) {
            xy(196 * 1, 0)
            scale = 3.0
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 50)
            scale = 1.5
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 100)
            scale = 0.5
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 150)
            scale = 0.25
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 175)
            scale = 0.15
            program = MsdfRender.PROGRAM_MSDF
        }
        image(outputPng) {
            xy(196 * 1, 200)
            scale = 0.1
            program = MsdfRender.PROGRAM_MSDF
        }

        image(msdfBitmap) {
            xy(196 * 2, 0)
            scale = 3.0
        }

        image(msdfBitmap) {
            xy(196 * 3, 0)
            scale = 3.0
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 0, 256)
            scale = 2.0
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 2, 256)
            scale = 0.5
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 3, 256)
            scale = 0.25
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontAsBitmap.atlas.bitmap) {
            xy(196 * 4, 256)
            scale = 2.0
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
