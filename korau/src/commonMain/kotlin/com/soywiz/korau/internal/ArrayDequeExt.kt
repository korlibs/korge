package com.soywiz.korau.internal

import com.soywiz.kds.*

private val temp = FloatArray(1)

internal fun FloatArrayDeque.write(value: Float) {
    temp[0] = value
    write(temp, 0, 1)
}