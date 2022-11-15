package samples

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.text.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

class MainEditor : Scene() {
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

        image(DefaultTtfFontMsdf.atlas.bitmap) {
            xy(196 * 0, 256)
            scale = 2.0
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontMsdf.atlas.bitmap) {
            xy(196 * 2, 256)
            scale = 0.5
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontMsdf.atlas.bitmap) {
            xy(196 * 3, 256)
            scale = 0.25
            program = MsdfRender.PROGRAM_MSDF
        }

        image(DefaultTtfFontMsdf.atlas.bitmap) {
            xy(196 * 4, 256)
            scale = 2.0
        }

        return

        if (true) {
            //val font1 = resourcesVfs["msdf/SaniTrixieSans.fnt"].readBitmapFont()
            val font2 = resourcesVfs["msdf/SaniTrixieSans.json"].readBitmapFont()
            val font1 = DefaultTtfFontMsdf

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
                    if (n == 0 ){
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
                ctx2d.drawText(RichTextData.fromHTML("HELLO, World! <b>this</b> is a test!", RichTextData.Style(textSize = 64.0, font = font2, color = Colors.WHITE)))
            })

            //val image = text("HELLO, World!\nthis is test!", font = font2, color = Colors.WHITE)
//
            //image.simpleAnimator.sequence(looped = true) {
            //    tween(image::scale[15.0], time = 3.seconds)
            //    tween(image::scale[1.0], time = 3.seconds)
            //}

            //textBlock(RichTextData("hello world!", 128.0, font1, color = Colors.WHITE), width = 1024.0, height = 1024.0)
            return

            //val image = image(resourcesVfs["msdf/SaniTrixieSans.png"].readBitmap().mipmaps()).scale(0.25).also {
            //    it.program = MsdfRender.PROGRAM_MSDF_I
            //}

            /*
        val rfont = DefaultTtfFont
        val size = 48.0
        val codePoint = 'e'.code
        val metrics = rfont.getGlyphMetrics(size, codePoint)
        val glyph = rfont.getGlyphPath(size, codePoint)

        //val metrics = glyph.metrics1px
        println("metrics=$metrics, DefaultTtfFont.unitsPerEm=${DefaultTtfFont.unitsPerEm}")
        println("glyph!!.path.path=${glyph!!.path.toSvgString()}")
        val msdf = glyph!!.path.clone().applyTransform(glyph.transform * Matrix().translate(0.0 + size / 4, metrics.height + size / 4)).msdf(size.toInt(), size.toInt())
        msdf.normalizeUniform()

        val image = image(msdf.toBMP32().mipmaps().slice()).xy(100, 100).scale(0.24).also {
            //it.program = MsdfRender.PROGRAM_MSDF
            it.program = MsdfRender.PROGRAM_SDF_A
        }
         */

            keys {
                down(Key.RETURN) {
                    //image.program = if (image.program == MsdfRender.PROGRAM_SDF_A) MsdfRender.PROGRAM_MSDF else MsdfRender.PROGRAM_SDF_A
                    //println("TOGGLED PROGRAM!: ${image.program?.name}")
                }
            }

            return
        }

        val font2 = DefaultTtfFont.toBitmapFont(16.0, CharacterSet.LATIN_ALL + CharacterSet.CYRILLIC)
        //val font2 = DefaultTtfFont

        for (n in 0 until 10) {
            text("HELLO АБВГДЕЖ HELLO АБВГДЕЖ HELLO АБВГДЕЖ HELLO АБВГДЕЖ", font = font2, renderer = DefaultStringTextRenderer).xy(100, 100 + n * 2)
        }

        //return@Korge

        //val result = UrlVfs("https://raw.githubusercontent.com/korlibs/korio/master/README.md").readString()
        //println("result=$result")

        //image(resourcesVfs["korge-256.png"].readBitmap()).xy(0, 0)
        //image(resourcesVfs["korio-128.png"].readBitmap()).xy(128, 128)
        //return@Korge

        val font = DefaultTtfFont.toBitmapFont(16.0)
        //val font = DefaultTtfFont
        uiSkin = UISkin {
            this.textFont = font
        }
        //solidRect(100, 100, Colors.RED).xy(0, 0)
        ////solidRect(100, 100, Colors.BLUE).xy(50, 50)
        //text("A", 32.0, font = font)


        /*
        uiBreadCrumbArray("hello", "world") {
            onClickPath {
                println(it)
                gameWindow.showContextMenu {
                    item("hello", action = {})
                    separator()
                    item("world", action = {})
                }
            }
        }

        //val component = injector.get<ViewsDebuggerComponent>()
        //ktreeEditorKorge(stage, component.actions, views, BaseKorgeFileToEdit(MemoryVfsMix(mapOf("test.ktree" to "<ktree></ktree>"))["test.ktree"]), { })

        //val grid = OrthographicGrid(20, 20)
        //renderableView() { grid.draw(ctx, 500.0, 500.0, globalMatrix) }
        */

        //deferred(deferred = false) {
        //deferred(deferred = true) {
        //container {
        uiVerticalStack {
            xy(600, 200)
            val group = UIRadioButtonGroup()
            uiRadioButton(group = group)
            uiRadioButton(group = group)
            uiSpacing()
            uiCheckBox(checked = false)
            uiCheckBox(checked = true)
        }
        uiVerticalStack {
            xy(400, 200)
            val group = UIRadioButtonGroup()
            uiRadioButton(group = group)
            uiRadioButton(group = group)
            uiRadioButton(group = group)
            uiSpacing()
            uiRadioButton(group = group)
        }
        uiVerticalStack(padding = 4.0) {
            xy(800, 100)
            uiButton("BUTTON")
            uiButton("NAME")
            uiButton("TEST").disable()
        }

        /*
        uiContainer {
            //append(UIContainer(200.0, 200.0)) {
            for (mx in 0 until 20) {
                for (my in 0 until 20) {
                    uiButton(100.0, 32.0, "$mx,$my").xy(100 * mx, 32 * my)
                    //uiButton(100.0, 32.0, "$mx,$my").xy(100 * mx + 5, 32 * my + 5)
                    //mybutton(font).xy(100 * mx, 100 * my)
                }
            }
        }
        */

        val solidRect = solidRect(100, 100, Colors.RED).position(300, 300).anchor(Anchor.CENTER)
        uiWindow("Properties", 300.0, 100.0) {
            //it.isCloseable = false
            it.container.mobileBehaviour = false
            it.container.overflowRate = 0.0
            uiVerticalStack(300.0) {
                uiText("Properties") { textColor = Colors.RED }
                uiPropertyNumberRow("Alpha", *UIEditableNumberPropsList(solidRect::alpha))
                uiPropertyNumberRow("Position", *UIEditableNumberPropsList(solidRect::x, solidRect::y, min = -1024.0, max = +1024.0, clamped = false))
                uiPropertyNumberRow("Size", *UIEditableNumberPropsList(solidRect::width, solidRect::height, min = -1024.0, max = +1024.0, clamped = false))
                uiPropertyNumberRow("Scale", *UIEditableNumberPropsList(solidRect::scaleX, solidRect::scaleY, min = -1.0, max = +1.0, clamped = false))
                uiPropertyNumberRow("Rotation", *UIEditableNumberPropsList(solidRect::rotationDeg, min = -360.0, max = +360.0, clamped = true))
                val skewProp = uiPropertyNumberRow("Skew", *UIEditableNumberPropsList(solidRect::skewXDeg, solidRect::skewYDeg, min = -360.0, max = +360.0, clamped = true))
                append(UIPropertyRow("Visible")) {
                    this.container.append(uiCheckBox(checked = solidRect.visible, text = "").also {
                        it.onChange {
                            solidRect.visible = it.checked
                        }
                    })
                }

                println(skewProp.getVisibleGlobalArea())

            }
        }.xy(100, 150)

        //text("HELLO", font = font)
        uiContainer {
            uiTextInput("HELLO").position(0.0, 0.0)
            uiTextInput("WORLD").position(0.0, 32.0)
            uiTextInput("DEMO").position(0.0, 64.0)
            uiTextInput("TEST").position(0.0, 96.0)
            uiTextInput("LOL").position(0.0, 128.0)
        }

        renderableView(width, height) {
            ctx2d.materialRoundRect(0.0, 0.0, 64.0, 64.0, radius = RectCorners(32.0, 16.0, 8.0, 0.0))
        }.xy(500, 500)

        val richTextData = RichTextData.fromHTML("hello world,<br /><br />this is a long test to see how <font size=24 color='red'><b><i>rich text</i></b></font> <b color=yellow>works</b>! And <i>see</i> if this is going to show ellipsis if the text is too long")
        //println("richTextData=${richTextData.toHTML()}")
        val textBlock = textBlock(
            richTextData
        ) {
            align = TextAlignment.MIDDLE_JUSTIFIED
            //align = TextAlignment.TOP_LEFT
            //autoSize = true
            xy(600, 500)
        }

        textBlock.simpleAnimator.sequence(looped = true) {
            tween(textBlock::width[300.0], time = 5.seconds)
            tween(textBlock::width[1.0], time = 5.seconds)
        }

        /*
        uiScrollable {
            uiVerticalList(object : UIVerticalList.Provider {
                override val numItems: Int = 1000
                override val fixedHeight: Double = 20.0
                override fun getItemHeight(index: Int): Double = fixedHeight
                override fun getItemView(index: Int): View = UIText("HELLO WORLD $index")
            })
        }
         */

        //mainVampire()
    }
}

private var View.rotationDeg: Double
    get() = rotation.degrees
    set(value) { rotation = value.degrees }

private var View.skewXDeg: Double
    get() = skewX.degrees
    set(value) { skewX = value.degrees }

private var View.skewYDeg: Double
    get() = skewY.degrees
    set(value) { skewY = value.degrees }

private fun Container.mybutton(font: Font): View {
    return container {
        solidRect(100, 100, Colors.BLUE)
        text("HELLO WORLD!", font = font)
    }
}
