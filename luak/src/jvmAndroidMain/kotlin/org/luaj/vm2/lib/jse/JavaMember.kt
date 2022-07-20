/*******************************************************************************
 * Copyright (c) 2011 Luaj.org. All rights reserved.
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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.Varargs
import org.luaj.vm2.internal.*
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceLuaToJava.Coercion
import kotlin.math.*

/**
 * Java method or constructor.
 *
 *
 * Primarily handles argument coercion for parameter lists including scoring of compatibility and
 * java varargs handling.
 *
 *
 * This class is not used directly.
 * It is an abstract base class for [JavaConstructor] and [JavaMethod].
 * @see JavaConstructor
 *
 * @see JavaMethod
 *
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
internal abstract class JavaMember protected constructor(params: Array<Class<*>>, modifiers: Int) : VarArgFunction() {

    private val isvarargs = modifiers and METHOD_MODIFIERS_VARARGS != 0
    @kotlin.jvm.JvmField
    val fixedargs: Array<Coercion> =
        Array(if (isvarargs) params.size - 1 else params.size) { CoerceLuaToJava.getCoercion(params[it]) }
    @kotlin.jvm.JvmField
    val varargs: Coercion? = if (isvarargs) CoerceLuaToJava.getCoercion(params[params.size - 1]) else null

    fun score(args: Varargs): Int {
        val n = args.narg()
        var s = if (n > fixedargs.size) CoerceLuaToJava.SCORE_WRONG_TYPE * (n - fixedargs.size) else 0
        for (j in fixedargs.indices) s += fixedargs[j].score(args.arg(j + 1))
        if (varargs != null) {
            for (k in fixedargs.size until n) s += varargs.score(args.arg(k + 1))
        }
        return s
    }

    protected fun convertArgs(args: Varargs): Array<Any> {
        val a: Array<Any?> = if (varargs == null) {
            Array(fixedargs.size) { fixedargs[it].coerce(args.arg(it + 1)) }
        } else {
            val n = max(fixedargs.size, args.narg())
            arrayOfNulls<Any>(n).also { a ->
                for (i in fixedargs.indices) a[i] = fixedargs[i].coerce(args.arg(i + 1))
                for (i in fixedargs.size until n) a[i] = varargs.coerce(args.arg(i + 1))
            }
        }
        return a as Array<Any>
    }

    companion object {
        const val METHOD_MODIFIERS_VARARGS = 0x80
    }
}
