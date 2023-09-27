package korlibs.datastructure

import kotlin.math.min

fun <T> genericSort(subject: T, left: Int, right: Int, ops: SortOps<T>): T =
    genericSort(subject, left, right, ops, false)

fun <T> genericSort(subject: T, left: Int, right: Int, ops: SortOps<T>, reversed: Boolean): T =
    subject.also {
        timSort(subject, left, right, ops, reversed)
    }

private fun Int.negateIf(doNegate: Boolean) = if (doNegate) -this else this

private fun <T> insertionSort(arr: T, left: Int, right: Int, ops: SortOps<T>, reversed: Boolean) {
    for (n in left + 1..right) {
        var m = n - 1

        while (m >= left) {
            if (ops.compare(arr, m, n).negateIf(reversed) <= 0) break
            m--
        }
        m++

        if (m != n) ops.shiftLeft(arr, m, n)
    }
}

private fun <T> merge(arr: T, start: Int, mid: Int, end: Int, ops: SortOps<T>, reversed: Boolean) {
    var s = start
    var m = mid
    var s2 = m + 1

    if (ops.compare(arr, m, s2).negateIf(reversed) <= 0) return

    while (s <= m && s2 <= end) {
        if (ops.compare(arr, s, s2).negateIf(reversed) <= 0) {
            s++
        } else {
            ops.shiftLeft(arr, s, s2)
            s++
            m++
            s2++
        }
    }
}

private fun <T> mergeSort(arr: T, l: Int, r: Int, ops: SortOps<T>, reversed: Boolean) {
    if (l < r) {
        val m = l + (r - l) / 2
        mergeSort(arr, l, m, ops, reversed)
        mergeSort(arr, m + 1, r, ops, reversed)
        merge(arr, l, m, r, ops, reversed)
    }
}

private fun <T> timSort(arr: T, l: Int, r: Int, ops: SortOps<T>, reversed: Boolean, RUN: Int = 32) {
    val n = r - l + 1
    for (i in 0 until n step RUN) {
        insertionSort(arr, l + i, l + min((i + RUN - 1), (n - 1)), ops, reversed)
    }
    var size = RUN
    while (size < n) {
        for (left in 0 until n step (2 * size)) {
            val rize = min(size, n - left - 1)
            val mid = left + rize - 1
            val right = min((left + 2 * rize - 1), (n - 1))
            merge(arr, l + left, l + mid, l + right, ops, reversed)
        }
        size *= 2
    }
}

abstract class SortOps<T> {
    abstract fun compare(subject: T, l: Int, r: Int): Int
    abstract fun swap(subject: T, indexL: Int, indexR: Int)
    open fun shiftLeft(subject: T, indexL: Int, indexR: Int) {
        for (n in indexR downTo indexL + 1) swap(subject, n - 1, n)
    }
    open fun reverse(subject: T, indexL: Int, indexR: Int) {
        val count = indexR - indexL + 1
        for (n in 0 until count / 2) {
            swap(subject, indexL + n, indexR - n)
        }
    }
}

object SortOpsComparable : SortOps<MutableList<Comparable<Any>>>() {
    override fun compare(subject: MutableList<Comparable<Any>>, l: Int, r: Int): Int
        = subject[l].compareTo(subject[r])

    override fun swap(subject: MutableList<Comparable<Any>>, indexL: Int, indexR: Int) {
        val tmp = subject[indexL]
        subject[indexL] = subject[indexR]
        subject[indexR] = tmp
    }
}

fun <T : Comparable<T>> MutableList<T>.genericSort(left: Int = 0, right: Int = size - 1): MutableList<T> =
    genericSort(this, left, right, SortOpsComparable as SortOps<MutableList<T>>, false)

fun <T : Comparable<T>> List<T>.genericSorted(left: Int = 0, right: Int = size - 1): List<T> =
    this.subList(left, right + 1).toMutableList().genericSort()

fun <T : Comparable<T>> List<T>.timSorted(): List<T> = this.toMutableList().also { it.timSort() }

fun <T : Comparable<T>> MutableList<T>.timSort(left: Int = 0, right: Int = size - 1): MutableList<T> {
    timSort(this, left, right, SortOpsComparable as SortOps<MutableList<T>>, false)
    //genericSort(this, left, right, SortOpsComparable as SortOps<MutableList<T>>, false)
    return this
}
