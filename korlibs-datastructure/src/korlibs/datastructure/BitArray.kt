package korlibs.datastructure

import korlibs.datastructure.internal.math.Math.divCeil
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Equivalent to [BooleanArray] but tightly packed to consume less memory,
 * at the cost of being ~3-4x slower.
 */
//class BitArray private constructor(val data: IntArray, size: Int) : AbstractList<Boolean>(), Collection<Boolean> {
class BitArray private constructor(val data: ByteArray, size: Int) : AbstractList<Boolean>(), Collection<Boolean> {
//class BitArray private constructor(val data: IntArray, size: Int) : AbstractCollection<Boolean>(), Collection<Boolean> {
    override val size: Int = size

    override fun iterator(): Iterator<Boolean> {
        var pos = 0
        return Iterator({ pos < size }, { this[pos++] })
    }

    //constructor(size: Int) : this(IntArray(size divCeil BITS_PER_WORD), size)
    constructor(size: Int) : this(ByteArray(size divCeil BITS_PER_WORD), size)

    private fun checkIndexBounds(index: Int) {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException()
        //if (index !in indices) throw IndexOutOfBoundsException()
    }

    private inline fun wordIndex(index: Int): Int = index ushr BITS_PER_WORD_SHIFT
    private inline fun indexInWord(index: Int): Int = index and BITS_MASK

    operator fun set(index: Int, value: Boolean) {
        checkIndexBounds(index)
        val wordIndex = wordIndex(index)
        val indexInWord = indexInWord(index)
        if (value) {
            data[wordIndex] = (data[wordIndex] or ((1 shl indexInWord).toByte()))
        } else {
            data[wordIndex] = (data[wordIndex] and (1 shl indexInWord).inv().toByte())
        }
    }

    override operator fun get(index: Int): Boolean {
        checkIndexBounds(index)
        return ((data[wordIndex(index)].toInt() ushr (indexInWord(index))) and 1) != 0
    }

    companion object {
        private const val BITS_PER_WORD_SHIFT = 3
        private const val BITS_PER_WORD = 1 shl BITS_PER_WORD_SHIFT
        private const val BITS_MASK = BITS_PER_WORD - 1

        inline operator fun invoke(size: Int, init: (Int) -> Boolean): BitArray = BitArray(size).also {
            for (n in 0 until size) it[n] = init(n)
        }
    }
}

fun bitArrayOf(vararg values: Boolean): BitArray = BitArray(values.size).also { for (n in values.indices) it[n] = values[n] }
