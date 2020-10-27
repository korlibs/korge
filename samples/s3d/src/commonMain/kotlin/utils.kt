import com.soywiz.klock.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge3d.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

@Korge3DExperimental
private suspend fun Stage3D.orbit(v: View3D, distance: Double, time: TimeSpan) {
	view.tween(time = time) { ratio ->
		val angle = 360.degrees * ratio
		camera.positionLookingAt(
			cos(angle) * distance, 0.0, sin(angle) * distance, // Orbiting camera
			v.transform.translation.x.toDouble(), v.transform.translation.y.toDouble(), v.transform.translation.z.toDouble()
		)
	}
}

class Button(text: String, handler: suspend () -> Unit) : Container() {
	val textField = Text(text, textSize = 32.0).apply { smoothing = false }
	private val bounds = textField.textBounds
	val g = Graphics().apply {
		fill(Colors.DARKGREY, 0.7) {
			roundRect(bounds.x, bounds.y, bounds.width + 16, bounds.height + 16, 8.0, 8.0)
		}
	}
	var enabledButton = true
		set(value) {
			field = value
			updateState()
		}
	private var overButton = false
		set(value) {
			field = value
			updateState()
		}

	fun updateState() {
		when {
			!enabledButton -> alpha = 0.3
			overButton -> alpha = 1.0
			else -> alpha = 0.8
		}
	}

	init {
		//this += this.solidRect(bounds.width, bounds.height, Colors.TRANSPARENT_BLACK)
		this += g.apply {
			mouseEnabled = true
		}
		this += textField.position(8, 8)

		mouse {
			over { overButton = true }
			out { overButton = false }
		}
		onClick {
			if (enabledButton) handler()
		}
		updateState()
	}
}


suspend inline fun <reified T : Scene> SceneContainer.changeToDisablingButtons(buttonContainer: Container) {
	for (child in buttonContainer.children.filterIsInstance<Button>()) {
		//println("DISABLE BUTTON: $child")
		child.enabledButton = false
	}
	try {
		changeTo<T>()
	} finally {
		for (child in buttonContainer.children.filterIsInstance<Button>()) {
			//println("ENABLE BUTTON: $child")
			child.enabledButton = true
		}
	}
}
