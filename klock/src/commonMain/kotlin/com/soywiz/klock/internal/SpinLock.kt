package com.soywiz.klock.internal

import com.soywiz.klock.hr.HRTimeSpan

internal fun spinlock(time: HRTimeSpan) {
    val start = HRTimeSpan.now()
    while (HRTimeSpan.now() - start < time) Unit
}
