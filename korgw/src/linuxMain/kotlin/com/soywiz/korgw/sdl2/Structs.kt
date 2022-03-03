package com.soywiz.korgw.sdl2

import kotlinx.cinterop.*

class SDL_DisplayMode {
    companion object {
        const val SIZE = 24
    }

    var format: Uint32 = 0
    var w: Int = 0
    var h: Int = 0
    var refresh_rate: Int = 0
    var driverdata: CPointer<*>? = null

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        format = idata[0]
        w = idata[1]
        h = idata[2]
        refresh_rate = idata[3]
        // driverdata = ??
    }
}

class SDL_Keysym {
    var scancode: Int = 0
    var sym: SDL_Keycode = 0
    var mod: Uint16 = 0 // offset: 8
    var unused: Uint32 = 0

    fun write(data: CPointer<ByteVarOf<Byte>>) {
        val idata = data.reinterpret<IntVar>()
        val sdata = data.reinterpret<ShortVar>()
        scancode = idata[0]
        sym = idata[1]
        mod = sdata[4]
    }
}
