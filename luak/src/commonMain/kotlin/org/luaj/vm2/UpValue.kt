/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
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


/** Upvalue used with Closure formulation
 *
 *
 * @see LuaClosure
 *
 * @see Prototype
 */
/**
 * Create an upvalue relative to a stack
 * @param stack the stack
 * @param index the index on the stack for the upvalue
 */
class UpValue(
    @kotlin.jvm.JvmField internal var array: Array<LuaValue?>, // initially the stack, becomes a holder
    @kotlin.jvm.JvmField internal var index: Int
) {

    /**
     * Get the value of the upvalue
     * @return the [LuaValue] for this upvalue
     */
    /**
     * Set the value of the upvalue
     * @param value the [LuaValue] to set it to
     */
    var value: LuaValue?
        get() = array[index]
        set(value) { array[index] = value }

    override fun toString(): String = "$index/${array.size} ${array[index]}"

    /**
     * Convert this upvalue to a Java String
     * @return the Java String for this upvalue.
     * @see LuaValue.tojstring
     */
    fun tojstring(): String = array[index]?.tojstring() ?: "null"

    /**
     * Close this upvalue so it is no longer on the stack
     */
    fun close() {
        val old = array
        array = arrayOf(old[index])
        old[index] = null
        index = 0
    }
}
