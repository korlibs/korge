package korlibs.datastructure

import korlibs.datastructure.iterators.*
import kotlin.reflect.*

inline fun IntRange.toIntList(): IntArrayList = IntArrayList((this.endInclusive - this.start).coerceAtLeast(0)).also { for (v in this.start .. this.endInclusive) it.add(v) }

inline fun Iterable<Int>.toIntList(): IntArrayList = IntArrayList().also { for (v in this) it.add(v) }
inline fun Iterable<Float>.toFloatList(): FloatArrayList = FloatArrayList().also { for (v in this) it.add(v) }
inline fun Iterable<Double>.toDoubleList(): DoubleArrayList = DoubleArrayList().also { for (v in this) it.add(v) }

fun <T> Iterator<T>.toList(): List<T> = asSequence().toList()

//  MAP
inline fun IntRange.mapInt(callback: (Int) -> Int): IntArrayList = IntArrayList((this.endInclusive - this.start).coerceAtLeast(0) / this.step + 1).also { for (v in this.start .. this.endInclusive step this.step) it.add(callback(v)) }
inline fun IntArrayList.mapInt(callback: (Int) -> Int): IntArrayList = IntArrayList(size).also { out -> this.fastForEach { out.add(callback(it)) } }
inline fun <T> Iterable<T>.mapInt(callback: (T) -> Int): IntArrayList = IntArrayList().also { for (v in this) it.add(callback(v)) }
inline fun <T> Iterable<T>.mapFloat(callback: (T) -> Float): FloatArrayList = FloatArrayList().also { for (v in this) it.add(callback(v)) }
inline fun <T> Iterable<T>.mapDouble(callback: (T) -> Double): DoubleArrayList = DoubleArrayList().also { for (v in this) it.add(callback(v)) }

inline fun BooleanArray.mapDouble(callback: (Boolean) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ByteArray.mapDouble(callback: (Byte) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ShortArray.mapDouble(callback: (Short) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun CharArray.mapDouble(callback: (Char) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun IntArray.mapDouble(callback: (Int) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun FloatArray.mapDouble(callback: (Float) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun DoubleArray.mapDouble(callback: (Double) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun <T> Array<T>.mapDouble(callback: (T) -> Double): DoubleArray = DoubleArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }

inline fun BooleanArray.mapFloat(callback: (Boolean) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ByteArray.mapFloat(callback: (Byte) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ShortArray.mapFloat(callback: (Short) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun CharArray.mapFloat(callback: (Char) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun IntArray.mapFloat(callback: (Int) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun FloatArray.mapFloat(callback: (Float) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun DoubleArray.mapFloat(callback: (Double) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun <T> Array<T>.mapFloat(callback: (T) -> Float): FloatArray = FloatArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }

inline fun BooleanArray.mapInt(callback: (Boolean) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ByteArray.mapInt(callback: (Byte) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun ShortArray.mapInt(callback: (Short) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun CharArray.mapInt(callback: (Char) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun IntArray.mapInt(callback: (Int) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun FloatArray.mapInt(callback: (Float) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun DoubleArray.mapInt(callback: (Double) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }
inline fun <T> Array<T>.mapInt(callback: (T) -> Int): IntArray = IntArray(size).also { for (n in 0 until size) it[n] = callback(this[n]) }

// FILTER
inline fun IntArrayList.filter(callback: (Int) -> Boolean): IntArrayList = IntArrayList().also { for (v in this) if (callback(v)) it.add(v) }
inline fun FloatArrayList.filter(callback: (Float) -> Boolean): FloatArrayList = FloatArrayList().also { for (v in this) if (callback(v)) it.add(v) }
inline fun DoubleArrayList.filter(callback: (Double) -> Boolean): DoubleArrayList = DoubleArrayList().also { for (v in this) if (callback(v)) it.add(v) }

private object IntArrayListSortOps : SortOps<IntArrayList>() {
    override fun compare(subject: IntArrayList, l: Int, r: Int): Int = subject.getAt(l).compareTo(subject.getAt(r))
    override fun swap(subject: IntArrayList, indexL: Int, indexR: Int) {
        val l = subject.getAt(indexL)
        val r = subject.getAt(indexR)
        subject[indexR] = l
        subject[indexL] = r
    }
}

private object DoubleArrayListSortOps : SortOps<DoubleArrayList>() {
    override fun compare(subject: DoubleArrayList, l: Int, r: Int): Int = subject.getAt(l).compareTo(subject.getAt(r))
    override fun swap(subject: DoubleArrayList, indexL: Int, indexR: Int) {
        val l = subject.getAt(indexL)
        val r = subject.getAt(indexR)
        subject[indexR] = l
        subject[indexL] = r
    }
}

private object FloatArrayListSortOps : SortOps<FloatArrayList>() {
    override fun compare(subject: FloatArrayList, l: Int, r: Int): Int = subject.getAt(l).compareTo(subject.getAt(r))
    override fun swap(subject: FloatArrayList, indexL: Int, indexR: Int) {
        val l = subject.getAt(indexL)
        val r = subject.getAt(indexR)
        subject[indexR] = l
        subject[indexL] = r
    }
}

fun IntArrayList.sort(start: Int = 0, end: Int = size, reversed: Boolean = false)
    = genericSort(this, start, end - 1, IntArrayListSortOps, reversed)

fun DoubleArrayList.sort(start: Int = 0, end: Int = size, reversed: Boolean = false)
    = genericSort(this, start, end - 1, DoubleArrayListSortOps, reversed)

fun FloatArrayList.sort(start: Int = 0, end: Int = size, reversed: Boolean = false)
    = genericSort(this, start, end - 1, FloatArrayListSortOps, reversed)

fun IntArrayList.reverse(start: Int = 0, end: Int = size)
    = IntArrayListSortOps.reverse(this, start, end - 1)

fun DoubleArrayList.reverse(start: Int = 0, end: Int = size)
    = DoubleArrayListSortOps.reverse(this, start, end - 1)

fun FloatArrayList.reverse(start: Int = 0, end: Int = size)
    = FloatArrayListSortOps.reverse(this, start, end - 1)

fun IntArrayList.toIntArrayList(): IntArrayList = IntArrayList(this)
fun DoubleArrayList.toDoubleArrayList(): DoubleArrayList = DoubleArrayList(this)

fun DoubleArrayList.toIntArrayList(): IntArrayList {
    val out = IntArrayList(this.size)
    this.fastForEach { out.add(it.toInt()) }
    return out
}

fun IntArrayList.toDoubleArrayList(): DoubleArrayList {
    val out = DoubleArrayList(this.size)
    this.fastForEach { out.add(it.toDouble()) }
    return out
}

fun <T> List<T>.rotated(offset: Int): List<T> = ArrayList<T>(this.size).also {
    for (n in 0 until this.size) it.add(this.getCyclic(n - offset))
}

fun IntArrayList.rotated(offset: Int): IntArrayList = IntArrayList(this.size).also {
    for (n in 0 until this.size) it.add(this.getCyclic(n - offset))
}

fun FloatArrayList.rotated(offset: Int): FloatArrayList = FloatArrayList(this.size).also {
    for (n in 0 until this.size) it.add(this.getCyclic(n - offset))
}

fun DoubleArrayList.rotated(offset: Int): DoubleArrayList = DoubleArrayList(this.size).also {
    for (n in 0 until this.size) it.add(this.getCyclic(n - offset))
}

fun <T> Iterable<T>.multisorted(vararg props: KProperty1<T, Comparable<*>>): List<T> {
    @Suppress("UNCHECKED_CAST")
    val props2 = props as Array<KProperty1<T, Comparable<Any>>>
    return sortedWith { a, b ->
        props2.fastForEach {
            val result = it.get(a).compareTo(it.get(b))
            if (result != 0) return@sortedWith result
        }
        return@sortedWith 0
    }
}
