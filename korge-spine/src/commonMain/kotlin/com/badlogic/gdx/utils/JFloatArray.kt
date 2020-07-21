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

/** A resizable, ordered or unordered float array. Avoids the boxing that occurs with ArrayList<Float>. If unordered, this class
 * avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * @author Nathan Sweet
</Float> */
class JFloatArray {

    var items: FloatArray

    var size: Int = 0

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

    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        items = FloatArray(capacity)
    }

    /** Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown.  */
    constructor(array: JFloatArray) {
        this.ordered = array.ordered
        size = array.size
        items = FloatArray(size)
        com.soywiz.kmem.arraycopy(array.items, 0, items, 0, size)
    }

    /** Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
     * so any subsequent elements added will cause the backing array to be grown.  */
    constructor(array: FloatArray) : this(true, array, 0, array.size) {}

    /** Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
     * subsequent elements added will cause the backing array to be grown.
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    constructor(ordered: Boolean, array: FloatArray, startIndex: Int, count: Int) : this(ordered, count) {
        size = count
        com.soywiz.kmem.arraycopy(array, startIndex, items, 0, count)
    }

    fun add(value: Float) {
        var items = this.items
        if (size == items.size) items = resize(kotlin.math.max(8, (size * 1.75f).toInt()))
        items[size++] = value
    }

    fun add(value1: Float, value2: Float) {
        var items = this.items
        if (size + 1 >= items.size) items = resize(kotlin.math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        size += 2
    }

    fun add(value1: Float, value2: Float, value3: Float) {
        var items = this.items
        if (size + 2 >= items.size) items = resize(kotlin.math.max(8, (size * 1.75f).toInt()))
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        size += 3
    }

    fun add(value1: Float, value2: Float, value3: Float, value4: Float) {
        var items = this.items
        if (size + 3 >= items.size) items = resize(kotlin.math.max(8, (size * 1.8f).toInt())) // 1.75 isn't enough when size=5.
        items[size] = value1
        items[size + 1] = value2
        items[size + 2] = value3
        items[size + 3] = value4
        size += 4
    }

    fun addAll(array: JFloatArray) {
        addAll(array.items, 0, array.size)
    }

    fun addAll(array: JFloatArray, offset: Int, length: Int) {
        require(offset + length <= array.size) { "offset + length must be <= size: " + offset + " + " + length + " <= " + array.size }
        addAll(array.items, offset, length)
    }

    fun addAll(vararg array: Float) {
        addAll(array, 0, array.size)
    }

    fun addAll(array: FloatArray, offset: Int, length: Int) {
        var items = this.items
        val sizeNeeded = size + length
        if (sizeNeeded > items.size) items = resize(kotlin.math.max(8, (sizeNeeded * 1.75f).toInt()))
        com.soywiz.kmem.arraycopy(array, offset, items, size, length)
        size += length
    }

    operator fun get(index: Int): Float {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        return items[index]
    }

    operator fun set(index: Int, value: Float) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] = value
    }

    fun incr(index: Int, value: Float) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] += value
    }

    fun incr(value: Float) {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            items[i] += value
            i++
        }
    }

    fun mul(index: Int, value: Float) {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        items[index] *= value
    }

    fun mul(value: Float) {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            items[i] *= value
            i++
        }
    }

    fun insert(index: Int, value: Float) {
        if (index > size) throw IndexOutOfBoundsException("index can't be > size: $index > $size")
        var items = this.items
        if (size == items.size) items = resize(kotlin.math.max(8, (size * 1.75f).toInt()))
        if (ordered)
            com.soywiz.kmem.arraycopy(items, index, items, index + 1, size - index)
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

    operator fun contains(value: Float): Boolean {
        var i = size - 1
        val items = this.items
        while (i >= 0)
            if (items[i--] == value) return true
        return false
    }

    fun indexOf(value: Float): Int {
        val items = this.items
        var i = 0
        val n = size
        while (i < n) {
            if (items[i] == value) return i
            i++
        }
        return -1
    }

    fun lastIndexOf(value: Float): Int {
        val items = this.items
        for (i in size - 1 downTo 0)
            if (items[i] == value) return i
        return -1
    }

    fun removeValue(value: Float): Boolean {
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
    fun removeIndex(index: Int): Float {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val items = this.items
        val value = items[index]
        size--
        if (ordered)
            com.soywiz.kmem.arraycopy(items, index + 1, items, index, size - index)
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
            com.soywiz.kmem.arraycopy(items, start + count, items, start, n - (start + count))
        else {
            val i = kotlin.math.max(lastIndex, end + 1)
            com.soywiz.kmem.arraycopy(items, i, items, start, n - i)
        }
        size = n - count
    }

    /** Removes from this array all of elements contained in the specified array.
     * @return true if this array was modified.
     */
    fun removeAll(array: JFloatArray): Boolean {
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
    fun pop(): Float {
        return items[--size]
    }

    /** Returns the last item.  */
    fun peek(): Float {
        return items[size - 1]
    }

    /** Returns the first item.  */
    fun first(): Float {
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
    fun shrink(): FloatArray {
        if (items.size != size) resize(size)
        return items
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return [.items]
     */
    fun ensureCapacity(additionalCapacity: Int): FloatArray {
        require(additionalCapacity >= 0) { "additionalCapacity must be >= 0: $additionalCapacity" }
        val sizeNeeded = size + additionalCapacity
        if (sizeNeeded > items.size) resize(kotlin.math.max(8, sizeNeeded))
        return items
    }

    /** Sets the array size, leaving any values beyond the current size undefined.
     * @return [.items]
     */
    fun setSize(newSize: Int): FloatArray {
        require(newSize >= 0) { "newSize must be >= 0: $newSize" }
        if (newSize > items.size) resize(kotlin.math.max(8, newSize))
        size = newSize
        return items
    }

    protected fun resize(newSize: Int): FloatArray {
        val newItems = FloatArray(newSize)
        val items = this.items
        com.soywiz.kmem.arraycopy(items, 0, newItems, 0, kotlin.math.min(size, newItems.size))
        this.items = newItems
        return newItems
    }

    fun toArray(): FloatArray {
        val array = FloatArray(size)
        com.soywiz.kmem.arraycopy(items, 0, array, 0, size)
        return array
    }

    override fun hashCode(): Int {
        if (!ordered) return super.hashCode()
        val items = this.items
        var h = 1
        var i = 0
        val n = size
        while (i < n) {
            h = h * 31 + items[i].toRawBits()
            i++
        }
        return h
    }

    /** Returns false if either array is unordered.  */
    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) return true
        if (!ordered) return false
        if (`object` !is JFloatArray) return false
        val array = `object` as JFloatArray?
        if (!array!!.ordered) return false
        val n = size
        if (n != array.size) return false
        val items1 = this.items
        val items2 = array.items
        for (i in 0 until n)
            if (items1[i] != items2[i]) return false
        return true
    }

    /** Returns false if either array is unordered.  */
    fun equals(`object`: Any, epsilon: Float): Boolean {
        if (`object` === this) return true
        if (`object` !is JFloatArray) return false
        val n = size
        if (n != `object`.size) return false
        if (!ordered) return false
        if (!`object`.ordered) return false
        val items1 = this.items
        val items2 = `object`.items
        for (i in 0 until n)
            if (kotlin.math.abs(items1[i] - items2[i]) > epsilon) return false
        return true
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val items = this.items
        val buffer = StringBuilder(32)
        buffer.append('[')
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(", ")
            buffer.append(items[i])
        }
        buffer.append(']')
        return buffer.toString()
    }

    fun toString(separator: String): String {
        if (size == 0) return ""
        val items = this.items
        val buffer = StringBuilder(32)
        buffer.append(items[0])
        for (i in 1 until size) {
            buffer.append(separator)
            buffer.append(items[i])
        }
        return buffer.toString()
    }

    companion object {

        /** @see .JFloatArray
         */
        fun with(vararg array: Float): JFloatArray {
            return JFloatArray(array)
        }
    }
}
/** Creates an ordered array with a capacity of 16.  */
