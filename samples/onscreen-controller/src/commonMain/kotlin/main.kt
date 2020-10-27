import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*

suspend fun main() = Korge(bgcolor = Colors.DARKBLUE) {
	val text1 = textOld("-").position(5, 5).apply { filtering = false }
	val buttonTexts = (0 until 2).map {
		textOld("-").position(5, 20 * (it + 1) + 5).apply { filtering = false }
	}

	addTouchGamepad(
		views.virtualWidth.toDouble(), views.virtualHeight.toDouble(),
		onStick = { x, y -> text1.setText("Stick: (${x.toStringDecimal(2)}, ${y.toStringDecimal(2)})") },
		onButton = { button, pressed -> buttonTexts[button].setText("Button: $button, $pressed") }
	)
}
