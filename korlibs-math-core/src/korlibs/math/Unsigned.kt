package korlibs.math

////////////////////
////////////////////

/** Returns an [Int] representing this [Byte] as if it was unsigned 0x00..0xFF */
inline val Byte.unsigned: Int get() = this.toInt() and 0xFF

/** Returns an [Int] representing this [Short] as if it was unsigned 0x0000..0xFFFF */
inline val Short.unsigned: Int get() = this.toInt() and 0xFFFF

/** Returns a [Long] representing this [Int] as if it was unsigned 0x00000000L..0xFFFFFFFFL */
inline val Int.unsigned: Long get() = this.toLong() and 0xFFFFFFFFL
