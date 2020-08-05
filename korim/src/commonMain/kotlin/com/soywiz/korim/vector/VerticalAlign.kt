package com.soywiz.korim.vector

inline class VerticalAlign(val ratio: Double) {
    companion object {
        val TOP = VerticalAlign(0.0)
        val MIDDLE = VerticalAlign(0.5)
        val BOTTOM = VerticalAlign(1.0)
        val BASELINE = VerticalAlign(Double.POSITIVE_INFINITY) // Special

        operator fun invoke(str: String): VerticalAlign = when (str) {
            "TOP" -> TOP
            "MIDDLE" -> MIDDLE
            "BOTTOM" -> BOTTOM
            "BASELINE" -> BASELINE
            else -> VerticalAlign(str.substringAfter('(').substringBefore(')').toDoubleOrNull() ?: 0.0)
        }
    }

    fun getOffsetY(height: Double, baseline: Double): Double = when (this) {
        BASELINE -> baseline
        else -> -height * ratio
    }

    override fun toString(): String = when (this) {
        TOP -> "TOP"
        MIDDLE -> "MIDDLE"
        BOTTOM -> "BOTTOM"
        BASELINE -> "BASELINE"
        else -> "VerticalAlign($ratio)"
    }
}
