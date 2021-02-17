package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korma.interpolation.*

fun Double.interpolate(a: TimeSpan, b: TimeSpan): TimeSpan = this.interpolate(a.milliseconds, b.milliseconds).milliseconds
