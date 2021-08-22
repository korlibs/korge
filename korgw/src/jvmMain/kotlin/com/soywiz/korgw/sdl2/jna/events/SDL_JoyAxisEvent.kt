@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint16
import com.soywiz.korgw.sdl2.jna.Uint16
import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "which", "axis", "padding1", "padding2", "padding3", "value", "padding4")
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
