package com.soywiz.klock.wrapped

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.annotations.KlockExperimental

@KlockExperimental
val DateTimeTz.wrapped get() = this
@KlockExperimental
val WDateTimeTz.value get() = this
@KlockExperimental
fun WDateTimeTz(value: DateTimeTz) = value

// Not inline
typealias WDateTimeTz = DateTimeTz
