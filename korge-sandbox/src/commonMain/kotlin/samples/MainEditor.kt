package samples

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.korge.animate.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.style.*
import korlibs.korge.tween.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*

class MainEditor : Scene() {
    override suspend fun SContainer.sceneMain() {
        for (n in 0 until 10) {
            text("HELLO АБВГДЕЖ HELLO АБВГДЕЖ HELLO АБВГДЕЖ HELLO АБВГДЕЖ", font = DefaultTtfFontAsBitmap, renderer = DefaultStringTextRenderer).xy(100, 100 + n * 2)
        }

        //return@Korge

        //val result = UrlVfs("https://raw.githubusercontent.com/korlibs/korio/master/README.md").readString()
        //println("result=$result")

        //image(resourcesVfs["korge-256.png"].readBitmap()).xy(0, 0)
        //image(resourcesVfs["korio-128.png"].readBitmap()).xy(128, 128)
        //return@Korge

        //val font = DefaultTtfFont
        styles {
            this.textFont = DefaultTtfFontAsBitmap
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
        uiVerticalStack(padding = 4f) {
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
        uiWindow("Properties", Size(300f, 100f)) {
            //it.isCloseable = false
            it.container.mobileBehaviour = false
            it.container.overflowRate = 0.0
            uiVerticalStack(300f, padding = 4f) {
                uiText("Properties").styles { textColor = Colors.RED }
                uiPropertyNumberRow("Alpha", *UIEditableNumberPropsList(solidRect::alphaF))
                uiPropertyNumberRow("Position", *UIEditableNumberPropsList(solidRect::x, solidRect::y, min = -1024f, max = +1024f, clamped = false))
                uiPropertyNumberRow("Size", *UIEditableNumberPropsList(solidRect::unscaledWidth, solidRect::unscaledHeight, min = -1024f, max = +1024f, clamped = false))
                uiPropertyNumberRow("Scale", *UIEditableNumberPropsList(solidRect::scaleX, solidRect::scaleY, min = -1f, max = +1f, clamped = false))
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

        renderableView(Size(width, height)) {
            ctx2d.materialRoundRect(0f, 0f, 64f, 64f, radius = RectCorners(32.0, 16.0, 8.0, 0.0))
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
            tween(textBlock::unscaledWidthD[300.0], time = 5.seconds)
            tween(textBlock::unscaledWidthD[1.0], time = 5.seconds)
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
