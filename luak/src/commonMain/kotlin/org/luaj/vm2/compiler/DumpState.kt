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
package org.luaj.vm2.compiler

import org.luaj.vm2.*
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

/** Class to dump a [Prototype] into an output stream, as part of compiling.
 *
 *
 * Generally, this class is not used directly, but rather indirectly via a command
 * line interface tool such as [luac].
 *
 *
 * A lua binary file is created via [DumpState.dump]:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * Prototype p = globals.compilePrototype(new StringReader("print('hello, world')"), "main.lua");
 * ByteArrayOutputStream o = new ByteArrayOutputStream();
 * DumpState.dump(p, o, false);
 * byte[] lua_binary_file_bytes = o.toByteArray();
` *  </pre>
 *
 * The [LoadState] may be used directly to undump these bytes:
 * <pre> `Prototypep = LoadState.instance.undump(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua");
 * LuaClosure c = new LuaClosure(p, globals);
 * c.call();
` *  </pre>
 *
 *
 * More commonly, the [Globals.undumper] may be used to undump them:
 * <pre> `Prototype p = globals.loadPrototype(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua", "b");
 * LuaClosure c = new LuaClosure(p, globals);
 * c.call();
` *  </pre>
 *
 * @see luac
 *
 * @see LoadState
 *
 * @see Globals
 *
 * @see Prototype
 */
class DumpState(w: LuaBinOutput, internal var strip: Boolean) {

    // header fields
    private var IS_LITTLE_ENDIAN = false
    private var NUMBER_FORMAT = NUMBER_FORMAT_DEFAULT
    private var SIZEOF_LUA_NUMBER = 8

    internal var writer: LuaBinOutput = w
    internal var status: Int = 0

    internal fun dumpBlock(b: ByteArray, size: Int) {
        writer.write(b, 0, size)
    }

    internal fun dumpChar(b: Int) {
        writer.write(b)
    }


    internal fun dumpInt(x: Int) {
        if (IS_LITTLE_ENDIAN) {
            writer.writeByte(x and 0xff)
            writer.writeByte(x shr 8 and 0xff)
            writer.writeByte(x shr 16 and 0xff)
            writer.writeByte(x shr 24 and 0xff)
        } else {
            writer.writeInt(x)
        }
    }


    internal fun dumpString(s: LuaString) {
        val len = s.len().toint()
        dumpInt(len + 1)
        s.write(writer, 0, len)
        writer.write(0)
    }


    internal fun dumpDouble(d: Double) {
        val l = (d).toRawBits()
        if (IS_LITTLE_ENDIAN) {
            dumpInt(l.toInt())
            dumpInt((l shr 32).toInt())
        } else {
            writer.writeLong(l)
        }
    }


    internal fun dumpCode(f: Prototype) {
        val code = f.code
        val n = code.size
        dumpInt(n)
        for (i in 0 until n)
            dumpInt(code[i])
    }


    internal fun dumpConstants(f: Prototype) {
        val k = f.k
        var i: Int
        var n = k.size
        dumpInt(n)
        i = 0
        while (i < n) {
            val o = k[i]
            when (o.type()) {
                LuaValue.TNIL -> writer.write(LuaValue.TNIL)
                LuaValue.TBOOLEAN -> {
                    writer.write(LuaValue.TBOOLEAN)
                    dumpChar(if (o.toboolean()) 1 else 0)
                }
                LuaValue.TNUMBER -> when (NUMBER_FORMAT) {
                    NUMBER_FORMAT_FLOATS_OR_DOUBLES -> {
                        writer.write(LuaValue.TNUMBER)
                        dumpDouble(o.todouble())
                    }
                    NUMBER_FORMAT_INTS_ONLY -> {
                        if (!ALLOW_INTEGER_CASTING && !o.isint())
                            throw IllegalArgumentException("not an integer: $o")
                        writer.write(LuaValue.TNUMBER)
                        dumpInt(o.toint())
                    }
                    NUMBER_FORMAT_NUM_PATCH_INT32 -> if (o.isint()) {
                        writer.write(LuaValue.TINT)
                        dumpInt(o.toint())
                    } else {
                        writer.write(LuaValue.TNUMBER)
                        dumpDouble(o.todouble())
                    }
                    else -> throw IllegalArgumentException("number format not supported: $NUMBER_FORMAT")
                }
                LuaValue.TSTRING -> {
                    writer.write(LuaValue.TSTRING)
                    dumpString(o as LuaString)
                }
                else -> throw IllegalArgumentException("bad type for $o")
            }
            i++
        }
        n = f.p.size
        dumpInt(n)
        i = 0
        while (i < n) {
            dumpFunction(f.p[i])
            i++
        }
    }


    internal fun dumpUpvalues(f: Prototype) {
        val n = f.upvalues.size
        dumpInt(n)
        for (i in 0 until n) {
            writer.writeByte(if (f.upvalues[i].instack) 1 else 0)
            writer.writeByte(f.upvalues[i].idx.toInt())
        }
    }


    internal fun dumpDebug(f: Prototype) {
        if (strip)
            dumpInt(0)
        else
            dumpString(f.source)
        var n = if (strip) 0 else f.lineinfo.size
        dumpInt(n)
        var i = 0
        while (i < n) {
            dumpInt(f.lineinfo[i])
            i++
        }
        n = if (strip) 0 else f.locvars.size
        dumpInt(n)
        i = 0
        while (i < n) {
            val lvi = f.locvars[i]
            dumpString(lvi.varname)
            dumpInt(lvi.startpc)
            dumpInt(lvi.endpc)
            i++
        }
        n = if (strip) 0 else f.upvalues.size
        dumpInt(n)
        i = 0
        while (i < n) {
            dumpString(f.upvalues[i].name!!)
            i++
        }
    }


    internal fun dumpFunction(f: Prototype) {
        dumpInt(f.linedefined)
        dumpInt(f.lastlinedefined)
        dumpChar(f.numparams)
        dumpChar(f.is_vararg)
        dumpChar(f.maxstacksize)
        dumpCode(f)
        dumpConstants(f)
        dumpUpvalues(f)
        dumpDebug(f)
    }


    internal fun dumpHeader() {
        writer.write(LoadState.LUA_SIGNATURE)
        writer.write(LoadState.LUAC_VERSION)
        writer.write(LoadState.LUAC_FORMAT)
        writer.write(if (IS_LITTLE_ENDIAN) 1 else 0)
        writer.write(SIZEOF_INT)
        writer.write(SIZEOF_SIZET)
        writer.write(SIZEOF_INSTRUCTION)
        writer.write(SIZEOF_LUA_NUMBER)
        writer.write(NUMBER_FORMAT)
        writer.write(LoadState.LUAC_TAIL)
    }

    companion object {

        /** set true to allow integer compilation  */
        @kotlin.jvm.JvmField
        var ALLOW_INTEGER_CASTING = false

        /** format corresponding to non-number-patched lua, all numbers are floats or doubles  */
        @kotlin.jvm.JvmField
        val NUMBER_FORMAT_FLOATS_OR_DOUBLES = 0

        /** format corresponding to non-number-patched lua, all numbers are ints  */
        @kotlin.jvm.JvmField
        val NUMBER_FORMAT_INTS_ONLY = 1

        /** format corresponding to number-patched lua, all numbers are 32-bit (4 byte) ints  */
        @kotlin.jvm.JvmField
        val NUMBER_FORMAT_NUM_PATCH_INT32 = 4

        /** default number format  */
        @kotlin.jvm.JvmField
        val NUMBER_FORMAT_DEFAULT = NUMBER_FORMAT_FLOATS_OR_DOUBLES

        private val SIZEOF_INT = 4
        private val SIZEOF_SIZET = 4
        private val SIZEOF_INSTRUCTION = 4

        /*
	** dump Lua function as precompiled chunk
	*/


        fun dump(f: Prototype, w: LuaBinOutput, strip: Boolean): Int {
            val D = DumpState(w, strip)
            D.dumpHeader()
            D.dumpFunction(f)
            return D.status
        }

        /**
         *
         * @param f the function to dump
         * @param w the output stream to dump to
         * @param stripDebug true to strip debugging info, false otherwise
         * @param numberFormat one of NUMBER_FORMAT_FLOATS_OR_DOUBLES, NUMBER_FORMAT_INTS_ONLY, NUMBER_FORMAT_NUM_PATCH_INT32
         * @param littleendian true to use little endian for numbers, false for big endian
         * @return 0 if dump succeeds
         * @com.soywiz.luak.compat.java.Throws IOException
         * @com.soywiz.luak.compat.java.Throws IllegalArgumentException if the number format it not supported
         */


        fun dump(f: Prototype, w: LuaBinOutput, stripDebug: Boolean, numberFormat: Int, littleendian: Boolean): Int {
            when (numberFormat) {
                NUMBER_FORMAT_FLOATS_OR_DOUBLES, NUMBER_FORMAT_INTS_ONLY, NUMBER_FORMAT_NUM_PATCH_INT32 -> {
                }
                else -> throw IllegalArgumentException("number format not supported: $numberFormat")
            }
            val D = DumpState(w, stripDebug)
            D.IS_LITTLE_ENDIAN = littleendian
            D.NUMBER_FORMAT = numberFormat
            D.SIZEOF_LUA_NUMBER = if (numberFormat == NUMBER_FORMAT_INTS_ONLY) 4 else 8
            D.dumpHeader()
            D.dumpFunction(f)
            return D.status
        }
    }
}
