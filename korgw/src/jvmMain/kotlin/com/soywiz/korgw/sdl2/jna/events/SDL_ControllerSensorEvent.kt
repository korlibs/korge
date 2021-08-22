@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "which", "sensor", "data")
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
