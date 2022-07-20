package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*

class MainEditor : Scene() {
    override suspend fun SContainer.sceneMain() {

        val font2 = DefaultTtfFont.toBitmapFont(16.0, CharacterSet.LATIN_ALL + CharacterSet.CYRILLIC)

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
            xy(400, 200)
            val group = UIRadioButtonGroup()
            uiRadioButton(group = group)
            uiRadioButton(group = group)
            uiRadioButton(group = group)
            uiSpacing()
            uiRadioButton(group = group)
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
        }

        //text("HELLO", font = font)
        uiContainer {
            uiTextInput("HELLO").position(0.0, 0.0)
            uiTextInput("WORLD").position(0.0, 32.0)
            uiTextInput("DEMO").position(0.0, 64.0)
            uiTextInput("TEST").position(0.0, 96.0)
            uiTextInput("LOL").position(0.0, 128.0)
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
