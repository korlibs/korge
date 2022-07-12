package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.ui.UISkin
import com.soywiz.korge.ui.buttonBackColor
import com.soywiz.korge.ui.textFont
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiCheckBox
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.ui.uiProgressBar
import com.soywiz.korge.ui.uiScrollBar
import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.ui.uiSkin
import com.soywiz.korge.ui.uiSkinBitmap
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korim.color.transform
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korio.file.std.resourcesVfs
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
                println("CLICKED!")
                gameWindow.close()
            }
            enable()
        }

        uiScrollBar(256.0, 32.0, 0.0, 32.0, 64.0) {
            position(64, 64)
            onChange {
                println(it.ratio)
            }
        }
        uiScrollBar(32.0, 256.0, 0.0, 16.0, 64.0) {
            position(64, 128)
            onChange {
                println(it.ratio)
            }
        }

        uiCheckBox {
            position(128, 128 + 64)
        }

        uiComboBox(items = listOf("ComboBox", "World", "this", "is", "a", "list", "of", "elements")) {
            position(128, 128 + 64 + 32)
        }

        uiScrollableArea(config = {
            position(480, 128)
        }) {
            for (n in 0 until 16) {
                uiButton(text = "HELLO $n").position(0, n * 64)
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
