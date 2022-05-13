package com.soywiz.klock.wrapped

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.annotations.KlockExperimental

@KlockExperimental
val DayOfWeek.wrapped get() = this
@KlockExperimental
val WDayOfWeek.value get() = this
@KlockExperimental
fun WDayOfWeek(value: DayOfWeek) = value

// Enum, not inline
typealias WDayOfWeek = DayOfWeek
