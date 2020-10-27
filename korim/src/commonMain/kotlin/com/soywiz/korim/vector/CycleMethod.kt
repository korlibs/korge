package com.soywiz.korim.vector

import com.soywiz.kmem.*

enum class CycleMethod {
    NO_CYCLE, REFLECT, REPEAT;

    val repeating get() = this != NO_CYCLE

    fun apply(ratio: Double, clamp: Boolean = false): Double = when (this) {
        NO_CYCLE -> if (clamp) ratio.clamp01() else ratio
        REPEAT -> ratio % 1
        REFLECT -> {
            val part = ratio % 2
            if (part > 1.0) 2.0 - part else part
        }
    }

    fun apply(value: Double, size: Double): Double = apply(value / size) * size
    fun apply(value: Double, min: Double, max: Double): Double = apply(value - min, max - min) + min

    companion object {
        fun fromRepeat(repeat: Boolean) = if (repeat) REPEAT else NO_CYCLE
    }
}
