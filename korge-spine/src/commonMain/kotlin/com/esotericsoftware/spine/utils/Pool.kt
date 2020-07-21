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

/** A pool of objects that can be reused to avoid allocation.
 * @see Pools
 *
 * @author Nathan Sweet
 */
abstract class Pool<T>
/** @param initialCapacity The initial size of the array supporting the pool. No objects are created unless preFill is true.
 * @param max The maximum number of free objects to store in this pool.
 * @param preFill Whether to pre-fill the pool with objects. The number of pre-filled objects will be equal to the initial
 * capacity.
 */
 constructor(
    initialCapacity: Int = 16,
    /** The maximum number of objects that will be pooled.  */
    val max: Int = Int.MAX_VALUE,
    preFill: Boolean = false
) {
    init {
        require(!(initialCapacity > max && preFill)) { "max must be larger than initialCapacity if preFill is set to true." }
    }

    /** The highest number of free objects. Can be reset any time.  */

    var peak: Int = 0

    private val freeObjects: ArrayList<T> = ArrayList<T>(initialCapacity).also { freeObjects ->
        if (preFill) {
            for (i in 0 until initialCapacity)
                freeObjects.add(newObject())
            peak = freeObjects.size
        }
    }

    /** The number of objects available to be obtained.  */
    val free: Int
        get() = freeObjects.size

    protected abstract fun newObject(): T

    /** Returns an object from this pool. The object may be new (from [.newObject]) or reused (previously
     * [freed][.free]).  */
    open fun obtain(): T {
        return if (freeObjects.size == 0) newObject() else freeObjects.removeAt(freeObjects.size - 1)
    }

    /** Puts the specified object in the pool, making it eligible to be returned by [.obtain]. If the pool already contains
     * [.max] free objects, the specified object is reset but not added to the pool.
     *
     *
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.  */
    fun free(`object`: T?) {
        requireNotNull(`object`) { "object cannot be null." }
        if (freeObjects.size < max) {
            freeObjects.add(`object`)
            peak = kotlin.math.max(peak, freeObjects.size)
        }
        reset(`object`)
    }

    /** Adds the specified number of new free objects to the pool. Usually called early on as a pre-allocation mechanism but can be
     * used at any time.
     *
     * @param size the number of objects to be added
     */
    fun fill(size: Int) {
        for (i in 0 until size)
            if (freeObjects.size < max) freeObjects.add(newObject())
        peak = kotlin.math.max(peak, freeObjects.size)
    }

    /** Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
     * [Poolable.reset] if the object is [Poolable].  */
    protected open fun reset(`object`: T) {
        if (`object` is Poolable) (`object` as Poolable).reset()
    }

    /** Puts the specified objects in the pool. Null objects within the array are silently ignored.
     *
     *
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     * @see .free
     */
    fun freeAll(objects: JArray<T>?) {
        requireNotNull(objects) { "objects cannot be null." }
        val freeObjects = this.freeObjects
        val max = this.max
        for (i in 0 until objects.size) {
            val `object` = objects[i] ?: continue
            if (freeObjects.size < max) freeObjects.add(`object`)
            reset(`object`)
        }
        peak = kotlin.math.max(peak, freeObjects.size)
    }

    /** Removes all free objects from this pool.  */
    fun clear() {
        freeObjects.clear()
    }

    /** Objects implementing this interface will have [.reset] called when passed to [Pool.free].  */
    interface Poolable {
        /** Resets the object for reuse. Object references should be nulled and fields may be set to default values.  */
        fun reset()
    }
}
/** Creates a pool with an initial capacity of 16 and no maximum.  */
/** Creates a pool with the specified initial capacity and no maximum.  */
/** @param max The maximum number of free objects to store in this pool.
 */
