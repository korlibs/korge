package korlibs.math

////////////////////
////////////////////

/** Checks if [this] is odd (not multiple of two) */
val Int.isOdd: Boolean get() = (this % 2) == 1
/** Checks if [this] is even (multiple of two) */
val Int.isEven: Boolean get() = (this % 2) == 0
