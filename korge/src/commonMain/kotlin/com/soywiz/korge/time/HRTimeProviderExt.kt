package com.soywiz.korge.time

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.hr.HRTimeProvider
import com.soywiz.korge.internal.KorgeInternal

@Deprecated("")
@KorgeInternal
fun HRTimeProvider.toTimeProvider(): TimeProvider = TimeProvider { DateTime.fromUnix(this.now().millisecondsDouble) }
