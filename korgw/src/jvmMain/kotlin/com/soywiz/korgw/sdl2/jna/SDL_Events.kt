package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.*
import com.soywiz.korgw.sdl2.jna.enums.SDL_EventType
import com.soywiz.korgw.sdl2.jna.structs.SDL_Keysym
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Union

typealias SDL_JoystickID = Sint32

@Structure.FieldOrder("type", "timestamp", "which", "iscapture", "padding1", "padding2", "padding3")
open class SDL_AudioDeviceEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: Uint32 = 0

    @JvmField
    var iscapture: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    class Ref(pointer: Pointer? = null) : SDL_AudioDeviceEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp")
open class SDL_CommonEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_CommonEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "axis", "padding1", "padding2", "padding3", "value", "padding4")
open class SDL_ControllerAxisEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var axis: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var value: Sint16 = 0

    @JvmField
    var padding4: Uint16 = 0

    class Ref(pointer: Pointer? = null) : SDL_ControllerAxisEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "button", "state", "padding1", "padding2")
open class SDL_ControllerButtonEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var button: Uint8 = 0

    @JvmField
    var state: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    class Ref(pointer: Pointer? = null) : SDL_ControllerButtonEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which")
open class SDL_ControllerDeviceEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_ControllerDeviceEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "sensor", "data")
open class SDL_ControllerSensorEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var sensor: Sint32 = 0

    @JvmField
    var data = FloatArray(3)

    class Ref(pointer: Pointer? = null) : SDL_ControllerSensorEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "touchpad", "finger", "x", "y", "pressure")
open class SDL_ControllerTouchpadEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var touchpad: Sint32 = 0

    @JvmField
    var finger: Sint32 = 0

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmField
    var pressure: Float = 0f

    class Ref(pointer: Pointer? = null) : SDL_ControllerTouchpadEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "display", "event", "padding1", "padding2", "padding3", "data1")
open class SDL_DisplayEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var display: Uint32 = 0

    @JvmField
    var event: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var data1: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_DisplayEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "touchId", "gestureId", "numFingers", "error", "x", "y")
open class SDL_DollarGestureEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var gestureId: SDL_GestureID = 0

    @JvmField
    var numFingers: Uint32 = 0

    @JvmField
    var error: Float = 0f

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    class Ref(pointer: Pointer? = null) : SDL_DollarGestureEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "file", "windowID")
open class SDL_DropEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var file: String = ""

    @JvmField
    var windowID: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_DropEvent(pointer), ByReference
}

open class SDL_Event(pointer: Pointer? = null) : Union(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var common = SDL_CommonEvent()

    @JvmField
    var display = SDL_DisplayEvent()

    @JvmField
    var window = SDL_WindowEvent()

    @JvmField
    var key = SDL_KeyboardEvent()

    @JvmField
    var edit = SDL_TextEditingEvent()

    @JvmField
    var text = SDL_TextInputEvent()

    @JvmField
    var motion = SDL_MouseMotionEvent()

    @JvmField
    var button = SDL_MouseButtonEvent()

    @JvmField
    var wheel = SDL_MouseWheelEvent()

    @JvmField
    var jaxis = SDL_JoyAxisEvent()

    @JvmField
    var jball = SDL_JoyBallEvent()

    @JvmField
    var jhat = SDL_JoyHatEvent()

    @JvmField
    var jbutton = SDL_JoyButtonEvent()

    @JvmField
    var jdevice = SDL_JoyDeviceEvent()

    @JvmField
    var caxis = SDL_ControllerAxisEvent()

    @JvmField
    var cbutton = SDL_ControllerButtonEvent()

    @JvmField
    var cdevice = SDL_ControllerDeviceEvent()

    @JvmField
    var ctouchpad = SDL_ControllerTouchpadEvent()

    @JvmField
    var csensor = SDL_ControllerSensorEvent()

    @JvmField
    var adevice = SDL_AudioDeviceEvent()

    @JvmField
    var sensor = SDL_SensorEvent()

    @JvmField
    var quit = SDL_QuitEvent()

    @JvmField
    var user = SDL_UserEvent()

    @JvmField
    var syswm = SDL_SysWMEvent()

    @JvmField
    var tfinger = SDL_TouchFingerEvent()

    @JvmField
    var mgesture = SDL_MultiGestureEvent()

    @JvmField
    var dgesture = SDL_DollarGestureEvent()

    @JvmField
    var drop = SDL_DropEvent()

    @JvmField
    var padding = ByteArray(56)

    override fun read() {
        super.read()
        val t = when (SDL_EventType.fromInt(type)) {
            SDL_EventType.DISPLAYEVENT -> SDL_DisplayEvent::class
            SDL_EventType.WINDOWEVENT -> SDL_WindowEvent::class
            SDL_EventType.SYSWMEVENT -> SDL_SysWMEvent::class
            SDL_EventType.KEYDOWN,
            SDL_EventType.KEYUP -> SDL_KeyboardEvent::class
            SDL_EventType.TEXTEDITING -> SDL_TextEditingEvent::class
            SDL_EventType.TEXTINPUT -> SDL_TextInputEvent::class
            SDL_EventType.MOUSEMOTION -> SDL_MouseMotionEvent::class
            SDL_EventType.MOUSEBUTTONDOWN,
            SDL_EventType.MOUSEBUTTONUP -> SDL_MouseButtonEvent::class
            SDL_EventType.MOUSEWHEEL -> SDL_MouseWheelEvent::class
            SDL_EventType.JOYAXISMOTION -> SDL_JoyAxisEvent::class
            SDL_EventType.JOYBALLMOTION -> SDL_JoyBallEvent::class
            SDL_EventType.JOYHATMOTION -> SDL_JoyHatEvent::class
            SDL_EventType.JOYBUTTONDOWN,
            SDL_EventType.JOYBUTTONUP -> SDL_JoyButtonEvent::class
            SDL_EventType.JOYDEVICEADDED,
            SDL_EventType.JOYDEVICEREMOVED -> SDL_JoyDeviceEvent::class
            SDL_EventType.CONTROLLERAXISMOTION -> SDL_ControllerAxisEvent::class
            SDL_EventType.CONTROLLERBUTTONDOWN,
            SDL_EventType.CONTROLLERBUTTONUP -> SDL_ControllerButtonEvent::class
            SDL_EventType.CONTROLLERDEVICEADDED,
            SDL_EventType.CONTROLLERDEVICEREMOVED,
            SDL_EventType.CONTROLLERDEVICEREMAPPED -> SDL_ControllerDeviceEvent::class
            SDL_EventType.CONTROLLERTOUCHPADDOWN,
            SDL_EventType.CONTROLLERTOUCHPADMOTION,
            SDL_EventType.CONTROLLERTOUCHPADUP -> SDL_ControllerTouchpadEvent::class
            SDL_EventType.CONTROLLERSENSORUPDATE -> SDL_ControllerSensorEvent::class
            SDL_EventType.FINGERDOWN,
            SDL_EventType.FINGERUP,
            SDL_EventType.FINGERMOTION -> SDL_TouchFingerEvent::class
            SDL_EventType.DOLLARGESTURE,
            SDL_EventType.DOLLARRECORD -> SDL_DollarGestureEvent::class
            SDL_EventType.MULTIGESTURE -> SDL_MultiGestureEvent::class
            SDL_EventType.DROPFILE,
            SDL_EventType.DROPTEXT,
            SDL_EventType.DROPBEGIN,
            SDL_EventType.DROPCOMPLETE -> SDL_DropEvent::class
            SDL_EventType.AUDIODEVICEADDED,
            SDL_EventType.AUDIODEVICEREMOVED -> SDL_AudioDeviceEvent::class
            SDL_EventType.SENSORUPDATE -> SDL_SensorEvent::class
            SDL_EventType.USEREVENT -> SDL_UserEvent::class
            else -> SDL_CommonEvent::class
        }
        setType(t.java)
    }

    class Ref(pointer: Pointer? = null) : SDL_Event(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "axis", "padding1", "padding2", "padding3", "value", "padding4")
open class SDL_JoyAxisEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var axis: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var value: Sint16 = 0

    @JvmField
    var padding4: Uint16 = 0

    class Ref(pointer: Pointer? = null) : SDL_JoyAxisEvent(pointer), ByReference
}

@Structure.FieldOrder(
    "type", "timestamp", "which", "ball",
    "padding1", "padding2", "padding3",
    "xrel", "yrel"
)
open class SDL_JoyBallEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var ball: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var xrel: Sint16 = 0

    @JvmField
    var yrel: Sint16 = 0

    class Ref(pointer: Pointer? = null) : SDL_JoyBallEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "button", "state", "padding1", "padding2")
open class SDL_JoyButtonEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var button: Uint8 = 0

    @JvmField
    var state: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    class Ref(pointer: Pointer? = null) : SDL_JoyButtonEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which")
open class SDL_JoyDeviceEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_JoyDeviceEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "hat", "value", "padding1", "padding2")
open class SDL_JoyHatEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: SDL_JoystickID = 0

    @JvmField
    var hat: Uint8 = 0

    @JvmField
    var value: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    class Ref(pointer: Pointer? = null) : SDL_JoyHatEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "windowID", "state", "repeat", "padding2", "padding3", "keysym")
open class SDL_KeyboardEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var state: Uint8 = 0

    @JvmField
    var repeat: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var keysym = SDL_Keysym()

    class Ref(pointer: Pointer? = null) : SDL_KeyboardEvent(pointer), ByReference
}

@Structure.FieldOrder(
    "type", "timestamp", "windowID",
    "which", "button", "state",
    "clicks", "padding1", "x", "y"
)
open class SDL_MouseButtonEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var which: Uint32 = 0

    @JvmField
    var button: Uint8 = 0

    @JvmField
    var state: Uint8 = 0

    @JvmField
    var clicks: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var x: Sint32 = 0

    @JvmField
    var y: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_MouseButtonEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "windowID", "which", "state", "x", "y", "xrel", "yrel")
open class SDL_MouseMotionEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var which: Uint32 = 0

    @JvmField
    var state: Uint32 = 0

    @JvmField
    var x: Sint32 = 0

    @JvmField
    var y: Sint32 = 0

    @JvmField
    var xrel: Sint32 = 0

    @JvmField
    var yrel: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_MouseMotionEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "windowID", "which", "x", "y", "direction")
open class SDL_MouseWheelEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var which: Uint32 = 0

    @JvmField
    var x: Sint32 = 0

    @JvmField
    var y: Sint32 = 0

    @JvmField
    var direction: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_MouseWheelEvent(pointer), ByReference
}

@Structure.FieldOrder(
    "type", "timestamp", "touchId", "dTheta", "dDist",
    "x", "y", "numFingers", "padding"
)
open class SDL_MultiGestureEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var dTheta: Float = 0f

    @JvmField
    var dDist: Float = 0f

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmField
    var numFingers: Uint16 = 0

    @JvmField
    var padding: Uint16 = 0

    class Ref(pointer: Pointer? = null) : SDL_MultiGestureEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp")
open class SDL_OSEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_OSEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp")
open class SDL_QuitEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_QuitEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "which", "data")
open class SDL_SensorEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var which: Sint32 = 0

    @JvmField
    var data = FloatArray(6)

    class Ref(pointer: Pointer? = null) : SDL_SensorEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "msg")
open class SDL_SysWMEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var msg: SDL_SysWMmsg? = null

    class Ref(pointer: Pointer? = null) : SDL_SysWMEvent(pointer), ByReference
}typealias SDL_SysWMmsg = Pointer

private const val SDL_TEXTEDITINGEVENT_TEXT_SIZE = 32

@Structure.FieldOrder("type", "timestamp", "windowID", "text", "start", "length")
open class SDL_TextEditingEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var text = CharArray(SDL_TEXTEDITINGEVENT_TEXT_SIZE)

    @JvmField
    var start: Sint32 = 0

    @JvmField
    var length: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_TextEditingEvent(pointer), ByReference
}

private const val SDL_TEXTINPUTEVENT_TEXT_SIZE = 32

@Structure.FieldOrder("type", "timestamp", "windowID", "text")
open class SDL_TextInputEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var text = CharArray(SDL_TEXTINPUTEVENT_TEXT_SIZE)

    class Ref(pointer: Pointer? = null) : SDL_TextInputEvent(pointer), ByReference
}

@Structure.FieldOrder(
    "type", "timestamp", "touchId", "fingerId",
    "x", "y", "dx", "dy", "pressure", "windowID"
)
open class SDL_TouchFingerEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var fingerId: SDL_FingerID = 0

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmField
    var dx: Float = 0f

    @JvmField
    var dy: Float = 0f

    @JvmField
    var pressure: Float = 0f

    @JvmField
    var windowID: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_TouchFingerEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "windowID", "code", "data1", "data2")
open class SDL_UserEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var code: Sint32 = 0

    @JvmField
    var data1: Pointer? = null

    @JvmField
    var data2: Pointer? = null

    class Ref(pointer: Pointer? = null) : SDL_UserEvent(pointer), ByReference
}

@Structure.FieldOrder("type", "timestamp", "windowID", "event", "padding1", "padding2", "padding3", "data1", "data2")
open class SDL_WindowEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var event: Uint8 = 0

    @JvmField
    var padding1: Uint8 = 0

    @JvmField
    var padding2: Uint8 = 0

    @JvmField
    var padding3: Uint8 = 0

    @JvmField
    var data1: Sint32 = 0

    @JvmField
    var data2: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_WindowEvent(pointer), ByReference
}
