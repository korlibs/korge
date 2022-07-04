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
package org.luaj.vm2.lib

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/** Abstract base class for Java function implementations that takes varaiable arguments and
 * returns multiple return values.
 *
 *
 * Subclasses need only implement [LuaValue.invoke] to complete this class,
 * simplifying development.
 * All other uses of [.call], [.invoke],etc,
 * are routed through this method by this class,
 * converting arguments to [Varargs] and
 * dropping or extending return values with `nil` values as required.
 *
 *
 * If between one and three arguments are required, and only one return value is returned,
 * [ZeroArgFunction], [OneArgFunction], [TwoArgFunction], or [ThreeArgFunction].
 *
 *
 * See [LibFunction] for more information on implementation libraries and library functions.
 * @see .invoke
 * @see LibFunction
 *
 * @see ZeroArgFunction
 *
 * @see OneArgFunction
 *
 * @see TwoArgFunction
 *
 * @see ThreeArgFunction
 */
abstract class VarArgFunction : LibFunction() {

    override fun call(): LuaValue {
        return invoke(LuaValue.NONE).arg1()
    }

    override fun call(arg: LuaValue): LuaValue {
        return invoke(arg).arg1()
    }

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        return invoke(LuaValue.varargsOf(arg1, arg2)).arg1()
    }

    override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
        return invoke(LuaValue.varargsOf(arg1, arg2, arg3)).arg1()
    }

    /**
     * Subclass responsibility.
     * May not have expected behavior for tail calls.
     * Should not be used if:
     * - function has a possibility of returning a TailcallVarargs
     * @param args the arguments to the function call.
     */
    override fun invoke(args: Varargs): Varargs {
        return onInvoke(args).eval()
    }

    override fun onInvoke(args: Varargs): Varargs {
        return invoke(args)
    }
} 
