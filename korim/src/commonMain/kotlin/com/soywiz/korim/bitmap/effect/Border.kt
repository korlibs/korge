package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

fun Bitmap32.border(r: Int, color: RGBA = Colors.BLACK): Bitmap32 {
    val out = Bitmap32(width + (r * 2), height + (r * 2), premultiplied = true)
    out.put(this, r, r)
    val distance = out.distanceMap()
    for (y in 0 until out.height) {
        for (x in 0 until out.width) {
            val dist = distance.getDist(x, y)
            if (dist < r + 1) {
                val alpha = if (dist >= r) 1f - dist % 1f else 1f
                val rcolor = if (alpha == 1f) color else color.withAf(color.af * alpha)
                out[x, y] = RGBA.mix(rcolor, out[x, y])
            }
        }
    }
    return out
}
