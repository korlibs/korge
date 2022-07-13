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
package org.luaj.vm2

import kotlin.test.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.lang.reflect.*
import kotlin.reflect.*

class TypeTest {

    private val sampleint = 77
    private val samplelong = 123400000000L
    private val sampledouble = 55.25
    private val samplestringstring = "abcdef"
    private val samplestringint = sampleint.toString()
    private val samplestringlong = samplelong.toString()
    private val samplestringdouble = sampledouble.toString()
    private val sampleobject = Any()
    private val sampledata = MyData()

    private val somenil = LuaValue.NIL
    private val sometrue = LuaValue.BTRUE
    private val somefalse = LuaValue.BFALSE
    private val zero = LuaValue.ZERO
    private val intint = LuaValue.valueOf(sampleint)
    private val longdouble = LuaValue.valueOf(samplelong.toDouble())
    private val doubledouble = LuaValue.valueOf(sampledouble)
    private val stringstring = LuaValue.valueOf(samplestringstring)
    private val stringint = LuaValue.valueOf(samplestringint)
    private val stringlong = LuaValue.valueOf(samplestringlong)
    private val stringdouble = LuaValue.valueOf(samplestringdouble)
    private val table = LuaValue.tableOf()
    private val somefunc = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.NONE
        }
    }
    private val thread = LuaThread(Globals(), somefunc)
    private val someclosure = LuaClosure(Prototype(), LuaTable())
    private val userdataobj = LuaValue.userdataOf(sampleobject)
    private val userdatacls = LuaValue.userdataOf(sampledata)

    class MyData

    // ===================== type checks =======================

    @Test
    fun testIsBoolean() {
        assertEquals(false, somenil.isboolean())
        assertEquals(true, sometrue.isboolean())
        assertEquals(true, somefalse.isboolean())
        assertEquals(false, zero.isboolean())
        assertEquals(false, intint.isboolean())
        assertEquals(false, longdouble.isboolean())
        assertEquals(false, doubledouble.isboolean())
        assertEquals(false, stringstring.isboolean())
        assertEquals(false, stringint.isboolean())
        assertEquals(false, stringlong.isboolean())
        assertEquals(false, stringdouble.isboolean())
        assertEquals(false, thread.isboolean())
        assertEquals(false, table.isboolean())
        assertEquals(false, userdataobj.isboolean())
        assertEquals(false, userdatacls.isboolean())
        assertEquals(false, somefunc.isboolean())
        assertEquals(false, someclosure.isboolean())
    }

    @Test
    fun testIsClosure() {
        assertEquals(false, somenil.isclosure())
        assertEquals(false, sometrue.isclosure())
        assertEquals(false, somefalse.isclosure())
        assertEquals(false, zero.isclosure())
        assertEquals(false, intint.isclosure())
        assertEquals(false, longdouble.isclosure())
        assertEquals(false, doubledouble.isclosure())
        assertEquals(false, stringstring.isclosure())
        assertEquals(false, stringint.isclosure())
        assertEquals(false, stringlong.isclosure())
        assertEquals(false, stringdouble.isclosure())
        assertEquals(false, thread.isclosure())
        assertEquals(false, table.isclosure())
        assertEquals(false, userdataobj.isclosure())
        assertEquals(false, userdatacls.isclosure())
        assertEquals(false, somefunc.isclosure())
        assertEquals(true, someclosure.isclosure())
    }


    @Test
    fun testIsFunction() {
        assertEquals(false, somenil.isfunction())
        assertEquals(false, sometrue.isfunction())
        assertEquals(false, somefalse.isfunction())
        assertEquals(false, zero.isfunction())
        assertEquals(false, intint.isfunction())
        assertEquals(false, longdouble.isfunction())
        assertEquals(false, doubledouble.isfunction())
        assertEquals(false, stringstring.isfunction())
        assertEquals(false, stringint.isfunction())
        assertEquals(false, stringlong.isfunction())
        assertEquals(false, stringdouble.isfunction())
        assertEquals(false, thread.isfunction())
        assertEquals(false, table.isfunction())
        assertEquals(false, userdataobj.isfunction())
        assertEquals(false, userdatacls.isfunction())
        assertEquals(true, somefunc.isfunction())
        assertEquals(true, someclosure.isfunction())
    }

    @Test
    fun testIsInt() {
        assertEquals(false, somenil.isint())
        assertEquals(false, sometrue.isint())
        assertEquals(false, somefalse.isint())
        assertEquals(true, zero.isint())
        assertEquals(true, intint.isint())
        assertEquals(false, longdouble.isint())
        assertEquals(false, doubledouble.isint())
        assertEquals(false, stringstring.isint())
        assertEquals(true, stringint.isint())
        assertEquals(false, stringdouble.isint())
        assertEquals(false, thread.isint())
        assertEquals(false, table.isint())
        assertEquals(false, userdataobj.isint())
        assertEquals(false, userdatacls.isint())
        assertEquals(false, somefunc.isint())
        assertEquals(false, someclosure.isint())
    }

    @Test
    fun testIsIntType() {
        assertEquals(false, somenil.isinttype())
        assertEquals(false, sometrue.isinttype())
        assertEquals(false, somefalse.isinttype())
        assertEquals(true, zero.isinttype())
        assertEquals(true, intint.isinttype())
        assertEquals(false, longdouble.isinttype())
        assertEquals(false, doubledouble.isinttype())
        assertEquals(false, stringstring.isinttype())
        assertEquals(false, stringint.isinttype())
        assertEquals(false, stringlong.isinttype())
        assertEquals(false, stringdouble.isinttype())
        assertEquals(false, thread.isinttype())
        assertEquals(false, table.isinttype())
        assertEquals(false, userdataobj.isinttype())
        assertEquals(false, userdatacls.isinttype())
        assertEquals(false, somefunc.isinttype())
        assertEquals(false, someclosure.isinttype())
    }

    @Test
    fun testIsLong() {
        assertEquals(false, somenil.islong())
        assertEquals(false, sometrue.islong())
        assertEquals(false, somefalse.islong())
        assertEquals(true, intint.isint())
        assertEquals(true, longdouble.islong())
        assertEquals(false, doubledouble.islong())
        assertEquals(false, stringstring.islong())
        assertEquals(true, stringint.islong())
        assertEquals(true, stringlong.islong())
        assertEquals(false, stringdouble.islong())
        assertEquals(false, thread.islong())
        assertEquals(false, table.islong())
        assertEquals(false, userdataobj.islong())
        assertEquals(false, userdatacls.islong())
        assertEquals(false, somefunc.islong())
        assertEquals(false, someclosure.islong())
    }

    @Test
    fun testIsNil() {
        assertEquals(true, somenil.isnil())
        assertEquals(false, sometrue.isnil())
        assertEquals(false, somefalse.isnil())
        assertEquals(false, zero.isnil())
        assertEquals(false, intint.isnil())
        assertEquals(false, longdouble.isnil())
        assertEquals(false, doubledouble.isnil())
        assertEquals(false, stringstring.isnil())
        assertEquals(false, stringint.isnil())
        assertEquals(false, stringlong.isnil())
        assertEquals(false, stringdouble.isnil())
        assertEquals(false, thread.isnil())
        assertEquals(false, table.isnil())
        assertEquals(false, userdataobj.isnil())
        assertEquals(false, userdatacls.isnil())
        assertEquals(false, somefunc.isnil())
        assertEquals(false, someclosure.isnil())
    }

    @Test
    fun testIsNumber() {
        assertEquals(false, somenil.isnumber())
        assertEquals(false, sometrue.isnumber())
        assertEquals(false, somefalse.isnumber())
        assertEquals(true, zero.isnumber())
        assertEquals(true, intint.isnumber())
        assertEquals(true, longdouble.isnumber())
        assertEquals(true, doubledouble.isnumber())
        assertEquals(false, stringstring.isnumber())
        assertEquals(true, stringint.isnumber())
        assertEquals(true, stringlong.isnumber())
        assertEquals(true, stringdouble.isnumber())
        assertEquals(false, thread.isnumber())
        assertEquals(false, table.isnumber())
        assertEquals(false, userdataobj.isnumber())
        assertEquals(false, userdatacls.isnumber())
        assertEquals(false, somefunc.isnumber())
        assertEquals(false, someclosure.isnumber())
    }

    @Test
    fun testIsString() {
        assertEquals(false, somenil.isstring())
        assertEquals(false, sometrue.isstring())
        assertEquals(false, somefalse.isstring())
        assertEquals(true, zero.isstring())
        assertEquals(true, longdouble.isstring())
        assertEquals(true, doubledouble.isstring())
        assertEquals(true, stringstring.isstring())
        assertEquals(true, stringint.isstring())
        assertEquals(true, stringlong.isstring())
        assertEquals(true, stringdouble.isstring())
        assertEquals(false, thread.isstring())
        assertEquals(false, table.isstring())
        assertEquals(false, userdataobj.isstring())
        assertEquals(false, userdatacls.isstring())
        assertEquals(false, somefunc.isstring())
        assertEquals(false, someclosure.isstring())
    }

    @Test
    fun testIsThread() {
        assertEquals(false, somenil.isthread())
        assertEquals(false, sometrue.isthread())
        assertEquals(false, somefalse.isthread())
        assertEquals(false, intint.isthread())
        assertEquals(false, longdouble.isthread())
        assertEquals(false, doubledouble.isthread())
        assertEquals(false, stringstring.isthread())
        assertEquals(false, stringint.isthread())
        assertEquals(false, stringdouble.isthread())
        assertEquals(true, thread.isthread())
        assertEquals(false, table.isthread())
        assertEquals(false, userdataobj.isthread())
        assertEquals(false, userdatacls.isthread())
        assertEquals(false, somefunc.isthread())
        assertEquals(false, someclosure.isthread())
    }

    @Test
    fun testIsTable() {
        assertEquals(false, somenil.istable())
        assertEquals(false, sometrue.istable())
        assertEquals(false, somefalse.istable())
        assertEquals(false, intint.istable())
        assertEquals(false, longdouble.istable())
        assertEquals(false, doubledouble.istable())
        assertEquals(false, stringstring.istable())
        assertEquals(false, stringint.istable())
        assertEquals(false, stringdouble.istable())
        assertEquals(false, thread.istable())
        assertEquals(true, table.istable())
        assertEquals(false, userdataobj.istable())
        assertEquals(false, userdatacls.istable())
        assertEquals(false, somefunc.istable())
        assertEquals(false, someclosure.istable())
    }

    @Test
    fun testIsUserdata() {
        assertEquals(false, somenil.isuserdata())
        assertEquals(false, sometrue.isuserdata())
        assertEquals(false, somefalse.isuserdata())
        assertEquals(false, intint.isuserdata())
        assertEquals(false, longdouble.isuserdata())
        assertEquals(false, doubledouble.isuserdata())
        assertEquals(false, stringstring.isuserdata())
        assertEquals(false, stringint.isuserdata())
        assertEquals(false, stringdouble.isuserdata())
        assertEquals(false, thread.isuserdata())
        assertEquals(false, table.isuserdata())
        assertEquals(true, userdataobj.isuserdata())
        assertEquals(true, userdatacls.isuserdata())
        assertEquals(false, somefunc.isuserdata())
        assertEquals(false, someclosure.isuserdata())
    }

    @Test
    fun testIsUserdataObject() {
        assertEquals(false, somenil.isuserdata(Any::class.java))
        assertEquals(false, sometrue.isuserdata(Any::class.java))
        assertEquals(false, somefalse.isuserdata(Any::class.java))
        assertEquals(false, longdouble.isuserdata(Any::class.java))
        assertEquals(false, doubledouble.isuserdata(Any::class.java))
        assertEquals(false, stringstring.isuserdata(Any::class.java))
        assertEquals(false, stringint.isuserdata(Any::class.java))
        assertEquals(false, stringdouble.isuserdata(Any::class.java))
        assertEquals(false, thread.isuserdata(Any::class.java))
        assertEquals(false, table.isuserdata(Any::class.java))
        assertEquals(true, userdataobj.isuserdata(Any::class.java))
        assertEquals(true, userdatacls.isuserdata(Any::class.java))
        assertEquals(false, somefunc.isuserdata(Any::class.java))
        assertEquals(false, someclosure.isuserdata(Any::class.java))
    }

    @Test
    fun testIsUserdataMyData() {
        assertEquals(false, somenil.isuserdata(MyData::class.java))
        assertEquals(false, sometrue.isuserdata(MyData::class.java))
        assertEquals(false, somefalse.isuserdata(MyData::class.java))
        assertEquals(false, longdouble.isuserdata(MyData::class.java))
        assertEquals(false, doubledouble.isuserdata(MyData::class.java))
        assertEquals(false, stringstring.isuserdata(MyData::class.java))
        assertEquals(false, stringint.isuserdata(MyData::class.java))
        assertEquals(false, stringdouble.isuserdata(MyData::class.java))
        assertEquals(false, thread.isuserdata(MyData::class.java))
        assertEquals(false, table.isuserdata(MyData::class.java))
        assertEquals(false, userdataobj.isuserdata(MyData::class.java))
        assertEquals(true, userdatacls.isuserdata(MyData::class.java))
        assertEquals(false, somefunc.isuserdata(MyData::class.java))
        assertEquals(false, someclosure.isuserdata(MyData::class.java))
    }


    // ===================== Coerce to Java =======================

    @Test
    fun testToBoolean() {
        assertEquals(false, somenil.toboolean())
        assertEquals(true, sometrue.toboolean())
        assertEquals(false, somefalse.toboolean())
        assertEquals(true, zero.toboolean())
        assertEquals(true, intint.toboolean())
        assertEquals(true, longdouble.toboolean())
        assertEquals(true, doubledouble.toboolean())
        assertEquals(true, stringstring.toboolean())
        assertEquals(true, stringint.toboolean())
        assertEquals(true, stringlong.toboolean())
        assertEquals(true, stringdouble.toboolean())
        assertEquals(true, thread.toboolean())
        assertEquals(true, table.toboolean())
        assertEquals(true, userdataobj.toboolean())
        assertEquals(true, userdatacls.toboolean())
        assertEquals(true, somefunc.toboolean())
        assertEquals(true, someclosure.toboolean())
    }

    @Test
    fun testToByte() {
        assertEquals(0.toByte(), somenil.tobyte())
        assertEquals(0.toByte(), somefalse.tobyte())
        assertEquals(0.toByte(), sometrue.tobyte())
        assertEquals(0.toByte(), zero.tobyte())
        assertEquals(sampleint.toByte(), intint.tobyte())
        assertEquals(samplelong.toByte(), longdouble.tobyte())
        assertEquals(sampledouble.toInt().toByte(), doubledouble.tobyte())
        assertEquals(0.toByte(), stringstring.tobyte())
        assertEquals(sampleint.toByte(), stringint.tobyte())
        assertEquals(samplelong.toByte(), stringlong.tobyte())
        assertEquals(sampledouble.toInt().toByte(), stringdouble.tobyte())
        assertEquals(0.toByte(), thread.tobyte())
        assertEquals(0.toByte(), table.tobyte())
        assertEquals(0.toByte(), userdataobj.tobyte())
        assertEquals(0.toByte(), userdatacls.tobyte())
        assertEquals(0.toByte(), somefunc.tobyte())
        assertEquals(0.toByte(), someclosure.tobyte())
    }

    @Test
    fun testToChar() {
        assertEquals(0.toChar(), somenil.tochar())
        assertEquals(0.toChar(), somefalse.tochar())
        assertEquals(0.toChar(), sometrue.tochar())
        assertEquals(0.toChar(), zero.tochar())
        assertEquals(sampleint.toChar().toInt(), intint.tochar().toInt())
        assertEquals(samplelong.toChar().toInt(), longdouble.tochar().toInt())
        assertEquals(sampledouble.toChar().toInt(), doubledouble.tochar().toInt())
        assertEquals(0.toChar(), stringstring.tochar())
        assertEquals(sampleint.toChar().toInt(), stringint.tochar().toInt())
        assertEquals(samplelong.toChar().toInt(), stringlong.tochar().toInt())
        assertEquals(sampledouble.toChar().toInt(), stringdouble.tochar().toInt())
        assertEquals(0.toChar(), thread.tochar())
        assertEquals(0.toChar(), table.tochar())
        assertEquals(0.toChar(), userdataobj.tochar())
        assertEquals(0.toChar(), userdatacls.tochar())
        assertEquals(0.toChar(), somefunc.tochar())
        assertEquals(0.toChar(), someclosure.tochar())
    }

    @Test
    fun testToDouble() {
        assertEquals(0.0, somenil.todouble())
        assertEquals(0.0, somefalse.todouble())
        assertEquals(0.0, sometrue.todouble())
        assertEquals(0.0, zero.todouble())
        assertEquals(sampleint.toDouble(), intint.todouble())
        assertEquals(samplelong.toDouble(), longdouble.todouble())
        assertEquals(sampledouble, doubledouble.todouble())
        assertEquals(0.toDouble(), stringstring.todouble())
        assertEquals(sampleint.toDouble(), stringint.todouble())
        assertEquals(samplelong.toDouble(), stringlong.todouble())
        assertEquals(sampledouble, stringdouble.todouble())
        assertEquals(0.0, thread.todouble())
        assertEquals(0.0, table.todouble())
        assertEquals(0.0, userdataobj.todouble())
        assertEquals(0.0, userdatacls.todouble())
        assertEquals(0.0, somefunc.todouble())
        assertEquals(0.0, someclosure.todouble())
    }

    @Test
    fun testToFloat() {
        assertEquals(0f, somenil.tofloat())
        assertEquals(0f, somefalse.tofloat())
        assertEquals(0f, sometrue.tofloat())
        assertEquals(0f, zero.tofloat())
        assertEquals(sampleint.toFloat(), intint.tofloat())
        assertEquals(samplelong.toFloat(), longdouble.tofloat())
        assertEquals(sampledouble.toFloat(), doubledouble.tofloat())
        assertEquals(0.toFloat(), stringstring.tofloat())
        assertEquals(sampleint.toFloat(), stringint.tofloat())
        assertEquals(samplelong.toFloat(), stringlong.tofloat())
        assertEquals(sampledouble.toFloat(), stringdouble.tofloat())
        assertEquals(0f, thread.tofloat())
        assertEquals(0f, table.tofloat())
        assertEquals(0f, userdataobj.tofloat())
        assertEquals(0f, userdatacls.tofloat())
        assertEquals(0f, somefunc.tofloat())
        assertEquals(0f, someclosure.tofloat())
    }

    @Test
    fun testToInt() {
        assertEquals(0, somenil.toint())
        assertEquals(0, somefalse.toint())
        assertEquals(0, sometrue.toint())
        assertEquals(0, zero.toint())
        assertEquals(sampleint, intint.toint())
        assertEquals(samplelong.toInt(), longdouble.toint())
        assertEquals(sampledouble.toInt(), doubledouble.toint())
        assertEquals(0, stringstring.toint())
        assertEquals(sampleint, stringint.toint())
        assertEquals(samplelong.toInt(), stringlong.toint())
        assertEquals(sampledouble.toInt(), stringdouble.toint())
        assertEquals(0, thread.toint())
        assertEquals(0, table.toint())
        assertEquals(0, userdataobj.toint())
        assertEquals(0, userdatacls.toint())
        assertEquals(0, somefunc.toint())
        assertEquals(0, someclosure.toint())
    }

    @Test
    fun testToLong() {
        assertEquals(0L, somenil.tolong())
        assertEquals(0L, somefalse.tolong())
        assertEquals(0L, sometrue.tolong())
        assertEquals(0L, zero.tolong())
        assertEquals(sampleint.toLong(), intint.tolong())
        assertEquals(samplelong, longdouble.tolong())
        assertEquals(sampledouble.toLong(), doubledouble.tolong())
        assertEquals(0.toLong(), stringstring.tolong())
        assertEquals(sampleint.toLong(), stringint.tolong())
        assertEquals(samplelong, stringlong.tolong())
        assertEquals(sampledouble.toLong(), stringdouble.tolong())
        assertEquals(0L, thread.tolong())
        assertEquals(0L, table.tolong())
        assertEquals(0L, userdataobj.tolong())
        assertEquals(0L, userdatacls.tolong())
        assertEquals(0L, somefunc.tolong())
        assertEquals(0L, someclosure.tolong())
    }

    @Test
    fun testToShort() {
        assertEquals(0.toShort(), somenil.toshort())
        assertEquals(0.toShort(), somefalse.toshort())
        assertEquals(0.toShort(), sometrue.toshort())
        assertEquals(0.toShort(), zero.toshort())
        assertEquals(sampleint.toShort(), intint.toshort())
        assertEquals(samplelong.toShort(), longdouble.toshort())
        assertEquals(sampledouble.toInt().toShort(), doubledouble.toshort())
        assertEquals(0.toShort(), stringstring.toshort())
        assertEquals(sampleint.toShort(), stringint.toshort())
        assertEquals(samplelong.toShort(), stringlong.toshort())
        assertEquals(sampledouble.toInt().toShort(), stringdouble.toshort())
        assertEquals(0.toShort(), thread.toshort())
        assertEquals(0.toShort(), table.toshort())
        assertEquals(0.toShort(), userdataobj.toshort())
        assertEquals(0.toShort(), userdatacls.toshort())
        assertEquals(0.toShort(), somefunc.toshort())
        assertEquals(0.toShort(), someclosure.toshort())
    }

    @Test
    fun testToString() {
        assertEquals("nil", somenil.tojstring())
        assertEquals("false", somefalse.tojstring())
        assertEquals("true", sometrue.tojstring())
        assertEquals("0", zero.tojstring())
        assertEquals(sampleint.toString(), intint.tojstring())
        assertEquals(samplelong.toString(), longdouble.tojstring())
        assertEquals(sampledouble.toString(), doubledouble.tojstring())
        assertEquals(samplestringstring, stringstring.tojstring())
        assertEquals(sampleint.toString(), stringint.tojstring())
        assertEquals(samplelong.toString(), stringlong.tojstring())
        assertEquals(sampledouble.toString(), stringdouble.tojstring())
        assertEquals("thread: ", thread.tojstring().substring(0, 8))
        assertEquals("table: ", table.tojstring().substring(0, 7))
        assertEquals(sampleobject.toString(), userdataobj.tojstring())
        assertEquals(sampledata.toString(), userdatacls.tojstring())
        assertEquals("function: ", somefunc.tojstring().substring(0, 10))
        assertEquals("function: ", someclosure.tojstring().substring(0, 10))
    }

    @Test
    fun testToUserdata() {
        assertEquals(null, somenil.touserdata())
        assertEquals(null, somefalse.touserdata())
        assertEquals(null, sometrue.touserdata())
        assertEquals(null, zero.touserdata())
        assertEquals(null, intint.touserdata())
        assertEquals(null, longdouble.touserdata())
        assertEquals(null, doubledouble.touserdata())
        assertEquals(null, stringstring.touserdata())
        assertEquals(null, stringint.touserdata())
        assertEquals(null, stringlong.touserdata())
        assertEquals(null, stringdouble.touserdata())
        assertEquals(null, thread.touserdata())
        assertEquals(null, table.touserdata())
        assertEquals(sampleobject, userdataobj.touserdata())
        assertEquals(sampledata, userdatacls.touserdata())
        assertEquals(null, somefunc.touserdata())
        assertEquals(null, someclosure.touserdata())
    }


    // ===================== Optional argument conversion =======================


    private fun throwsError(obj: LuaValue, method: String, argtype: Class<*>?, argument: Any) {
        try {
            obj.javaClass.getMethod(method, argtype).invoke(obj, argument)
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

        fail("failed to throw LuaError as required")
    }

    @Test
    fun testOptBoolean() {
        assertEquals(true, somenil.optboolean(true))
        assertEquals(false, somenil.optboolean(false))
        assertEquals(true, sometrue.optboolean(false))
        assertEquals(false, somefalse.optboolean(true))
        throwsError(zero, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(intint, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(longdouble, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(doubledouble, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(somefunc, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(someclosure, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(stringstring, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(stringint, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(stringlong, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(stringdouble, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(thread, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(table, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(userdataobj, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
        throwsError(userdatacls, "optboolean", Boolean::class.javaPrimitiveType, java.lang.Boolean.FALSE)
    }

    @Test
    fun testOptClosure() {
        assertEquals(someclosure, somenil.optclosure(someclosure))
        assertEquals(null, somenil.optclosure(null))
        throwsError(sometrue, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(somefalse, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(zero, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(intint, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(longdouble, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(doubledouble, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(somefunc, "optclosure", LuaClosure::class.java, someclosure)
        assertEquals(someclosure, someclosure.optclosure(someclosure))
        assertEquals(someclosure, someclosure.optclosure(null))
        throwsError(stringstring, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(stringint, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(stringlong, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(stringdouble, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(thread, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(table, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(userdataobj, "optclosure", LuaClosure::class.java, someclosure)
        throwsError(userdatacls, "optclosure", LuaClosure::class.java, someclosure)
    }

    @Test
    fun testOptDouble() {
        assertEquals(33.0, somenil.optdouble(33.0))
        throwsError(sometrue, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(somefalse, "optdouble", Double::class.javaPrimitiveType, 33.0)
        assertEquals(0.0, zero.optdouble(33.0))
        assertEquals(sampleint.toDouble(), intint.optdouble(33.0))
        assertEquals(samplelong.toDouble(), longdouble.optdouble(33.0))
        assertEquals(sampledouble, doubledouble.optdouble(33.0))
        throwsError(somefunc, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(someclosure, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(stringstring, "optdouble", Double::class.javaPrimitiveType, 33.0)
        assertEquals(sampleint.toDouble(), stringint.optdouble(33.0))
        assertEquals(samplelong.toDouble(), stringlong.optdouble(33.0))
        assertEquals(sampledouble, stringdouble.optdouble(33.0))
        throwsError(thread, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(table, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(userdataobj, "optdouble", Double::class.javaPrimitiveType, 33.0)
        throwsError(userdatacls, "optdouble", Double::class.javaPrimitiveType, 33.0)
    }

    @Test
    fun testOptFunction() {
        assertEquals(somefunc, somenil.optfunction(somefunc))
        assertEquals(null, somenil.optfunction(null))
        throwsError(sometrue, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(somefalse, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(zero, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(intint, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(longdouble, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(doubledouble, "optfunction", LuaFunction::class.java, somefunc)
        assertEquals(somefunc, somefunc.optfunction(null))
        assertEquals(someclosure, someclosure.optfunction(null))
        assertEquals(somefunc, somefunc.optfunction(somefunc))
        assertEquals(someclosure, someclosure.optfunction(somefunc))
        throwsError(stringstring, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(stringint, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(stringlong, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(stringdouble, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(thread, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(table, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(userdataobj, "optfunction", LuaFunction::class.java, somefunc)
        throwsError(userdatacls, "optfunction", LuaFunction::class.java, somefunc)
    }

    @Test
    fun testOptInt() {
        assertEquals(33, somenil.optint(33))
        throwsError(sometrue, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(somefalse, "optint", Int::class.javaPrimitiveType, 33)
        assertEquals(0, zero.optint(33))
        assertEquals(sampleint, intint.optint(33))
        assertEquals(samplelong.toInt(), longdouble.optint(33))
        assertEquals(sampledouble.toInt(), doubledouble.optint(33))
        throwsError(somefunc, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(someclosure, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(stringstring, "optint", Int::class.javaPrimitiveType, 33)
        assertEquals(sampleint, stringint.optint(33))
        assertEquals(samplelong.toInt(), stringlong.optint(33))
        assertEquals(sampledouble.toInt(), stringdouble.optint(33))
        throwsError(thread, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(table, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(userdataobj, "optint", Int::class.javaPrimitiveType, 33)
        throwsError(userdatacls, "optint", Int::class.javaPrimitiveType, 33)
    }

    @Test
    fun testOptInteger() {
        assertEquals(LuaValue.valueOf(33), somenil.optinteger(LuaValue.valueOf(33)))
        throwsError(sometrue, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(somefalse, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        assertEquals(zero, zero.optinteger(LuaValue.valueOf(33)) as LuaNumber?)
        assertEquals(LuaValue.valueOf(sampleint), intint.optinteger(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(samplelong.toInt()), longdouble.optinteger(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(sampledouble.toInt()), doubledouble.optinteger(LuaValue.valueOf(33)))
        throwsError(somefunc, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(someclosure, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(stringstring, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        assertEquals(LuaValue.valueOf(sampleint), stringint.optinteger(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(samplelong.toInt()), stringlong.optinteger(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(sampledouble.toInt()), stringdouble.optinteger(LuaValue.valueOf(33)))
        throwsError(thread, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(table, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(userdataobj, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
        throwsError(userdatacls, "optinteger", LuaInteger::class.java, LuaValue.valueOf(33))
    }

    @Test
    fun testOptLong() {
        assertEquals(33L, somenil.optlong(33))
        throwsError(sometrue, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(somefalse, "optlong", Long::class.javaPrimitiveType, 33)
        assertEquals(0L, zero.optlong(33))
        assertEquals(sampleint.toLong(), intint.optlong(33))
        assertEquals(samplelong, longdouble.optlong(33))
        assertEquals(sampledouble.toLong(), doubledouble.optlong(33))
        throwsError(somefunc, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(someclosure, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(stringstring, "optlong", Long::class.javaPrimitiveType, 33)
        assertEquals(sampleint.toLong(), stringint.optlong(33))
        assertEquals(samplelong, stringlong.optlong(33))
        assertEquals(sampledouble.toLong(), stringdouble.optlong(33))
        throwsError(thread, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(table, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(userdataobj, "optlong", Long::class.javaPrimitiveType, 33)
        throwsError(userdatacls, "optlong", Long::class.javaPrimitiveType, 33)
    }

    @Test
    fun testOptNumber() {
        assertEquals(LuaValue.valueOf(33), somenil.optnumber(LuaValue.valueOf(33)))
        throwsError(sometrue, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(somefalse, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        assertEquals(zero, zero.optnumber(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(sampleint), intint.optnumber(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(samplelong.toDouble()), longdouble.optnumber(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(sampledouble), doubledouble.optnumber(LuaValue.valueOf(33)))
        throwsError(somefunc, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(someclosure, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(stringstring, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        assertEquals(LuaValue.valueOf(sampleint), stringint.optnumber(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(samplelong.toDouble()), stringlong.optnumber(LuaValue.valueOf(33)))
        assertEquals(LuaValue.valueOf(sampledouble), stringdouble.optnumber(LuaValue.valueOf(33)))
        throwsError(thread, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(table, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(userdataobj, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
        throwsError(userdatacls, "optnumber", LuaNumber::class.java, LuaValue.valueOf(33))
    }

    @Test
    fun testOptTable() {
        assertEquals(table, somenil.opttable(table))
        assertEquals(null, somenil.opttable(null))
        throwsError(sometrue, "opttable", LuaTable::class.java, table)
        throwsError(somefalse, "opttable", LuaTable::class.java, table)
        throwsError(zero, "opttable", LuaTable::class.java, table)
        throwsError(intint, "opttable", LuaTable::class.java, table)
        throwsError(longdouble, "opttable", LuaTable::class.java, table)
        throwsError(doubledouble, "opttable", LuaTable::class.java, table)
        throwsError(somefunc, "opttable", LuaTable::class.java, table)
        throwsError(someclosure, "opttable", LuaTable::class.java, table)
        throwsError(stringstring, "opttable", LuaTable::class.java, table)
        throwsError(stringint, "opttable", LuaTable::class.java, table)
        throwsError(stringlong, "opttable", LuaTable::class.java, table)
        throwsError(stringdouble, "opttable", LuaTable::class.java, table)
        throwsError(thread, "opttable", LuaTable::class.java, table)
        assertEquals(table, table.opttable(table))
        assertEquals(table, table.opttable(null))
        throwsError(userdataobj, "opttable", LuaTable::class.java, table)
        throwsError(userdatacls, "opttable", LuaTable::class.java, table)
    }

    @Test
    fun testOptThread() {
        assertEquals(thread, somenil.optthread(thread))
        assertEquals(null, somenil.optthread(null))
        throwsError(sometrue, "optthread", LuaThread::class.java, thread)
        throwsError(somefalse, "optthread", LuaThread::class.java, thread)
        throwsError(zero, "optthread", LuaThread::class.java, thread)
        throwsError(intint, "optthread", LuaThread::class.java, thread)
        throwsError(longdouble, "optthread", LuaThread::class.java, thread)
        throwsError(doubledouble, "optthread", LuaThread::class.java, thread)
        throwsError(somefunc, "optthread", LuaThread::class.java, thread)
        throwsError(someclosure, "optthread", LuaThread::class.java, thread)
        throwsError(stringstring, "optthread", LuaThread::class.java, thread)
        throwsError(stringint, "optthread", LuaThread::class.java, thread)
        throwsError(stringlong, "optthread", LuaThread::class.java, thread)
        throwsError(stringdouble, "optthread", LuaThread::class.java, thread)
        throwsError(table, "optthread", LuaThread::class.java, thread)
        assertEquals(thread, thread.optthread(thread))
        assertEquals(thread, thread.optthread(null))
        throwsError(userdataobj, "optthread", LuaThread::class.java, thread)
        throwsError(userdatacls, "optthread", LuaThread::class.java, thread)
    }

    @Test
    fun testOptJavaString() {
        assertEquals("xyz", somenil.optjstring("xyz"))
        assertEquals(null, somenil.optjstring(null))
        throwsError(sometrue, "optjstring", String::class.java, "xyz")
        throwsError(somefalse, "optjstring", String::class.java, "xyz")
        assertEquals(zero.toString(), zero.optjstring("xyz"))
        assertEquals(intint.toString(), intint.optjstring("xyz"))
        assertEquals(longdouble.toString(), longdouble.optjstring("xyz"))
        assertEquals(doubledouble.toString(), doubledouble.optjstring("xyz"))
        throwsError(somefunc, "optjstring", String::class.java, "xyz")
        throwsError(someclosure, "optjstring", String::class.java, "xyz")
        assertEquals(samplestringstring, stringstring.optjstring("xyz"))
        assertEquals(samplestringint, stringint.optjstring("xyz"))
        assertEquals(samplestringlong, stringlong.optjstring("xyz"))
        assertEquals(samplestringdouble, stringdouble.optjstring("xyz"))
        throwsError(thread, "optjstring", String::class.java, "xyz")
        throwsError(table, "optjstring", String::class.java, "xyz")
        throwsError(userdataobj, "optjstring", String::class.java, "xyz")
        throwsError(userdatacls, "optjstring", String::class.java, "xyz")
    }

    @Test
    fun testOptLuaString() {
        assertEquals(LuaValue.valueOf("xyz"), somenil.optstring(LuaValue.valueOf("xyz")))
        assertEquals(null, somenil.optstring(null))
        throwsError(sometrue, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        throwsError(somefalse, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        assertEquals(LuaValue.valueOf("0"), zero.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringint, intint.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringlong, longdouble.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringdouble, doubledouble.optstring(LuaValue.valueOf("xyz")))
        throwsError(somefunc, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        throwsError(someclosure, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        assertEquals(stringstring, stringstring.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringint, stringint.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringlong, stringlong.optstring(LuaValue.valueOf("xyz")))
        assertEquals(stringdouble, stringdouble.optstring(LuaValue.valueOf("xyz")))
        throwsError(thread, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        throwsError(table, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        throwsError(userdataobj, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
        throwsError(userdatacls, "optstring", LuaString::class.java, LuaValue.valueOf("xyz"))
    }

    @Test
    fun testOptUserdata() {
        assertEquals(sampleobject, somenil.optuserdata(sampleobject))
        assertEquals(sampledata, somenil.optuserdata(sampledata))
        assertEquals(null, somenil.optuserdata(null))
        throwsError(sometrue, "optuserdata", Any::class.java, sampledata)
        throwsError(somefalse, "optuserdata", Any::class.java, sampledata)
        throwsError(zero, "optuserdata", Any::class.java, sampledata)
        throwsError(intint, "optuserdata", Any::class.java, sampledata)
        throwsError(longdouble, "optuserdata", Any::class.java, sampledata)
        throwsError(doubledouble, "optuserdata", Any::class.java, sampledata)
        throwsError(somefunc, "optuserdata", Any::class.java, sampledata)
        throwsError(someclosure, "optuserdata", Any::class.java, sampledata)
        throwsError(stringstring, "optuserdata", Any::class.java, sampledata)
        throwsError(stringint, "optuserdata", Any::class.java, sampledata)
        throwsError(stringlong, "optuserdata", Any::class.java, sampledata)
        throwsError(stringdouble, "optuserdata", Any::class.java, sampledata)
        throwsError(table, "optuserdata", Any::class.java, sampledata)
        assertEquals(sampleobject, userdataobj.optuserdata(sampledata))
        assertEquals(sampleobject, userdataobj.optuserdata(null))
        assertEquals(sampledata, userdatacls.optuserdata(sampleobject))
        assertEquals(sampledata, userdatacls.optuserdata(null))
    }

    private fun throwsErrorOptUserdataClass(obj: LuaValue, arg1: Class<*>, arg2: Any) {
        try {
            obj.javaClass.getMethod("optuserdata", KClass::class.java, Any::class.java).invoke(obj, arg1.kotlin, arg2)
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

        fail("failed to throw LuaError as required")
    }

    @Test
    fun testOptUserdataClass() {
        assertEquals(sampledata, somenil.optuserdata(MyData::class.java, sampledata))
        assertEquals(sampleobject, somenil.optuserdata(Any::class.java, sampleobject))
        assertEquals(null, somenil.optuserdata(null))
        throwsErrorOptUserdataClass(sometrue, Any::class.java, sampledata)
        throwsErrorOptUserdataClass(zero, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(intint, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(longdouble, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(somefunc, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(someclosure, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(stringstring, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(stringint, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(stringlong, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(stringlong, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(stringdouble, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(table, MyData::class.java, sampledata)
        throwsErrorOptUserdataClass(thread, MyData::class.java, sampledata)
        assertEquals(sampleobject, userdataobj.optuserdata(Any::class.java, sampleobject))
        assertEquals(sampleobject, userdataobj.optuserdata(null))
        assertEquals(sampledata, userdatacls.optuserdata(MyData::class.java, sampledata))
        assertEquals(sampledata, userdatacls.optuserdata(Any::class.java, sampleobject))
        assertEquals(sampledata, userdatacls.optuserdata(null))
        // should fail due to wrong class
        try {
            val o = userdataobj.optuserdata(MyData::class.java, sampledata)
            fail("did not throw bad type error")
            assertTrue(o is MyData)
        } catch (le: LuaError) {
            assertEquals("org.luaj.vm2.TypeTest\$MyData expected, got userdata", le.message)
        }

    }

    @Test
    fun testOptValue() {
        assertEquals(zero, somenil.optvalue(zero))
        assertEquals(stringstring, somenil.optvalue(stringstring))
        assertEquals(sometrue, sometrue.optvalue(LuaValue.BTRUE))
        assertEquals(somefalse, somefalse.optvalue(LuaValue.BTRUE))
        assertEquals(zero, zero.optvalue(LuaValue.BTRUE))
        assertEquals(intint, intint.optvalue(LuaValue.BTRUE))
        assertEquals(longdouble, longdouble.optvalue(LuaValue.BTRUE))
        assertEquals(somefunc, somefunc.optvalue(LuaValue.BTRUE))
        assertEquals(someclosure, someclosure.optvalue(LuaValue.BTRUE))
        assertEquals(stringstring, stringstring.optvalue(LuaValue.BTRUE))
        assertEquals(stringint, stringint.optvalue(LuaValue.BTRUE))
        assertEquals(stringlong, stringlong.optvalue(LuaValue.BTRUE))
        assertEquals(stringdouble, stringdouble.optvalue(LuaValue.BTRUE))
        assertEquals(thread, thread.optvalue(LuaValue.BTRUE))
        assertEquals(table, table.optvalue(LuaValue.BTRUE))
        assertEquals(userdataobj, userdataobj.optvalue(LuaValue.BTRUE))
        assertEquals(userdatacls, userdatacls.optvalue(LuaValue.BTRUE))
    }


    // ===================== Required argument conversion =======================


    private fun throwsErrorReq(obj: LuaValue, method: String) {
        try {
            obj.javaClass.getMethod(method).invoke(obj)
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

        fail("failed to throw LuaError as required")
    }

    @Test
    fun testCheckBoolean() {
        throwsErrorReq(somenil, "checkboolean")
        assertEquals(true, sometrue.checkboolean())
        assertEquals(false, somefalse.checkboolean())
        throwsErrorReq(zero, "checkboolean")
        throwsErrorReq(intint, "checkboolean")
        throwsErrorReq(longdouble, "checkboolean")
        throwsErrorReq(doubledouble, "checkboolean")
        throwsErrorReq(somefunc, "checkboolean")
        throwsErrorReq(someclosure, "checkboolean")
        throwsErrorReq(stringstring, "checkboolean")
        throwsErrorReq(stringint, "checkboolean")
        throwsErrorReq(stringlong, "checkboolean")
        throwsErrorReq(stringdouble, "checkboolean")
        throwsErrorReq(thread, "checkboolean")
        throwsErrorReq(table, "checkboolean")
        throwsErrorReq(userdataobj, "checkboolean")
        throwsErrorReq(userdatacls, "checkboolean")
    }

    @Test
    fun testCheckClosure() {
        throwsErrorReq(somenil, "checkclosure")
        throwsErrorReq(sometrue, "checkclosure")
        throwsErrorReq(somefalse, "checkclosure")
        throwsErrorReq(zero, "checkclosure")
        throwsErrorReq(intint, "checkclosure")
        throwsErrorReq(longdouble, "checkclosure")
        throwsErrorReq(doubledouble, "checkclosure")
        throwsErrorReq(somefunc, "checkclosure")
        assertEquals(someclosure, someclosure.checkclosure())
        assertEquals(someclosure, someclosure.checkclosure())
        throwsErrorReq(stringstring, "checkclosure")
        throwsErrorReq(stringint, "checkclosure")
        throwsErrorReq(stringlong, "checkclosure")
        throwsErrorReq(stringdouble, "checkclosure")
        throwsErrorReq(thread, "checkclosure")
        throwsErrorReq(table, "checkclosure")
        throwsErrorReq(userdataobj, "checkclosure")
        throwsErrorReq(userdatacls, "checkclosure")
    }

    @Test
    fun testCheckDouble() {
        throwsErrorReq(somenil, "checkdouble")
        throwsErrorReq(sometrue, "checkdouble")
        throwsErrorReq(somefalse, "checkdouble")
        assertEquals(0.0, zero.checkdouble())
        assertEquals(sampleint.toDouble(), intint.checkdouble())
        assertEquals(samplelong.toDouble(), longdouble.checkdouble())
        assertEquals(sampledouble, doubledouble.checkdouble())
        throwsErrorReq(somefunc, "checkdouble")
        throwsErrorReq(someclosure, "checkdouble")
        throwsErrorReq(stringstring, "checkdouble")
        assertEquals(sampleint.toDouble(), stringint.checkdouble())
        assertEquals(samplelong.toDouble(), stringlong.checkdouble())
        assertEquals(sampledouble, stringdouble.checkdouble())
        throwsErrorReq(thread, "checkdouble")
        throwsErrorReq(table, "checkdouble")
        throwsErrorReq(userdataobj, "checkdouble")
        throwsErrorReq(userdatacls, "checkdouble")
    }

    @Test
    fun testCheckFunction() {
        throwsErrorReq(somenil, "checkfunction")
        throwsErrorReq(sometrue, "checkfunction")
        throwsErrorReq(somefalse, "checkfunction")
        throwsErrorReq(zero, "checkfunction")
        throwsErrorReq(intint, "checkfunction")
        throwsErrorReq(longdouble, "checkfunction")
        throwsErrorReq(doubledouble, "checkfunction")
        assertEquals(somefunc, somefunc.checkfunction())
        assertEquals(someclosure, someclosure.checkfunction())
        assertEquals(somefunc, somefunc.checkfunction())
        assertEquals(someclosure, someclosure.checkfunction())
        throwsErrorReq(stringstring, "checkfunction")
        throwsErrorReq(stringint, "checkfunction")
        throwsErrorReq(stringlong, "checkfunction")
        throwsErrorReq(stringdouble, "checkfunction")
        throwsErrorReq(thread, "checkfunction")
        throwsErrorReq(table, "checkfunction")
        throwsErrorReq(userdataobj, "checkfunction")
        throwsErrorReq(userdatacls, "checkfunction")
    }

    @Test
    fun testCheckInt() {
        throwsErrorReq(somenil, "checkint")
        throwsErrorReq(sometrue, "checkint")
        throwsErrorReq(somefalse, "checkint")
        assertEquals(0, zero.checkint())
        assertEquals(sampleint, intint.checkint())
        assertEquals(samplelong.toInt(), longdouble.checkint())
        assertEquals(sampledouble.toInt(), doubledouble.checkint())
        throwsErrorReq(somefunc, "checkint")
        throwsErrorReq(someclosure, "checkint")
        throwsErrorReq(stringstring, "checkint")
        assertEquals(sampleint, stringint.checkint())
        assertEquals(samplelong.toInt(), stringlong.checkint())
        assertEquals(sampledouble.toInt(), stringdouble.checkint())
        throwsErrorReq(thread, "checkint")
        throwsErrorReq(table, "checkint")
        throwsErrorReq(userdataobj, "checkint")
        throwsErrorReq(userdatacls, "checkint")
    }

    @Test
    fun testCheckInteger() {
        throwsErrorReq(somenil, "checkinteger")
        throwsErrorReq(sometrue, "checkinteger")
        throwsErrorReq(somefalse, "checkinteger")
        assertEquals(zero, zero.checkinteger() as LuaNumber?)
        assertEquals(LuaValue.valueOf(sampleint), intint.checkinteger())
        assertEquals(LuaValue.valueOf(samplelong.toInt()), longdouble.checkinteger())
        assertEquals(LuaValue.valueOf(sampledouble.toInt()), doubledouble.checkinteger())
        throwsErrorReq(somefunc, "checkinteger")
        throwsErrorReq(someclosure, "checkinteger")
        throwsErrorReq(stringstring, "checkinteger")
        assertEquals(LuaValue.valueOf(sampleint), stringint.checkinteger())
        assertEquals(LuaValue.valueOf(samplelong.toInt()), stringlong.checkinteger())
        assertEquals(LuaValue.valueOf(sampledouble.toInt()), stringdouble.checkinteger())
        throwsErrorReq(thread, "checkinteger")
        throwsErrorReq(table, "checkinteger")
        throwsErrorReq(userdataobj, "checkinteger")
        throwsErrorReq(userdatacls, "checkinteger")
    }

    @Test
    fun testCheckLong() {
        throwsErrorReq(somenil, "checklong")
        throwsErrorReq(sometrue, "checklong")
        throwsErrorReq(somefalse, "checklong")
        assertEquals(0L, zero.checklong())
        assertEquals(sampleint.toLong(), intint.checklong())
        assertEquals(samplelong, longdouble.checklong())
        assertEquals(sampledouble.toLong(), doubledouble.checklong())
        throwsErrorReq(somefunc, "checklong")
        throwsErrorReq(someclosure, "checklong")
        throwsErrorReq(stringstring, "checklong")
        assertEquals(sampleint.toLong(), stringint.checklong())
        assertEquals(samplelong, stringlong.checklong())
        assertEquals(sampledouble.toLong(), stringdouble.checklong())
        throwsErrorReq(thread, "checklong")
        throwsErrorReq(table, "checklong")
        throwsErrorReq(userdataobj, "checklong")
        throwsErrorReq(userdatacls, "checklong")
    }

    @Test
    fun testCheckNumber() {
        throwsErrorReq(somenil, "checknumber")
        throwsErrorReq(sometrue, "checknumber")
        throwsErrorReq(somefalse, "checknumber")
        assertEquals(zero, zero.checknumber())
        assertEquals(LuaValue.valueOf(sampleint), intint.checknumber())
        assertEquals(LuaValue.valueOf(samplelong.toDouble()), longdouble.checknumber())
        assertEquals(LuaValue.valueOf(sampledouble), doubledouble.checknumber())
        throwsErrorReq(somefunc, "checknumber")
        throwsErrorReq(someclosure, "checknumber")
        throwsErrorReq(stringstring, "checknumber")
        assertEquals(LuaValue.valueOf(sampleint), stringint.checknumber())
        assertEquals(LuaValue.valueOf(samplelong.toDouble()), stringlong.checknumber())
        assertEquals(LuaValue.valueOf(sampledouble), stringdouble.checknumber())
        throwsErrorReq(thread, "checknumber")
        throwsErrorReq(table, "checknumber")
        throwsErrorReq(userdataobj, "checknumber")
        throwsErrorReq(userdatacls, "checknumber")
    }

    @Test
    fun testCheckTable() {
        throwsErrorReq(somenil, "checktable")
        throwsErrorReq(sometrue, "checktable")
        throwsErrorReq(somefalse, "checktable")
        throwsErrorReq(zero, "checktable")
        throwsErrorReq(intint, "checktable")
        throwsErrorReq(longdouble, "checktable")
        throwsErrorReq(doubledouble, "checktable")
        throwsErrorReq(somefunc, "checktable")
        throwsErrorReq(someclosure, "checktable")
        throwsErrorReq(stringstring, "checktable")
        throwsErrorReq(stringint, "checktable")
        throwsErrorReq(stringlong, "checktable")
        throwsErrorReq(stringdouble, "checktable")
        throwsErrorReq(thread, "checktable")
        assertEquals(table, table.checktable())
        assertEquals(table, table.checktable())
        throwsErrorReq(userdataobj, "checktable")
        throwsErrorReq(userdatacls, "checktable")
    }

    @Test
    fun testCheckThread() {
        throwsErrorReq(somenil, "checkthread")
        throwsErrorReq(sometrue, "checkthread")
        throwsErrorReq(somefalse, "checkthread")
        throwsErrorReq(zero, "checkthread")
        throwsErrorReq(intint, "checkthread")
        throwsErrorReq(longdouble, "checkthread")
        throwsErrorReq(doubledouble, "checkthread")
        throwsErrorReq(somefunc, "checkthread")
        throwsErrorReq(someclosure, "checkthread")
        throwsErrorReq(stringstring, "checkthread")
        throwsErrorReq(stringint, "checkthread")
        throwsErrorReq(stringlong, "checkthread")
        throwsErrorReq(stringdouble, "checkthread")
        throwsErrorReq(table, "checkthread")
        assertEquals(thread, thread.checkthread())
        assertEquals(thread, thread.checkthread())
        throwsErrorReq(userdataobj, "checkthread")
        throwsErrorReq(userdatacls, "checkthread")
    }

    @Test
    fun testCheckJavaString() {
        throwsErrorReq(somenil, "checkjstring")
        throwsErrorReq(sometrue, "checkjstring")
        throwsErrorReq(somefalse, "checkjstring")
        assertEquals(zero.toString(), zero.checkjstring())
        assertEquals(intint.toString(), intint.checkjstring())
        assertEquals(longdouble.toString(), longdouble.checkjstring())
        assertEquals(doubledouble.toString(), doubledouble.checkjstring())
        throwsErrorReq(somefunc, "checkjstring")
        throwsErrorReq(someclosure, "checkjstring")
        assertEquals(samplestringstring, stringstring.checkjstring())
        assertEquals(samplestringint, stringint.checkjstring())
        assertEquals(samplestringlong, stringlong.checkjstring())
        assertEquals(samplestringdouble, stringdouble.checkjstring())
        throwsErrorReq(thread, "checkjstring")
        throwsErrorReq(table, "checkjstring")
        throwsErrorReq(userdataobj, "checkjstring")
        throwsErrorReq(userdatacls, "checkjstring")
    }

    @Test
    fun testCheckLuaString() {
        throwsErrorReq(somenil, "checkstring")
        throwsErrorReq(sometrue, "checkstring")
        throwsErrorReq(somefalse, "checkstring")
        assertEquals(LuaValue.valueOf("0"), zero.checkstring())
        assertEquals(stringint, intint.checkstring())
        assertEquals(stringlong, longdouble.checkstring())
        assertEquals(stringdouble, doubledouble.checkstring())
        throwsErrorReq(somefunc, "checkstring")
        throwsErrorReq(someclosure, "checkstring")
        assertEquals(stringstring, stringstring.checkstring())
        assertEquals(stringint, stringint.checkstring())
        assertEquals(stringlong, stringlong.checkstring())
        assertEquals(stringdouble, stringdouble.checkstring())
        throwsErrorReq(thread, "checkstring")
        throwsErrorReq(table, "checkstring")
        throwsErrorReq(userdataobj, "checkstring")
        throwsErrorReq(userdatacls, "checkstring")
    }

    @Test
    fun testCheckUserdata() {
        throwsErrorReq(somenil, "checkuserdata")
        throwsErrorReq(sometrue, "checkuserdata")
        throwsErrorReq(somefalse, "checkuserdata")
        throwsErrorReq(zero, "checkuserdata")
        throwsErrorReq(intint, "checkuserdata")
        throwsErrorReq(longdouble, "checkuserdata")
        throwsErrorReq(doubledouble, "checkuserdata")
        throwsErrorReq(somefunc, "checkuserdata")
        throwsErrorReq(someclosure, "checkuserdata")
        throwsErrorReq(stringstring, "checkuserdata")
        throwsErrorReq(stringint, "checkuserdata")
        throwsErrorReq(stringlong, "checkuserdata")
        throwsErrorReq(stringdouble, "checkuserdata")
        throwsErrorReq(table, "checkuserdata")
        assertEquals(sampleobject, userdataobj.checkuserdata())
        assertEquals(sampleobject, userdataobj.checkuserdata())
        assertEquals(sampledata, userdatacls.checkuserdata())
        assertEquals(sampledata, userdatacls.checkuserdata())
    }

    private fun throwsErrorReqCheckUserdataClass(obj: LuaValue, arg: Class<*>) {
        try {
            obj.javaClass.getMethod("checkuserdata", KClass::class.java).invoke(obj, arg.kotlin)
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

        fail("failed to throw LuaError as required")
    }

    @Test
    fun testCheckUserdataClass() {
        throwsErrorReqCheckUserdataClass(somenil, Any::class.java)
        throwsErrorReqCheckUserdataClass(somenil, MyData::class.java)
        throwsErrorReqCheckUserdataClass(sometrue, Any::class.java)
        throwsErrorReqCheckUserdataClass(zero, MyData::class.java)
        throwsErrorReqCheckUserdataClass(intint, MyData::class.java)
        throwsErrorReqCheckUserdataClass(longdouble, MyData::class.java)
        throwsErrorReqCheckUserdataClass(somefunc, MyData::class.java)
        throwsErrorReqCheckUserdataClass(someclosure, MyData::class.java)
        throwsErrorReqCheckUserdataClass(stringstring, MyData::class.java)
        throwsErrorReqCheckUserdataClass(stringint, MyData::class.java)
        throwsErrorReqCheckUserdataClass(stringlong, MyData::class.java)
        throwsErrorReqCheckUserdataClass(stringlong, MyData::class.java)
        throwsErrorReqCheckUserdataClass(stringdouble, MyData::class.java)
        throwsErrorReqCheckUserdataClass(table, MyData::class.java)
        throwsErrorReqCheckUserdataClass(thread, MyData::class.java)
        assertEquals(sampleobject, userdataobj.checkuserdata(Any::class.java))
        assertEquals(sampleobject, userdataobj.checkuserdata())
        assertEquals(sampledata, userdatacls.checkuserdata(MyData::class.java))
        assertEquals(sampledata, userdatacls.checkuserdata(Any::class.java))
        assertEquals(sampledata, userdatacls.checkuserdata())
        // should fail due to wrong class
        try {
            val o = userdataobj.checkuserdata(MyData::class.java)
            fail("did not throw bad type error")
            assertTrue(o is MyData)
        } catch (le: LuaError) {
            assertEquals("org.luaj.vm2.TypeTest\$MyData expected, got userdata", le.message)
        }

    }

    @Test
    fun testCheckValue() {
        throwsErrorReq(somenil, "checknotnil")
        assertEquals(sometrue, sometrue.checknotnil())
        assertEquals(somefalse, somefalse.checknotnil())
        assertEquals(zero, zero.checknotnil())
        assertEquals(intint, intint.checknotnil())
        assertEquals(longdouble, longdouble.checknotnil())
        assertEquals(somefunc, somefunc.checknotnil())
        assertEquals(someclosure, someclosure.checknotnil())
        assertEquals(stringstring, stringstring.checknotnil())
        assertEquals(stringint, stringint.checknotnil())
        assertEquals(stringlong, stringlong.checknotnil())
        assertEquals(stringdouble, stringdouble.checknotnil())
        assertEquals(thread, thread.checknotnil())
        assertEquals(table, table.checknotnil())
        assertEquals(userdataobj, userdataobj.checknotnil())
        assertEquals(userdatacls, userdatacls.checknotnil())
    }

    companion object {
        init {
            JsePlatform.debugGlobals()
        }
    }

}
