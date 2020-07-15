package com.soywiz.korim.vector

inline class HorizontalAlign(val ratio: Double) {
    companion object {
        val JUSTIFY = HorizontalAlign(-0.001)
        val LEFT = HorizontalAlign(0.0)
        val CENTER = HorizontalAlign(0.5)
        val RIGHT = HorizontalAlign(1.0)
    }

    fun getOffsetX(width: Double): Double = when (this) {
        JUSTIFY -> 0.0
        else -> width * ratio
    }
}
