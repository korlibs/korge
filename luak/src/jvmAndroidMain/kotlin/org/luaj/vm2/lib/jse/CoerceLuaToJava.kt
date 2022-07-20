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

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Helper class to coerce values from lua to Java within the luajava library.
 *
 *
 * This class is primarily used by the [LuajavaLib],
 * but can also be used directly when working with Java/lua bindings.
 *
 *
 * To coerce to specific Java values, generally the `toType()` methods
 * on [LuaValue] may be used:
 *
 *  * [LuaValue.toboolean]
 *  * [LuaValue.tobyte]
 *  * [LuaValue.tochar]
 *  * [LuaValue.toshort]
 *  * [LuaValue.toint]
 *  * [LuaValue.tofloat]
 *  * [LuaValue.todouble]
 *  * [LuaValue.tojstring]
 *  * [LuaValue.touserdata]
 *  * [LuaValue.touserdata]
 *
 *
 *
 * For data in lua tables, the various methods on [LuaTable] can be used directly
 * to convert data to something more useful.
 *
 * @see LuajavaLib
 *
 * @see CoerceJavaToLua
 */
object CoerceLuaToJava {

    internal var SCORE_NULL_VALUE = 0x10
    internal var SCORE_WRONG_TYPE = 0x100
    internal var SCORE_UNCOERCIBLE = 0x10000

    internal val COERCIONS: MutableMap<Class<*>, Coercion> = HashMap()

    internal interface Coercion {
        fun score(value: LuaValue): Int
        fun coerce(value: LuaValue): Any?
    }

    /**
     * Coerce a LuaValue value to a specified java class
     * @param value LuaValue to coerce
     * @param clazz Class to coerce into
     * @return Object of type clazz (or a subclass) with the corresponding value.
     */
    fun coerce(value: LuaValue, clazz: Class<*>): Any? {
        return getCoercion(clazz).coerce(value)
    }

    internal class BoolCoercion : Coercion {
        override fun toString(): String {
            return "BoolCoercion()"
        }

        override fun score(value: LuaValue): Int {
            when (value.type()) {
                LuaValue.TBOOLEAN -> return 0
            }
            return 1
        }

        override fun coerce(value: LuaValue): Any {
            return value.toboolean()
        }
    }

    internal class NumericCoercion(val targetType: Int) : Coercion {
        override fun toString(): String {
            return "NumericCoercion(" + TYPE_NAMES[targetType] + ")"
        }

        override fun score(value: LuaValue): Int {
            var value = value
            var fromStringPenalty = 0
            if (value.type() == LuaValue.TSTRING) {
                value = value.tonumber()
                if (value.isnil()) {
                    return SCORE_UNCOERCIBLE
                }
                fromStringPenalty = 4
            }
            return if (value.isint()) {
                when (targetType) {
                    TARGET_TYPE_BYTE -> {
                        val i = value.toint()
                        fromStringPenalty + if (i == i.toByte().toInt()) 0 else SCORE_WRONG_TYPE
                    }
                    TARGET_TYPE_CHAR -> {
                        val i = value.toint()
                        fromStringPenalty + if (i == i.toByte().toInt()) 1 else if (i == i.toChar().toInt()) 0 else SCORE_WRONG_TYPE
                    }
                    TARGET_TYPE_SHORT -> {
                        val i = value.toint()
                        fromStringPenalty + if (i == i.toByte().toInt()) 1 else if (i == i.toShort().toInt()) 0 else SCORE_WRONG_TYPE
                    }
                    TARGET_TYPE_INT -> {
                        val i = value.toint()
                        fromStringPenalty + if (i == i.toByte().toInt()) 2 else if (i == i.toChar().toInt() || i == i.toShort().toInt()) 1 else 0
                    }
                    TARGET_TYPE_FLOAT -> fromStringPenalty + 1
                    TARGET_TYPE_LONG -> fromStringPenalty + 1
                    TARGET_TYPE_DOUBLE -> fromStringPenalty + 2
                    else -> SCORE_WRONG_TYPE
                }
            } else if (value.isnumber()) {
                when (targetType) {
                    TARGET_TYPE_BYTE -> SCORE_WRONG_TYPE
                    TARGET_TYPE_CHAR -> SCORE_WRONG_TYPE
                    TARGET_TYPE_SHORT -> SCORE_WRONG_TYPE
                    TARGET_TYPE_INT -> SCORE_WRONG_TYPE
                    TARGET_TYPE_LONG -> {
                        val d = value.todouble()
                        fromStringPenalty + if (d == d.toLong().toDouble()) 0 else SCORE_WRONG_TYPE
                    }
                    TARGET_TYPE_FLOAT -> {
                        val d = value.todouble()
                        fromStringPenalty + if (d == d.toFloat().toDouble()) 0 else SCORE_WRONG_TYPE
                    }
                    TARGET_TYPE_DOUBLE -> {
                        val d = value.todouble()
                        fromStringPenalty + if (d == d.toLong().toDouble() || d == d.toFloat().toDouble()) 1 else 0
                    }
                    else -> SCORE_WRONG_TYPE
                }
            } else {
                SCORE_UNCOERCIBLE
            }
        }

        override fun coerce(value: LuaValue): Any? {
            when (targetType) {
                TARGET_TYPE_BYTE -> return value.toint().toByte()
                TARGET_TYPE_CHAR -> return value.toint().toChar()
                TARGET_TYPE_SHORT -> return value.toint().toShort()
                TARGET_TYPE_INT -> return value.toint()
                TARGET_TYPE_LONG -> return value.todouble().toLong()
                TARGET_TYPE_FLOAT -> return value.todouble().toFloat()
                TARGET_TYPE_DOUBLE -> return value.todouble()
                else -> return null
            }
        }

        companion object {
            val TARGET_TYPE_BYTE = 0
            val TARGET_TYPE_CHAR = 1
            val TARGET_TYPE_SHORT = 2
            val TARGET_TYPE_INT = 3
            val TARGET_TYPE_LONG = 4
            val TARGET_TYPE_FLOAT = 5
            val TARGET_TYPE_DOUBLE = 6
            val TYPE_NAMES = arrayOf("byte", "char", "short", "int", "long", "float", "double")
        }
    }

    internal class StringCoercion(val targetType: Int) : Coercion {
        override fun toString(): String {
            return "StringCoercion(" + (if (targetType == TARGET_TYPE_STRING) "String" else "byte[]") + ")"
        }

        override fun score(value: LuaValue): Int {
            when (value.type()) {
                LuaValue.TSTRING -> return if (value.checkstring()!!.isValidUtf8)
                    if (targetType == TARGET_TYPE_STRING) 0 else 1
                else
                    if (targetType == TARGET_TYPE_BYTES) 0 else SCORE_WRONG_TYPE
                LuaValue.TNIL -> return SCORE_NULL_VALUE
                else -> return if (targetType == TARGET_TYPE_STRING) SCORE_WRONG_TYPE else SCORE_UNCOERCIBLE
            }
        }

        override fun coerce(value: LuaValue): Any? {
            if (value.isnil())
                return null
            if (targetType == TARGET_TYPE_STRING)
                return value.tojstring()
            val s = value.checkstring()
            val b = ByteArray(s!!.m_length)
            s.copyInto(0, b, 0, b.size)
            return b
        }

        companion object {
            val TARGET_TYPE_STRING = 0
            val TARGET_TYPE_BYTES = 1
        }
    }

    internal class ArrayCoercion(val componentType: Class<*>) : Coercion {
        val componentCoercion: Coercion

        init {
            this.componentCoercion = getCoercion(componentType)
        }

        override fun toString(): String {
            return "ArrayCoercion(" + componentType.name + ")"
        }

        override fun score(value: LuaValue): Int {
            when (value.type()) {
                LuaValue.TTABLE -> return if (value.length() == 0) 0 else componentCoercion.score(value[1])
                LuaValue.TUSERDATA -> return inheritanceLevels(
                    componentType,
                    value.touserdata()!!.javaClass.componentType
                )
                LuaValue.TNIL -> return SCORE_NULL_VALUE
                else -> return SCORE_UNCOERCIBLE
            }
        }

        override fun coerce(value: LuaValue): Any? {
            when (value.type()) {
                LuaValue.TTABLE -> {
                    val n = value.length()
                    val a = java.lang.reflect.Array.newInstance(componentType, n)
                    for (i in 0 until n)
                        java.lang.reflect.Array.set(a, i, componentCoercion.coerce(value[i + 1]))
                    return a
                }
                LuaValue.TUSERDATA -> return value.touserdata()
                LuaValue.TNIL -> return null
                else -> return null
            }

        }
    }

    /**
     * Determine levels of inheritance between a base class and a subclass
     * @param baseclass base class to look for
     * @param subclass class from which to start looking
     * @return number of inheritance levels between subclass and baseclass,
     * or SCORE_UNCOERCIBLE if not a subclass
     */
    internal fun inheritanceLevels(baseclass: Class<*>, subclass: Class<*>?): Int {
        if (subclass == null)
            return SCORE_UNCOERCIBLE
        if (baseclass == subclass)
            return 0
        var min = Math.min(SCORE_UNCOERCIBLE, inheritanceLevels(baseclass, subclass.superclass) + 1)
        val ifaces = subclass.interfaces
        for (i in ifaces.indices)
            min = Math.min(min, inheritanceLevels(baseclass, ifaces[i]) + 1)
        return min
    }

    internal class ObjectCoercion(val targetType: Class<*>) : Coercion {
        override fun toString(): String {
            return "ObjectCoercion(" + targetType.name + ")"
        }

        override fun score(value: LuaValue): Int {
            when (value.type()) {
                LuaValue.TNUMBER -> return inheritanceLevels(
                    targetType,
                    if (value.isint()) Int::class.java else Double::class.java
                )
                LuaValue.TBOOLEAN -> return inheritanceLevels(targetType, Boolean::class.java)
                LuaValue.TSTRING -> return inheritanceLevels(targetType, String::class.java)
                LuaValue.TUSERDATA -> return inheritanceLevels(targetType, value.touserdata()!!.javaClass)
                LuaValue.TNIL -> return SCORE_NULL_VALUE
                else -> return inheritanceLevels(targetType, value.javaClass)
            }
        }

        override fun coerce(value: LuaValue): Any? {
            when (value.type()) {
                LuaValue.TNUMBER -> return if (value.isint()) value.toint() else value.todouble()
                LuaValue.TBOOLEAN -> return value.toboolean()
                LuaValue.TSTRING -> return value.tojstring()
                LuaValue.TUSERDATA -> return value.optuserdata(targetType.kotlin, null)
                LuaValue.TNIL -> return null
                else -> return value
            }
        }
    }

    init {
        val boolCoercion = BoolCoercion()
        val byteCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_BYTE)
        val charCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_CHAR)
        val shortCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_SHORT)
        val intCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_INT)
        val longCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_LONG)
        val floatCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_FLOAT)
        val doubleCoercion = NumericCoercion(NumericCoercion.TARGET_TYPE_DOUBLE)
        val stringCoercion = StringCoercion(StringCoercion.TARGET_TYPE_STRING)
        val bytesCoercion = StringCoercion(StringCoercion.TARGET_TYPE_BYTES)

        COERCIONS[Boolean::class.javaPrimitiveType!!] = boolCoercion
        COERCIONS[Boolean::class.javaObjectType] = boolCoercion
        COERCIONS[Byte::class.javaPrimitiveType!!] = byteCoercion
        COERCIONS[Byte::class.javaObjectType] = byteCoercion
        COERCIONS[Char::class.javaPrimitiveType!!] = charCoercion
        COERCIONS[Char::class.javaObjectType] = charCoercion
        COERCIONS[Short::class.javaPrimitiveType!!] = shortCoercion
        COERCIONS[Short::class.javaObjectType] = shortCoercion
        COERCIONS[Int::class.javaPrimitiveType!!] = intCoercion
        COERCIONS[Int::class.javaObjectType] = intCoercion
        COERCIONS[Long::class.javaPrimitiveType!!] = longCoercion
        COERCIONS[Long::class.javaObjectType] = longCoercion
        COERCIONS[Float::class.javaPrimitiveType!!] = floatCoercion
        COERCIONS[Float::class.javaObjectType] = floatCoercion
        COERCIONS[Double::class.javaPrimitiveType!!] = doubleCoercion
        COERCIONS[Double::class.javaObjectType] = doubleCoercion
        COERCIONS[String::class.java] = stringCoercion
        COERCIONS[ByteArray::class.java] = bytesCoercion
    }

    internal fun getCoercion(c: Class<*>): Coercion {
        var co: Coercion? = COERCIONS[c]
        if (co != null) {
            return co
        }
        if (c.isArray) {
            val typ = c.componentType
            co = ArrayCoercion(c.componentType)
        } else {
            co = ObjectCoercion(c)
        }
        COERCIONS[c] = co
        return co
    }
}
