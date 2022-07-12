package samples

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.korge.component.onStageResized
import com.soywiz.korge.input.gamepad
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.textOld

class MainInput : ScaledScene(1920, 1080) {
    override suspend fun SContainer.sceneMain() {
        var line = 0
        fun textLine(text: String) = textOld(text).position(2, line++ * 20 + 5).apply { filtering = false }
        fun nowTime() = DateTime.now().local.format(DateFormat("HH:mm:ss.SSS"))

        textLine("Events :")
        val keysEvText = textLine("Keys")
        val keysDownText = textLine("Keys:Down")
        val keysUpText = textLine("Keys:Up")
        val mouseEvText = textLine("Mouse")
        val mouseMoveText = textLine("MouseMove")
        val mouseDownText = textLine("MouseDown")
        val mouseUpText = textLine("MouseUp")
        val mouseClickText = textLine("MouseClick")
        val mouseScrollText = textLine("MouseScroll")
        val resizeText = textLine("Resize")
        val gamepadConnectedText = textLine("GamepadConnectedEv")
        val gamepadButtonText = textLine("GamepadButtonEv")
        val gamepadStickText = textLine("GamepadStickEv")
        val gamepadUpdateText = textLine("GamepadUpdateEv")
        val gamepadUpdate2Text = textLine("GamepadUpdate2Ev")

        onStageResized { width, height ->
            resizeText.text = "Resize ${nowTime()} $width,$height"
        }

        gamepad {
            button.invoke { gamepadButtonText.text = "$it" }
            stick.invoke { gamepadStickText.text = "$it" }
            connection.invoke { gamepadConnectedText.text = "${nowTime()}:$it" }
        }

        keys {
            down { keysDownText.text = "Key:Down ${nowTime()} ${it.key}" }
            up { keysUpText.text = "Key:Up ${nowTime()} ${it.key}" }
        }

        mouse {
            onMove { mouseMoveText.text = "Mouse:Move ${nowTime()} $it" }
            onDown { mouseDownText.text = "Mouse:Down ${nowTime()} $it" }
            onUp { mouseUpText.text = "Mouse:Up ${nowTime()} $it" }
            onClick { mouseClickText.text = "Mouse:Click ${nowTime()} $it" }
            onScroll { mouseScrollText.text = "Mouse:Scroll ${nowTime()} $it" }
        }
    }
}
