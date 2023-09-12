package korlibs.datastructure

// Note: Due to autoboxing, the get()/set() methods are implemented in the typed implementations
// without an override (meaning we don't require the interface to have get/set methods).
//
// https://discuss.kotlinlang.org/t/performance-question-related-to-boxing-and-interface-implementation/17387
interface IArray2<E> : Iterable<E> {
    companion object {
        fun checkArraySize(width: Int, height: Int, arraySize: Int) {
            check(arraySize >= width * height) { "backing array of size=$arraySize, has less elements than $width * $height" }
        }
    }
    val width: Int
    val height: Int

    val size: Int
        get() = width * height

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    // Prints the value at the given index.
    fun printAt(idx: Int)

    fun printAt(x: Int, y: Int) = printAt(index(x, y))

    fun setAt(idx: Int, value: E)

    // Returns true if the value at `idx` equals the `value`.
    fun equalsAt(idx: Int, value: E): Boolean

    fun getAt(idx: Int): E

    fun getAt(x: Int, y: Int) = getAt(index(x, y))

    fun setAt(x: Int, y: Int, value: E) {
        setAt(index(x, y), value)
    }

    fun set(rows: List<List<E>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                setAt(n++, row[x])
            }
        }
    }

    operator fun contains(v: E): Boolean {
        return this.iterator().asSequence().any { it == v }
    }

    fun getPositionsWithValue(value: E) =
        (0 until size).filter { equalsAt(it, value) }.map { Pair(it % width, it / width) }

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                printAt(x, y)
            }
            println()
        }
    }

    fun toStringList(charMap: (E) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + CharArray(width) { x -> charMap(getAt(x, y)) }.concatToString()
        }
    }

    fun asString(margin: String = "", charMap: (E) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun asString(map: Map<E, Char>, margin: String = ""): String =
        asString(margin = margin) { map[it] ?: ' ' }

    fun asString(): String = (0 until height)
        .joinToString("\n") { y ->
            (0 until width).map { x -> getAt(x, y) }.joinToString(", ")
        }
}

inline fun <E> IArray2<E>.fill(gen: (old: E) -> E) {
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            setAt(n, gen(getAt(n)))
            n++
        }
    }
}

inline fun <E> IArray2<E>.each(callback: (x: Int, y: Int, v: E) -> Unit) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            callback(x, y, getAt(x, y))
        }
    }
}

fun <E> IArray2<E>.index(x: Int, y: Int): Int {
    if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
    return y * width + x
}
