/*******************************************************************************
 * Copyright (c) 2010-2011 Luaj.org. All rights reserved.
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

/**
 * Subclass of [Varargs] that represents a lua tail call
 * in a Java library function execution environment.
 *
 *
 * Since Java doesn't have direct support for tail calls,
 * any lua function whose [Prototype] contains the
 * [Lua.OP_TAILCALL] bytecode needs a mechanism
 * for tail calls when converting lua-bytecode to java-bytecode.
 *
 *
 * The tail call holds the next function and arguments,
 * and the client a call to [.eval] executes the function
 * repeatedly until the tail calls are completed.
 *
 *
 * Normally, users of luaj need not concern themselves with the
 * details of this mechanism, as it is built into the core
 * execution framework.
 * @see Prototype
 *
 * @see org.luaj.vm2.luajc.LuaJC
 */
class TailcallVarargs : Varargs {

    private var func: LuaValue? = null
    private var args: Varargs? = null
    private var result: Varargs? = null

    constructor(f: LuaValue, args: Varargs) {
        this.func = f
        this.args = args
    }

    constructor(`object`: LuaValue, methodname: LuaValue, args: Varargs) {
        this.func = `object`.get(methodname)
        this.args = LuaValue.varargsOf(`object`, args)
    }

    override fun isTailcall(): Boolean {
        return true
    }

    override fun eval(): Varargs {
        while (result == null) {
            val r = func!!.onInvoke(args!!)
            if (r.isTailcall()) {
                val t = r as TailcallVarargs
                func = t.func
                args = t.args
            } else {
                result = r
                func = null
                args = null
            }
        }
        return result!!
    }

    override fun arg(i: Int): LuaValue {
        if (result == null) eval()
        return result!!.arg(i)
    }

    override fun arg1(): LuaValue {
        if (result == null) eval()
        return result!!.arg1()
    }

    override fun narg(): Int {
        if (result == null) eval()
        return result!!.narg()
    }

    override fun subargs(start: Int): Varargs {
        if (result == null) eval()
        return result!!.subargs(start)
    }
}
