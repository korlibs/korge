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

/** Interface used to select items within an iterator against a predicate.
 * @author Xoppa
 */
interface Predicate<T> {

    /** @return true if the item matches the criteria and should be included in the iterator's items
     */
    fun evaluate(arg0: T): Boolean

    class PredicateIterator<T>(iterator: MutableIterator<T?>, predicate: Predicate<T?>) : MutableIterator<T?> {
        lateinit var iterator: MutableIterator<T?>
        lateinit var predicate: Predicate<T?>
        var end = false
        var peeked = false
        var next: T? = null

        constructor(iterable: MutableIterable<T>, predicate: Predicate<T?>) : this(iterable.iterator(), predicate) {}

        init {
            set(iterator, predicate)
        }

        fun set(iterable: MutableIterable<T?>, predicate: Predicate<T?>) {
            set(iterable.iterator(), predicate)
        }

        fun set(iterator: MutableIterator<T?>, predicate: Predicate<T?>) {
            this.iterator = iterator
            this.predicate = predicate
            peeked = false
            end = peeked
            next = null
        }

        override fun hasNext(): Boolean {
            if (end) return false
            if (next != null) return true
            peeked = true
            while (iterator.hasNext()) {
                val n = iterator.next()
                if (predicate.evaluate(n)) {
                    next = n
                    return true
                }
            }
            end = true
            return false
        }

        override fun next(): T? {
            if (next == null && !hasNext()) return null
            val result = next
            next = null
            peeked = false
            return result
        }

        override fun remove() {
            if (peeked) throw GdxRuntimeException("Cannot remove between a call to hasNext() and next().")
            iterator.remove()
        }
    }

    class PredicateIterable<T>(iterable: MutableIterable<T?>, predicate: Predicate<T?>) : MutableIterable<T?> {
        lateinit var iterable: MutableIterable<T?>
        lateinit var predicate: Predicate<T?>
        var iterator: PredicateIterator<T?>? = null

        init {
            set(iterable, predicate)
        }

        operator fun set(iterable: MutableIterable<T?>, predicate: Predicate<T?>) {
            this.iterable = iterable
            this.predicate = predicate
        }

        /** Returns an iterator. Remove is supported.
         *
         *
         * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use
         * the [Predicate.PredicateIterator] constructor for nested or multithreaded iteration.  */
        override fun iterator(): MutableIterator<T?> {
            if (Collections.allocateIterators) return PredicateIterator(iterable.iterator(), predicate)
            if (iterator == null)
                iterator = PredicateIterator(iterable.iterator(), predicate)
            else
                iterator!!.set(iterable.iterator(), predicate)
            return iterator!!
        }
    }
}
