package korlibs.number

internal inline class BFloat @PublishedApi internal constructor(val rawBits: Int) {
    companion object {
        const val BITS = 21
        const val MASK = (1 shl BITS) - 1

        inline fun fromBits(bits: Int): BFloat = BFloat(bits)
        inline fun packLong(a: BFloat, b: BFloat, c: BFloat): Long =
            (a.rawBits.toLong() shl 0) or (b.rawBits.toLong() shl 21) or (c.rawBits.toLong() shl 42)
        inline fun unpackLong(long: Long, index: Int): BFloat =
            fromBits(((long ushr (21 * index)) and MASK.toLong()).toInt())
    }

    constructor(v: Float) : this(v.toRawBits() ushr 12)
    val float: Float get() = Float.fromBits(rawBits shl 12)
    fun toFloat(): Float = float
}
