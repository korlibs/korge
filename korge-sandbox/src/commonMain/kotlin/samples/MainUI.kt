package samples

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.style.*
import korlibs.korge.tween.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.render.*
import korlibs.time.*

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

        styles {
            val colorTransform = ColorTransform(0.7, 0.9, 1.0)
            //this.uiSkinBitmap = this.uiSkinBitmap.withColorTransform(colorTransform)
            this.buttonBackColor = this.buttonBackColor.transform(colorTransform)
            this.textFont = resourcesVfs["uifont.fnt"].readBitmapFont()
        }

        uiVerticalStack(padding = 8f, adjustSize = true) {
            position(128, 128)
            widthD = 256.0

            uiButton(size = Size(256, 32)) {
                text = "Disabled Button"
                onClick {
                    println("CLICKED!")
                }
                disable()
            }
            uiButton(size = Size(256.0, 32.0)) {
                text = "Close Window"
                onClick {
                    if (gameWindow.confirm("Are you sure to close the window?")) {
                        gameWindow.close()
                    }
                }
                enable()
            }


            uiCheckBox(text = "CheckBox1")
            uiCheckBox(text = "CheckBox2").styles { uiSelectedColor = MaterialColors.TEAL_700 }
            val selectedItemLabel = uiText("-")
            uiComboBox(items = listOf("ComboBox", "World", "this", "is", "a", "list", "of", "elements")) {
                onSelectionUpdate {
                    selectedItemLabel.text = "${it.selectedIndex}: ${it.selectedItem}"
                }
            }
            val group = UIRadioButtonGroup()
            val rb = uiRadioButton(text = "Radio 1", group = group)
            uiRadioButton(text = "Radio 2", group = group).styles { uiSelectedColor = MaterialColors.TEAL_700; uiUnselectedColor = MaterialColors.CYAN_800 }
            //rb.simpleAnimator.tween(rb::height[120.0], time = 4.seconds)
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
                    uiButton("HELLO $x,$y").position(x * 108, y * 40)
                }
            }
        }

        val progress = uiProgressBar {
            position(64, 32)
            current = 0.5f
        }
        val job = launchImmediately {
            while (true) {
                tween(progress::ratio[1.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                tween(progress::ratio[1.0, 0.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            }
        }
        uiButton("Stop Progress").position(Point(64 + progress.widthD, 32.0)).mouse { onClick { job.cancel() } }

    }
}
