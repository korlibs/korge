@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.SDL_TouchID
import com.soywiz.korgw.sdl2.jna.Uint16
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder(
    "type", "timestamp", "touchId", "dTheta", "dDist",
    "x", "y", "numFingers", "padding"
)
open class SDL_MultiGestureEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var dTheta: Float = 0f

    @JvmField
    var dDist: Float = 0f

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmField
    var numFingers: Uint16 = 0

    @JvmField
    var padding: Uint16 = 0

    class Ref(pointer: Pointer? = null) : SDL_MultiGestureEvent(pointer), ByReference
}
