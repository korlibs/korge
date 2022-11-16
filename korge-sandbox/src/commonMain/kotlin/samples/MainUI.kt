package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.Easing

class MainUI : Scene() {
    override suspend fun SContainer.sceneMain() {

        uiSkin = UISkin {
            val colorTransform = ColorTransform(0.7, 0.9, 1.0)
            this.uiSkinBitmap = this.uiSkinBitmap.withColorTransform(colorTransform)
            this.buttonBackColor = this.buttonBackColor.transform(colorTransform)
            this.textFont = resourcesVfs["uifont.fnt"].readBitmapFont()
        }

        uiButton(256.0, 32.0) {
            text = "Disabled Button"
            position(128, 128)
            onClick {
                println("CLICKED!")
            }
            disable()
        }
        uiButton(256.0, 32.0) {
            text = "Close Window"
            position(128, 128 + 32)
            onClick {
                if (gameWindow.confirm("Are you sure to close the window?")) {
                    gameWindow.close()
                }
            }
            enable()
        }

        uiCheckBox(text = "HELLO WORLD!") {
            position(128, 128 + 64)
        }

        uiComboBox(items = listOf("ComboBox", "World", "this", "is", "a", "list", "of", "elements")) {
            position(128, 128 + 64 + 32)
        }

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
            for (n in 0 until 16) {
                uiButton(text = "HELLO $n").position(n * 16, n * 32)
            }
        }

        val progress = uiProgressBar {
            position(64, 32)
            current = 0.5
        }

        while (true) {
            tween(progress::ratio[1.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            tween(progress::ratio[1.0, 0.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
