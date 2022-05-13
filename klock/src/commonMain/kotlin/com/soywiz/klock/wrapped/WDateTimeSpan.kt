package com.soywiz.klock.wrapped

import com.soywiz.klock.DateTimeSpan
import com.soywiz.klock.annotations.KlockExperimental

@KlockExperimental
val DateTimeSpan.wrapped get() = WDateTimeSpan(this)
@KlockExperimental
val WDateTimeSpan.value get() = this
@KlockExperimental
fun WDateTimeSpan(value: DateTimeSpan) = value

// It is not inline
//@KlockExperimental
typealias WDateTimeSpan = DateTimeSpan
