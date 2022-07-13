/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2

import org.luaj.vm2.internal.*
import kotlin.jvm.*
import kotlin.math.*

/**
 * Subclass of [LuaValue] for representing lua tables.
 *
 *
 * Almost all API's implemented in [LuaTable] are defined and documented in [LuaValue].
 *
 *
 * If a table is needed, the one of the type-checking functions can be used such as
 * [.istable],
 * [.checktable], or
 * [.opttable]
 *
 *
 * The main table operations are defined on [LuaValue]
 * for getting and setting values with and without metatag processing:
 *
 *  * [.get]
 *  * [.set]
 *  * [.rawget]
 *  * [.rawset]
 *  * plus overloads such as [.get], [.get], and so on
 *
 *
 *
 * To iterate over key-value pairs from Java, use
 * <pre> `LuaValue k = LuaValue.NIL;
 * while ( true ) {
 * Varargs n = table.next(k);
 * if ( (k = n.arg1()).isnil() )
 * break;
 * LuaValue v = n.arg(2)
 * process( k, v )
 * }`</pre>
 *
 *
 *
 * As with other types, [LuaTable] instances should be constructed via one of the table constructor
 * methods on [LuaValue]:
 *
 *  * [LuaValue.tableOf] empty table
 *  * [LuaValue.tableOf] table with capacity
 *  * [LuaValue.listOf] initialize array part
 *  * [LuaValue.listOf] initialize array part
 *  * [LuaValue.tableOf] initialize named hash part
 *  * [LuaValue.tableOf] initialize named hash part
 *  * [LuaValue.tableOf] initialize array and named parts
 *  * [LuaValue.tableOf] initialize array and named parts
 *
 * @see LuaValue
 */
open class LuaTable : LuaValue, Metatable {

    /** the array values  */
    protected var array: Array<LuaValue?> = LuaValue.NOVALS

    /** the hash part  */
    protected var hash: Array<Slot?> = NOBUCKETS

    /** the number of hash entries  */
    var hashEntries: Int = 0

    /** metatable for this table, or null  */
    protected var m_metatable: Metatable? = null

    /**
     * Get the length of the array part of the table.
     * @return length of the array part, does not relate to count of objects in the table.
     */
    val arrayLength: Int get() = array.size

    /**
     * Get the length of the hash part of the table.
     * @return length of the hash part, does not relate to count of objects in the table.
     */
    val hashLength: Int get() = hash.size

    /** Construct empty table  */
    constructor() {
        array = LuaValue.NOVALS
        hash = NOBUCKETS
    }

    /**
     * Construct table with preset capacity.
     * @param narray capacity of array part
     * @param nhash capacity of hash part
     */
    constructor(narray: Int, nhash: Int) {
        presize(narray, nhash)
    }

    /**
     * Construct table with named and unnamed parts.
     * @param named Named elements in order `key-a, value-a, key-b, value-b, ... `
     * @param unnamed Unnamed elements in order `value-1, value-2, ... `
     * @param lastarg Additional unnamed values beyond `unnamed.length`
     */
    constructor(named: Array<LuaValue>?, unnamed: Array<LuaValue>?, lastarg: Varargs?) {
        val nn = named?.size ?: 0
        val nu = unnamed?.size ?: 0
        val nl = lastarg?.narg() ?: 0
        presize(nu + nl, nn shr 1)
        for (i in 0 until nu)
            rawset(i + 1, unnamed!![i])
        if (lastarg != null) {
            var i = 1
            val n = lastarg.narg()
            while (i <= n) {
                rawset(nu + i, lastarg.arg(i))
                ++i
            }
        }
        var i = 0
        while (i < nn) {
            if (!named!![i + 1].isnil())
                rawset(named[i], named[i + 1])
            i += 2
        }
    }

    /**
     * Construct table of unnamed elements.
     * @param varargs Unnamed elements in order `value-1, value-2, ... `
     * @param firstarg the index in varargs of the first argument to include in the table
     */
    @JvmOverloads
    constructor(varargs: Varargs, firstarg: Int = 1) {
        val nskip = firstarg - 1
        val n = max(varargs.narg() - nskip, 0)
        presize(n, 1)
        set(N, LuaValue.valueOf(n))
        for (i in 1..n)
            set(i, varargs.arg(i + nskip))
    }

    override fun type(): Int {
        return LuaValue.TTABLE
    }

    override fun typename(): String {
        return "table"
    }

    override fun istable(): Boolean {
        return true
    }

    override fun checktable(): LuaTable? {
        return this
    }

    override fun opttable(defval: LuaTable?): LuaTable? {
        return this
    }

    override fun presize(narray: Int) {
        if (narray > array.size)
            array = resize(array, 1 shl log2(narray))
    }

    fun presize(narray: Int, nhash: Int) {
        var nhash = nhash
        if (nhash > 0 && nhash < MIN_HASH_CAPACITY)
            nhash = MIN_HASH_CAPACITY
        // Size of both parts must be a power of two.
        array = if (narray > 0) arrayOfNulls(1 shl log2(narray)) else LuaValue.NOVALS
        hash = if (nhash > 0) arrayOfNulls(1 shl log2(nhash)) else NOBUCKETS
        hashEntries = 0
    }

    override fun getmetatable(): LuaValue? {
        return if (m_metatable != null) m_metatable!!.toLuaValue() else null
    }

    override fun setmetatable(metatable: LuaValue?): LuaValue {
        val hadWeakKeys = m_metatable != null && m_metatable!!.useWeakKeys()
        val hadWeakValues = m_metatable != null && m_metatable!!.useWeakValues()
        m_metatable = LuaValue.metatableOf(metatable)
        if (hadWeakKeys != (m_metatable != null && m_metatable!!.useWeakKeys()) || hadWeakValues != (m_metatable != null && m_metatable!!.useWeakValues())) {
            // force a rehash
            rehash(0)
        }
        return this
    }

    override fun get(key: Int): LuaValue {
        val v = rawget(key)
        return if (v.isnil() && m_metatable != null) LuaValue.gettable(this, LuaValue.valueOf(key)) else v
    }

    override fun get(key: LuaValue): LuaValue {
        val v = rawget(key)
        return if (v.isnil() && m_metatable != null) LuaValue.gettable(this, key) else v
    }

    override fun rawget(key: Int): LuaValue {
        if (key > 0 && key <= array.size) {
            val v = if (m_metatable == null) array[key - 1] else m_metatable!!.arrayget(array, key - 1)
            return v ?: LuaValue.NIL
        }
        return hashget(LuaInteger.valueOf(key))
    }

    override fun rawget(key: LuaValue): LuaValue {
        if (key.isinttype()) {
            val ikey = key.toint()
            if (ikey > 0 && ikey <= array.size) {
                val v = if (m_metatable == null)
                    array[ikey - 1]
                else
                    m_metatable!!.arrayget(array as Array<LuaValue?>, ikey - 1)
                return v ?: LuaValue.NIL
            }
        }
        return hashget(key)
    }

    protected fun hashget(key: LuaValue): LuaValue {
        if (hashEntries > 0) {
            var slot: Slot? = hash[hashSlot(key)]
            while (slot != null) {
                val foundSlot: StrongSlot?
                if ((run { foundSlot = slot!!.find(key); foundSlot }) != null) {
                    return foundSlot!!.value()!!
                }
                slot = slot.rest()
            }
        }
        return LuaValue.NIL
    }

    override fun set(key: Int, value: LuaValue) {
        if (m_metatable == null || !rawget(key).isnil() || !LuaValue.settable(this, LuaInteger.valueOf(key), value))
            rawset(key, value)
    }

    /** caller must ensure key is not nil  */
    override fun set(key: LuaValue, value: LuaValue) {
        if (!key.isvalidkey() && !metatag(LuaValue.NEWINDEX).isfunction())
            typerror("table index")
        if (m_metatable == null || !rawget(key).isnil() || !LuaValue.settable(this, key, value))
            rawset(key, value)
    }

    override fun rawset(key: Int, value: LuaValue) {
        if (!arrayset(key, value))
            hashset(LuaInteger.valueOf(key), value)
    }

    /** caller must ensure key is not nil  */
    override fun rawset(key: LuaValue, value: LuaValue) {
        if (!key.isinttype() || !arrayset(key.toint(), value))
            hashset(key, value)
    }

    /** Set an array element  */
    private fun arrayset(key: Int, value: LuaValue): Boolean {
        if (key > 0 && key <= array.size) {
            array[key - 1] = when {
                value.isnil() -> null
                m_metatable != null -> m_metatable!!.wrap(value)
                else -> value
            }
            return true
        }
        return false
    }

    /** Remove the element at a position in a list-table
     *
     * @param pos the position to remove
     * @return The removed item, or [.NONE] if not removed
     */
    fun remove(pos: Int): LuaValue {
        var pos = pos
        val n = rawlen()
        if (pos == 0)
            pos = n
        else if (pos > n)
            return LuaValue.NONE
        val v = rawget(pos)
        var r = v
        while (!r.isnil()) {
            r = rawget(pos + 1)
            rawset(pos++, r)
        }
        return if (v.isnil()) LuaValue.NONE else v
    }

    /** Insert an element at a position in a list-table
     *
     * @param pos the position to remove
     * @param value The value to insert
     */
    fun insert(pos: Int, value: LuaValue) {
        var pos = pos
        var value = value
        if (pos == 0)
            pos = rawlen() + 1
        while (!value.isnil()) {
            val v = rawget(pos)
            rawset(pos++, value)
            value = v
        }
    }

    /** Concatenate the contents of a table efficiently, using [Buffer]
     *
     * @param sep [LuaString] separater to apply between elements
     * @param i the first element index
     * @param j the last element index, inclusive
     * @return [LuaString] value of the concatenation
     */
    fun concat(sep: LuaString, i: Int, j: Int): LuaValue {
        var i = i
        val sb = Buffer()
        if (i <= j) {
            sb.append(get(i).checkstring()!!)
            while (++i <= j) {
                sb.append(sep)
                sb.append(get(i).checkstring()!!)
            }
        }
        return sb.tostring()
    }

    override fun length(): Int {
        return if (m_metatable != null) len().toint() else rawlen()
    }

    override fun len(): LuaValue {
        val h = metatag(LuaValue.LEN)
        return if (h.toboolean()) h.call(this) else LuaInteger.valueOf(rawlen())
    }

    override fun rawlen(): Int {
        val a = arrayLength
        var n = a + 1
        var m = 0
        while (!rawget(n).isnil()) {
            m = n
            n += a + hashLength + 1
        }
        while (n > m + 1) {
            val k = (n + m) / 2
            if (!rawget(k).isnil())
                m = k
            else
                n = k
        }
        return m
    }

    /**
     * Get the next element after a particular key in the table
     * @return key,value or nil
     */
    override fun next(key: LuaValue): Varargs {
        var i = 0
        do {
            // find current key index
            if (!key.isnil()) {
                if (key.isinttype()) {
                    i = key.toint()
                    if (i > 0 && i <= array.size) {
                        break
                    }
                }
                if (hash.size == 0)
                    LuaValue.error("invalid key to 'next'")
                i = hashSlot(key)
                var found = false
                var slot: Slot? = hash[i]
                while (slot != null) {
                    if (found) {
                        val nextEntry = slot.first()
                        if (nextEntry != null) {
                            return nextEntry.toVarargs()
                        }
                    } else if (slot.keyeq(key)) {
                        found = true
                    }
                    slot = slot.rest()
                }
                if (!found) {
                    LuaValue.error("invalid key to 'next'")
                }
                i += 1 + array.size
            }
        } while (false)

        // check array part
        while (i < array.size) {
            if (array[i] != null) {
                val value = if (m_metatable == null) array[i] else m_metatable!!.arrayget(array, i)
                if (value != null) {
                    return LuaValue.varargsOf(LuaInteger.valueOf(i + 1), value)
                }
            }
            ++i
        }

        // check hash part
        i -= array.size
        while (i < hash.size) {
            var slot: Slot? = hash[i]
            while (slot != null) {
                val first = slot.first()
                if (first != null)
                    return first.toVarargs()
                slot = slot.rest()
            }
            ++i
        }

        // nothing found, push nil, return nil.
        return LuaValue.NIL
    }

    /**
     * Get the next element after a particular key in the
     * contiguous array part of a table
     * @return key,value or none
     */
    override fun inext(key: LuaValue): Varargs {
        val k = key.checkint() + 1
        val v = rawget(k)
        return if (v.isnil()) LuaValue.NONE else LuaValue.varargsOf(LuaInteger.valueOf(k), v)
    }

    /**
     * Set a hashtable value
     * @param key key to set
     * @param value value to set
     */
    fun hashset(key: LuaValue, value: LuaValue) {
        if (value.isnil())
            hashRemove(key)
        else {
            var index = 0
            if (hash.size > 0) {
                index = hashSlot(key)
                var slot: Slot? = hash[index]
                while (slot != null) {
                    var foundSlot: StrongSlot? = null
                    if ((run { foundSlot = slot!!.find(key); foundSlot }) != null) {
                        hash[index] = hash[index]!!.set(foundSlot!!, value)
                        return
                    }
                    slot = slot.rest()
                }
            }
            if (checkLoadFactor()) {
                if (key.isinttype() && key.toint() > 0) {
                    // a rehash might make room in the array portion for this key.
                    rehash(key.toint())
                    if (arrayset(key.toint(), value))
                        return
                } else {
                    rehash(-1)
                }
                index = hashSlot(key)
            }
            val entry = if (m_metatable != null)
                m_metatable!!.entry(key, value)
            else
                defaultEntry(key, value)
            hash[index] = if (hash[index] != null) hash[index]!!.add(entry!!) else entry
            ++hashEntries
        }
    }

    /**
     * Find the hashtable slot to use
     * @param key key to look for
     * @return slot to use
     */
    private fun hashSlot(key: LuaValue): Int {
        return hashSlot(key, hash.size - 1)
    }

    private fun hashRemove(key: LuaValue) {
        if (hash.size > 0) {
            val index = hashSlot(key)
            var slot: Slot? = hash[index]
            while (slot != null) {
                var foundSlot: StrongSlot? = null
                if ((run { foundSlot = slot!!.find(key); foundSlot }) != null) {
                    hash[index] = hash[index]!!.remove(foundSlot!!)
                    --hashEntries
                    return
                }
                slot = slot.rest()
            }
        }
    }

    private fun checkLoadFactor(): Boolean {
        return hashEntries >= hash.size
    }

    private fun countHashKeys(): Int {
        var keys = 0
        for (i in hash.indices) {
            var slot: Slot? = hash[i]
            while (slot != null) {
                if (slot.first() != null)
                    keys++
                slot = slot.rest()
            }
        }
        return keys
    }

    private fun dropWeakArrayValues() {
        for (i in array.indices) {
            m_metatable!!.arrayget(array, i)
        }
    }

    private fun countIntKeys(nums: IntArray): Int {
        var total = 0
        var i = 1

        // Count integer keys in array part
        for (bit in 0..30) {
            if (i > array.size)
                break
            val j = min(array.size, 1 shl bit)
            var c = 0
            while (i <= j) {
                if (array[i++ - 1] != null)
                    c++
            }
            nums[bit] = c
            total += c
        }

        // Count integer keys in hash part
        i = 0
        while (i < hash.size) {
            var s: Slot? = hash[i]
            while (s != null) {
                val k: Int
                if ((run { k = s!!.arraykey(Int.MAX_VALUE); k }) > 0) {
                    nums[log2(k)]++
                    total++
                }
                s = s.rest()
            }
            ++i
        }

        return total
    }

    /*
	 * newKey > 0 is next key to insert
	 * newKey == 0 means number of keys not changing (__mode changed)
	 * newKey < 0 next key will go in hash part
	 */
    private fun rehash(newKey: Int) {
        if (m_metatable != null && (m_metatable!!.useWeakKeys() || m_metatable!!.useWeakValues())) {
            // If this table has weak entries, hashEntries is just an upper bound.
            hashEntries = countHashKeys()
            if (m_metatable!!.useWeakValues()) {
                dropWeakArrayValues()
            }
        }
        val nums = IntArray(32)
        var total = countIntKeys(nums)
        if (newKey > 0) {
            total++
            nums[log2(newKey)]++
        }

        // Choose N such that N <= sum(nums[0..log(N)]) < 2N
        var keys = nums[0]
        var newArraySize = 0
        for (log in 1..31) {
            keys += nums[log]
            if (total * 2 < 1 shl log) {
                // Not enough integer keys.
                break
            } else if (keys >= 1 shl log - 1) {
                newArraySize = 1 shl log
            }
        }

        val oldArray = array
        val oldHash = hash
        val newArray: Array<LuaValue?>
        val newHash: Array<Slot?>

        // Copy existing array entries and compute number of moving entries.
        var movingToArray = 0
        if (newKey > 0 && newKey <= newArraySize) {
            movingToArray--
        }
        if (newArraySize != oldArray.size) {
            newArray = arrayOfNulls(newArraySize)
            if (newArraySize > oldArray.size) {
                var i = log2(oldArray.size + 1)
                val j = log2(newArraySize) + 1
                while (i < j) {
                    movingToArray += nums[i]
                    ++i
                }
            } else if (oldArray.size > newArraySize) {
                var i = log2(newArraySize + 1)
                val j = log2(oldArray.size) + 1
                while (i < j) {
                    movingToArray -= nums[i]
                    ++i
                }
            }
            arraycopy(oldArray, 0, newArray, 0, min(oldArray.size, newArraySize))
        } else {
            newArray = array
        }

        val newHashSize =
            hashEntries - movingToArray + if (newKey < 0 || newKey > newArraySize) 1 else 0 // Make room for the new entry
        val oldCapacity = oldHash.size
        val newCapacity: Int
        val newHashMask: Int

        if (newHashSize > 0) {
            // round up to next power of 2.
            newCapacity = if (newHashSize < MIN_HASH_CAPACITY)
                MIN_HASH_CAPACITY
            else
                1 shl log2(newHashSize)
            newHashMask = newCapacity - 1
            newHash = arrayOfNulls(newCapacity)
        } else {
            newCapacity = 0
            newHashMask = 0
            newHash = NOBUCKETS
        }

        // Move hash buckets
        for (i in 0 until oldCapacity) {
            var slot: Slot? = oldHash[i]
            while (slot != null) {
                val k: Int
                if ((run { k = slot!!.arraykey(newArraySize); k }) > 0) {
                    val entry = slot.first()
                    if (entry != null)
                        newArray[k - 1] = entry.value()
                } else {
                    val j = slot.keyindex(newHashMask)
                    newHash[j] = slot.relink(newHash[j])
                }
                slot = slot.rest()
            }
        }

        // Move array values into hash portion
        var i = newArraySize
        while (i < oldArray.size) {
            var v: LuaValue? = null
            if ((run { v = oldArray[i++]; v }) != null) {
                val slot = hashmod(LuaInteger.hashCode(i), newHashMask)
                val newEntry: Slot?
                if (m_metatable != null) {
                    newEntry = m_metatable!!.entry(LuaValue.valueOf(i), v!!)
                    if (newEntry == null)
                        continue
                } else {
                    newEntry = defaultEntry(LuaValue.valueOf(i), v!!)
                }
                newHash[slot] = if (newHash[slot] != null)
                    newHash[slot]?.add(newEntry)
                else
                    newEntry
            }
        }

        hash = newHash
        array = newArray
        hashEntries -= movingToArray
    }

    override fun entry(key: LuaValue, value: LuaValue): Slot? {
        return defaultEntry(key, value)
    }

    // ----------------- sort support -----------------------------
    //
    // implemented heap sort from wikipedia
    //
    // Only sorts the contiguous array part.
    //
    /** Sort the table using a comparator.
     * @param comparator [LuaValue] to be called to compare elements.
     */
    fun sort(comparator: LuaValue) {
        if (m_metatable != null && m_metatable!!.useWeakValues()) {
            dropWeakArrayValues()
        }
        var n = array.size
        while (n > 0 && array[n - 1] == null)
            --n
        if (n > 1)
            heapSort(n, comparator)
    }

    private fun heapSort(count: Int, cmpfunc: LuaValue) {
        heapify(count, cmpfunc)
        var end = count - 1
        while (end > 0) {
            swap(end, 0)
            siftDown(0, --end, cmpfunc)
        }
    }

    private fun heapify(count: Int, cmpfunc: LuaValue) {
        for (start in count / 2 - 1 downTo 0)
            siftDown(start, count - 1, cmpfunc)
    }

    private fun siftDown(start: Int, end: Int, cmpfunc: LuaValue) {
        var root = start
        while (root * 2 + 1 <= end) {
            var child = root * 2 + 1
            if (child < end && compare(child, child + 1, cmpfunc))
                ++child
            if (compare(root, child, cmpfunc)) {
                swap(root, child)
                root = child
            } else
                return
        }
    }

    private fun compare(i: Int, j: Int, cmpfunc: LuaValue): Boolean {
        val a: LuaValue?
        val b: LuaValue?
        if (m_metatable == null) {
            a = array[i]
            b = array[j]
        } else {
            a = m_metatable!!.arrayget(array, i)
            b = m_metatable!!.arrayget(array, j)
        }
        if (a == null || b == null)
            return false
        return if (!cmpfunc.isnil()) {
            cmpfunc.call(a, b).toboolean()
        } else {
            a.lt_b(b)
        }
    }

    private fun swap(i: Int, j: Int) {
        val a = array[i]
        array[i] = array[j]
        array[j] = a
    }

    /** This may be deprecated in a future release.
     * It is recommended to count via iteration over next() instead
     * @return count of keys in the table
     */
    fun keyCount(): Int {
        var k = LuaValue.NIL
        var i = 0
        while (true) {
            val n = next(k)
            if ((run { k = n.arg1(); k }).isnil())
                return i
            i++
        }
    }

    /** This may be deprecated in a future release.
     * It is recommended to use next() instead
     * @return array of keys in the table
     */
    fun keys(): Array<LuaValue> {
        val l = ArrayList<LuaValue>()
        var k = LuaValue.NIL
        while (true) {
            val n = next(k)
            if ((run { k = n.arg1(); k }).isnil())
                break
            l.add(k)
        }
        return l.toTypedArray()
    }

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue): LuaValue {
        return if (eq_b(`val`)) LuaValue.BTRUE else LuaValue.BFALSE
    }

    override fun eq_b(`val`: LuaValue): Boolean {
        if (this === `val`) return true
        if (m_metatable == null || !`val`.istable()) return false
        val valmt = `val`.getmetatable()
        return valmt != null && LuaValue.eqmtcall(this, m_metatable!!.toLuaValue(), `val`, valmt)
    }

    /** Unpack the elements from i to j inclusive  */
    @JvmOverloads
    fun unpack(i: Int = 1, j: Int = this.rawlen()): Varargs {
        var n = j + 1 - i
        when (n) {
            0 -> return LuaValue.NONE
            1 -> return get(i)
            2 -> return LuaValue.varargsOf(get(i), get(i + 1))
            else -> {
                if (n < 0)
                    return LuaValue.NONE
                val v = arrayOfNulls<LuaValue>(n)
                while (--n >= 0)
                    v[n] = get(i + n)
                return LuaValue.varargsOf(v as Array<LuaValue>)
            }
        }
    }

    /**
     * Represents a slot in the hash table.
     */
    interface Slot {

        /** Return hash{pow2,mod}( first().key().hashCode(), sizeMask )  */
        fun keyindex(hashMask: Int): Int

        /** Return first Entry, if still present, or null.  */
        fun first(): StrongSlot?

        /** Compare given key with first()'s key; return first() if equal.  */
        fun find(key: LuaValue): StrongSlot?

        /**
         * Compare given key with first()'s key; return true if equal. May
         * return true for keys no longer present in the table.
         */
        fun keyeq(key: LuaValue?): Boolean

        /** Return rest of elements  */
        fun rest(): Slot?

        /**
         * Return first entry's key, iff it is an integer between 1 and max,
         * inclusive, or zero otherwise.
         */
        fun arraykey(max: Int): Int

        /**
         * Set the value of this Slot's first Entry, if possible, or return a
         * new Slot whose first entry has the given value.
         */
        operator fun set(target: StrongSlot, value: LuaValue): Slot?

        /**
         * Link the given new entry to this slot.
         */
        fun add(newEntry: Slot): Slot?

        /**
         * Return a Slot with the given value set to nil; must not return null
         * for next() to behave correctly.
         */
        fun remove(target: StrongSlot): Slot?

        /**
         * Return a Slot with the same first key and value (if still present)
         * and rest() equal to rest.
         */
        fun relink(rest: Slot?): Slot?
    }

    /**
     * Subclass of Slot guaranteed to have a strongly-referenced key and value,
     * to support weak tables.
     */
    interface StrongSlot : Slot {
        /** Return first entry's key  */
        fun key(): LuaValue

        /** Return first entry's value  */
        fun value(): LuaValue?

        /** Return varargsOf(key(), value()) or equivalent  */
        fun toVarargs(): Varargs
    }

    class LinkSlot constructor(private var entry: Entry?, private var next: Slot?) : StrongSlot {

        override fun key(): LuaValue {
            return entry!!.key()
        }

        override fun keyindex(hashMask: Int): Int {
            return entry!!.keyindex(hashMask)
        }

        override fun value(): LuaValue {
            return entry!!.value()!!
        }

        override fun toVarargs(): Varargs {
            return entry!!.toVarargs()
        }

        override fun first(): StrongSlot? {
            return entry
        }

        override fun find(key: LuaValue): StrongSlot? {
            return if (entry!!.keyeq(key)) this else null
        }

        override fun keyeq(key: LuaValue?): Boolean {
            return entry!!.keyeq(key)
        }

        override fun rest(): Slot? {
            return next
        }

        override fun arraykey(max: Int): Int {
            return entry!!.arraykey(max)
        }

        override fun set(target: StrongSlot, value: LuaValue): Slot? {
            if (target === this) {
                entry = entry!!.set(value)
                return this
            } else {
                return setnext(next!!.set(target, value))
            }
        }

        override fun add(entry: Slot): Slot? {
            return setnext(next!!.add(entry))
        }

        override fun remove(target: StrongSlot): Slot {
            if (this === target) {
                return DeadSlot(key(), next)
            } else {
                this.next = next!!.remove(target)
            }
            return this
        }

        override fun relink(rest: Slot?): Slot? {
            // This method is (only) called during rehash, so it must not change this.next.
            return rest?.let { LinkSlot(entry, it) } ?: entry as Slot?
        }

        // this method ensures that this.next is never set to null.
        private fun setnext(next: Slot?): Slot? {
            if (next != null) {
                this.next = next
                return this
            } else {
                return entry
            }
        }

        override fun toString(): String {
            return entry.toString() + "; " + next
        }
    }

    /**
     * Base class for regular entries.
     *
     *
     *
     * If the key may be an integer, the [.arraykey] method must be
     * overridden to handle that case.
     */
    abstract class Entry : Varargs(), StrongSlot {
        abstract override fun key(): LuaValue
        abstract override fun value(): LuaValue?
        internal abstract fun set(value: LuaValue): Entry
        abstract override fun keyeq(key: LuaValue?): Boolean
        abstract override fun keyindex(hashMask: Int): Int

        override fun arraykey(max: Int): Int {
            return 0
        }

        override fun arg(i: Int): LuaValue {
            when (i) {
                1 -> return key()
                2 -> return value()!!
            }
            return LuaValue.NIL
        }

        override fun narg(): Int {
            return 2
        }

        /**
         * Subclasses should redefine as "return this;" whenever possible.
         */
        override fun toVarargs(): Varargs {
            return LuaValue.varargsOf(key(), value()!!)
        }

        override fun arg1(): LuaValue {
            return key()
        }

        override fun subargs(start: Int): Varargs {
            when (start) {
                1 -> return this
                2 -> return value()!!
            }
            return LuaValue.NONE
        }

        override fun first(): StrongSlot? {
            return this
        }

        override fun rest(): Slot? {
            return null
        }

        override fun find(key: LuaValue): StrongSlot? {
            return if (keyeq(key)) this else null
        }

        override fun set(target: StrongSlot, value: LuaValue): Slot {
            return set(value)
        }

        override fun add(entry: Slot): Slot {
            return LinkSlot(this, entry)
        }

        override fun remove(target: StrongSlot): Slot {
            return DeadSlot(key(), null)
        }

        override fun relink(rest: Slot?): Slot? {
            return if (rest != null) LinkSlot(this, rest) else this
        }
    }

    class NormalEntry(private val key: LuaValue, private var value: LuaValue?) : Entry() {

        override fun key(): LuaValue {
            return key
        }

        override fun value(): LuaValue? {
            return value
        }

        public override fun set(value: LuaValue): Entry {
            this.value = value
            return this
        }

        override fun toVarargs(): Varargs {
            return this
        }

        override fun keyindex(hashMask: Int): Int {
            return hashSlot(key, hashMask)
        }

        override fun keyeq(key: LuaValue?): Boolean {
            return key!!.raweq(this.key)
        }
    }

    class IntKeyEntry constructor(private val key: Int, private var value: LuaValue?) : Entry() {

        override fun key(): LuaValue {
            return LuaValue.valueOf(key)
        }

        override fun arraykey(max: Int): Int {
            return if (key >= 1 && key <= max) key else 0
        }

        override fun value(): LuaValue? {
            return value
        }

        public override fun set(value: LuaValue): Entry {
            this.value = value
            return this
        }

        override fun keyindex(mask: Int): Int {
            return hashmod(LuaInteger.hashCode(key), mask)
        }

        override fun keyeq(key: LuaValue?): Boolean {
            return key!!.raweq(this.key)
        }
    }

    /**
     * Entry class used with numeric values, but only when the key is not an integer.
     */
    private class NumberValueEntry internal constructor(private val key: LuaValue, private var value: Double) :
        Entry() {

        override fun key(): LuaValue {
            return key
        }

        override fun value(): LuaValue {
            return LuaValue.valueOf(value)
        }

        public override fun set(value: LuaValue): Entry {
            val n = value.tonumber()
            if (!n.isnil()) {
                this.value = n.todouble()
                return this
            } else {
                return NormalEntry(this.key, value)
            }
        }

        override fun keyindex(mask: Int): Int {
            return hashSlot(key, mask)
        }

        override fun keyeq(key: LuaValue?): Boolean {
            return key!!.raweq(this.key)
        }
    }

    /**
     * A Slot whose value has been set to nil. The key is kept in a weak reference so that
     * it can be found by next().
     */
    class DeadSlot constructor(key: LuaValue, private var next: Slot?) : Slot {

        private val key: Any

        init {
            this.key = if (isLargeKey(key)) WeakReference(key) else key
        }

        private fun key(): LuaValue? {
            return (if (key is WeakReference<*>) key.get() else key) as LuaValue
        }

        override fun keyindex(hashMask: Int): Int {
            // Not needed: this entry will be dropped during rehash.
            return 0
        }

        override fun first(): StrongSlot? {
            return null
        }

        override fun find(key: LuaValue): StrongSlot? {
            return null
        }

        override fun keyeq(key: LuaValue?): Boolean {
            val k = key()
            return k != null && key!!.raweq(k)
        }

        override fun rest(): Slot? {
            return next
        }

        override fun arraykey(max: Int): Int {
            return -1
        }

        override fun set(target: StrongSlot, value: LuaValue): Slot? {
            val next = if (this.next != null) this.next!!.set(target, value) else null
            if (key() != null) {
                // if key hasn't been garbage collected, it is still potentially a valid argument
                // to next(), so we can't drop this entry yet.
                this.next = next
                return this
            } else {
                return next
            }
        }

        override fun add(newEntry: Slot): Slot? {
            return if (next != null) next!!.add(newEntry) else newEntry
        }

        override fun remove(target: StrongSlot): Slot? {
            if (key() != null) {
                next = next!!.remove(target)
                return this
            } else {
                return next
            }
        }

        override fun relink(rest: Slot?): Slot? {
            return rest
        }

        override fun toString(): String {
            val buf = StringBuilder()
            buf.append("<dead")
            val k = key()
            if (k != null) {
                buf.append(": ")
                buf.append(k.toString())
            }
            buf.append('>')
            if (next != null) {
                buf.append("; ")
                buf.append(next!!.toString())
            }
            return buf.toString()
        }
    }

    // Metatable operations

    override fun useWeakKeys(): Boolean {
        return false
    }

    override fun useWeakValues(): Boolean {
        return false
    }

    override fun toLuaValue(): LuaValue {
        return this
    }

    override fun wrap(value: LuaValue): LuaValue {
        return value
    }

    override fun arrayget(array: Array<LuaValue?>, index: Int): LuaValue? {
        return array[index]
    }

    companion object {
        private val MIN_HASH_CAPACITY = 2
        private val N = LuaValue.valueOf("n")

        /** Resize the table  */
        private fun resize(old: Array<LuaValue?>, n: Int): Array<LuaValue?> {
            val v = arrayOfNulls<LuaValue>(n)
            arraycopy(old, 0, v, 0, old.size)
            return v
        }

        fun hashpow2(hashCode: Int, mask: Int): Int {
            return hashCode and mask
        }

        fun hashmod(hashCode: Int, mask: Int): Int {
            return (hashCode and 0x7FFFFFFF) % mask
        }

        /**
         * Find the hashtable slot index to use.
         * @param key the key to look for
         * @param hashMask N-1 where N is the number of hash slots (must be power of 2)
         * @return the slot index
         */
        fun hashSlot(key: LuaValue, hashMask: Int): Int {
            when (key.type()) {
                LuaValue.TNUMBER, LuaValue.TTABLE, LuaValue.TTHREAD, LuaValue.TLIGHTUSERDATA, LuaValue.TUSERDATA -> return hashmod(
                    key.hashCode(),
                    hashMask
                )
                else -> return hashpow2(key.hashCode(), hashMask)
            }
        }

        // Compute ceil(log2(x))
        internal fun log2(x: Int): Int {
            var x = x
            var lg = 0
            x -= 1
            if (x < 0)
            // 2^(-(2^31)) is approximately 0
                return Int.MIN_VALUE
            if (x and -0x10000 != 0) {
                lg = 16
                x = x ushr 16
            }
            if (x and 0xFF00 != 0) {
                lg += 8
                x = x ushr 8
            }
            if (x and 0xF0 != 0) {
                lg += 4
                x = x ushr 4
            }
            when (x) {
                0x0 -> return 0
                0x1 -> lg += 1
                0x2 -> lg += 2
                0x3 -> lg += 2
                0x4 -> lg += 3
                0x5 -> lg += 3
                0x6 -> lg += 3
                0x7 -> lg += 3
                0x8 -> lg += 4
                0x9 -> lg += 4
                0xA -> lg += 4
                0xB -> lg += 4
                0xC -> lg += 4
                0xD -> lg += 4
                0xE -> lg += 4
                0xF -> lg += 4
            }
            return lg
        }

        protected fun isLargeKey(key: LuaValue): Boolean {
            when (key.type()) {
                LuaValue.TSTRING -> return key.rawlen() > LuaString.RECENT_STRINGS_MAX_LENGTH
                LuaValue.TNUMBER, LuaValue.TBOOLEAN -> return false
                else -> return true
            }
        }

        fun defaultEntry(key: LuaValue, value: LuaValue): Entry {
            return when {
                key.isinttype() -> IntKeyEntry(key.toint(), value)
                value.type() == LuaValue.TNUMBER -> NumberValueEntry(key, value.todouble())
                else -> NormalEntry(key, value)
            }
        }

        private val NOBUCKETS = arrayOf<Slot?>()
    }
}
/**
 * Construct table of unnamed elements.
 * @param varargs Unnamed elements in order `value-1, value-2, ... `
 */
/** Unpack all the elements of this table  */
/** Unpack all the elements of this table from element i  */
