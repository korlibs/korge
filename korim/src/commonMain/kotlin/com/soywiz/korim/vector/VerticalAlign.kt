package com.soywiz.korim.vector

inline class VerticalAlign(val ratio: Double) {
    companion object {
        val TOP = VerticalAlign(0.0)
        val MIDDLE = VerticalAlign(0.5)
        val BOTTOM = VerticalAlign(1.0)
        val BASELINE = VerticalAlign(Double.POSITIVE_INFINITY) // Special
    }

    fun getOffsetY(height: Double, baseline: Double): Double = when (this) {
        BASELINE -> baseline
        else -> -height * ratio
    }
}
