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

import java.util.Arrays

import com.badlogic.gdx.math.MathUtils

/** A resizable, ordered or unordered short array. Avoids the boxing that occurs with ArrayList<Short>. If unordered, this class
 * avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * @author Nathan Sweet
</Short> */
class JShortArray {
    @JvmField
    var items: ShortArray
    @JvmField
    var size: Int = 0
    @JvmField
    var ordered: Boolean = false

    /** Returns true if the array is empty.  */
    val isEmpty: Boolean
        get() = size == 0

    /** Creates an ordered array with the specified capacity.  */
    constructor(capacity: Int) : this(true, capacity) {}

    /** @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    @JvmOverloads
    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        items = ShortArray(capacity)
    }

    /** Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown.  */
    constructor(array: JShortArray) {
        this.ordered = array.ordered
        size = array.size
        items = ShortArray(size)
        System.arraycopy(array.items, 0, items, 0, size)
    }

    /** Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
     * so any subsequent elements added will cause the backing array to be grown.  */
    constructor(array: ShortArray) : this(true, array, 0, array.size) {}

    /** Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
     * subsequent elements added will cause the backing array to be grown.
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    constructor(ordered: Boolean, array: ShortArray, startIndex: Int, count: Int) : this(ordered, count) {
        size = count
        System.arraycopy(array, startIndex, items, 0, count)
    }

    /** Casts the specified value to short and adds it.  */
    fun add(value: Int) {
        var items = this.items
        if (size == items.size) items = resize(Math.max(8, (size * 1.75f).toInt()))
        items[size++] = value.toShort()
    }

    fun add(value: Short) {
        var items = this.items
        if (size == items.size) items = resize(Math.max(8, (size * 1.75f).toInt()))
        items[size++] = value
    }

    fun add(value1: Short, value2: Short) {
        var items = this.items
        if (size + 1 >= items.size) items = resize(Math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        size += 2
    }

    fun add(value1: Short, value2: Short, value3: Short) {
        var items = this.items
        if (size + 2 >= items.size) items = resize(Math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        size += 3
    }

    fun add(value1: Short, value2: Short, value3: Short, value4: Short) {
        var items = this.items
        if (size + 3 >= items.size) items = resize(Math.max(8, (size * 1.8f).toInt())) // 1.75 isn't enough when size=5.
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        items[size + 3] = value4
        size += 4
    }

    fun addAll(array: JShortArray) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: JShortArray, offset: Int, length: Int) {
        require(offset + length <= array.size) { "offset + length must be <= size: " + offset + " + " + length + " <= " + array.size }
        addAll(array.items, offset, length)
    }

    fun addAll(vararg array: Short) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: ShortArray, offset: Int, length: Int) {
        var items = this.items
        val sizeNeeded = size + length
        if (sizeNeeded > items.size) items = resize(Math.max(8, (sizeNeeded * 1.75f).toInt()))
        System.arraycopy(array, offset, items, size, length)
        size += length
    }

    operator fun get(index: Int): Short {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        return items[index]
    }

    operator fun set(index: Int, value: Short) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = value
    }

    fun incr(index: Int, value: Short) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = (items[index] + value).toShort()
    }

    fun incr(value: Short) {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            items[i] = (items[i] + value).toShort()
            i++
        }
    }

    fun mul(index: Int, value: Short) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = (items[index] * value).toShort()
    }

    fun mul(value: Short) {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            items[i] = (items[i] * value).toShort()
            i++
        }
    }

    fun insert(index: Int, value: Short) {
        if (index > size) throw IndexOutOfBoundsException("index can't be > size: $index > $size")
        var items = this.items
        if (size == items.size) items = resize(Math.max(8, (size * 1.75f).toInt()))
        if (ordered)
            System.arraycopy(items, index, items, index + 1, size - index)
        else
            items[size] = items[index]
        size++
        items[index] = value
    }

    fun swap(first: Int, second: Int) {
        if (first >= size) throw IndexOutOfBoundsException("first can't be >= size: $first >= $size")
        if (second >= size) throw IndexOutOfBoundsException("second can't be >= size: $second >= $size")
        val items = this.items
        val firstValue = items[first]
        items[first] = items[second]
        items[second] = firstValue
    }

    operator fun contains(value: Short): Boolean {
        var i = size - 1
        val items = this.items
        while (i >= 0)
            if (items[i--] == value) return true
        return false
    }

    fun indexOf(value: Short): Int {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            if (items[i] == value) return i
            i++
        }
        return -1
    }

    fun lastIndexOf(value: Char): Int {
        val items = this.items
        for (i in size - 1 downTo 0)
            if (items[i] == value.toShort()) return i
        return -1
    }

    fun removeValue(value: Short): Boolean {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            if (items[i] == value) {
                removeIndex(i)
                return true
            }
            i++
        }
        return false
    }

    /** Removes and returns the item at the specified index.  */
    fun removeIndex(index: Int): Short {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val items = this.items
        val value = items[index]
        size--
        if (ordered)
            System.arraycopy(items, index + 1, items, index, size - index)
        else
            items[index] = items[size]
        return value
    }

    /** Removes the items between the specified indices, inclusive.  */
    fun removeRange(start: Int, end: Int) {
        val n = size
        if (end >= n) throw IndexOutOfBoundsException("end can't be >= size: $end >= $size")
        if (start > end) throw IndexOutOfBoundsException("start can't be > end: $start > $end")
        val count = end - start + 1
        val lastIndex = n - count
        if (ordered)
            System.arraycopy(items, start + count, items, start, n - (start + count))
        else {
            val i = Math.max(lastIndex, end + 1)
            System.arraycopy(items, i, items, start, n - i)
        }
        size = n - count
    }

    /** Removes from this array all of elements contained in the specified array.
     * @return true if this array was modified.
     */
    fun removeAll(array: JShortArray): Boolean {
        var size = this.size
        val startSize = size
        val items = this.items
        var i = 0
        val n = array.size
        while (i < n) {
            val item = array[i]
            for (ii in 0 until size) {
                if (item == items[ii]) {
                    removeIndex(ii)
                    size--
                    break
                }
            }
            i++
        }
        return size != startSize
    }

    /** Removes and returns the last item.  */
    fun pop(): Short {
        return items[--size]
    }

    /** Returns the last item.  */
    fun peek(): Short {
        return items[size - 1]
    }

    /** Returns the first item.  */
    fun first(): Short {
        check(size != 0) { "Array is empty." }
        return items[0]
    }

    /** Returns true if the array has one or more items.  */
    fun notEmpty(): Boolean {
        return size > 0
    }

    fun clear() {
        size = 0
    }

    /** Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return [.items]
     */
    fun shrink(): ShortArray {
        if (items.size != size) resize(size)
        return items
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return [.items]
     */
    fun ensureCapacity(additionalCapacity: Int): ShortArray {
        require(additionalCapacity >= 0) { "additionalCapacity must be >= 0: $additionalCapacity" }
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded > items.size) resize(Math.max(8, sizeNeeded))
        return items
    }

    /** Sets the array size, leaving any values beyond the current size undefined.
     * @return [.items]
     */
    fun setSize(newSize: Int): ShortArray {
        require(newSize >= 0) { "newSize must be >= 0: $newSize" }
        if (newSize > items.size) resize(Math.max(8, newSize))
        size = newSize
        return items
    }

    protected fun resize(newSize: Int): ShortArray {
        val newItems = ShortArray(newSize)
        val items = this.items
        System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.size))
        this.items = newItems
        return newItems
    }

    fun sort() {
        Arrays.sort(items, 0, size)
    }

    fun reverse() {
        val items = this.items
        var i = 0
        val lastIndex = size - 1
        val n = size / 2
        while (i < n) {
            val ii = lastIndex - i
            val temp = items[i]
            items[i] = items[ii]
            items[ii] = temp
            i++
        }
    }

    fun shuffle() {
        val items = this.items
        for (i in size - 1 downTo 0) {
            val ii = MathUtils.random(i)
            val temp = items[i]
            items[i] = items[ii]
            items[ii] = temp
        }
    }

    /** Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.  */
    fun truncate(newSize: Int) {
        if (size > newSize) size = newSize
    }

    /** Returns a random item from the array, or zero if the array is empty.  */
    fun random(): Short {
        return if (size == 0) 0 else items[MathUtils.random(0, size - 1)]
    }

    fun toArray(): ShortArray {
        val array = ShortArray(size)
        System.arraycopy(items, 0, array, 0, size)
        return array
    }

    override fun hashCode(): Int {
        if (!ordered) return super.hashCode()
        val items = this.items
        var h = 1
        var i = 0
        val n = size
        while (i < n) {
            h = h * 31 + items[i]
            i++
        }
        return h
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) return true
        if (!ordered) return false
        if (`object` !is JShortArray) return false
        val array = `object` as JShortArray?
        if (!array!!.ordered) return false
        val n = size
        if (n != array.size) return false
        val items1 = this.items
        val items2 = array.items
        for (i in 0 until n)
            if (items1[i] != items2[i]) return false
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val items = this.items
        val buffer = StringBuilder(32)
        buffer.append('[')
        buffer.append(items[0].toInt())
        for (i in 1 until size) {
            buffer.append(", ")
            buffer.append(items[i].toInt())
        }
        buffer.append(']')
        return buffer.toString()
    }

    fun toString(separator: String): String {
        if (size == 0) return ""
        val items = this.items
        val buffer = StringBuilder(32)
        buffer.append(items[0].toInt())
        for (i in 1 until size) {
            buffer.append(separator)
            buffer.append(items[i].toInt())
        }
        return buffer.toString()
    }

    companion object {

        /** @see .JShortArray
         */
        fun with(vararg array: Short): JShortArray {
            return JShortArray(array)
        }
    }
}
/** Creates an ordered array with a capacity of 16.  */
