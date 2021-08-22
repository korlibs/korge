@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.soywiz.korgw.sdl2.jna.structs.SDL_Keysym
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "windowID", "state", "repeat", "padding2", "padding3", "keysym")
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
