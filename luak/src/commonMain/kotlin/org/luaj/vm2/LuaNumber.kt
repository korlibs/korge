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
 * Base class for representing numbers as lua values directly.
 *
 *
 * The main subclasses are [LuaInteger] which holds values that fit in a java int,
 * and [LuaDouble] which holds all other number values.
 * @see LuaInteger
 *
 * @see LuaDouble
 *
 * @see LuaValue
 */
abstract class LuaNumber : LuaValue() {

    override fun type(): Int = LuaValue.TNUMBER
    override fun typename(): String = "number"
    override fun checknumber(): LuaNumber? = this
    override fun checknumber(errmsg: String): LuaNumber = this
    override fun optnumber(defval: LuaNumber?): LuaNumber? = this
    override fun tonumber(): LuaValue = this
    override fun isnumber(): Boolean = true
    override fun isstring(): Boolean = true
    override fun getmetatable(): LuaValue? = s_metatable
    override fun concat(rhs: LuaValue): LuaValue = rhs.concatTo(this)
    override fun concat(rhs: Buffer): Buffer = rhs.concatTo(this)
    override fun concatTo(lhs: LuaNumber): LuaValue = strvalue()!!.concatTo(lhs.strvalue()!!)
    override fun concatTo(lhs: LuaString): LuaValue = strvalue()!!.concatTo(lhs)

    companion object {
        /** Shared static metatable for all number values represented in lua.  */
        var s_metatable: LuaValue?
            get() = LuaNumber_metatable
            set(value) {
                LuaNumber_metatable = value
            }
    }

}

@ThreadLocal
private var LuaNumber_metatable: LuaValue? = null
