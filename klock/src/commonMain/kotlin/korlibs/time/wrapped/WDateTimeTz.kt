package korlibs.time.wrapped

import korlibs.time.DateTimeTz
import korlibs.time.annotations.KlockExperimental

@KlockExperimental
val DateTimeTz.wrapped get() = this
@KlockExperimental
val WDateTimeTz.value get() = this
@KlockExperimental
fun WDateTimeTz(value: DateTimeTz) = value

// Not inline
typealias WDateTimeTz = DateTimeTz
