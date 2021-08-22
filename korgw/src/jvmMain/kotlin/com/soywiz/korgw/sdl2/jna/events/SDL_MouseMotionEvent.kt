@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "windowID", "which", "state", "x", "y", "xrel", "yrel")
open class SDL_MouseMotionEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var which: Uint32 = 0

    @JvmField
    var state: Uint32 = 0

    @JvmField
    var x: Sint32 = 0

    @JvmField
    var y: Sint32 = 0

    @JvmField
    var xrel: Sint32 = 0

    @JvmField
    var yrel: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_MouseMotionEvent(pointer), ByReference
}
