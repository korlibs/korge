package korlibs.datastructure.internal

internal expect fun anyIdentityHashCode(obj: Any?): Int

@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class KdsInternalApi

@PublishedApi internal fun <T> Array<T>.fill(value: T) { for (n in indices) this[n] = value }
@PublishedApi internal fun IntArray.fill(value: Int) { for (n in indices) this[n] = value }

@PublishedApi internal inline fun <T> contentHashCode(size: Int, gen: (index: Int) -> T): Int {
    var result = 1
    for (n in 0 until size) result = 31 * result + gen(n).hashCode()
    return result
}

@PublishedApi internal inline fun hashCoder(count: Int, gen: (index: Int) -> Int): Int {
    var out = 0
    for (n in 0 until count) {
        out *= 7
        out += gen(n)
    }
    return out
}

internal fun <T> Array<T>.contentHashCode(src: Int, dst: Int): Int = hashCoder(dst - src) { this[src + it].hashCode() }
internal fun IntArray.contentHashCode(src: Int, dst: Int): Int = hashCoder(dst - src) { this[src + it].toInt() }
internal fun ShortArray.contentHashCode(src: Int, dst: Int): Int = hashCoder(dst - src) { this[src + it].toInt() }
internal fun FloatArray.contentHashCode(src: Int, dst: Int): Int = hashCoder(dst - src) { this[src + it].toRawBits() }
internal fun DoubleArray.contentHashCode(src: Int, dst: Int): Int = hashCoder(dst - src) { this[src + it].toInt() } // Do not want to use Long (.toRawBits) to prevent boxing on JS

internal fun <T> Array<out T>.contentEquals(that: Array<T>, src: Int, dst: Int): Boolean = equaler(dst - src) { this[src + it] == that[src + it] }
internal fun IntArray.contentEquals(that: IntArray, src: Int, dst: Int): Boolean = equaler(dst - src) { this[src + it] == that[src + it] }
internal fun ShortArray.contentEquals(that: ShortArray, src: Int, dst: Int): Boolean = equaler(dst - src) { this[src + it] == that[src + it] }
internal fun FloatArray.contentEquals(that: FloatArray, src: Int, dst: Int): Boolean = equaler(dst - src) { this[src + it] == that[src + it] }
internal fun DoubleArray.contentEquals(that: DoubleArray, src: Int, dst: Int): Boolean = equaler(dst - src) { this[src + it] == that[src + it] } // Do not want to use Long (.toRawBits) to prevent boxing on JS

internal inline fun equaler(count: Int, gen: (index: Int) -> Boolean): Boolean {
    for (n in 0 until count) if (!gen(n)) return false
    return true
}
