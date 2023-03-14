package com.soywiz.kmem.pack

actual typealias Int4Pack = Vector128

actual val Int4Pack.i0: Int get() = this.getIntAt(0)
actual val Int4Pack.i1: Int get() = this.getIntAt(1)
actual val Int4Pack.i2: Int get() = this.getIntAt(2)
actual val Int4Pack.i3: Int get() = this.getIntAt(3)
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = vectorOf(i0, i1, i2, i3)
