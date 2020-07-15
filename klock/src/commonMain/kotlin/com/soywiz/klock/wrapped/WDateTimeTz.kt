package com.soywiz.klock.wrapped

import com.soywiz.klock.*
import com.soywiz.klock.annotations.*

@KlockExperimental
val DateTimeTz.wrapped get() = this
@KlockExperimental
val WDateTimeTz.value get() = this
@KlockExperimental
fun WDateTimeTz(value: DateTimeTz) = value

// Not inline
typealias WDateTimeTz = DateTimeTz
