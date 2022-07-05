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

/**
 * String buffer for use in string library methods, optimized for production
 * of StrValue instances.
 *
 *
 * The buffer can begin initially as a wrapped [LuaValue]
 * and only when concatenation actually occurs are the bytes first copied.
 *
 *
 * To convert back to a [LuaValue] again,
 * the function [Buffer.value] is used.
 * @see LuaValue
 *
 * @see LuaValue.buffer
 * @see LuaString
 */
class Buffer {

    /** Bytes in this buffer  */
    private var bytes: ByteArray

    /** Length of this buffer  */
    private var length: Int = 0

    /** Offset into the byte array  */
    private var offset: Int = 0

    /** Value of this buffer, when not represented in bytes  */
    private var value: LuaValue? = null

    /**
     * Create buffer with specified initial capacity
     * @param initialCapacity the initial capacity
     */
    @JvmOverloads
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) {
        bytes = ByteArray(initialCapacity)
        length = 0
        offset = 0
        value = null
    }

    /**
     * Create buffer with specified initial value
     * @param value the initial value
     */
    constructor(value: LuaValue) {
        bytes = NOBYTES
        offset = 0
        length = offset
        this.value = value
    }

    /**
     * Get buffer contents as a [LuaValue]
     * @return value as a [LuaValue], converting as necessary
     */
    fun value(): LuaValue {
        return if (value != null) value!! else this.tostring()
    }

    /**
     * Set buffer contents as a [LuaValue]
     * @param value value to set
     */
    fun setvalue(value: LuaValue): Buffer {
        bytes = NOBYTES
        length = 0
        offset = length
        this.value = value
        return this
    }

    /**
     * Convert the buffer to a [LuaString]
     * @return the value as a [LuaString]
     */
    fun tostring(): LuaString {
        realloc(length, 0)
        return LuaString.valueOf(bytes, offset, length)
    }

    /**
     * Convert the buffer to a Java String
     * @return the value as a Java String
     */
    fun tojstring(): String {
        return value().tojstring()
    }

    /**
     * Convert the buffer to a Java String
     * @return the value as a Java String
     */
    override fun toString(): String {
        return tojstring()
    }

    /**
     * Append a single byte to the buffer.
     * @return `this` to allow call chaining
     */
    fun append(b: Byte): Buffer {
        makeroom(0, 1)
        bytes!![offset + length++] = b
        return this
    }

    /**
     * Append a [LuaValue] to the buffer.
     * @return `this` to allow call chaining
     */
    fun append(`val`: LuaValue): Buffer {
        append(`val`.strvalue()!!)
        return this
    }

    /**
     * Append a [LuaString] to the buffer.
     * @return `this` to allow call chaining
     */
    fun append(str: LuaString): Buffer {
        val n = str.m_length
        makeroom(0, n)
        str.copyInto(0, bytes, offset + length, n)
        length += n
        return this
    }

    /**
     * Append a Java String to the buffer.
     * The Java string will be converted to bytes using the UTF8 encoding.
     * @return `this` to allow call chaining
     * @see LuaString.encodeToUtf8
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun append(str: String): Buffer {
        val c = str.toCharArray()
        val n = LuaString.lengthAsUtf8(c)
        makeroom(0, n)
        LuaString.encodeToUtf8(c, c.size, bytes, offset + length)
        length += n
        return this
    }

    /** Concatenate this buffer onto a [LuaValue]
     * @param lhs the left-hand-side value onto which we are concatenating `this`
     * @return [Buffer] for use in call chaining.
     */
    fun concatTo(lhs: LuaValue): Buffer {
        return setvalue(lhs.concat(value()))
    }

    /** Concatenate this buffer onto a [LuaString]
     * @param lhs the left-hand-side value onto which we are concatenating `this`
     * @return [Buffer] for use in call chaining.
     */
    fun concatTo(lhs: LuaString): Buffer {
        return if (value != null && !value!!.isstring()) setvalue(lhs.concat(value!!)) else prepend(lhs)
    }

    /** Concatenate this buffer onto a [LuaNumber]
     *
     *
     * The [LuaNumber] will be converted to a string before concatenating.
     * @param lhs the left-hand-side value onto which we are concatenating `this`
     * @return [Buffer] for use in call chaining.
     */
    fun concatTo(lhs: LuaNumber): Buffer {
        return if (value != null && !value!!.isstring()) setvalue(lhs.concat(value!!)) else prepend(lhs.strvalue()!!)
    }

    /** Concatenate bytes from a [LuaString] onto the front of this buffer
     * @param s the left-hand-side value which we will concatenate onto the front of `this`
     * @return [Buffer] for use in call chaining.
     */
    fun prepend(s: LuaString): Buffer {
        val n = s.m_length
        makeroom(n, 0)
        arraycopy(s.m_bytes, s.m_offset, bytes!!, offset - n, n)
        offset -= n
        length += n
        value = null
        return this
    }

    /** Ensure there is enough room before and after the bytes.
     * @param nbefore number of unused bytes which must precede the data after this completes
     * @param nafter number of unused bytes which must follow the data after this completes
     */
    fun makeroom(nbefore: Int, nafter: Int) {
        if (value != null) {
            val s = value!!.strvalue()
            value = null
            length = s!!.m_length
            offset = nbefore
            bytes = ByteArray(nbefore + length + nafter)
            arraycopy(s.m_bytes, s.m_offset, bytes!!, offset, length)
        } else if (offset + length + nafter > bytes!!.size || offset < nbefore) {
            val n = nbefore + length + nafter
            val m = if (n < 32) 32 else if (n < length * 2) length * 2 else n
            realloc(m, if (nbefore == 0) 0 else m - length - nafter)
        }
    }

    /** Reallocate the internal storage for the buffer
     * @param newSize the size of the buffer to use
     * @param newOffset the offset to use
     */
    private fun realloc(newSize: Int, newOffset: Int) {
        if (newSize != bytes!!.size) {
            val newBytes = ByteArray(newSize)
            arraycopy(bytes!!, offset, newBytes, newOffset, length)
            bytes = newBytes
            offset = newOffset
        }
    }

    companion object {

        /** Default capacity for a buffer: 64  */
        private val DEFAULT_CAPACITY = 64

        /** Shared static array with no bytes  */
        private val NOBYTES = byteArrayOf()
    }

}
/**
 * Create buffer with default capacity
 * @see .DEFAULT_CAPACITY
 */
