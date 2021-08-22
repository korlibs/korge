@file:Suppress("ClassName", "PropertyName")

package com.soywiz.korgw.sdl2.jna.structs

import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("format", "w", "h", "refresh_rate", "driverdata")
open class SDL_DisplayMode(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var format: Uint32 = 0

    @JvmField
    var w: Int = 0

    @JvmField
    var h: Int = 0

    @JvmField
    var refresh_rate: Int = 0

    @JvmField
    var driverdata: Pointer? = null

    class Ref(pointer: Pointer? = null) : SDL_DisplayMode(pointer), ByReference
}
