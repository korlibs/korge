package com.soywiz.korim.vector

import com.soywiz.kmem.clamp01
import com.soywiz.kmem.fract
import com.soywiz.kmem.umod

enum class CycleMethod {
    NO_CYCLE, NO_CYCLE_CLAMP, REPEAT, REFLECT;

    val repeating: Boolean get() = this != NO_CYCLE && this != NO_CYCLE_CLAMP

    fun apply(ratio: Float): Float = when (this) {
        NO_CYCLE -> ratio
        NO_CYCLE_CLAMP -> ratio.clamp01()
        REPEAT -> fract(ratio)
        REFLECT -> {
            val part = ratio umod 2f
            if (part > 1f) 2f - part else part
        }
    }

    fun apply(ratio: Double): Double = apply(ratio.toFloat()).toDouble()

    fun apply(value: Double, size: Double): Double = apply(value / size) * size
    fun apply(value: Double, min: Double, max: Double): Double = apply(value - min, max - min) + min

    fun apply(value: Float, size: Float): Float = apply(value / size) * size
    fun apply(value: Float, min: Float, max: Float): Float = apply(value - min, max - min) + min

    companion object {
        fun fromRepeat(repeat: Boolean) = if (repeat) REPEAT else NO_CYCLE
    }
}
