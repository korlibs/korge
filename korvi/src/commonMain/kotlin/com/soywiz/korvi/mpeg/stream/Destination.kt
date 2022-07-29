package com.soywiz.korvi.mpeg.stream

import com.soywiz.kmem.Uint8ClampedBuffer

interface Destination {
}

interface AudioDestination : Destination {
    fun play(rate: Int, left: FloatArray, right: FloatArray)
    val enqueuedTime: Double
}

interface VideoDestination : Destination {
    fun render(y: Uint8ClampedBuffer?, Cr: Uint8ClampedBuffer?, Cb: Uint8ClampedBuffer?, v: Boolean)
    fun resize(width: Int, height: Int)
}
