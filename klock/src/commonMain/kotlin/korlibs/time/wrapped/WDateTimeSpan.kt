package korlibs.time.wrapped

import korlibs.time.DateTimeSpan
import korlibs.time.annotations.KlockExperimental

@KlockExperimental
val DateTimeSpan.wrapped get() = WDateTimeSpan(this)
@KlockExperimental
val WDateTimeSpan.value get() = this
@KlockExperimental
fun WDateTimeSpan(value: DateTimeSpan) = value

// It is not inline
//@KlockExperimental
typealias WDateTimeSpan = DateTimeSpan
