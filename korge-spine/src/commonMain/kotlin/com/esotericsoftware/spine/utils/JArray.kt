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

package com.esotericsoftware.spine.utils

/** A resizable, ordered or unordered array of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 * @author Nathan Sweet
 */
class JArray<T> : Iterable<T> {
    /** Provides direct access to the underlying array. If the Array's generic type is not Object, this field may only be accessed
     * if the [JArray.JArray] constructor was used.  */
    private var items: MutableList<T>

    var size: Int
        set(value) {
            val items: MutableList<T?> = this.items as MutableList<T?>
            while (items.size > value) items.removeAt(items.size - 1)
        }
        get() = items.size


    var ordered: Boolean = false

    /** Returns true if the array is empty.  */
    val isEmpty: Boolean
        get() = size == 0

    /** Creates an ordered array with the specified capacity.  */
    constructor(capacity: Int) : this(true, capacity)

    /** @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */

    constructor(ordered: Boolean = true, capacity: Int = 16) {
        this.ordered = ordered
        items = ArrayList(capacity)
    }

    fun add(value: T) {
        items.add(value)
    }

    fun addAll(array: JArray<out T>, start: Int, count: Int) {
        for (n in 0 until count) add(array[start + n])
    }

    operator fun get(index: Int): T = items[index]

    operator fun set(index: Int, value: T) {
        if (index >= items.size) {
            val items = this.items as MutableList<Any?>
            while (items.size <= index) items.add(null)
        }
        items[index] = value
    }

    /** Returns true if this array contains the specified value.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    fun contains(value: T?, identity: Boolean): Boolean = items.contains(value)

    /** Returns the index of first occurrence of value in the array, or -1 if no such value exists.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in array or -1 if no such value exists
     */
    fun indexOf(value: T?, identity: Boolean): Int = items.indexOf(value)

    /** Removes the first instance of the specified value in the array.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise
     */
    fun removeValue(value: T?, identity: Boolean): Boolean {
        return items.remove(value)
    }

    /** Removes and returns the item at the specified index.  */
    fun removeIndex(index: Int): T = items.removeAt(index)

    /** Removes and returns the last item.  */
    fun pop(): T = items.removeAt(items.size - 1)

    /** Returns the last item.  */
    fun peek(): T {
        check(size != 0) { "Array is empty." }
        return items[size - 1]
    }

    /** Returns the first item.  */
    fun first(): T {
        check(size != 0) { "Array is empty." }
        return items[0]
    }

    fun clear() {
        items.clear()
    }

    /** Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return [.items]
     */
    fun shrink() {
        if (items.size != size) resize(size)
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return [.items]
     */
    fun ensureCapacity(additionalCapacity: Int) {
    }

    /** Sets the array size, leaving any values beyond the current size null.
     * @return [.items]
     */
    fun setSize(newSize: Int): JArray<T> {
        truncate(newSize)
        if (newSize > items.size) resize(kotlin.math.max(8, newSize))
        return this
    }

    /** Creates a new backing array with the specified size containing the current items.  */
    @PublishedApi
    internal fun resize(newSize: Int) {
        this.size = newSize
    }

    /** Returns an iterator for the items in the array. Remove is supported.
     *
     *
     * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called.
     * Use the [ArrayIterator] constructor for nested or multithreaded iteration.  */
    override fun iterator(): MutableListIterator<T> = items.listIterator()

    /** Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.  */
    fun truncate(newSize: Int) {
        require(newSize >= 0) { "newSize must be >= 0: $newSize" }
        while (items.size > newSize) {
            items.removeAt(items.size - 1)
        }
    }

    override fun hashCode(): Int = items.hashCode()

    /** Returns false if either array is unordered.  */
    override fun equals(that: Any?): Boolean {
        if (that === this) return true
        if (!ordered) return false
        if (that !is JArray<*>) return false
        return this.items.equals(that.items)
    }

    override fun toString(): String {
        if (size == 0) return "[]"
        val buffer = StringBuilder(32)
        buffer.append('[')
        appendItems(buffer, ", ")
        buffer.append(']')
        return buffer.toString()
    }

    fun toString(separator: String): String {
        if (size == 0) return ""
        val buffer = StringBuilder(32)
        appendItems(buffer, separator)
        return buffer.toString()
    }

    private fun appendItems(buffer: Appendable, separator: String) {
        buffer.append(items[0].toString())
        for (i in 1 until size) {
            buffer.append(separator)
            buffer.append(items[i].toString())
        }
    }

    companion object {
        fun <T> arraycopy(src: JArray<T>, srcPos: Int, dest: JArray<T>, destPos: Int, length: Int) {
            com.soywiz.kmem.arraycopy(src.items, srcPos, dest.items, destPos, length)
        }
    }

}
/** Creates an ordered array with a capacity of 16.  */
