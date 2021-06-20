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

import org.luaj.vm2.internal.*
import kotlin.js.*
import kotlin.math.*
import kotlin.native.concurrent.*

/**
 * Base class for functions implemented in Java.
 *
 *
 * Direct subclass include [org.luaj.vm2.lib.LibFunction]
 * which is the base class for
 * all built-in library functions coded in Java,
 * and [LuaClosure], which represents a lua closure
 * whose bytecode is interpreted when the function is invoked.
 * @see LuaValue
 *
 * @see LuaClosure
 *
 * @see org.luaj.vm2.lib.LibFunction
 */
abstract class LuaFunction : LuaValue() {

    override fun type(): Int {
        return LuaValue.TFUNCTION
    }

    override fun typename(): String {
        return "function"
    }

    override fun isfunction(): Boolean {
        return true
    }

    override fun checkfunction(): LuaFunction? {
        return this
    }

    override fun optfunction(defval: LuaFunction?): LuaFunction? {
        return this
    }

    override fun getmetatable(): LuaValue? {
        return s_metatable
    }

    override fun tojstring(): String {
        return "function: " + classnamestub()
    }

    override fun strvalue(): LuaString? {
        return LuaValue.valueOf(tojstring())
    }

    /** Return the last part of the class name, to be used as a function name in tojstring and elsewhere.
     * @return String naming the last part of the class name after the last dot (.) or dollar sign ($).
     */
    fun classnamestub(): String {
        val s = this::class.portableName
        return s.substring(max(s.lastIndexOf('.'), s.lastIndexOf('$')) + 1)
    }

    /** Return a human-readable name for this function.  Returns the last part of the class name by default.
     * Is overridden by LuaClosure to return the source file and line, and by LibFunctions to return the name.
     * @return common name for this function.
     */
    @JsName("fname")
    open fun name(): String {
        return classnamestub()
    }

    companion object {

        /** Shared static metatable for all functions and closures.  */
        var s_metatable: LuaValue?
            get() = LuaFunction_metatable
            set(value) {
                LuaFunction_metatable = value
            }
    }
}


@ThreadLocal
private var LuaFunction_metatable: LuaValue? = null
