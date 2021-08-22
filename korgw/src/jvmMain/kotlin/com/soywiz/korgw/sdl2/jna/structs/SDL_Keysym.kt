@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.structs

import com.soywiz.korgw.sdl2.jna.SDL_Keycode
import com.soywiz.korgw.sdl2.jna.Uint16
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("scancode", "sym", "mod", "unused")
open class SDL_Keysym(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var scancode: Int = 0

    @JvmField
    var sym: SDL_Keycode = 0

    @JvmField
    var mod: Uint16 = 0

    @JvmField
    var unused: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_Keysym(pointer), ByReference
}
