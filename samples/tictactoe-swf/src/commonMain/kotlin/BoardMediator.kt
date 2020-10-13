import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlin.coroutines.*

var Board.Cell.view by Extra.Property<View?> { null }
val Board.Cell.onPress by Extra.Property { Signal<Unit>() }

fun Board.Cell.set(type: Chip) {
	this.value = type
	view["chip"].play(when (type) {
		Chip.EMPTY -> "empty"
		Chip.CIRCLE -> "circle"
		Chip.CROSS -> "cross"
	})
}

suspend fun Board.Cell.setAnimate(type: Chip) {
	set(type)
	launchImmediately(coroutineContext) {
		view.tween(
			(view["chip"]::alpha[0.7, 1.0]).linear(),
			(view["chip"]::scale[0.8, 1.0]).easeOutElastic(),
			time = 300.milliseconds
		)
	}
}

var Board.Cell.highlighting by Extra.Property { false }

suspend fun Board.Cell.highlight(highlight: Boolean) {
	view["highlight"].first.play(if (highlight) "highlight" else "none")
	this.highlighting = highlight
	if (highlight) {
		launchImmediately(coroutineContext) {
			val hl = view["highlight"]!!
			while (highlighting) {
				hl.tween((hl::alpha[0.7]).easeInOutQuad(), time = 300.milliseconds)
				hl.tween((hl::alpha[1.0]).easeInOutQuad(), time = 200.milliseconds)
			}
		}

		launchImmediately(coroutineContext) {
			val ch = view["chip"]!!
			ch.tween((ch::scale[0.4]).easeOutQuad(), time = 100.milliseconds)
			ch.tween((ch::scale[1.2]).easeOutElastic(), time = 300.milliseconds)
			while (highlighting) {
				ch.tween((ch::scale[1.0]).easeOutQuad(), time = 300.milliseconds)
				ch.tween((ch::scale[1.2]).easeOutElastic(), time = 300.milliseconds)
			}
		}
	}
}

suspend fun Board.Cell.lowlight(lowlight: Boolean) {
	launchImmediately(coroutineContext) {
		view.tween(
			view!!::scale[1.0, 0.7],
			view!!::alpha[0.3],
			time = 300.milliseconds, easing = Easing.EASE_OUT_QUAD
		)
	}
}

suspend fun Board.reset() {
	for (cell in cells) {
		//cell.view?.removeAllComponents()
		cell.set(Chip.EMPTY)
		cell.highlight(false)
		cell.view?.scale = 1.0
		cell.view?.alpha = 1.0
		cell.view["chip"]?.scale = 1.0
		cell.view["chip"]?.alpha = 1.0
	}
}

fun Board.Cell.init(view: View) {
	this.view = view
	set(this.value)
	view["hit"].onClick {
		onPress(Unit)
	}
}
