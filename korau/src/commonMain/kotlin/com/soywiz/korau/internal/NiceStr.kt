package com.soywiz.korau.internal

import kotlin.math.floor

internal val Double.niceStr: String get() = if (floor(this) == this) "${this.toInt()}" else "$this"
