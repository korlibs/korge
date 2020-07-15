package com.soywiz.korim.bitmap

import com.soywiz.korio.lang.*

class DistanceBitmap(
    val width: Int,
    val height: Int,
    val dx: FloatArray = FloatArray(width * height),
    val dy: FloatArray = FloatArray(width * height)
) {
    val area = width * height
    init {
        assert(dx.size >= area)
        assert(dy.size >= area)
    }

    fun index(x: Int, y: Int) = y * width + x
    fun set(x: Int, y: Int, dx: Float, dy: Float) {
        val i = index(x, y)
        this.dx[i] = dx
        this.dy[i] = dy
    }
    fun getX(x: Int, y: Int): Float = this.dx[index(x, y)]
    fun getY(x: Int, y: Int): Float = this.dy[index(x, y)]
}
