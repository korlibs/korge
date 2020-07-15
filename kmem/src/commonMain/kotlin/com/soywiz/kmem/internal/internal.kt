package com.soywiz.kmem.internal

import kotlin.math.*

internal inline fun <S, D> arraycopyBase(src: S, srcPos: Int, dst: D, dstPos: Int, size: Int, set: (s: Int, d: Int) -> Unit) {
    if (src === dst && dstPos > srcPos) { // overlapping
        var n = size
        while (--n >= 0) set(srcPos + n, dstPos + n)
    } else {
        for (n in 0 until size) set(srcPos + n, dstPos + n)
    }
}
