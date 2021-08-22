@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.enums.SDL_EventType
import com.sun.jna.Pointer
import com.sun.jna.Union

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
