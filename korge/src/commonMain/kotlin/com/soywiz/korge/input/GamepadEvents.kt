package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korev.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import kotlin.jvm.*

class GamePadEvents(val view: View) {
    @PublishedApi
    internal lateinit var views: Views
    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

    val gamepads = GamePadUpdateEvent()
	val updated = Signal<GamePadUpdateEvent>()
    val updatedGamepad = Signal<GamepadInfo>()
	val stick = Signal<GamePadStickEvent>()
	val button = Signal<GamePadButtonEvent>()
	val connection = Signal<GamePadConnectionEvent>()

	fun stick(callback: suspend (playerId: Int, stick: GameStick, x: Double, y: Double) -> Unit) {
		stick { e -> launchImmediately(coroutineContext) { callback(e.gamepad, e.stick, e.x, e.y) } }
	}

	fun button(callback: suspend (playerId: Int, pressed: Boolean, button: GameButton, value: Double) -> Unit) {
		button { e ->
            launchImmediately(coroutineContext) { callback(e.gamepad, e.type == GamePadButtonEvent.Type.DOWN, e.button, e.value) }
		}
	}

	fun button(playerId: Int, callback: suspend (pressed: Boolean, button: GameButton, value: Double) -> Unit) {
		button { e ->
			if (e.gamepad == playerId) launchImmediately(coroutineContext) { callback(e.type == GamePadButtonEvent.Type.DOWN, e.button, e.value) }
		}
	}

	fun down(playerId: Int, button: GameButton, callback: suspend () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.DOWN) launchImmediately(coroutineContext) { callback() }
		}
	}

	fun up(playerId: Int, button: GameButton, callback: suspend () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.UP) launchImmediately(coroutineContext) { callback() }
		}
	}

    fun updatedGamepad(callback: (GamepadInfo) -> Unit) {
        this.updatedGamepad.add(callback)
    }

	fun connected(callback: suspend (playerId: Int) -> Unit) {
		connection { e ->
			if (e.type == GamePadConnectionEvent.Type.CONNECTED) launchImmediately(coroutineContext) { callback(e.gamepad) }
		}
	}

	fun disconnected(callback: suspend (playerId: Int) -> Unit) {
		connection { e ->
			if (e.type == GamePadConnectionEvent.Type.DISCONNECTED) launchImmediately(coroutineContext) { callback(e.gamepad) }
		}
	}

	private val oldGamepads = GamePadUpdateEvent()

	private val stickEvent = GamePadStickEvent()
	private val buttonEvent = GamePadButtonEvent()

    init {
        view.onEvent(GamePadUpdateEvent) { event ->
            this.views = event.target as Views
            gamepads.copyFrom(event)
            var gamepadsUpdated = false
            // Compute diff
            for (gamepadIndex in 0 until event.gamepadsLength) {
                val gamepad = event.gamepads[gamepadIndex]
                val oldGamepad = this.oldGamepads.gamepads[gamepadIndex]
                var updateCount = 0
                GameButton.BUTTONS.fastForEach { button ->
                    if (gamepad[button] != oldGamepad[button]) {
                        updateCount++
                        //println("CHANGED: button=$button: ${gamepad[button]}")
                        button(buttonEvent.apply {
                            this.gamepad = gamepad.index
                            this.type = if (gamepad[button] != 0.0) GamePadButtonEvent.Type.DOWN else GamePadButtonEvent.Type.UP
                            this.button = button
                            this.value = gamepad[button]
                        })
                    }
                }
                GameStick.STICKS.fastForEach { stick ->
                    val vector = gamepad[stick]
                    if (vector != oldGamepad[stick]) {
                        updateCount++
                        stick(stickEvent.apply {
                            this.gamepad = gamepad.index
                            this.stick = stick
                            this.x = vector.x
                            this.y = vector.y
                        })
                    }
                }
                if (updateCount > 0) {
                    updatedGamepad(gamepad)
                    gamepadsUpdated = true
                }
            }
            oldGamepads.copyFrom(event)
            if (gamepadsUpdated) updated(event)
        }
        view.onEvents(*GamePadConnectionEvent.Type.ALL) { event ->
            this.views = event.target as Views
            connection(event)
        }
    }
}

data class GamePadStickEvent(
    var gamepad: Int = 0,
    var stick: GameStick = GameStick.LEFT,
    var x: Double = 0.0,
    var y: Double = 0.0
) : TypedEvent<GamePadStickEvent>(GamePadStickEvent) {
    companion object : EventType<GamePadStickEvent>

    fun copyFrom(other: GamePadStickEvent) {
        this.gamepad = other.gamepad
        this.stick = other.stick
        this.x = other.x
        this.y = other.y
    }
}

data class GamePadButtonEvent @JvmOverloads constructor(
    override var type: Type = Type.DOWN,
    var gamepad: Int = 0,
    var button: GameButton = GameButton.BUTTON_SOUTH,
    var value: Double = 0.0
) : Event(), TEvent<GamePadButtonEvent> {
    //companion object : EventType<GamePadButtonEvent>

    enum class Type : EventType<GamePadButtonEvent> { UP, DOWN }

    fun copyFrom(other: GamePadButtonEvent) {
        this.type = other.type
        this.gamepad = other.gamepad
        this.button = other.button
        this.value = other.value
    }
}

val View.gamepad: GamePadEvents by Extra.PropertyThis { GamePadEvents(this) }
inline fun <T> View.gamepad(callback: GamePadEvents.() -> T): T = gamepad.run(callback)
