@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.random

import korlibs.datastructure.lock.Lock
import kotlin.random.Random

// Copy of XorWowRandom from the Kotlin Standard library but optimizing some methods
open class FastRandom private constructor(
    private var x: Int,
    private var y: Int,
    private var z: Int,
    private var w: Int,
    private var v: Int,
    private var addend: Int
) : Random() {
    companion object : FastRandom() {
        private val _instance = FastRandom(Random.Default.nextLong())
        val Default: FastRandom get() = this
        private val lock = Lock()
        override fun nextBits(bitCount: Int): Int = lock { _instance.nextBits(bitCount) }
    }

    private constructor(seed1: Int, seed2: Int) :
        this(seed1, seed2, 0, 0, seed1.inv(), (seed1 shl 10) xor (seed2 ushr 4))

    constructor(seed: Long) : this(seed.toInt(), (seed ushr 32).toInt())
    constructor() : this(Random.Default.nextLong())

    init {
        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }
        repeat(64) { nextInt() }
    }

    override fun nextInt(): Int {
        // Equivalent to the xorxow algorithm
        // From Marsaglia, G. 2003. Xorshift RNGs. J. Statis. Soft. 8, 14, p. 5
        var t = x
        t = t xor (t ushr 2)
        x = y
        y = z
        z = w
        val v0 = v
        w = v0
        t = (t xor (t shl 1)) xor v0 xor (v0 shl 4)
        v = t
        addend += 362437
        return t + addend
    }

    override fun nextBits(bitCount: Int): Int = nextInt().takeUpperBits(bitCount)
    override fun nextInt(from: Int, until: Int): Int {
        require(until > from) { boundsErrorMessage(from, until) }
        return (nextBits(31) % (until - from)) + from
    }
    override fun nextLong(from: Long, until: Long): Long {
        require(until > from) { boundsErrorMessage(from, until) }
        return (nextLong() and 0x7FFFFFFFFFFFFFFFL % (until - from)) + from
    }
    private inline fun Int.takeUpperBits(bitCount: Int): Int = this ushr (32 - bitCount)
    private fun boundsErrorMessage(from: Any, until: Any) = "Random range is empty: [$from, $until)."
}

inline fun <T> Collection<T>.fastRandom() = this.random(FastRandom)
inline fun <T> Array<T>.fastRandom() = this.random(FastRandom)
inline fun BooleanArray.fastRandom() = this.random(FastRandom)
inline fun CharArray.fastRandom() = this.random(FastRandom)
inline fun ShortArray.fastRandom() = this.random(FastRandom)
inline fun IntArray.fastRandom() = this.random(FastRandom)
inline fun LongArray.fastRandom() = this.random(FastRandom)
inline fun FloatArray.fastRandom() = this.random(FastRandom)
inline fun DoubleArray.fastRandom() = this.random(FastRandom)
