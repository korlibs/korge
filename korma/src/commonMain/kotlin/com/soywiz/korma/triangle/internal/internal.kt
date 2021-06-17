package com.soywiz.korma.triangle.internal

import kotlin.math.*

internal val Float.niceStr2: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
internal val Double.niceStr2: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"

internal object Constants {
    const val EPSILON: Double = 1e-12
    const val PI_2: Double = PI / 2.0
    const val PI_3div4: Double = 3 * PI / 4
}
