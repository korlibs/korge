@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

private const val SDL_TEXTINPUTEVENT_TEXT_SIZE = 32

@FieldOrder("type", "timestamp", "windowID", "text")
open class SDL_TextInputEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var text = CharArray(SDL_TEXTINPUTEVENT_TEXT_SIZE)

    class Ref(pointer: Pointer? = null) : SDL_TextInputEvent(pointer), ByReference
}
