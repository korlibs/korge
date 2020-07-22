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

import com.soywiz.kds.iterators.*

/** A resizable, ordered or unordered array of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 * @author Nathan Sweet
 */
class JArray<T>(capacity: Int = 16) : Iterable<T> {
    /** Provides direct access to the underlying array. If the Array's generic type is not Object, this field may only be accessed
     * if the [JArray.JArray] constructor was used.  */
    internal var items: MutableList<T> = ArrayList(capacity)

    var size: Int
        set(newSize) = truncate(newSize)
        get() = items.size

    inline fun fastForEach(block: (T) -> Unit) {
        var n = 0
        while (n < size) {
            block(this[n])
            n++
        }
    }

    fun add(value: T) {
        items.add(value)
    }

    operator fun get(index: Int): T = items[index]

    fun set(index: Int, value: T) {
        if (index >= items.size) {
            val items = this.items as MutableList<Any?>
            while (items.size <= index) items.add(null)
        }
        items[index] = value
    }

    fun indexOf(value: T?, identity: Boolean): Int {
        if (identity) {
            items.fastForEachWithIndex { index, current -> if (current === value) return index }
            return -1
        } else {
            return items.indexOf(value)
        }
    }
    fun contains(value: T?, identity: Boolean): Boolean = indexOf(value, identity) >= 0
    fun removeValue(value: T?, identity: Boolean): Boolean {
        val index = indexOf(value, identity)
        val found = index >= 0
        if (found) removeIndex(index)
        return found
    }
    fun removeIndex(index: Int): T = items.removeAt(index)
    fun pop(): T = items.removeLast()
    fun peek(): T = items.last()
    fun first(): T = items.first()
    fun clear() = run { items.clear() }
    fun shrink() = run { if (items.size != size) resize(size) }
    fun ensureCapacity(additionalCapacity: Int) = Unit
    fun setSize(newSize: Int): JArray<T> {
        this.size = kotlin.math.max(8, newSize)
        return this
    }
    fun resize(newSize: Int) = run { this.size = newSize }
    override fun iterator(): MutableListIterator<T> = items.listIterator()

    fun truncate(newSize: Int) {
        require(newSize >= 0) { "newSize must be >= 0: $newSize" }
        while (items.size > newSize) items.removeAt(items.size - 1)
    }

    override fun hashCode(): Int = items.hashCode()

    override fun equals(that: Any?): Boolean {
        if (that === this) return true
        if (that !is JArray<*>) return false
        return this.items == that.items
    }

    override fun toString(): String = items.toString()
}
/** Creates an ordered array with a capacity of 16.  */
