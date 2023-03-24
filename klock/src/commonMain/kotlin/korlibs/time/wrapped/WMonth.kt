package korlibs.time.wrapped

import korlibs.time.Month
import korlibs.time.annotations.KlockExperimental

@KlockExperimental
val Month.wrapped get() = this
@KlockExperimental
val WMonth.value get() = this
@KlockExperimental
fun WMonth(value: Month) = value

// An enum, thus not inline
typealias WMonth = Month