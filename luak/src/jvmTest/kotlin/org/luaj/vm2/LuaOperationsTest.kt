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

import org.luaj.vm2.TypeTest.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.*
import java.lang.reflect.*
import kotlin.test.*

class LuaOperationsTest {

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
    private val table = LuaValue.listOf(arrayOf<LuaValue>(LuaValue.valueOf("aaa"), LuaValue.valueOf("bbb")))
    private val somefunc = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.NONE
        }
    }
    private val thread = LuaThread(Globals(), somefunc)
    private val proto = Prototype(1)
    private val someclosure = LuaClosure(proto, table)
    private val userdataobj = LuaValue.userdataOf(sampleobject)
    private val userdatacls = LuaValue.userdataOf(sampledata)

    private fun throwsLuaError(methodName: String, obj: Any) {
        try {
            LuaValue::class.java.getMethod(methodName).invoke(obj)
            fail("failed to throw LuaError as required")
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

    }

    private fun throwsLuaError(methodName: String, obj: Any, arg: Any) {
        try {
            LuaValue::class.java.getMethod(methodName, LuaValue::class.java).invoke(obj, arg)
            fail("failed to throw LuaError as required")
        } catch (e: InvocationTargetException) {
            if (e.targetException !is LuaError)
                fail("not a LuaError: " + e.targetException)
            return  // pass
        } catch (e: Exception) {
            fail("bad exception: $e")
        }

    }

    @Test
    fun testLen() {
        throwsLuaError("len", somenil)
        throwsLuaError("len", sometrue)
        throwsLuaError("len", somefalse)
        throwsLuaError("len", zero)
        throwsLuaError("len", intint)
        throwsLuaError("len", longdouble)
        throwsLuaError("len", doubledouble)
        assertEquals(LuaInteger.valueOf(samplestringstring.length), stringstring.len())
        assertEquals(LuaInteger.valueOf(samplestringint.length), stringint.len())
        assertEquals(LuaInteger.valueOf(samplestringlong.length), stringlong.len())
        assertEquals(LuaInteger.valueOf(samplestringdouble.length), stringdouble.len())
        assertEquals(LuaInteger.valueOf(2), table.len())
        throwsLuaError("len", somefunc)
        throwsLuaError("len", thread)
        throwsLuaError("len", someclosure)
        throwsLuaError("len", userdataobj)
        throwsLuaError("len", userdatacls)
    }

    @Test
    fun testLength() {
        throwsLuaError("length", somenil)
        throwsLuaError("length", sometrue)
        throwsLuaError("length", somefalse)
        throwsLuaError("length", zero)
        throwsLuaError("length", intint)
        throwsLuaError("length", longdouble)
        throwsLuaError("length", doubledouble)
        assertEquals(samplestringstring.length, stringstring.length())
        assertEquals(samplestringint.length, stringint.length())
        assertEquals(samplestringlong.length, stringlong.length())
        assertEquals(samplestringdouble.length, stringdouble.length())
        assertEquals(2, table.length())
        throwsLuaError("length", somefunc)
        throwsLuaError("length", thread)
        throwsLuaError("length", someclosure)
        throwsLuaError("length", userdataobj)
        throwsLuaError("length", userdatacls)
    }

    fun createPrototype(script: String, name: String): Prototype? {
        try {
            val globals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals()
            val reader = StrLuaReader(script)
            return globals.compilePrototype(reader, name)
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            fail(e.toString())
            return null
        }

    }

    @Test
    fun testFunctionClosureThreadEnv() {

        // set up suitable environments for execution
        val aaa = LuaValue.valueOf("aaa")
        val eee = LuaValue.valueOf("eee")
        val globals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals()
        val newenv = LuaValue.tableOf(
            arrayOf<LuaValue>(
                LuaValue.valueOf("a"),
                LuaValue.valueOf("aaa"),
                LuaValue.valueOf("b"),
                LuaValue.valueOf("bbb")
            )
        )
        val mt = LuaValue.tableOf(arrayOf(LuaValue.INDEX, globals))
        newenv.setmetatable(mt)
        globals.set("a", aaa)
        newenv.set("a", eee)

        // function tests
        run {
            val f = object : ZeroArgFunction() {
                override fun call(): LuaValue {
                    return globals.get("a")
                }
            }
            assertEquals(aaa, f.call())
        }

        // closure tests
        run {
            val p = createPrototype("return a\n", "closuretester")
            var c = LuaClosure(p!!, globals)

            // Test that a clusure with a custom enviroment uses that environment.
            assertEquals(aaa, c.call())
            c = LuaClosure(p, newenv)
            assertEquals(newenv, c.upValues[0]!!.value)
            assertEquals(eee, c.call())
        }
    }
}
