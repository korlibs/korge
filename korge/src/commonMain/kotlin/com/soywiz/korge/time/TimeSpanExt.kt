package com.soywiz.korge.time

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korma.interpolation.interpolate

fun Double.interpolate(a: TimeSpan, b: TimeSpan): TimeSpan = this.interpolate(a.milliseconds, b.milliseconds).milliseconds
