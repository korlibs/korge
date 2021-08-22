@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint16
import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder(
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
