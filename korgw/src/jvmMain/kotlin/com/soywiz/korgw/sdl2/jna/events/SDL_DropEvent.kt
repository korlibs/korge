@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "file", "windowID")
open class SDL_DropEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var file: String = ""

    @JvmField
    var windowID: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_DropEvent(pointer), ByReference
}
