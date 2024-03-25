package korlibs.math

////////////////////
////////////////////

/** Converts this [Boolean] into integer: 1 for true, 0 for false */
inline fun Boolean.toInt(): Int = if (this) 1 else 0
inline fun Boolean.toByte(): Byte = if (this) 1 else 0
inline fun Byte.toBoolean(): Boolean = this.toInt() != 0
