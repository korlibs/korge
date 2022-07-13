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

import org.luaj.vm2.io.*

/**
 * Class to undump compiled lua bytecode into a [Prototype] instances.
 *
 *
 * The [LoadState] class provides the default [Globals.Undumper]
 * which is used to undump a string of bytes that represent a lua binary file
 * using either the C-based lua compiler, or luaj's
 * [org.luaj.vm2.compiler.LuaC] compiler.
 *
 *
 * The canonical method to load and execute code is done
 * indirectly using the Globals:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * LuaValue chunk = globasl.load("print('hello, world')", "main.lua");
 * chunk.call();
` *  </pre>
 * This should work regardless of which [Globals.Compiler] or [Globals.Undumper]
 * have been installed.
 *
 *
 * By default, when using [org.luaj.vm2.lib.jse.JsePlatform] or
 * [org.luaj.vm2.lib.jme.JmePlatform]
 * to construct globals, the [LoadState] default undumper is installed
 * as the default [Globals.Undumper].
 *
 *
 *
 * A lua binary file is created via the [org.luaj.vm2.compiler.DumpState] class
 * :
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * Prototype p = globals.compilePrototype(new StringReader("print('hello, world')"), "main.lua");
 * ByteArrayOutputStream o = new ByteArrayOutputStream();
 * org.luaj.vm2.compiler.DumpState.dump(p, o, false);
 * byte[] lua_binary_file_bytes = o.toByteArray();
` *  </pre>
 *
 * The [LoadState]'s default undumper [.instance]
 * may be used directly to undump these bytes:
 * <pre> `Prototypep = LoadState.instance.undump(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua");
 * LuaClosure c = new LuaClosure(p, globals);
 * c.call();
` *  </pre>
 *
 *
 * More commonly, the [Globals.Undumper] may be used to undump them:
 * <pre> `Prototype p = globals.loadPrototype(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua", "b");
 * LuaClosure c = new LuaClosure(p, globals);
 * c.call();
` *  </pre>
 *
 * @see Globals.Compiler
 *
 * @see Globals.Undumper
 *
 * @see LuaClosure
 *
 * @see LuaFunction
 *
 * @see org.luaj.vm2.compiler.LuaC
 *
 * @see org.luaj.vm2.luajc.LuaJC
 *
 * @see Globals.compiler
 *
 * @see Globals.load
 */
class LoadState
/** Private constructor for create a load state  */
private constructor(
    stream: LuaBinInput,
    /** Name of what is being loaded?  */
    internal var name: String
) {

    // values read from the header
    private var luacVersion: Int = 0
    private var luacFormat: Int = 0
    private var luacLittleEndian: Boolean = false
    private var luacSizeofInt: Int = 0
    private var luacSizeofSizeT: Int = 0
    private var luacSizeofInstruction: Int = 0
    private var luacSizeofLuaNumber: Int = 0
    private var luacNumberFormat: Int = 0

    /** input stream from which we are loading  */
    val `is`: LuaBinInput = stream

    /** Read buffer  */
    private var buf = ByteArray(512)

    /** Load a 4-byte int value from the input stream
     * @return the int value laoded.
     */

    internal fun loadInt(): Int {
        `is`.readFully(buf, 0, 4)
        return if (luacLittleEndian)
            buf[3].toInt() shl 24 or (0xff and buf[2].toInt() shl 16) or (0xff and buf[1].toInt() shl 8) or (0xff and buf[0].toInt())
        else
            buf[0].toInt() shl 24 or (0xff and buf[1].toInt() shl 16) or (0xff and buf[2].toInt() shl 8) or (0xff and buf[3].toInt())
    }

    /** Load an array of int values from the input stream
     * @return the array of int values laoded.
     */

    internal fun loadIntArray(): IntArray {
        val n = loadInt()
        if (n == 0)
            return NOINTS

        // read all data at once
        val m = n shl 2
        if (buf.size < m)
            buf = ByteArray(m)
        `is`.readFully(buf, 0, m)
        val array = IntArray(n)
        var i = 0
        var j = 0
        while (i < n) {
            array[i] = if (luacLittleEndian)
                buf[j + 3].toInt() shl 24 or (0xff and buf[j + 2].toInt() shl 16) or (0xff and buf[j + 1].toInt() shl 8) or (0xff and buf[j + 0].toInt())
            else
                buf[j + 0].toInt() shl 24 or (0xff and buf[j + 1].toInt() shl 16) or (0xff and buf[j + 2].toInt() shl 8) or (0xff and buf[j + 3].toInt())
            ++i
            j += 4
        }

        return array
    }

    /** Load a long  value from the input stream
     * @return the long value laoded.
     */

    internal fun loadInt64(): Long {
        val a: Int
        val b: Int
        if (this.luacLittleEndian) {
            a = loadInt()
            b = loadInt()
        } else {
            b = loadInt()
            a = loadInt()
        }
        return b.toLong() shl 32 or (a.toLong() and 0xffffffffL)
    }

    /** Load a lua strin gvalue from the input stream
     * @return the [LuaString] value laoded.
     */

    internal fun loadString(): LuaString? {
        val size = if (this.luacSizeofSizeT == 8) loadInt64().toInt() else loadInt()
        if (size == 0)
            return null
        val bytes = ByteArray(size)
        `is`.readFully(bytes, 0, size)
        return LuaString.valueUsing(bytes, 0, bytes.size - 1)
    }

    /**
     * Load a number from a binary chunk
     * @return the [LuaValue] loaded
     * @com.soywiz.luak.compat.java.Throws IOException if an i/o exception occurs
     */

    internal fun loadNumber(): LuaValue {
        return if (luacNumberFormat == NUMBER_FORMAT_INTS_ONLY) {
            LuaInteger.valueOf(loadInt())
        } else {
            longBitsToLuaNumber(loadInt64())
        }
    }

    /**
     * Load a list of constants from a binary chunk
     * @param f the function prototype
     * @com.soywiz.luak.compat.java.Throws IOException if an i/o exception occurs
     */

    internal fun loadConstants(f: Prototype) {
        var n = loadInt()
        val values: Array<LuaValue?> = if (n > 0) arrayOfNulls<LuaValue>(n) else NOVALUES
        for (i in 0 until n) {
            when (`is`.readByte().toInt()) {
                LUA_TNIL -> values[i] = LuaValue.NIL
                LUA_TBOOLEAN -> values[i] = (if (0 != `is`.readUnsignedByte()) LuaValue.BTRUE else LuaValue.BFALSE)
                LUA_TINT -> values[i] = LuaInteger.valueOf(loadInt())
                LUA_TNUMBER -> values[i] = loadNumber()
                LUA_TSTRING -> values[i] = loadString()
                else -> throw IllegalStateException("bad constant")
            }
        }
        f.k = values as Array<LuaValue>

        n = loadInt()
        val protos: Array<Prototype?> = if (n > 0) arrayOfNulls<Prototype>(n) else NOPROTOS as Array<Prototype?>
        for (i in 0 until n)
            protos[i] = loadFunction(f.source)
        f.p = protos as Array<Prototype>
    }



    internal fun loadUpvalues(f: Prototype) {
        val n = loadInt()
        f.upvalues = if (n > 0) arrayOfNulls<Upvaldesc>(n) as Array<Upvaldesc> else NOUPVALDESCS
        for (i in 0 until n) {
            val instack = `is`.readByte().toInt() != 0
            val idx = `is`.readByte().toInt() and 0xff
            f.upvalues[i] = Upvaldesc(null, instack, idx)
        }
    }

    /**
     * Load the debug info for a function prototype
     * @param f the function Prototype
     * @com.soywiz.luak.compat.java.Throws IOException if there is an i/o exception
     */

    internal fun loadDebug(f: Prototype) {
        f.source = loadString() ?: LuaString.valueOf("Unknown")
        f.lineinfo = loadIntArray()
        var n = loadInt()
        f.locvars = if (n > 0) arrayOfNulls<LocVars>(n) as Array<LocVars> else NOLOCVARS
        for (i in 0 until n) {
            val varname = loadString()
            val startpc = loadInt()
            val endpc = loadInt()
            f.locvars[i] = LocVars(varname!!, startpc, endpc)
        }

        n = loadInt()
        for (i in 0 until n)
            f.upvalues[i].name = loadString()
    }

    /**
     * Load a function prototype from the input stream
     * @param p name of the source
     * @return [Prototype] instance that was loaded
     * @com.soywiz.luak.compat.java.Throws IOException
     */

    fun loadFunction(p: LuaString): Prototype {
        val f = Prototype()
        ////		this.L.push(f);
        //		f.source = loadString();
        //		if ( f.source == null )
        //			f.source = p;
        f.linedefined = loadInt()
        f.lastlinedefined = loadInt()
        f.numparams = `is`.readUnsignedByte()
        f.is_vararg = `is`.readUnsignedByte()
        f.maxstacksize = `is`.readUnsignedByte()
        f.code = loadIntArray()
        loadConstants(f)
        loadUpvalues(f)
        loadDebug(f)

        // TODO: add check here, for debugging purposes, I believe
        // see ldebug.c
        //		 IF (!luaG_checkcode(f), "bad code");

        //		 this.L.pop();
        return f
    }

    /**
     * Load the lua chunk header values.
     * @com.soywiz.luak.compat.java.Throws IOException if an i/o exception occurs.
     */

    fun loadHeader() {
        luacVersion = `is`.readByte().toInt()
        luacFormat = `is`.readByte().toInt()
        luacLittleEndian = 0 != `is`.readByte().toInt()
        luacSizeofInt = `is`.readByte().toInt()
        luacSizeofSizeT = `is`.readByte().toInt()
        luacSizeofInstruction = `is`.readByte().toInt()
        luacSizeofLuaNumber = `is`.readByte().toInt()
        luacNumberFormat = `is`.readByte().toInt()
        for (i in LUAC_TAIL.indices)
            if (`is`.readByte() != LUAC_TAIL[i])
                throw LuaError("Unexpeted byte in luac tail of header, index=$i")
    }


    private class GlobalsUndumper : Globals.Undumper {

        override fun undump(stream: LuaBinInput, chunkname: String): Prototype? {
            return LoadState.undump(stream, chunkname)
        }
    }

    companion object {

        /** Shared instance of Globals.Undumper to use loading prototypes from binary lua files  */
        @kotlin.jvm.JvmField val instance: Globals.Undumper = GlobalsUndumper()

        /** format corresponding to non-number-patched lua, all numbers are floats or doubles  */
        @kotlin.jvm.JvmField val NUMBER_FORMAT_FLOATS_OR_DOUBLES = 0

        /** format corresponding to non-number-patched lua, all numbers are ints  */
        @kotlin.jvm.JvmField val NUMBER_FORMAT_INTS_ONLY = 1

        /** format corresponding to number-patched lua, all numbers are 32-bit (4 byte) ints  */
        @kotlin.jvm.JvmField val NUMBER_FORMAT_NUM_PATCH_INT32 = 4

        // type constants
        @kotlin.jvm.JvmField val LUA_TINT = -2
        @kotlin.jvm.JvmField val LUA_TNONE = -1
        @kotlin.jvm.JvmField val LUA_TNIL = 0
        @kotlin.jvm.JvmField val LUA_TBOOLEAN = 1
        @kotlin.jvm.JvmField val LUA_TLIGHTUSERDATA = 2
        @kotlin.jvm.JvmField val LUA_TNUMBER = 3
        @kotlin.jvm.JvmField val LUA_TSTRING = 4
        @kotlin.jvm.JvmField val LUA_TTABLE = 5
        @kotlin.jvm.JvmField val LUA_TFUNCTION = 6
        @kotlin.jvm.JvmField val LUA_TUSERDATA = 7
        @kotlin.jvm.JvmField val LUA_TTHREAD = 8
        @kotlin.jvm.JvmField val LUA_TVALUE = 9

        /** The character encoding to use for file encoding.  Null means the default encoding  */
        @kotlin.jvm.JvmField var encoding: String? = null

        /** Signature byte indicating the file is a compiled binary chunk  */
        @kotlin.jvm.JvmField val LUA_SIGNATURE = byteArrayOf('\u001b'.toByte(), 'L'.toByte(), 'u'.toByte(), 'a'.toByte())

        /** Data to catch conversion errors  */
        @kotlin.jvm.JvmField val LUAC_TAIL =
            byteArrayOf(0x19.toByte(), 0x93.toByte(), '\r'.toByte(), '\n'.toByte(), 0x1a.toByte(), '\n'.toByte())


        /** Name for compiled chunks  */
        @kotlin.jvm.JvmField val SOURCE_BINARY_STRING = "binary string"


        /** for header of binary files -- this is Lua 5.2  */
        @kotlin.jvm.JvmField val LUAC_VERSION = 0x52

        /** for header of binary files -- this is the official format  */
        @kotlin.jvm.JvmField val LUAC_FORMAT = 0

        /** size of header of binary files  */
        @kotlin.jvm.JvmField val LUAC_HEADERSIZE = 12

        private val NOVALUES = arrayOf<LuaValue?>()
        private val NOPROTOS = arrayOf<Prototype>()
        private val NOLOCVARS = arrayOf<LocVars>()
        private val NOSTRVALUES = arrayOf<LuaString>()
        private val NOUPVALDESCS = arrayOf<Upvaldesc>()
        private val NOINTS = intArrayOf()

        /** Install this class as the standard Globals.Undumper for the supplied Globals  */
         fun install(globals: Globals) {
            globals.undumper = instance
        }

        /**
         * Convert bits in a long value to a [LuaValue].
         * @param bits long value containing the bits
         * @return [LuaInteger] or [LuaDouble] whose value corresponds to the bits provided.
         */
         fun longBitsToLuaNumber(bits: Long): LuaValue {
            if (bits and (1L shl 63) - 1 == 0L) {
                return LuaValue.ZERO
            }

            val e = (bits shr 52 and 0x7ffL).toInt() - 1023

            if (e >= 0 && e < 31) {
                val f = bits and 0xFFFFFFFFFFFFFL
                val shift = 52 - e
                val intPrecMask = (1L shl shift) - 1
                if (f and intPrecMask == 0L) {
                    val intValue = (f shr shift).toInt() or (1 shl e)
                    return LuaInteger.valueOf(if (bits shr 63 != 0L) -intValue else intValue)
                }
            }

            return LuaValue.valueOf(Double.fromBits(bits))
        }

        /**
         * Load input stream as a lua binary chunk if the first 4 bytes are the lua binary signature.
         * @param stream InputStream to read, after having read the first byte already
         * @param chunkname Name to apply to the loaded chunk
         * @return [Prototype] that was loaded, or null if the first 4 bytes were not the lua signature.
         * @com.soywiz.luak.compat.java.Throws IOException if an IOException occurs
         */

         fun undump(stream: LuaBinInput, chunkname: String): Prototype? {
            // check rest of signature
            if (stream.read() != LUA_SIGNATURE[0].toInt()
                || stream.read() != LUA_SIGNATURE[1].toInt()
                || stream.read() != LUA_SIGNATURE[2].toInt()
                || stream.read() != LUA_SIGNATURE[3].toInt()
            )
                return null

            // load file as a compiled chunk
            val sname = getSourceName(chunkname)
            val s = LoadState(stream, sname)
            s.loadHeader()

            // check format
            when (s.luacNumberFormat) {
                NUMBER_FORMAT_FLOATS_OR_DOUBLES, NUMBER_FORMAT_INTS_ONLY, NUMBER_FORMAT_NUM_PATCH_INT32 -> {
                }
                else -> throw LuaError("unsupported int size")
            }
            return s.loadFunction(LuaString.valueOf(sname))
        }

        /**
         * Construct a source name from a supplied chunk name
         * @param name String name that appears in the chunk
         * @return source file name
         */
         fun getSourceName(name: String): String {
            var sname = name
            if (name.startsWith("@") || name.startsWith("="))
                sname = name.substring(1)
            else if (name.startsWith("\u001b"))
                sname = SOURCE_BINARY_STRING
            return sname
        }
    }
}
