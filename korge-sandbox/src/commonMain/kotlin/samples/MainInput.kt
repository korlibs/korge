package samples

import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*

class MainInput : Scene() {
    override suspend fun SContainer.sceneMain() {
        var line = 0
        val font = debugBmpFont()
        fun textLine(text: String) = text(text, font = font, textSize = 8.0).position(2, line++ * 12 + 5).apply { smoothing = false }
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
        val gamepadText = textLine("GamepadTextEv")
        val gamepadButtonText = textLine("GamepadButtonEv")
        val gamepadStickText = textLine("GamepadStickEv")
        val gamepadUpdateText = textLine("GamepadUpdateEv")
        val gamepadUpdate2Text = textLine("GamepadUpdate2Ev")

        onStageResized { width, height ->
            resizeText.text = "Resize ${nowTime()} $width,$height"
        }

        fun updateGamepads() {
            gamepadUpdate2Text.text = views.input.connectedGamepads.joinToString("\n") { "${it.index}:${it.fullName}" }
        }

        gamepad {
            updatedGamepad.invoke { gamepadText.text = "$it"; updateGamepads() }
        }

        gamepad {
            button.invoke { gamepadButtonText.text = "$it"; updateGamepads() }
            stick.invoke { gamepadStickText.text = "$it"; updateGamepads() }
            connection.invoke { gamepadConnectedText.text = "${nowTime()}:$it"; updateGamepads() }
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
