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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * Helper class to coerce values from Java to lua within the luajava library.
 *
 *
 * This class is primarily used by the [LuajavaLib],
 * but can also be used directly when working with Java/lua bindings.
 *
 *
 * To coerce scalar types, the various, generally the `valueOf(type)` methods
 * on [LuaValue] may be used:
 *
 *  * [LuaValue.valueOf]
 *  * [LuaValue.valueOf]
 *  * [LuaValue.valueOf]
 *  * [LuaValue.valueOf]
 *  * [LuaValue.valueOf]
 *
 *
 *
 * To coerce arrays of objects and lists, the `listOf(..)` and `tableOf(...)` methods
 * on [LuaValue] may be used:
 *
 *  * [LuaValue.listOf]
 *  * [LuaValue.listOf]
 *  * [LuaValue.tableOf]
 *  * [LuaValue.tableOf]
 *
 * The method [CoerceJavaToLua.coerce] looks as the type and dimesioning
 * of the argument and tries to guess the best fit for corrsponding lua scalar,
 * table, or table of tables.
 *
 * @see CoerceJavaToLua.coerce
 * @see LuajavaLib
 */
object CoerceJavaToLua {


    internal val COERCIONS: MutableMap<Class<*>, Coercion> = HashMap()

    internal val instanceCoercion: Coercion = InstanceCoercion()

    internal val arrayCoercion: Coercion = ArrayCoercion()

    internal val luaCoercion: Coercion = LuaCoercion()

    internal interface Coercion {
        fun coerce(javaValue: Any): LuaValue
    }

    private class BoolCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            val b = javaValue as Boolean
            return if (b) LuaValue.BTRUE else LuaValue.BFALSE
        }
    }

    private class IntCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            val n = javaValue as Number
            return LuaInteger.valueOf(n.toInt())
        }
    }

    private class CharCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            val c = javaValue as Char
            return LuaInteger.valueOf(c.toInt())
        }
    }

    private class DoubleCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            val n = javaValue as Number
            return LuaDouble.valueOf(n.toDouble())
        }
    }

    private class StringCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            return LuaString.valueOf(javaValue.toString())
        }
    }

    private class BytesCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            return LuaValue.valueOf(javaValue as ByteArray)
        }
    }

    private class ClassCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            return JavaClass.forClass(javaValue as Class<*>)
        }
    }

    private class InstanceCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            return JavaInstance(javaValue)
        }
    }

    private class ArrayCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            // should be userdata?
            return JavaArray(javaValue)
        }
    }

    private class LuaCoercion : Coercion {
        override fun coerce(javaValue: Any): LuaValue {
            return javaValue as LuaValue
        }
    }

    init {
        val boolCoercion = BoolCoercion()
        val intCoercion = IntCoercion()
        val charCoercion = CharCoercion()
        val doubleCoercion = DoubleCoercion()
        val stringCoercion = StringCoercion()
        val bytesCoercion = BytesCoercion()
        val classCoercion = ClassCoercion()
        COERCIONS[Boolean::class.javaObjectType] = boolCoercion
        COERCIONS[Byte::class.javaObjectType] = intCoercion
        COERCIONS[Char::class.javaObjectType] = charCoercion
        COERCIONS[Short::class.javaObjectType] = intCoercion
        COERCIONS[Int::class.javaObjectType] = intCoercion
        COERCIONS[Long::class.javaObjectType] = doubleCoercion
        COERCIONS[Float::class.javaObjectType] = doubleCoercion
        COERCIONS[Double::class.javaObjectType] = doubleCoercion
        COERCIONS[String::class.javaObjectType] = stringCoercion
        COERCIONS[ByteArray::class.javaObjectType] = bytesCoercion
        COERCIONS[Class::class.javaObjectType] = classCoercion
    }

    /**
     * Coerse a Java object to a corresponding lua value.
     *
     *
     * Integral types `boolean`, `byte`,  `char`, and `int`
     * will become [LuaInteger];
     * `long`, `float`, and `double` will become [LuaDouble];
     * `String` and `byte[]` will become [LuaString];
     * types inheriting from [LuaValue] will be returned without coercion;
     * other types will become [LuaUserdata].
     * @param o Java object needing conversion
     * @return [LuaValue] corresponding to the supplied Java value.
     * @see LuaValue
     *
     * @see LuaInteger
     *
     * @see LuaDouble
     *
     * @see LuaString
     *
     * @see LuaUserdata
     */
    fun coerce(o: Any?): LuaValue {
        if (o == null)
            return LuaValue.NIL
        val clazz = o.javaClass
        var c: Coercion? = COERCIONS[clazz]
        if (c == null) {
            c = when {
                clazz.isArray -> arrayCoercion
                o is LuaValue -> luaCoercion
                else -> instanceCoercion
            }
            COERCIONS[clazz] = c
        }
        return c.coerce(o)
    }
}
