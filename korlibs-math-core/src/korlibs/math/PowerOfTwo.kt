package korlibs.math


/** Returns the next power of two of [this] */
val Int.nextPowerOfTwo: Int get() {
    var v = this
    v--
    v = v or (v shr 1)
    v = v or (v shr 2)
    v = v or (v shr 4)
    v = v or (v shr 8)
    v = v or (v shr 16)
    v++
    return v
}
/** Checks if [this] value is power of two */
val Int.isPowerOfTwo: Boolean get() = this.nextPowerOfTwo == this

/** Returns the previous power of two of [this] */
val Int.prevPowerOfTwo: Int get() = if (isPowerOfTwo) this else (nextPowerOfTwo ushr 1)


