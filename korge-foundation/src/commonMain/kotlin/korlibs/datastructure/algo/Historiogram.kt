package korlibs.datastructure.algo

import korlibs.datastructure.*
import korlibs.datastructure.lock.*

/**
 * Supports getting a map determining the number of occurrences for different [Int]
 *
 * For example:
 *
 * ```kotlin
 * intArrayOf(1, 1, 5, 1, 9, 5) // 3x 1, 2x 5, 1x 9
 * ==
 * intIntMapOf((1 to 3), (5 to 2), (9 to 1))
 * ```
 **/
class Historiogram(private val out: IntIntMap = IntIntMap()) {
    private val lock = NonRecursiveLock()

    /** Adds a new [value] to this historiogram */
    fun add(value: Int) {
        lock {
            out.getOrPut(value) { 0 }
            out[value]++
        }
    }

    /** Adds a set of [values] in the optional range [start]..<[end] to this historiogram */
    fun addArray(values: IntArray, start: Int = 0, end: Int = values.size) {
        lock {
            for (n in start until end) {
                val value = values[n]
                out.getOrPut(value) { 0 }
                out[value]++
            }
        }

    }

    /** Gets a copy of the map representing each value with its frequency. */
    fun getMapCopy(): IntIntMap {
        val map = IntIntMap()
        lock { out.fastForEach { key, value -> map[key] = value } }
        return map
    }

    /** Creates a new Historiogram */
    fun clone(): Historiogram = Historiogram(getMapCopy())

    override fun toString(): String = "Historiogram(${getMapCopy().toMap()})"

    companion object {
        /**
         * From an IntArray of [values] in the optional range [start], [end].
         * Computes the Historiogram, and returns an [IntIntMap] mapping each different to the number of occurrences.
         **/
        fun values(values: IntArray, start: Int = 0, end: Int = values.size): IntIntMap {
            return Historiogram().also { it.addArray(values, start, end) }.out
        }
    }
}
