package korlibs.time.internal

import korlibs.time.hr.HRTimeSpan

internal fun spinlock(time: HRTimeSpan) {
    val start = HRTimeSpan.now()
    while (HRTimeSpan.now() - start < time) Unit
}