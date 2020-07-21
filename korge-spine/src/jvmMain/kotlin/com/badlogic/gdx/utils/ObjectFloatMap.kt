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

import com.badlogic.gdx.utils.ObjectSet.Companion.tableSize

/** An unordered map where the keys are objects and the values are unboxed floats. Null keys are not allowed. No allocation is
 * done except when growing the table size.
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
class ObjectFloatMap<K>
/** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
 * growing the backing table.
 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
 */
@JvmOverloads constructor(initialCapacity: Int = 51, internal var loadFactor: Float = 0.8f) : Iterable<ObjectFloatMap.Entry<K>> {
    var size: Int = 0

    internal var keyTable: Array<K>
    internal var valueTable: FloatArray
    internal var threshold: Int = 0

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

    internal var entries1: Entries<*>? = null
    lateinit internal var entries2: Entries<*>
    internal var values1: Values? = null
    lateinit internal var values2: Values
    internal var keys1: Keys<*>? = null
    lateinit internal var keys2: Keys<*>

    /** Returns true if the map is empty.  */
    val isEmpty: Boolean
        get() = size == 0

    init {
        require(!(loadFactor <= 0f || loadFactor >= 1f)) { "loadFactor must be > 0 and < 1: $loadFactor" }

        val tableSize = ObjectSet.tableSize(initialCapacity, loadFactor)
        threshold = (tableSize * loadFactor).toInt()
        mask = tableSize - 1
        shift = mask.toLong().countLeadingZeroBits()

        keyTable = arrayOfNulls<Any>(tableSize) as Array<K>
        valueTable = FloatArray(tableSize)
    }

    /** Creates a new map identical to the specified map.  */
    constructor(map: ObjectFloatMap<out K>) : this(Math.floor((map.keyTable.size * map.loadFactor).toDouble()).toInt(), map.loadFactor) {
        System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.size)
        System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.size)
        size = map.size
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
    protected fun place(item: K): Int {
        return (item.hashCode() * -0x61c8864680b583ebL).ushr(shift).toInt()
    }

    /** Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * pacakge to compare for equality differently than [Object.equals].  */
    internal fun locateKey(key: K?): Int {
        requireNotNull(key) { "key cannot be null." }
        val keyTable = this.keyTable
        var i = place(key)
        while (true) {
            val other = keyTable[i] ?: return -(i + 1)
// Empty space is available.
            if (other == key) return i // Same key was found.
            i = i + 1 and mask
        }
    }

    /** You can use [.get] with a defaultValue of [Float.NaN] if you want to tell with certainty that a
     * key is not present; comparing with NaN is tricky but [Float.isNaN] makes it easy. If isNaN returns true, you
     * can generally act like another Map had returned null in the same situation (meaning the value is unusable). This works
     * because this class will never insert a NaN value into the map unless one is explicitly inserted, and since NaN acts so
     * strangely in its everyday usage, virtually all code will not place NaN in a map.  */
    fun put(key: K, value: Float) {
        var i = locateKey(key)
        if (i >= 0) { // Existing key was found.
            valueTable[i] = value
            return
        }
        i = -(i + 1) // Empty space was found.
        keyTable[i] = key
        valueTable[i] = value
        if (++size >= threshold) resize(keyTable.size shl 1)
    }

    fun putAll(map: ObjectFloatMap<out K>) {
        ensureCapacity(map.size)
        val keyTable = map.keyTable
        val valueTable = map.valueTable
        var key: K?
        var i = 0
        val n = keyTable.size
        while (i < n) {
            key = keyTable[i]
            if (key != null) put(key, valueTable[i])
            i++
        }
    }

    /** Skips checks for existing keys, doesn't increment size.  */
    private fun putResize(key: K, value: Float) {
        val keyTable = this.keyTable
        var i = place(key)
        while (true) {
            if (keyTable[i] == null) {
                keyTable[i] = key
                valueTable[i] = value
                return
            }
            i = i + 1 and mask
        }
    }

    /** Returns the value for the specified key, or the default value if the key is not in the map.  */
    operator fun get(key: K, defaultValue: Float): Float {
        val i = locateKey(key)
        return if (i < 0) defaultValue else valueTable[i]
    }

    /** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
     * put into the map and defaultValue is returned.  */
    fun getAndIncrement(key: K, defaultValue: Float, increment: Float): Float {
        var i = locateKey(key)
        if (i >= 0) { // Existing key was found.
            val oldValue = valueTable[i]
            valueTable[i] += increment
            return oldValue
        }
        i = -(i + 1) // Empty space was found.
        keyTable[i] = key
        valueTable[i] = defaultValue + increment
        if (++size >= threshold) resize(keyTable.size shl 1)
        return defaultValue
    }

    fun remove(key: K, defaultValue: Float): Float {
        var key = key
        var i = locateKey(key)
        if (i < 0) return defaultValue
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        val oldValue = valueTable[i]
        val mask = this.mask
        var next = i + 1 and mask
        while ((keyTable[next].also { key = it }) != null) {
            val placement = place(key)
            if (next - placement and mask > i - placement and mask) {
                keyTable[i] = key
                valueTable[i] = valueTable[next]
                i = next
            }
            next = next + 1 and mask
        }
        (keyTable as Array<Any?>)[i] = null
        size--
        return oldValue
    }

    /** Returns true if the map has one or more items.  */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.  */
    fun shrink(maximumCapacity: Int) {
        require(maximumCapacity >= 0) { "maximumCapacity must be >= 0: $maximumCapacity" }
        val tableSize = tableSize(maximumCapacity, loadFactor)
        if (keyTable.size > tableSize) resize(tableSize)
    }

    /** Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.  */
    fun clear(maximumCapacity: Int) {
        val tableSize = tableSize(maximumCapacity, loadFactor)
        if (keyTable.size <= tableSize) {
            clear()
            return
        }
        size = 0
        resize(tableSize)
    }

    fun clear() {
        if (size == 0) return
        size = 0
        (keyTable as Array<Any?>).fill(null)
    }

    /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.  */
    fun containsValue(value: Float): Boolean {
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        for (i in valueTable.indices.reversed())
            if (keyTable[i] != null && valueTable[i] == value) return true
        return false
    }

    fun containsKey(key: K): Boolean {
        return locateKey(key) >= 0
    }

    /** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.  */
    fun findKey(value: Float): K? {
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        for (i in valueTable.indices.reversed()) {
            val key = keyTable[i]
            if (key != null && valueTable[i] == value) return key
        }
        return null
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.  */
    fun ensureCapacity(additionalCapacity: Int) {
        val tableSize = ObjectSet.tableSize(size + additionalCapacity, loadFactor)
        if (keyTable.size < tableSize) resize(tableSize)
    }

    internal fun resize(newSize: Int) {
        val oldCapacity = keyTable.size
        threshold = (newSize * loadFactor).toInt()
        mask = newSize - 1
        shift = mask.toLong().countLeadingZeroBits()

        val oldKeyTable = keyTable
        val oldValueTable = valueTable

        keyTable = arrayOfNulls<Any>(newSize) as Array<K>
        valueTable = FloatArray(newSize)

        if (size > 0) {
            for (i in 0 until oldCapacity) {
                val key = oldKeyTable[i]
                if (key != null) putResize(key, oldValueTable[i])
            }
        }
    }

    override fun hashCode(): Int {
        var h = size
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            val key = keyTable[i]
            if (key != null) h += key.hashCode() + valueTable[i].toRawBits()
            i++
        }
        return h
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) return true
        if (obj !is ObjectFloatMap<*>) return false
        val other = obj as ObjectFloatMap<K>?
        if (other!!.size != size) return false
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        var i = 0
        val n = keyTable.size
        while (i < n) {
            val key = keyTable[i]
            if (key != null) {
                val otherValue = other.get(key, 0f)
                if (otherValue == 0f && !other.containsKey(key)) return false
                if (otherValue != valueTable[i]) return false
            }
            i++
        }
        return true
    }

    fun toString(separator: String): String {
        return toString(separator, false)
    }

    override fun toString(): String {
        return toString(", ", true)
    }

    private fun toString(separator: String, braces: Boolean): String {
        if (size == 0) return if (braces) "{}" else ""
        val buffer = StringBuilder(32)
        if (braces) buffer.append('{')
        val keyTable = this.keyTable
        val valueTable = this.valueTable
        var i = keyTable.size
        while (i-- > 0) {
            val key = keyTable[i] ?: continue
            buffer.append(key)
            buffer.append('=')
            buffer.append(valueTable[i])
            break
        }
        while (i-- > 0) {
            val key = keyTable[i] ?: continue
            buffer.append(separator)
            buffer.append(key)
            buffer.append('=')
            buffer.append(valueTable[i])
        }
        if (braces) buffer.append('}')
        return buffer.toString()
    }

    override fun iterator(): Entries<K> {
        return entries()
    }

    /** Returns an iterator for the entries in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Entries] constructor for nested or multithreaded iteration.  */
    fun entries(): Entries<K> {
        //if (Collections.allocateIterators) return Entries(this)
        if (entries1 == null) {
            entries1 = Entries(this)
            entries2 = Entries(this)
        }
        if (!entries1!!.valid) {
            entries1!!.reset()
            entries1!!.valid = true
            entries2.valid = false
            return entries1 as Entries<K>
        }
        entries2.reset()
        entries2.valid = true
        entries1!!.valid = false
        return entries2 as Entries<K>
    }

    /** Returns an iterator for the values in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Values] constructor for nested or multithreaded iteration.  */
    fun values(): Values {
        //if (Collections.allocateIterators) return Values(this)
        if (values1 == null) {
            values1 = Values(this)
            values2 = Values(this)
        }
        if (!values1!!.valid) {
            values1!!.reset()
            values1!!.valid = true
            values2.valid = false
            return values1 as Values
        }
        values2.reset()
        values2.valid = true
        values1!!.valid = false
        return values2
    }

    /** Returns an iterator for the keys in the map. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [Keys] constructor for nested or multithreaded iteration.  */
    fun keys(): Keys<K> {
        //if (Collections.allocateIterators) return Keys(this)
        if (keys1 == null) {
            keys1 = Keys(this)
            keys2 = Keys(this)
        }
        if (!keys1!!.valid) {
            keys1!!.reset()
            keys1!!.valid = true
            keys2.valid = false
            return keys1 as Keys<K>
        }
        keys2.reset()
        keys2.valid = true
        keys1!!.valid = false
        return keys2 as Keys<K>
    }

    class Entry<K> {
        var key: K? = null
        var value: Float = 0.toFloat()

        override fun toString(): String {
            return key.toString() + "=" + value
        }
    }

    open class MapIterator<K>(@PublishedApi internal val map: ObjectFloatMap<K>) {
        var hasNext: Boolean = false
        internal var nextIndex: Int = 0
        internal var currentIndex: Int = 0
        internal var valid = true

        init {
            reset()
        }

        fun reset() {
            currentIndex = -1
            nextIndex = -1
            findNextIndex()
        }

        internal fun findNextIndex() {
            val keyTable = map.keyTable
            val n = keyTable.size
            while (++nextIndex < n) {
                if (keyTable[nextIndex] != null) {
                    hasNext = true
                    return
                }
            }
            hasNext = false
        }

        fun remove() {
            var i = currentIndex
            check(i >= 0) { "next must be called before remove." }
            val keyTable = map.keyTable
            val valueTable = map.valueTable
            val mask = map.mask
            var next = i + 1 and mask
            var key: K
            while ((keyTable[next].also { key = it }) != null) {
                val placement = map.place(key)
                if (next - placement and mask > i - placement and mask) {
                    keyTable[i] = key
                    valueTable[i] = valueTable[next]
                    i = next
                }
                next = next + 1 and mask
            }
            (keyTable as Array<Any?>)[i] = null
            map.size--
            if (i != currentIndex) --nextIndex
            currentIndex = -1
        }
    }

    class Entries<K>(map: ObjectFloatMap<K>) : MapIterator<K>(map), Iterable<Entry<K>>, Iterator<Entry<K>> {
        internal var entry = Entry<K>()

        /** Note the same entry instance is returned each time this method is called.  */
        override fun next(): Entry<K> {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) error("#iterator() cannot be used nested.")
            val keyTable = map.keyTable
            entry.key = keyTable[nextIndex]
            entry.value = map.valueTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return entry
        }

        override fun hasNext(): Boolean {
            if (!valid) error("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun iterator(): Entries<K> {
            return this
        }
    }

    class Values(map: ObjectFloatMap<*>) : MapIterator<Any>(map as ObjectFloatMap<Any>) {

        operator fun hasNext(): Boolean {
            if (!valid) error("#iterator() cannot be used nested.")
            return hasNext
        }

        operator fun next(): Float {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) error("#iterator() cannot be used nested.")
            val value = map.valueTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return value
        }

        operator fun iterator(): Values {
            return this
        }

        /** Returns a new array containing the remaining values.  */
        fun toArray(): JFloatArray {
            val array = JFloatArray(true, map.size)
            while (hasNext)
                array.add(next())
            return array
        }

        /** Adds the remaining values to the specified array.  */
        fun toArray(array: JFloatArray): JFloatArray {
            while (hasNext)
                array.add(next())
            return array
        }
    }

    class Keys<K>(map: ObjectFloatMap<K>) : MapIterator<K>(map), Iterable<K>, Iterator<K> {

        override fun hasNext(): Boolean {
            if (!valid) error("#iterator() cannot be used nested.")
            return hasNext
        }

        override fun next(): K {
            if (!hasNext) throw NoSuchElementException()
            if (!valid) error("#iterator() cannot be used nested.")
            val key = map.keyTable[nextIndex]
            currentIndex = nextIndex
            findNextIndex()
            return key
        }

        override fun iterator(): Keys<K> {
            return this
        }

        /** Adds the remaining keys to the array.  */
        @JvmOverloads
        fun toArray(array: JArray<K>): JArray<K> {
            while (hasNext)
                array.add(next())
            return array
        }

    }
    /** Returns a new array containing the remaining keys.  */
}
/** Creates a new map with an initial capacity of 51 and a load factor of 0.8.  */
/** Creates a new map with a load factor of 0.8.
 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
 */

inline fun <reified K> ObjectFloatMap.Keys<K>.toArray(): JArray<K> {
    return toArray(JArray<K>(true, map.size))
}
