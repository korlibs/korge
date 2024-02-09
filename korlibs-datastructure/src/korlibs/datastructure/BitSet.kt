package korlibs.datastructure

import korlibs.datastructure.internal.*
import korlibs.math.divCeil
import kotlin.collections.Collection
import kotlin.collections.Iterator
import kotlin.collections.any
import kotlin.collections.contentEquals
import kotlin.collections.contentHashCode
import kotlin.collections.map

/**
 * Fixed size [BitSet]. Similar to a [BooleanArray] but tightly packed to reduce memory usage.
 */
class BitSet(override val size: Int) : Collection<Boolean> {
    private val data = IntArray(size divCeil 32)

    private fun part(index: Int) = index ushr 5
    private fun bit(index: Int) = index and 0x1f

    operator fun get(index: Int): Boolean = ((data[part(index)] ushr (bit(index))) and 1) != 0
    operator fun set(index: Int, value: Boolean) {
        val i = part(index)
        val b = bit(index)
        if (value) {
            data[i] = data[i] or (1 shl b)
        } else {
            data[i] = data[i] and (1 shl b).inv()
        }
    }

    fun set(index: Int): Unit = set(index, true)
    fun unset(index: Int): Unit = set(index, false)

    fun clear(): Unit = data.fill(0)

    override fun contains(element: Boolean): Boolean = (0 until size).any { this[it] == element }
    override fun containsAll(elements: Collection<Boolean>): Boolean = when {
        elements.contains(true) && !this.contains(true) -> false
        elements.contains(false) && !this.contains(false) -> false
        else -> true
    }

    override fun isEmpty(): Boolean = size == 0
    override fun iterator(): Iterator<Boolean> = (0 until size).map { this[it] }.iterator()

    override fun hashCode(): Int = data.contentHashCode() + size
    override fun equals(other: Any?): Boolean = (other is BitSet) && this.size == other.size && this.data.contentEquals(other.data)
}
