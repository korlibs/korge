package com.soywiz.korim

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bounds

fun Iterable<Rectangle>.render(): Bitmap32 {
    val colors = listOf(Colors.RED, Colors.GREEN, Colors.BLUE, Colors.BLACK)
    val bounds = this.bounds()
    val out = Bitmap32(bounds.width.toInt(), bounds.height.toInt(), premultiplied = false)
    for ((index, r) in this.withIndex()) {
        val color = colors[index % colors.size]
        out.fill(color, r.x.toInt(), r.y.toInt(), r.width.toInt(), r.height.toInt())
    }
    return out
}
