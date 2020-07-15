package com.soywiz.korim.vector

import com.soywiz.kmem.*

enum class CycleMethod { NO_CYCLE, REFLECT, REPEAT }

fun CycleMethod.apply(ratio: Double, clamp: Boolean = false): Double = when (this) {
    CycleMethod.NO_CYCLE -> if (clamp) ratio.clamp01() else ratio
    CycleMethod.REPEAT -> ratio % 1
    CycleMethod.REFLECT -> {
        val part = ratio % 2
        if (part > 1.0) 2.0 - part else part
    }
}

fun CycleMethod.apply(value: Double, size: Double): Double = apply(value / size) * size
fun CycleMethod.apply(value: Double, min: Double, max: Double): Double = apply(value - min, max - min) + min
