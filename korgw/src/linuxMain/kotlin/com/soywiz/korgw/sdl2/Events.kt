package com.soywiz.korgw.sdl2

import com.soywiz.korgw.sdl2.SDL_EventType.*
import kotlinx.cinterop.*

typealias SDL_JoystickID = Sint32
typealias SDL_SysWMmsg = CPointer<*>

private const val SDL_TEXTEDITINGEVENT_TEXT_SIZE = 32
private const val SDL_TEXTINPUTEVENT_TEXT_SIZE = 32

class SDL_Event {
    companion object {
        const val SIZE = 56
    }

    var type: Uint32 = 0
    var common = SDL_CommonEvent()
    var display = SDL_DisplayEvent()
    var window = SDL_WindowEvent()
    var key = SDL_KeyboardEvent()
    var edit = SDL_TextEditingEvent()
    var text = SDL_TextInputEvent()
    var motion = SDL_MouseMotionEvent()
    var button = SDL_MouseButtonEvent()
    var wheel = SDL_MouseWheelEvent()
    var jaxis = SDL_JoyAxisEvent()
    var jball = SDL_JoyBallEvent()
    var jhat = SDL_JoyHatEvent()
    var jbutton = SDL_JoyButtonEvent()
    var jdevice = SDL_JoyDeviceEvent()
    var caxis = SDL_ControllerAxisEvent()
    var cbutton = SDL_ControllerButtonEvent()
    var cdevice = SDL_ControllerDeviceEvent()
    var ctouchpad = SDL_ControllerTouchpadEvent()
    var csensor = SDL_ControllerSensorEvent()
    var adevice = SDL_AudioDeviceEvent()
    var sensor = SDL_SensorEvent()
    var quit = SDL_QuitEvent()
    var user = SDL_UserEvent()
    var syswm = SDL_SysWMEvent()
    var tfinger = SDL_TouchFingerEvent()
    var mgesture = SDL_MultiGestureEvent()
    var dgesture = SDL_DollarGestureEvent()
    var drop = SDL_DropEvent()

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        type = data.reinterpret<IntVar>()[0]
        when (SDL_EventType.fromInt(type)) {
            QUIT -> quit.write(data)
            DISPLAYEVENT -> display.write(data)
            WINDOWEVENT -> window.write(data)
            SYSWMEVENT -> syswm.write(data)
            KEYDOWN,
            KEYUP -> key.write(data)
            TEXTEDITING -> edit.write(data)
            TEXTINPUT -> text.write(data)
            MOUSEMOTION -> motion.write(data)
            MOUSEBUTTONDOWN,
            MOUSEBUTTONUP -> button.write(data)
            MOUSEWHEEL -> wheel.write(data)
            JOYAXISMOTION -> jaxis.write(data)
            JOYBALLMOTION -> jball.write(data)
            JOYHATMOTION -> jhat.write(data)
            JOYBUTTONDOWN,
            JOYBUTTONUP -> jbutton.write(data)
            JOYDEVICEADDED,
            JOYDEVICEREMOVED -> jdevice.write(data)
            CONTROLLERAXISMOTION -> caxis.write(data)
            CONTROLLERBUTTONDOWN,
            CONTROLLERBUTTONUP -> cbutton.write(data)
            CONTROLLERDEVICEADDED,
            CONTROLLERDEVICEREMOVED,
            CONTROLLERDEVICEREMAPPED -> cdevice.write(data)
            CONTROLLERTOUCHPADDOWN,
            CONTROLLERTOUCHPADMOTION,
            CONTROLLERTOUCHPADUP -> ctouchpad.write(data)
            CONTROLLERSENSORUPDATE -> csensor.write(data)
            FINGERDOWN,
            FINGERUP,
            FINGERMOTION -> tfinger.write(data)
            DOLLARGESTURE,
            DOLLARRECORD -> dgesture.write(data)
            MULTIGESTURE -> mgesture.write(data)
            DROPCOMPLETE -> drop.write(data)
            AUDIODEVICEADDED,
            AUDIODEVICEREMOVED -> adevice.write(data)
            SENSORUPDATE -> sensor.write(data)
            else -> {
                if (type >= USEREVENT.value) {
                    user.write(data)
                } else {
                    common.write(data)
                }
            }
        }
    }
}

class SDL_CommonEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
    }
}

class SDL_DisplayEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var display: Uint32 = 0 // offset: 8
    var event: Uint8 = 0 // Offset: 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var data1: Sint32 = 0 // Offset: 16

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        display = idata[2]
        event = data[12]
        data1 = idata[4]
    }
}

class SDL_WindowEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var event: Uint8 = 0 // offset 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var data1: Sint32 = 0 // offset 16
    var data2: Sint32 = 0 // offset 20

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        event = data[12]
        data1 = idata[4]
        data2 = idata[5]
    }
}

class SDL_KeyboardEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var state: Uint8 = 0 // offset: 12
    var repeat: Uint8 = 0 // offset: 13
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var keysym = SDL_Keysym() // offset: 16

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        state = data[12]
        repeat = data[13]
        keysym.write(data.plus(16)!!)
    }
}

class SDL_TextEditingEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var text: String = "" // offset: 12
    var start: Sint32 = 0 // offset: 44
    var length: Sint32 = 0 // offset: 48

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        text = data.plus(12)!!.readBytes(SDL_TEXTEDITINGEVENT_TEXT_SIZE).decodeToString()
        start = idata[11]
        length = idata[12]
    }
}

class SDL_TextInputEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var text: String = "" // offset: 12

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        text = data.plus(12)!!.readBytes(SDL_TEXTINPUTEVENT_TEXT_SIZE).decodeToString()
    }
}

class SDL_MouseMotionEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var which: Uint32 = 0 // offset: 12
    var state: Uint32 = 0 // offset: 16
    var x: Sint32 = 0 // offset: 20
    var y: Sint32 = 0 // offset: 24
    var xrel: Sint32 = 0 // offset: 28
    var yrel: Sint32 = 0 // offset: 32

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        which = idata[3]
        state = idata[4]
        x = idata[5]
        y = idata[6]
        xrel = idata[7]
        yrel = idata[8]
    }
}

class SDL_MouseButtonEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var which: Uint32 = 0 // offset: 12
    var button: Uint8 = 0 // offset: 16
    var state: Uint8 = 0 // offset: 17
    var clicks: Uint8 = 0 // offset: 18
    var padding1: Uint8 = 0
    var x: Sint32 = 0 // offset: 20
    var y: Sint32 = 0 // offset: 24

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        which = idata[3]
        button = data[16]
        state = data[17]
        clicks = data[18]
        x = idata[5]
        y = idata[6]
    }
}

class SDL_MouseWheelEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var which: Uint32 = 0 // offset: 12
    var x: Sint32 = 0 // offset: 16
    var y: Sint32 = 0 // offset: 20
    var direction: Uint32 = 0 // offset: 24

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        which = idata[2]
        x = idata[3]
        y = idata[4]
        direction = idata[5]
    }
}

class SDL_JoyAxisEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var axis: Uint8 = 0 // offset: 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var value: Sint16 = 0 // offset: 16
    var padding4: Uint16 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val sdata = data.reinterpret<ShortVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        axis = data[12]
        value = sdata[8]
    }
}

class SDL_JoyBallEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var ball: Uint8 = 0 // offset: 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var xrel: Sint16 = 0 // offset: 16
    var yrel: Sint16 = 0 // offset: 18

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val sdata = data.reinterpret<ShortVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        ball = data[12]
        xrel = sdata[8]
        yrel = sdata[9]
    }
}

class SDL_JoyHatEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var hat: Uint8 = 0 // offset: 12
    var value: Uint8 = 0 // offset: 13
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        hat = data[12]
        value = data[13]
    }
}

class SDL_JoyButtonEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var button: Uint8 = 0 // offset: 12
    var state: Uint8 = 0 // offset: 13
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        button = data[12]
        state = data[13]
    }
}

class SDL_JoyDeviceEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: Sint32 = 0 // offset: 8

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
    }
}

class SDL_ControllerAxisEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var axis: Uint8 = 0 // offset: 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0
    var value: Sint16 = 0 // offset: 16
    var padding4: Uint16 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val sdata = data.reinterpret<ShortVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        axis = data[12]
        value = sdata[8]
    }
}

class SDL_ControllerButtonEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var button: Uint8 = 0 // offset: 12
    var state: Uint8 = 0 // offset: 13
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        button = data[12]
        state = data[13]
    }
}

class SDL_ControllerDeviceEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: Sint32 = 0 // offset: 8

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
    }
}

class SDL_ControllerTouchpadEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var touchpad: Sint32 = 0 // offset: 12
    var finger: Sint32 = 0 // offset: 16
    var x: Float = 0f // offset: 20
    var y: Float = 0f // offset: 24
    var pressure: Float = 0f // offset: 28

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val fdata = data.reinterpret<FloatVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        touchpad = idata[3]
        finger = idata[4]
        x = fdata[5]
        y = fdata[6]
        pressure = fdata[7]
    }
}

class SDL_ControllerSensorEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: SDL_JoystickID = 0 // offset: 8
    var sensor: Sint32 = 0 // offset: 12
    var data = FloatArray(3) // offset: 16

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val fdata = data.reinterpret<FloatVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        sensor = idata[3]
        val d0 = fdata[4]
        val d1 = fdata[5]
        val d2 = fdata[6]
        this.data = floatArrayOf(d0, d1, d2)
    }
}

class SDL_AudioDeviceEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: Uint32 = 0 // offset: 8
    var iscapture: Uint8 = 0 // offset: 12
    var padding1: Uint8 = 0
    var padding2: Uint8 = 0
    var padding3: Uint8 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        iscapture = data[12]
    }
}

class SDL_SensorEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var which: Sint32 = 0 // offset: 8
    var data = FloatArray(6) // offset: 12

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val fdata = data.reinterpret<FloatVar>()
        type = idata[0]
        timestamp = idata[1]
        which = idata[2]
        val d0 = fdata[3]
        val d1 = fdata[4]
        val d2 = fdata[5]
        val d3 = fdata[6]
        val d4 = fdata[7]
        val d5 = fdata[8]
        this.data = floatArrayOf(d0, d1, d2, d3, d4, d5)
    }
}

class SDL_QuitEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
    }
}

class SDL_UserEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var windowID: Uint32 = 0 // offset: 8
    var code: Sint32 = 0 // offset: 12
    var data1: CPointer<*>? = null // offset: 16
    var data2: CPointer<*>? = null // offset: 20

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        windowID = idata[2]
        code = idata[3]
        // data1 = ??
        // data2 = ??
    }
}

class SDL_SysWMEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var msg: SDL_SysWMmsg? = null // offset: 8

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        // msg = ??
    }
}

class SDL_TouchFingerEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var touchId: SDL_TouchID = 0 // offset: 8
    var fingerId: SDL_FingerID = 0 // offset: 16
    var x: Float = 0f // offset: 24
    var y: Float = 0f // offset: 28
    var dx: Float = 0f // offset: 32
    var dy: Float = 0f // offset: 36
    var pressure: Float = 0f // offset: 40
    var windowID: Uint32 = 0 // offset: 44

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val ldata = data.reinterpret<LongVar>()
        val fdata = data.reinterpret<FloatVar>()
        type = idata[0]
        timestamp = idata[1]
        touchId = ldata[1]
        fingerId = ldata[2]
        x = fdata[6]
        y = fdata[7]
        dx = fdata[8]
        dy = fdata[9]
        pressure = fdata[10]
        windowID = idata[11]
    }
}

class SDL_MultiGestureEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var touchId: SDL_TouchID = 0 // offset: 8
    var dTheta: Float = 0f // offset: 16
    var dDist: Float = 0f // offset: 20
    var x: Float = 0f // offset: 24
    var y: Float = 0f // offset: 28
    var numFingers: Uint16 = 0 // offset: 32
    var padding: Uint16 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val ldata = data.reinterpret<LongVar>()
        val fdata = data.reinterpret<FloatVar>()
        val sdata = data.reinterpret<ShortVar>()
        type = idata[0]
        timestamp = idata[1]
        touchId = ldata[1]
        dTheta = fdata[4]
        dDist = fdata[5]
        x = fdata[6]
        y = fdata[7]
        numFingers = sdata[16]
    }
}

class SDL_DollarGestureEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var touchId: SDL_TouchID = 0 // offset: 8
    var gestureId: SDL_GestureID = 0 // offset: 16
    var numFingers: Uint32 = 0 // offset: 24
    var error: Float = 0f // offset: 28
    var x: Float = 0f // offset: 32
    var y: Float = 0f // offset: 36

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val ldata = data.reinterpret<LongVar>()
        val fdata = data.reinterpret<FloatVar>()
        type = idata[0]
        timestamp = idata[1]
        touchId = ldata[1]
        gestureId = ldata[2]
        numFingers = idata[6]
        error = fdata[7]
        x = fdata[8]
        y = fdata[9]
    }
}

class SDL_DropEvent {
    var type: Uint32 = 0 // offset: 0
    var timestamp: Uint32 = 0 // offset: 4
    var file: String = "" // offset: 8 (pointer)
    var windowID: Uint32 = 0 // offset: 12

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        type = idata[0]
        timestamp = idata[1]
        file = data.plus(8)!!.toKString()
        windowID = idata[3]
    }
}
