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
import kotlin.reflect.*

/**
 * Class to encapsulate behavior of the singleton instance `nil`
 *
 *
 * There will be one instance of this class, [LuaValue.NIL],
 * per Java virtual machine.
 * However, the [Varargs] instance [LuaValue.NONE]
 * which is the empty list,
 * is also considered treated as a nil value by default.
 *
 *
 * Although it is possible to test for nil using Java == operator,
 * the recommended approach is to use the method [LuaValue.isnil]
 * instead.  By using that any ambiguities between
 * [LuaValue.NIL] and [LuaValue.NONE] are avoided.
 * @see LuaValue
 *
 * @see LuaValue.NIL
 */
open class LuaNil internal constructor() : LuaValue() {

    override fun type(): Int = LuaValue.TNIL
    override fun toString(): String = "nil"
    override fun typename(): String = "nil"
    override fun tojstring(): String = "nil"
    override fun not(): LuaValue = LuaValue.BTRUE
    override fun toboolean(): Boolean = false
    override fun isnil(): Boolean = true
    override fun getmetatable(): LuaValue? = s_metatable
    override fun equals(o: Any?): Boolean = o is LuaNil
    override fun checknotnil(): LuaValue = argerror("value")
    override fun isvalidkey(): Boolean = false

    // optional argument conversions - nil alwas falls badk to default value
    override fun optboolean(defval: Boolean): Boolean = defval
    override fun optclosure(defval: LuaClosure?): LuaClosure? = defval
    override fun optdouble(defval: Double): Double = defval
    override fun optfunction(defval: LuaFunction?): LuaFunction? = defval
    override fun optint(defval: Int): Int = defval
    override fun optinteger(defval: LuaInteger?): LuaInteger? = defval
    override fun optlong(defval: Long): Long = defval
    override fun optnumber(defval: LuaNumber?): LuaNumber? = defval
    override fun opttable(defval: LuaTable?): LuaTable? = defval
    override fun optthread(defval: LuaThread?): LuaThread? = defval
    override fun optjstring(defval: String?): String? = defval
    override fun optstring(defval: LuaString?): LuaString? = defval
    override fun optuserdata(defval: Any?): Any? = defval
    override fun optuserdata(c: KClass<*>, defval: Any?): Any? = defval
    override fun optvalue(defval: LuaValue): LuaValue = defval

    companion object {
        @kotlin.jvm.JvmField internal val _NIL = LuaNil()
        var s_metatable: LuaValue?
            get() = LuaNil_metatable
            set(value) {
                LuaNil_metatable = value
            }
    }
}

@ThreadLocal
private var LuaNil_metatable: LuaValue? = null
