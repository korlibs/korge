package korlibs.io.util
import kotlin.native.concurrent.ThreadLocal

val LONG_ZERO_TO_MAX_RANGE = 0L..Long.MAX_VALUE
fun IntRange.toLongRange() = this.start.toLong()..this.endInclusive.toLong()
