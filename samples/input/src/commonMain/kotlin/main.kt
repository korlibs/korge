import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*

suspend fun main() = Korge {
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

	//stage.addEventListener<KeyEvent> { keysEvText.text = "${nowTime()}:$it" }
	//stage.addEventListener<MouseEvent> { mouseEvText.text = "${nowTime()}:$it" }
	stage.addEventListener<ReshapeEvent> { resizeText.text = "Resize ${nowTime()} $it" }
	stage.addEventListener<GamePadConnectionEvent> { gamepadConnectedText.text = "${nowTime()}:$it" }
	//stage.addEventListener<GamePadUpdateEvent> {
	//	gamepadUpdateText.text = "${nowTime()}:$it"
	//	gamepadUpdate2Text.text = "" + it.gamepads.lastOrNull { it.connected }?.rawButtonsPressed
	//}

    //stage.addComponent(object : GamepadComponent {
    //    override val view: View = stage
    //    override fun onGamepadEvent(views: Views, event: GamePadUpdateEvent) {
    //        println(event)
    //    }
    //    override fun onGamepadEvent(views: Views, event: GamePadConnectionEvent) {
    //        println(event)
    //    }
    //})

	gamepad {
		button.invoke { gamepadButtonText.text = "$it" }
		stick.invoke { gamepadStickText.text = "$it" }
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
