package com.soywiz.korev

import kotlin.jvm.*


data class GamePadConnectionEvent(
    override var type: Type = Type.CONNECTED,
    var gamepad: Int = 0
) : Event(), TEvent<GamePadConnectionEvent> {

    enum class Type : EventType<GamePadConnectionEvent> {
        CONNECTED, DISCONNECTED;
        companion object {
            fun fromConnected(connected: Boolean): Type = if (connected) CONNECTED else DISCONNECTED
        }
    }

    fun copyFrom(other: GamePadConnectionEvent) {
        this.type = other.type
        this.gamepad = other.gamepad
    }
}

@Suppress("ArrayInDataClass")
data class GamePadUpdateEvent @JvmOverloads constructor(
    var gamepadsLength: Int = 0,
    val gamepads: Array<GamepadInfo> = Array(GamepadInfo.MAX_CONTROLLERS) { GamepadInfo(it) },
) : Event(), TEvent<GamePadUpdateEvent> {
    override val type: EventType<GamePadUpdateEvent> get() = GamePadUpdateEvent
    companion object : EventType<GamePadUpdateEvent>

    fun copyFrom(that: GamePadUpdateEvent) {
        this.gamepadsLength = that.gamepadsLength
        for (n in 0 until gamepads.size) {
            this.gamepads[n].copyFrom(that.gamepads[n])
        }
    }

    override fun toString(): String = "GamePadUpdateEvent(${gamepads.filter { it.connected }})"
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

@Deprecated("")
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
