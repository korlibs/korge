/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.ObjectSet.*

import java.util.Arrays
import java.util.NoSuchElementException

/** An unordered set where the items are unboxed ints. No allocation is done except when growing the table size.
 *
 *
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 *
 *
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 *
 *
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see [Malte
 * Skarupke's blog post](https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/)). Linear probing continues to work even when all hashCodes collide, just more slowly.
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
class JIntSet
/** Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
 * growing the backing table.
 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
 */
@JvmOverloads constructor(initialCapacity: Int = 51, private val loadFactor: Float = 0.8f) {
    @JvmField
    var size: Int = 0

    internal var keyTable: IntArray
    internal var hasZeroValue: Boolean = false
    private var threshold: Int = 0

    /** Used by [.place] to bit shift the upper bits of a `long` into a usable range (&gt;= 0 and &lt;=
     * [.mask]). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
     * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
     * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
     * shifts.
     *
     *
     * [.mask] can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
     * [.place] is overridden.  */
    protected var shift: Int = 0

    /** A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
     * minus 1. If [.place] is overriden, this can be used instead of [.shift] to isolate usable bits of a
     * hash.  */
    protected var mask: Int = 0

    private var iterator1: IntSetIterator? = null
    private var iterator2: IntSetIterator? = null

    /** Returns true if the set is empty.  */
    val isEmpty: Boolean
        get() = size == 0

    init {
        require(!(loadFactor <= 0f || loadFactor >= 1f)) { "loadFactor must be > 0 and < 1: $loadFactor" }

        val tableSize = ObjectSet.tableSize(initialCapacity, loadFactor)
        threshold = (tableSize * loadFactor).toInt()
        mask = tableSize - 1
        shift = java.lang.Long.numberOfLeadingZeros(mask.toLong())

        keyTable = IntArray(tableSize)
    }

    /** Creates a new set identical to the specified set.  */
    constructor(set: JIntSet) : this((set.keyTable.size * set.loadFactor).toInt(), set.loadFactor) {
        System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.size)
        size = set.size
        hasZeroValue = set.hasZeroValue
    }

    /** Returns an index >= 0 and <= [.mask] for the specified `item`.
     *
     *
     * The default implementation uses Fibonacci hashing on the item's [Object.hashCode]: the hashcode is multiplied by a
     * long constant (2 to the 64th, divided by the golden ratio) then the uppermost bits are shifted into the lowest positions to
     * obtain an index in the desired range. Multiplication by a long may be slower than int (eg on GWT) but greatly improves
     * rehashing, allowing even very poor hashcodes, such as those that only differ in their upper bits, to be used without high
     * collision rates. Fibonacci hashing has increased collision rates when all or most hashcodes are multiples of larger
     * Fibonacci numbers (see [Malte
 * Skarupke's blog post](https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/)).
     *
     *
     * This method can be overriden to customizing hashing. This may be useful eg in the unlikely event that most hashcodes are
     * Fibonacci numbers, if keys provide poor or incorrect hashcodes, or to simplify hashing if keys provide high quality
     * hashcodes and don't need Fibonacci hashing: `return item.hashCode() & mask;`  */
    protected fun place(item: Int): Int {
        return (item * -0x61c8864680b583ebL).ushr(shift).toInt()
    }

    /** Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * pacakge to compare for equality differently than [Object.equals].  */
    private fun locateKey(key: Int): Int {
        val keyTable = this.keyTable
        var i = place(key)
        while (true) {
            val other = keyTable[i]
            if (other == 0) return -(i + 1) // Empty space is available.
            if (other == key) return i // Same key was found.
            i = i + 1 and mask
        }
    }

    /** Returns true if the key was not already in the set.  */
    fun add(key: Int): Boolean {
        if (key == 0) {
            if (hasZeroValue) return false
            hasZeroValue = true
            size++
            return true
        }
        var i = locateKey(key)
        if (i >= 0) return false // Existing key was found.
        i = -(i + 1) // Empty space was found.
        keyTable[i] = key
        if (++size >= threshold) resize(keyTable.size shl 1)
        return true
    }

    fun addAll(array: JIntArray) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: JIntArray, offset: Int, length: Int) {
        require(offset + length <= array.size) { "offset + length must be <= size: " + offset + " + " + length + " <= " + array.size }
        addAll(array.items, offset, length)
    }

    fun addAll(vararg array: Int) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: IntArray, offset: Int, length: Int) {
        ensureCapacity(length)
        var i = offset
        val n = i + length
        while (i < n) {
            add(array[i])
            i++
        }
    }

    fun addAll(set: JIntSet) {
        ensureCapacity(set.size)
        if (set.hasZeroValue) add(0)
        val keyTable = set.keyTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            val key = keyTable[i]
            if (key != 0) add(key)
            i++
        }
    }

    /** Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.  */
    private fun addResize(key: Int) {
        val keyTable = this.keyTable
        var i = place(key)
        while (true) {
            if (keyTable[i] == 0) {
                keyTable[i] = key
                return
            }
            i = i + 1 and mask
        }
    }

    /** Returns true if the key was removed.  */
    fun remove(key: Int): Boolean {
        var key = key
        if (key == 0) {
            if (!hasZeroValue) return false
            hasZeroValue = false
            size--
            return true
        }

        var i = locateKey(key)
        if (i < 0) return false
        val keyTable = this.keyTable
        val mask = this.mask
        var next = i + 1 and mask
        while ((keyTable[next].also { key = it }) != 0) {
            val placement = place(key)
            if (next - placement and mask > i - placement and mask) {
                keyTable[i] = key
                i = next
            }
            next = next + 1 and mask
        }
        keyTable[i] = 0
        size--
        return true
    }

    /** Returns true if the set has one or more items.  */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.  */
    fun shrink(maximumCapacity: Int) {
        require(maximumCapacity >= 0) { "maximumCapacity must be >= 0: $maximumCapacity" }
        val tableSize = ObjectSet.tableSize(maximumCapacity, loadFactor)
        if (keyTable.size > tableSize) resize(tableSize)
    }

    /** Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.  */
    fun clear(maximumCapacity: Int) {
        val tableSize = ObjectSet.tableSize(maximumCapacity, loadFactor)
        if (keyTable.size <= tableSize) {
            clear()
            return
        }
        size = 0
        hasZeroValue = false
        resize(tableSize)
    }

    fun clear() {
        if (size == 0) return
        size = 0
        Arrays.fill(keyTable, 0)
        hasZeroValue = false
    }

    operator fun contains(key: Int): Boolean {
        return if (key == 0) hasZeroValue else locateKey(key) >= 0
    }

    fun first(): Int {
        if (hasZeroValue) return 0
        val keyTable = this.keyTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            if (keyTable[i] != 0) return keyTable[i]
            i++
        }
        throw IllegalStateException("IntSet is empty.")
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.  */
    fun ensureCapacity(additionalCapacity: Int) {
        val tableSize = ObjectSet.tableSize(size + additionalCapacity, loadFactor)
        if (keyTable.size < tableSize) resize(tableSize)
    }

    private fun resize(newSize: Int) {
        val oldCapacity = keyTable.size
        threshold = (newSize * loadFactor).toInt()
        mask = newSize - 1
        shift = java.lang.Long.numberOfLeadingZeros(mask.toLong())

        val oldKeyTable = keyTable

        keyTable = IntArray(newSize)

        if (size > 0) {
            for (i in 0 until oldCapacity) {
                val key = oldKeyTable[i]
                if (key != 0) addResize(key)
            }
        }
    }

    override fun hashCode(): Int {
        var h = size
        val keyTable = this.keyTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            val key = keyTable[i]
            if (key != 0) h += key
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is JIntSet) return false
        val other = obj as JIntSet?
        if (other!!.size != size) return false
        if (other.hasZeroValue != hasZeroValue) return false
        val keyTable = this.keyTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            if (keyTable[i] != 0 && !other.contains(keyTable[i])) return false
            i++
        }
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val buffer = java.lang.StringBuilder(32)
        buffer.append('[')
        val keyTable = this.keyTable
        var i = keyTable.size
        if (hasZeroValue)
            buffer.append("0")
        else {
            while (i-- > 0) {
                val key = keyTable[i]
                if (key == 0) continue
                buffer.append(key)
                break
            }
        }
        while (i-- > 0) {
            val key = keyTable[i]
            if (key == 0) continue
            buffer.append(", ")
            buffer.append(key)
        }
        buffer.append(']')
        return buffer.toString()
    }

    /** Returns an iterator for the keys in the set. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [IntSetIterator] constructor for nested or multithreaded iteration.  */
    operator fun iterator(): IntSetIterator {
        //if (Collections.allocateIterators) return IntSetIterator(this)
        if (iterator1 == null) {
            iterator1 = IntSetIterator(this)
            iterator2 = IntSetIterator(this)
        }
        if (!iterator1!!.valid) {
            iterator1!!.reset()
            iterator1!!.valid = true
            iterator2!!.valid = false
            return iterator1!!
        }
        iterator2!!.reset()
        iterator2!!.valid = true
        iterator1!!.valid = false
        return iterator2!!
    }

    class IntSetIterator(internal val set: JIntSet) {

        var hasNext: Boolean = false
        internal var nextIndex: Int = 0
        internal var currentIndex: Int = 0
        internal var valid = true

        init {
            reset()
        }

        fun reset() {
            currentIndex = INDEX_ILLEGAL
            nextIndex = INDEX_ZERO
            if (set.hasZeroValue)
                hasNext = true
            else
                findNextIndex()
        }

        internal fun findNextIndex() {
            val keyTable = set.keyTable
            val n = keyTable.size
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != 0) {
                    hasNext = true
                    return
                }
            }
            hasNext = false
        }

        fun remove() {
            var i = currentIndex
            if (i == INDEX_ZERO && set.hasZeroValue) {
                set.hasZeroValue = false
            } else check(i >= 0) { "next must be called before remove." }
                val keyTable = set.keyTable
                val mask = set.mask
                var next = i + 1 and mask
                var key: Int
                while ((keyTable[next].also { key = it }) != 0) {
                    val placement = set.place(key)
                    if (next - placement and mask > i - placement and mask) {
                        keyTable[i] = key
                        i = next
                    }
                    next = next + 1 and mask
                }
                keyTable[i] = 0
                if (i != currentIndex) --nextIndex
            currentIndex = INDEX_ILLEGAL
            set.size--
        }

        operator fun next(): Int {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) throw GdxRuntimeException("#iterator() cannot be used nested.")
            val key = if (nextIndex == INDEX_ZERO) 0 else set.keyTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        /** Returns a new array containing the remaining keys.  */
        fun toArray(): JIntArray {
            val array = JIntArray(true, set.size)
            while (hasNext)
                array.add(next())
            return array
        }

        companion object {
            private val INDEX_ILLEGAL = -2
            private val INDEX_ZERO = -1
        }
    }

    companion object {

        fun with(vararg array: Int): JIntSet {
            val set = JIntSet()
            set.addAll(*array)
            return set
        }
    }
}
/** Creates a new set with an initial capacity of 51 and a load factor of 0.8.  */
/** Creates a new set with a load factor of 0.8.
 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
 */
