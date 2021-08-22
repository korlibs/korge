@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.Sint32
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

private const val SDL_TEXTEDITINGEVENT_TEXT_SIZE = 32

@FieldOrder("type", "timestamp", "windowID", "text", "start", "length")
open class SDL_TextEditingEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var windowID: Uint32 = 0

    @JvmField
    var text = CharArray(SDL_TEXTEDITINGEVENT_TEXT_SIZE)

    @JvmField
    var start: Sint32 = 0

    @JvmField
    var length: Sint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_TextEditingEvent(pointer), ByReference
}
