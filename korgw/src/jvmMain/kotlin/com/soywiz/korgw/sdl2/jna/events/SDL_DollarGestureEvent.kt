@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.SDL_GestureID
import com.soywiz.korgw.sdl2.jna.SDL_TouchID
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder("type", "timestamp", "touchId", "gestureId", "numFingers", "error", "x", "y")
open class SDL_DollarGestureEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var gestureId: SDL_GestureID = 0

    @JvmField
    var numFingers: Uint32 = 0

    @JvmField
    var error: Float = 0f

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    class Ref(pointer: Pointer? = null) : SDL_DollarGestureEvent(pointer), ByReference
}
