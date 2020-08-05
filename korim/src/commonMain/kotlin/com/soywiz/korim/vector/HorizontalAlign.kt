package com.soywiz.korim.vector

inline class HorizontalAlign(val ratio: Double) {
    companion object {
        val JUSTIFY = HorizontalAlign(-0.001)
        val LEFT = HorizontalAlign(0.0)
        val CENTER = HorizontalAlign(0.5)
        val RIGHT = HorizontalAlign(1.0)

        operator fun invoke(str: String): HorizontalAlign = when (str) {
            "LEFT" -> LEFT
            "CENTER" -> CENTER
            "RIGHT" -> RIGHT
            "JUSTIFY" -> JUSTIFY
            else -> HorizontalAlign(str.substringAfter('(').substringBefore(')').toDoubleOrNull() ?: 0.0)
        }
    }

    fun getOffsetX(width: Double): Double = when (this) {
        JUSTIFY -> 0.0
        else -> width * ratio
    }

    override fun toString(): String = when (this) {
        LEFT -> "LEFT"
        CENTER -> "CENTER"
        RIGHT -> "RIGHT"
        JUSTIFY -> "JUSTIFY"
        else -> "HorizontalAlign($ratio)"
    }
}
