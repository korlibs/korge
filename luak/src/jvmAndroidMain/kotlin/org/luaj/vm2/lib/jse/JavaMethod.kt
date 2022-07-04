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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/**
 * LuaValue that represents a Java method.
 *
 *
 * Can be invoked via call(LuaValue...) and related methods.
 *
 *
 * This class is not used directly.
 * It is returned by calls to calls to [JavaInstance.get]
 * when a method is named.
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
internal class JavaMethod private constructor(val method: Method) :
    JavaMember(method.parameterTypes, method.modifiers) {

    init {
        try {
            if (!method.isAccessible)
                method.isAccessible = true
        } catch (s: SecurityException) {
        }

    }

    override fun call(): LuaValue {
        return LuaValue.error("method cannot be called without instance")
    }

    override fun call(arg: LuaValue): LuaValue {
        return invokeMethod(arg.checkuserdata(), LuaValue.NONE)
    }

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        return invokeMethod(arg1.checkuserdata(), arg2)
    }

    override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
        return invokeMethod(arg1.checkuserdata(), LuaValue.varargsOf(arg2, arg3))
    }

    override fun invoke(args: Varargs): Varargs {
        return invokeMethod(args.checkuserdata(1), args.subargs(2))
    }

    fun invokeMethod(instance: Any?, args: Varargs): LuaValue {
        val a = convertArgs(args)
        try {
            return CoerceJavaToLua.coerce(method.invoke(instance, *a))
        } catch (e: InvocationTargetException) {
            throw LuaError(e.targetException)
        } catch (e: Exception) {
            return LuaValue.error("coercion error $e")
        }

    }

    /**
     * LuaValue that represents an overloaded Java method.
     *
     *
     * On invocation, will pick the best method from the list, and invoke it.
     *
     *
     * This class is not used directly.
     * It is returned by calls to calls to [JavaInstance.get]
     * when an overloaded method is named.
     */
    internal class Overload(val methods: Array<JavaMethod>) : LuaFunction() {

        override fun call(): LuaValue {
            return LuaValue.error("method cannot be called without instance")
        }

        override fun call(arg: LuaValue): LuaValue {
            return invokeBestMethod(arg.checkuserdata(), LuaValue.NONE)
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            return invokeBestMethod(arg1.checkuserdata(), arg2)
        }

        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            return invokeBestMethod(arg1.checkuserdata(), LuaValue.varargsOf(arg2, arg3))
        }

        override fun invoke(args: Varargs): Varargs {
            return invokeBestMethod(args.checkuserdata(1), args.subargs(2))
        }

        private fun invokeBestMethod(instance: Any?, args: Varargs): LuaValue {
            var best: JavaMethod? = null
            var score = CoerceLuaToJava.SCORE_UNCOERCIBLE
            for (i in methods.indices) {
                val s = methods[i].score(args)
                if (s < score) {
                    score = s
                    best = methods[i]
                    if (score == 0)
                        break
                }
            }

            // any match?
            if (best == null)
                LuaValue.error("no coercible public method")

            // invoke it
            return best!!.invokeMethod(instance, args)
        }
    }

    companion object {

        @kotlin.jvm.JvmField
        val methods: MutableMap<Method, JavaMethod> = HashMap()

         fun forMethod(m: Method): JavaMethod {
            return methods[m] ?: return JavaMethod(m).also { methods[m] = it }
        }

         fun forMethods(m: Array<JavaMethod>): LuaFunction {
            return Overload(m)
        }
    }

}
