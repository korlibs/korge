package korlibs.datastructure

import korlibs.datastructure.internal.math.Math.umod
import kotlin.math.max
import kotlin.math.min

inline fun count(cond: (index: Int) -> Boolean): Int {
    var counter = 0
    while (cond(counter)) counter++
    return counter
}
inline fun <reified T> mapWhile(cond: (index: Int) -> Boolean, gen: (Int) -> T): List<T> = arrayListOf<T>().apply { while (cond(this.size)) this += gen(this.size) }
inline fun <reified T> mapWhileArray(cond: (index: Int) -> Boolean, gen: (Int) -> T): Array<T> = mapWhile(cond, gen).toTypedArray()
inline fun mapWhileInt(cond: (index: Int) -> Boolean, gen: (Int) -> Int): IntArray = IntArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toIntArray()
inline fun mapWhileFloat(cond: (index: Int) -> Boolean, gen: (Int) -> Float): FloatArray = FloatArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toFloatArray()
inline fun mapWhileDouble(cond: (index: Int) -> Boolean, gen: (Int) -> Double): DoubleArray = DoubleArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toDoubleArray()

inline fun <reified T> mapWhileNotNull(gen: (Int) -> T?): List<T> = arrayListOf<T>().apply {
    while (true) {
        this += gen(this.size) ?: break
    }
}

inline fun <reified T> mapWhileCheck(check: (T) -> Boolean, gen: (Int) -> T): List<T> = arrayListOf<T>().apply {
    while (true) {
        val res = gen(this.size)
        if (!check(res)) break
        this += res
    }
}

fun <T> List<T>.getCyclic(index: Int): T = this[index umod this.size]
fun <T> List<T>.getCyclicOrNull(index: Int): T? = if (this.isEmpty()) null else this.getOrNull(index umod this.size)
fun <T> Array<T>.getCyclic(index: Int) = this[index umod this.size]
fun IntArray.getCyclic(index: Int) = this[index umod this.size]
fun FloatArray.getCyclic(index: Int) = this[index umod this.size]
fun DoubleArray.getCyclic(index: Int) = this[index umod this.size]
fun IntArrayList.getCyclic(index: Int) = this.getAt(index umod this.size)
fun FloatArrayList.getCyclic(index: Int) = this.getAt(index umod this.size)
fun DoubleArrayList.getCyclic(index: Int) = this.getAt(index umod this.size)

fun <T> Array2<T>.getCyclic(x: Int, y: Int) = this[x umod this.width, y umod this.height]
fun IntArray2.getCyclic(x: Int, y: Int) = this[x umod this.width, y umod this.height]
fun FloatArray2.getCyclic(x: Int, y: Int) = this[x umod this.width, y umod this.height]
fun DoubleArray2.getCyclic(x: Int, y: Int) = this[x umod this.width, y umod this.height]

fun <T : Comparable<T>> comparator(): Comparator<T> = kotlin.Comparator { a, b -> a.compareTo(b) }

fun <K, V> linkedHashMapOf(vararg pairs: Pair<K, V>): LinkedHashMap<K, V> = LinkedHashMap<K, V>().also { for ((key, value) in pairs) it[key] = value }
fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> = LinkedHashMap<K, V>().also { for ((key, value) in this) it[key] = value }

fun <K, V> Map<K, V>.flip(): Map<V, K> = this.map { Pair(it.value, it.key) }.toMap()
fun <T> List<T>.countMap(): Map<T, Int> = LinkedHashMap<T, Int>().also { for (key in this) it.incr(key, +1) }

fun <K> MutableMap<K, Int>.incr(key: K, delta: Int = +1): Int {
    val next = this.getOrPut(key) { 0 } + delta
    this[key] = next
    return next
}

/**
 * Returns the index of an item or a negative number in the case the item is not found.
 * The negative index represents the nearest position after negating + 1.
 */
fun IntArray.binarySearch(v: Int, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this[it].compareTo(v) })
fun FloatArray.binarySearch(v: Float, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this[it].compareTo(v) })
fun DoubleArray.binarySearch(v: Double, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this[it].compareTo(v) })
fun IntArrayList.binarySearch(v: Int, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun FloatArrayList.binarySearch(v: Float, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun DoubleArrayList.binarySearch(v: Double, fromIndex: Int = 0, toIndex: Int = size): BSearchResult = (genericBinarySearchResult(fromIndex, toIndex) { this.getAt(it).compareTo(v) })

fun IntArray.binarySearchLeft(v: Int, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this[it].compareTo(v) })
fun FloatArray.binarySearchLeft(v: Float, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this[it].compareTo(v) })
fun DoubleArray.binarySearchLeft(v: Double, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this[it].compareTo(v) })
fun IntArrayList.binarySearchLeft(v: Int, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun FloatArrayList.binarySearchLeft(v: Float, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun DoubleArrayList.binarySearchLeft(v: Double, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchLeft(fromIndex, toIndex) { this.getAt(it).compareTo(v) })

fun IntArray.binarySearchRight(v: Int, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this[it].compareTo(v) })
fun FloatArray.binarySearchRight(v: Float, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this[it].compareTo(v) })
fun DoubleArray.binarySearchRight(v: Double, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this[it].compareTo(v) })
fun IntArrayList.binarySearchRight(v: Int, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun FloatArrayList.binarySearchRight(v: Float, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this.getAt(it).compareTo(v) })
fun DoubleArrayList.binarySearchRight(v: Double, fromIndex: Int = 0, toIndex: Int = size) = (genericBinarySearchRight(fromIndex, toIndex) { this.getAt(it).compareTo(v) })

inline fun genericBinarySearchResult(fromIndex: Int, toIndex: Int, check: (value: Int) -> Int): BSearchResult = BSearchResult(genericBinarySearch(fromIndex, toIndex, { _, _, low, _ -> -low - 1 }, check))

inline fun genericBinarySearchLeft(fromIndex: Int, toIndex: Int, check: (value: Int) -> Int): Int =
    genericBinarySearch(fromIndex, toIndex, invalid = { from, to, low, high -> min(low, high).coerceIn(from, to - 1) }, check = check)
inline fun genericBinarySearchRight(fromIndex: Int, toIndex: Int, check: (value: Int) -> Int): Int =
    genericBinarySearch(fromIndex, toIndex, invalid = { from, to, low, high -> max(low, high).coerceIn(from, to - 1) }, check = check)

inline fun genericBinarySearch(
    fromIndex: Int,
    toIndex: Int,
    invalid: (from: Int, to: Int, low: Int, high: Int) -> Int = { from, to, low, high -> -low - 1 },
    check: (value: Int) -> Int
): Int {
    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high) / 2
        val mval = check(mid)

        when {
            mval < 0 -> low = mid + 1
            mval > 0 -> high = mid - 1
            else -> return mid
        }
    }
    return invalid(fromIndex, toIndex, low, high)
}

inline class BSearchResult(val raw: Int) {
    val found: Boolean get() = raw >= 0
    val index: Int get() = if (found) raw else -1
    val nearIndex: Int get() = if (found) raw else -raw - 1
}
