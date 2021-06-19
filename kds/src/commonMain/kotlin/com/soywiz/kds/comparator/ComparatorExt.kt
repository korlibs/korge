package com.soywiz.kds.comparator

object ComparatorComparable : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = a.compareTo(b)
}

object ReverseComparatorComparable : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = -a.compareTo(b)
}

inline fun <T : Comparable<T>> ComparatorComparable() = ComparatorComparable as Comparator<T>
inline fun <T : Comparable<T>> ReverseComparatorComparable() = ReverseComparatorComparable as Comparator<T>
