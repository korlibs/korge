@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.soywiz.korgw.sdl2.jna.Uint8
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "windowID", "event", "padding1", "padding2", "padding3", "data1", "data2")
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
