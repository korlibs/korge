@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.comparator

object ComparatorComparable : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = a.compareTo(b)
}

object ReverseComparatorComparable : Comparator<Comparable<Any>> {
    override fun compare(a: Comparable<Any>, b: Comparable<Any>): Int = -a.compareTo(b)
}

fun <T : Comparable<T>> ComparatorComparable() = ComparatorComparable as Comparator<T>
fun <T : Comparable<T>> ReverseComparatorComparable() = ReverseComparatorComparable as Comparator<T>
