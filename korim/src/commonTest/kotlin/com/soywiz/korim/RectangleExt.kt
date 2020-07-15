package com.soywiz.korim

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

fun Iterable<Rectangle>.render(): Bitmap32 {
    val colors = listOf(Colors.RED, Colors.GREEN, Colors.BLUE, Colors.BLACK)
    val bounds = this.bounds()
    val out = Bitmap32(bounds.width.toInt(), bounds.height.toInt())
    for ((index, r) in this.withIndex()) {
        val color = colors[index % colors.size]
        out.fill(color, r.x.toInt(), r.y.toInt(), r.width.toInt(), r.height.toInt())
    }
    return out
}
