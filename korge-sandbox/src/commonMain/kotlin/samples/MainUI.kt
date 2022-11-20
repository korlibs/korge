package samples

import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*

class MainUI : Scene() {
    @ViewProperty
    var yayProperty: Double = 0.0

    @ViewProperty
    fun yayAction() {
        launchImmediately {
            gameWindow.alert("yay!")
        }
    }

    override suspend fun SContainer.sceneMain() {
        println("[1]")

        uiSkin = UISkin {
            val colorTransform = ColorTransform(0.7, 0.9, 1.0)
            this.uiSkinBitmap = this.uiSkinBitmap.withColorTransform(colorTransform)
            this.buttonBackColor = this.buttonBackColor.transform(colorTransform)
            this.textFont = resourcesVfs["uifont.fnt"].readBitmapFont()
        }

        uiVerticalStack(padding = 8.0, adjustSize = true) {
            position(128, 128)
            width = 256.0

            uiButton(256.0, 32.0) {
                text = "Disabled Button"
                onClick {
                    println("CLICKED!")
                }
                disable()
            }
            uiButton(256.0, 32.0) {
                text = "Close Window"
                onClick {
                    if (gameWindow.confirm("Are you sure to close the window?")) {
                        gameWindow.close()
                    }
                }
                enable()
            }


            uiCheckBox(text = "CheckBox1")
            uiCheckBox(text = "CheckBox2") { skin = UIBaseCheckBoxSkinMaterial(MaterialColors.TEAL_700) }
            val selectedItemLabel = uiText("-")
            uiComboBox(items = listOf("ComboBox", "World", "this", "is", "a", "list", "of", "elements")) {
                onSelectionUpdate {
                    selectedItemLabel.text = "${it.selectedIndex}: ${it.selectedItem}"
                }
            }
            val group = UIRadioButtonGroup()
            uiRadioButton(text = "Radio 1", group = group)
            uiRadioButton(text = "Radio 2", group = group) { skin = UIBaseCheckBoxSkinMaterial(MaterialColors.TEAL_700, MaterialColors.CYAN_800) }
            //uiSwitch(text = "Switch1")
            //uiSwitch(text = "Switch2")
            uiSlider(value = 0, min = -32, max = +32).changed {}
            uiTextInput("TextInput")
        }

        //shapeView(buildVectorPath { this.circle(0, 0, 100) })
        //image(resourcesVfs["korge.png"].readBitmap()).scale(0.25)

        //uiMaterialLayer(50.0, 50.0) {
        //    xy(128, 260)
        //    bgColor = Colors.RED
        //    radius = RectCorners(25.0)
        //    borderColor = Colors.YELLOW
        //    borderSize = 2.0
        //    //addHighlight(Point(0.5, 0.5))
        //}

        uiScrollable {
            it.position(480, 128)
            //for (n in 0 until 20) {
            for (y in 0 until 30) {
                for (x in 0 until 15) {
                    uiButton(text = "HELLO $x,$y").position(x * 108, y * 40)
                }
            }
        }

        val progress = uiProgressBar {
            position(64, 32)
            current = 0.5
        }

        //while (true) {
        //    tween(progress::ratio[1.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        //    tween(progress::ratio[1.0, 0.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        //}
    }
}
