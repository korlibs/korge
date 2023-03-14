package com.soywiz.kmem.pack

actual data class Int4Pack(val x: Int, val y: Int, val z: Int, val w: Int)
actual val Int4Pack.i0: Int get() = this.x
actual val Int4Pack.i1: Int get() = this.y
actual val Int4Pack.i2: Int get() = this.z
actual val Int4Pack.i3: Int get() = this.w
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = Int4Pack(i0, i1, i2, i3)
