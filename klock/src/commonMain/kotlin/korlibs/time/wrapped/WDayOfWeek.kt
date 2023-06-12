package korlibs.time.wrapped

import korlibs.time.DayOfWeek
import korlibs.time.annotations.KlockExperimental

@KlockExperimental
val DayOfWeek.wrapped get() = this
@KlockExperimental
val WDayOfWeek.value get() = this
@KlockExperimental
fun WDayOfWeek(value: DayOfWeek) = value

// Enum, not inline
typealias WDayOfWeek = DayOfWeek
