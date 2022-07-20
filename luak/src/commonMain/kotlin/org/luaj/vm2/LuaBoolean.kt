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

import kotlin.native.concurrent.*

/**
 * Extension of [LuaValue] which can hold a Java boolean as its value.
 *
 *
 * These instance are not instantiated directly by clients.
 * Instead, there are exactly twon instances of this class,
 * [LuaValue.BTRUE] and [LuaValue.BFALSE]
 * representing the lua values `true` and `false`.
 * The function [LuaValue.valueOf] will always
 * return one of these two values.
 *
 *
 * Any [LuaValue] can be converted to its equivalent
 * boolean representation using [LuaValue.toboolean]
 *
 *
 * @see LuaValue
 *
 * @see LuaValue.valueOf
 * @see LuaValue.BTRUE
 *
 * @see LuaValue.BFALSE
 */
class LuaBoolean internal constructor(
    /** The value of the boolean  */
    val v: Boolean
) : LuaValue() {

    override fun type(): Int = LuaValue.TBOOLEAN
    override fun typename(): String = "boolean"
    override fun isboolean(): Boolean = true
    override fun not(): LuaValue = if (v) LuaValue.BFALSE else LuaValue.BTRUE

    /**
     * Return the boolean value for this boolean
     * @return value as a Java boolean
     */
    fun booleanValue(): Boolean = v
    override fun toboolean(): Boolean = v
    override fun tojstring(): String = if (v) "true" else "false"
    override fun optboolean(defval: Boolean): Boolean = this.v
    override fun checkboolean(): Boolean = v
    override fun getmetatable(): LuaValue? = s_metatable

    companion object {
        /** The singleton instance representing lua `true`  */
        @kotlin.jvm.JvmField internal val _TRUE = LuaBoolean(true)
        /** The singleton instance representing lua `false`  */
        @kotlin.jvm.JvmField internal val _FALSE = LuaBoolean(false)
        /** Shared static metatable for boolean values represented in lua.  */
        var s_metatable: LuaValue?
            get() = LuaBoolean_metatable
            set(value) {
                LuaBoolean_metatable = value
            }
    }
}

@ThreadLocal
private var LuaBoolean_metatable: LuaValue? = null
