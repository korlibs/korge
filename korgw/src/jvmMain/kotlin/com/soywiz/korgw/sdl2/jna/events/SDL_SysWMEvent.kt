@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

typealias SDL_SysWMmsg = Pointer

@FieldOrder("type", "timestamp", "msg")
open class SDL_SysWMEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var msg: SDL_SysWMmsg? = null

    class Ref(pointer: Pointer? = null) : SDL_SysWMEvent(pointer), ByReference
}
