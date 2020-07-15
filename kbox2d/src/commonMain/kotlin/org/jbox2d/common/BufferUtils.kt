package org.jbox2d.common

import org.jbox2d.internal.*

object BufferUtils {
    /** Reallocate a buffer.  */

    fun <T : Any> reallocateBuffer(
        klass: () -> T, oldBuffer: Array<T>?, oldCapacity: Int,
        newCapacity: Int
    ): Array<T> {
        assert(newCapacity > oldCapacity)
        return Array<Any>(newCapacity) { if (oldBuffer != null && it < oldCapacity) oldBuffer[it] else klass() } as Array<T>
    }

    /** Reallocate a buffer.  */

    fun reallocateBuffer(oldBuffer: IntArray?, oldCapacity: Int, newCapacity: Int): IntArray {
        assert(newCapacity > oldCapacity)
        val newBuffer = IntArray(newCapacity)
        if (oldBuffer != null) {
            arraycopy(oldBuffer, 0, newBuffer, 0, oldCapacity)
        }
        return newBuffer
    }

    /** Reallocate a buffer.  */

    fun reallocateBuffer(oldBuffer: FloatArray?, oldCapacity: Int, newCapacity: Int): FloatArray {
        assert(newCapacity > oldCapacity)
        val newBuffer = FloatArray(newCapacity)
        if (oldBuffer != null) {
            arraycopy(oldBuffer, 0, newBuffer, 0, oldCapacity)
        }
        return newBuffer
    }

    /**
     * Reallocate a buffer. A 'deferred' buffer is reallocated only if it is not NULL. If
     * 'userSuppliedCapacity' is not zero, buffer is user supplied and must be kept.
     */

    fun <T : Any> reallocateBuffer(
        klass: () -> T, buffer: Array<T>?, userSuppliedCapacity: Int,
        oldCapacity: Int, newCapacity: Int, deferred: Boolean
    ): Array<T> {
        var buffer = buffer
        assert(newCapacity > oldCapacity)
        assert(userSuppliedCapacity == 0 || newCapacity <= userSuppliedCapacity)
        if ((!deferred || buffer != null) && userSuppliedCapacity == 0) {
            buffer = reallocateBuffer(klass, buffer, oldCapacity, newCapacity)
        }
        return buffer!!
    }

    /**
     * Reallocate an int buffer. A 'deferred' buffer is reallocated only if it is not NULL. If
     * 'userSuppliedCapacity' is not zero, buffer is user supplied and must be kept.
     */

    fun reallocateBuffer(
        buffer: IntArray?, userSuppliedCapacity: Int, oldCapacity: Int,
        newCapacity: Int, deferred: Boolean
    ): IntArray {
        var buffer = buffer
        assert(newCapacity > oldCapacity)
        assert(userSuppliedCapacity == 0 || newCapacity <= userSuppliedCapacity)
        if ((!deferred || buffer != null) && userSuppliedCapacity == 0) {
            buffer = reallocateBuffer(buffer, oldCapacity, newCapacity)
        }
        return buffer!!
    }

    /**
     * Reallocate a float buffer. A 'deferred' buffer is reallocated only if it is not NULL. If
     * 'userSuppliedCapacity' is not zero, buffer is user supplied and must be kept.
     */

    fun reallocateBuffer(
        buffer: FloatArray?, userSuppliedCapacity: Int, oldCapacity: Int,
        newCapacity: Int, deferred: Boolean
    ): FloatArray {
        var buffer = buffer
        assert(newCapacity > oldCapacity)
        assert(userSuppliedCapacity == 0 || newCapacity <= userSuppliedCapacity)
        if ((!deferred || buffer != null) && userSuppliedCapacity == 0) {
            buffer = reallocateBuffer(buffer, oldCapacity, newCapacity)
        }
        return buffer!!
    }

    /** Rotate an array, see std::rotate  */

    fun <T> rotate(ray: Array<T>, first: Int, new_first: Int, last: Int) {
        var first = first
        var new_first = new_first
        var next = new_first
        while (next != first) {
            val temp = ray[first]
            ray[first] = ray[next]
            ray[next] = temp
            first++
            next++
            if (next == last) {
                next = new_first
            } else if (first == new_first) {
                new_first = next
            }
        }
    }

    /** Rotate an array, see std::rotate  */

    fun rotate(ray: IntArray, first: Int, new_first: Int, last: Int) {
        var first = first
        var new_first = new_first
        var next = new_first
        while (next != first) {
            val temp = ray[first]
            ray[first] = ray[next]
            ray[next] = temp
            first++
            next++
            if (next == last) {
                next = new_first
            } else if (first == new_first) {
                new_first = next
            }
        }
    }

    /** Rotate an array, see std::rotate  */

    fun rotate(ray: FloatArray, first: Int, new_first: Int, last: Int) {
        var first = first
        var new_first = new_first
        var next = new_first
        while (next != first) {
            val temp = ray[first]
            ray[first] = ray[next]
            ray[next] = temp
            first++
            next++
            if (next == last) {
                next = new_first
            } else if (first == new_first) {
                new_first = next
            }
        }
    }
}
