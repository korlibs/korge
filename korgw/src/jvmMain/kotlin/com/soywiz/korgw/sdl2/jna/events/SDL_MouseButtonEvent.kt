@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder(
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
