@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "windowID", "code", "data1", "data2")
open class SDL_UserEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var code: Sint32 = 0

    @JvmField
    var data1: Pointer? = null

    @JvmField
    var data2: Pointer? = null

    class Ref(pointer: Pointer? = null) : SDL_UserEvent(pointer), ByReference
}
