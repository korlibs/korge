@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.events

import com.soywiz.korgw.sdl2.jna.SDL_FingerID
import com.soywiz.korgw.sdl2.jna.SDL_TouchID
import com.soywiz.korgw.sdl2.jna.Uint32
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

@FieldOrder(
    "type", "timestamp", "touchId", "fingerId",
    "x", "y", "dx", "dy", "pressure", "windowID"
)
open class SDL_TouchFingerEvent(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField
    var type: Uint32 = 0

    @JvmField
    var timestamp: Uint32 = 0

    @JvmField
    var touchId: SDL_TouchID = 0

    @JvmField
    var fingerId: SDL_FingerID = 0

    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmField
    var dx: Float = 0f

    @JvmField
    var dy: Float = 0f

    @JvmField
    var pressure: Float = 0f

    @JvmField
    var windowID: Uint32 = 0

    class Ref(pointer: Pointer? = null) : SDL_TouchFingerEvent(pointer), ByReference
}
