package korlibs.io.util

val IntRange.length: Int get() = (this.last - this.first) + 1
val LongRange.length: Long get() = (this.last - this.first) + 1

val IntRange.endExclusiveWrapped: Int get() = this.last + 1
val LongRange.endExclusiveWrapped: Long get() = this.last + 1
val IntRange.endExclusiveClamped: Int get() = if (this.last == Int.MAX_VALUE) Int.MAX_VALUE else this.last + 1
val LongRange.endExclusiveClamped: Long get() = if (this.last == Long.MAX_VALUE) Long.MAX_VALUE else this.last + 1
