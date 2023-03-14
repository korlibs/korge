package com.soywiz.kmem.pack

expect class Int4Pack
expect val Int4Pack.i0: Int
expect val Int4Pack.i1: Int
expect val Int4Pack.i2: Int
expect val Int4Pack.i3: Int
expect fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack

fun Int4Pack.copy(i0: Int = this.i0, i1: Int = this.i1, i2: Int = this.i2, i3: Int = this.i3): Int4Pack =
    int4PackOf(i0, i1, i2, i3)
