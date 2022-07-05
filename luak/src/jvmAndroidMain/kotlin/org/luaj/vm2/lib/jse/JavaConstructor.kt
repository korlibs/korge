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

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

/**
 * LuaValue that represents a particular public Java constructor.
 *
 *
 * May be called with arguments to return a JavaInstance
 * created by calling the constructor.
 *
 *
 * This class is not used directly.
 * It is returned by calls to [JavaClass.new]
 * when the value of key is "new".
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
internal class JavaConstructor private constructor(val constructor: Constructor<*>) :
    JavaMember(constructor.parameterTypes, constructor.modifiers) {

    override fun invoke(args: Varargs): Varargs {
        val a = convertArgs(args)
        try {
            return CoerceJavaToLua.coerce(constructor.newInstance(*a))
        } catch (e: InvocationTargetException) {
            throw LuaError(e.targetException)
        } catch (e: Exception) {
            return LuaValue.error("coercion error $e")
        }

    }

    /**
     * LuaValue that represents an overloaded Java constructor.
     *
     *
     * On invocation, will pick the best method from the list, and invoke it.
     *
     *
     * This class is not used directly.
     * It is returned by calls to calls to [JavaClass.get]
     * when key is "new" and there is more than one public constructor.
     */
    internal class Overload(val constructors: Array<JavaConstructor>) : VarArgFunction() {

        override fun invoke(args: Varargs): Varargs {
            var best: JavaConstructor? = null
            var score = CoerceLuaToJava.SCORE_UNCOERCIBLE
            for (i in constructors.indices) {
                val s = constructors[i].score(args)
                if (s < score) {
                    score = s
                    best = constructors[i]
                    if (score == 0)
                        break
                }
            }

            // any match?
            if (best == null)
                LuaValue.error("no coercible public method")

            // invoke it
            return best!!.invoke(args)
        }
    }

    companion object {

        @kotlin.jvm.JvmField val constructors: MutableMap<Constructor<*>, JavaConstructor> = HashMap()

         fun forConstructor(c: Constructor<*>): JavaConstructor {
            return constructors[c] ?: return JavaConstructor(c).also { constructors[c] = it }
        }

         fun forConstructors(array: Array<JavaConstructor>): LuaValue {
            return Overload(array)
        }
    }
}
