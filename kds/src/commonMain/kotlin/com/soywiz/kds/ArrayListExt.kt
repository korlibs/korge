package com.soywiz.kds

import com.soywiz.kds.iterators.*

inline fun IntRange.toIntList(): IntArrayList = IntArrayList(this.endInclusive - this.start).also { for (v in this.start .. this.endInclusive) it.add(v) }

inline fun Iterable<Int>.toIntList(): IntArrayList = IntArrayList().also { for (v in this) it.add(v) }
inline fun Iterable<Float>.toFloatList(): FloatArrayList = FloatArrayList().also { for (v in this) it.add(v) }
inline fun Iterable<Double>.toDoubleList(): DoubleArrayList = DoubleArrayList().also { for (v in this) it.add(v) }

//  MAP
inline fun <T> Iterable<T>.mapInt(callback: (T) -> Int): IntArrayList = IntArrayList().also { for (v in this) it.add(callback(v)) }
inline fun <T> Iterable<T>.mapFloat(callback: (T) -> Float): FloatArrayList = FloatArrayList().also { for (v in this) it.add(callback(v)) }
inline fun <T> Iterable<T>.mapDouble(callback: (T) -> Double): DoubleArrayList = DoubleArrayList().also { for (v in this) it.add(callback(v)) }

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

fun DoubleArrayList.toIntArrayList(): IntArrayList {
    val out = IntArrayList(this.size)
    this.fastForEach { out.add(it.toInt()) }
    return out
}

fun IntArrayList.toIntArrayList(): DoubleArrayList {
    val out = DoubleArrayList(this.size)
    this.fastForEach { out.add(it.toDouble()) }
    return out
}

fun <T> MutableList<T>.swap(lIndex: Int, rIndex: Int) {
    val temp = this[lIndex]
    this[lIndex] = this[rIndex]
    this[rIndex] = temp
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
