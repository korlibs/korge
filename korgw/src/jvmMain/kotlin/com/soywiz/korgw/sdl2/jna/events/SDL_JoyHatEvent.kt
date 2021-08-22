@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

typealias SDL_JoystickID = Sint32

@FieldOrder("type", "timestamp", "which", "hat", "value", "padding1", "padding2")
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
