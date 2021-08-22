@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "which", "touchpad", "finger", "x", "y", "pressure")
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
