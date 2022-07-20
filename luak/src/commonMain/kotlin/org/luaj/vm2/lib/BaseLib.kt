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

import org.luaj.vm2.*
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

/**
 * Subclass of [LibFunction] which implements the lua basic library functions.
 *
 *
 * This contains all library functions listed as "basic functions" in the lua documentation for JME.
 * The functions dofile and loadfile use the
 * [Globals.finder] instance to find resource files.
 * Since JME has no file system by default, [BaseLib] implements
 * [ResourceFinder] using [Class.getResource],
 * which is the closest equivalent on JME.
 * The default loader chain in [PackageLib] will use these as well.
 *
 *
 * To use basic library functions that include a [ResourceFinder] based on
 * directory lookup, use [org.luaj.vm2.lib.jse.JseBaseLib] instead.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or
 * [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.get("print").call(LuaValue.valueOf("hello, world"));
` *  </pre>
 *
 *
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.get("print").call(LuaValue.valueOf("hello, world"));
` *  </pre>
 * Doing so will ensure the library is properly initialized
 * and loaded into the globals table.
 *
 *
 * This is a direct port of the corresponding library in C.
 * @see org.luaj.vm2.lib.jse.JseBaseLib
 *
 * @see ResourceFinder
 *
 * @see Globals.finder
 *
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 Base Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.1)
 */
open class BaseLib : TwoArgFunction(), ResourceFinder {

    internal var globals: Globals? = null


    /** Perform one-time initialization on the library by adding base functions
     * to the supplied environment, and returning it as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        globals!!.finder = this
        globals!!.baselib = this
        env["_G"] = env
        env["_VERSION"] = Lua._VERSION
        env["assert"] = _Assert()
        env["collectgarbage"] = Collectgarbage()
        env["dofile"] = Dofile()
        env["error"] = Error()
        env["getmetatable"] = Getmetatable()
        env["load"] = Load()
        env["loadfile"] = Loadfile()
        env["pcall"] = Pcall()
        env["print"] = Print(this)
        env["rawequal"] = Rawequal()
        env["rawget"] = Rawget()
        env["rawlen"] = Rawlen()
        env["rawset"] = Rawset()
        env["select"] = Select()
        env["setmetatable"] = Setmetatable()
        env["tonumber"] = Tonumber()
        env["tostring"] = Tostring()
        env["type"] = Type()
        env["xpcall"] = Xpcall()

        val next = Next()
        env["next"] = next
        env["pairs"] = Pairs(next)
        env["ipairs"] = Ipairs()

        return env
    }

    /** ResourceFinder implementation
     *
     * Tries to open the file as a resource, which can work for JSE and JME.
     */
    override fun findResource(filename: String): LuaBinInput? {
        return this::class.getResourceAsStreamPortable(if (filename.startsWith("/")) filename else "/$filename")
    }


    // "assert", // ( v [,message] ) -> v, message | ERR
    internal class _Assert : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg1().toboolean())
                LuaValue.error(if (args.narg() > 1) args.optjstring(2, "assertion failed!")!! else "assertion failed!")
            return args
        }
    }

    // "collectgarbage", // ( opt [,arg] ) -> value
    internal class Collectgarbage : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val s = args.optjstring(1, "collect")
            when (s) {
                "collect" -> {
                    JSystem.gc()
                    return LuaValue.ZERO
                }
                "count" -> {
                    val used = JSystem.totalMemory() - JSystem.freeMemory()
                    return LuaValue.varargsOf(LuaValue.valueOf(used / 1024.0), LuaValue.valueOf((used % 1024).toDouble()))
                }
                "step" -> {
                    JSystem.gc()
                    return LuaValue.BTRUE
                }
                else -> this.argerror("gc op")
            }
        }
    }

    // "dofile", // ( filename ) -> result1, ...
    internal inner class Dofile : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil")
            val filename = if (args.isstring(1)) args.tojstring(1) else null
            val v = if (filename == null)
                loadStream(globals!!.STDIN, "=stdin", "bt", globals)
            else
                loadFile(args.checkjstring(1), "bt", globals)
            return if (v.isnil(1)) LuaValue.error(v.tojstring(2)) else v.arg1().invoke()
        }
    }

    // "error", // ( message [,level] ) -> ERR
    internal class Error : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            throw if (arg1.isnil())
                LuaError(null!!, arg2.optint(1))
            else if (arg1.isstring())
                LuaError(arg1.tojstring(), arg2.optint(1))
            else
                LuaError(arg1)
        }
    }

    // "getmetatable", // ( object ) -> table
    internal class Getmetatable : LibFunction() {
        override fun call(): LuaValue {
            return LuaValue.argerror(1, "value")
        }

        override fun call(arg: LuaValue): LuaValue {
            val mt = arg.getmetatable()
            return mt?.rawget(LuaValue.METATABLE)?.optvalue(mt) ?: LuaValue.NIL
        }
    }

    // "load", // ( ld [, source [, mode [, env]]] ) -> chunk | nil, msg
    internal inner class Load : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ld = args.arg1()
            args.argcheck(ld.isstring() || ld.isfunction(), 1, "ld must be string or function")
            val source = args.optjstring(2, if (ld.isstring()) ld.tojstring() else "=(load)")
            val mode = args.optjstring(3, "bt")
            val env = args.optvalue(4, globals!!)
            return loadStream(
                if (ld.isstring())
                    ld.strvalue()!!.toLuaBinInput()
                else
                    StringInputStream(ld.checkfunction()!!), source, mode, env
            )
        }
    }

    // "loadfile", // ( [filename [, mode [, env]]] ) -> chunk | nil, msg
    internal inner class Loadfile : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil")
            val filename = if (args.isstring(1)) args.tojstring(1) else null
            val mode = args.optjstring(2, "bt")
            val env = args.optvalue(3, globals!!)
            return filename?.let { loadFile(it, mode, env) } ?: loadStream(globals!!.STDIN, "=stdin", mode, env)
        }
    }

    // "pcall", // (f, arg1, ...) -> status, result1, ...
    internal inner class Pcall : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func = args.checkvalue(1)
            if (globals != null && globals!!.debuglib != null)
                globals!!.debuglib!!.onCall(this)
            try {
                return LuaValue.varargsOf(LuaValue.BTRUE, func.invoke(args.subargs(2)))
            } catch (le: LuaError) {
                val m = le.messageObject
                return LuaValue.varargsOf(LuaValue.BFALSE, m ?: LuaValue.NIL)
            } catch (e: Exception) {
                val m = e.message
                return LuaValue.varargsOf(LuaValue.BFALSE, LuaValue.valueOf(m ?: e.toString()))
            } finally {
                if (globals != null && globals!!.debuglib != null)
                    globals!!.debuglib!!.onReturn()
            }
        }
    }

    // "print", // (...) -> void
    internal inner class Print(val baselib: BaseLib) : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val tostring = globals!!["tostring"]
            var i = 1
            val n = args.narg()
            while (i <= n) {
                if (i > 1) globals!!.STDOUT.print('\t')
                val s = tostring.call(args.arg(i)).strvalue()
                globals!!.STDOUT.print(s!!.tojstring())
                i++
            }
            globals!!.STDOUT.println()
            return LuaValue.NONE
        }
    }


    // "rawequal", // (v1, v2) -> boolean
    internal class Rawequal : LibFunction() {
        override fun call(): LuaValue {
            return LuaValue.argerror(1, "value")
        }

        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.argerror(2, "value")
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            return LuaValue.valueOf(arg1.raweq(arg2))
        }
    }

    // "rawget", // (table, index) -> value
    internal class Rawget : LibFunction() {
        override fun call(): LuaValue {
            return LuaValue.argerror(1, "value")
        }

        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.argerror(2, "value")
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            return arg1.checktable()!!.rawget(arg2)
        }
    }


    // "rawlen", // (v) -> value
    internal class Rawlen : LibFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(arg.rawlen())
        }
    }

    // "rawset", // (table, index, value) -> table
    internal class Rawset : LibFunction() {
        override fun call(table: LuaValue): LuaValue {
            return LuaValue.argerror(2, "value")
        }

        override fun call(table: LuaValue, index: LuaValue): LuaValue {
            return LuaValue.argerror(3, "value")
        }

        override fun call(table: LuaValue, index: LuaValue, value: LuaValue): LuaValue {
            val t = table.checktable()
            t!!.rawset(index.checknotnil(), value)
            return t
        }
    }

    // "select", // (f, ...) -> value1, ...
    internal class Select : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val n = args.narg() - 1
            if (args.arg1() == LuaValue.valueOf("#"))
                return LuaValue.valueOf(n)
            val i = args.checkint(1)
            if (i == 0 || i < -n)
                LuaValue.argerror(1, "index out of range")
            return args.subargs(if (i < 0) n + i + 2 else i + 1)
        }
    }

    // "setmetatable", // (table, metatable) -> table
    internal class Setmetatable : LibFunction() {
        override fun call(table: LuaValue): LuaValue {
            return LuaValue.argerror(2, "value")
        }

        override fun call(table: LuaValue, metatable: LuaValue): LuaValue {
            val mt0 = table.checktable()!!.getmetatable()
            if (mt0 != null && !mt0.rawget(LuaValue.METATABLE).isnil())
                LuaValue.error("cannot change a protected metatable")
            return table.setmetatable(if (metatable.isnil()) null else metatable.checktable())
        }
    }

    // "tonumber", // (e [,base]) -> value
    internal class Tonumber : LibFunction() {
        override fun call(e: LuaValue): LuaValue {
            return e.tonumber()
        }

        override fun call(e: LuaValue, base: LuaValue): LuaValue {
            if (base.isnil())
                return e.tonumber()
            val b = base.checkint()
            if (b < 2 || b > 36)
                LuaValue.argerror(2, "base out of range")
            return e.checkstring()!!.tonumber(b)
        }
    }

    // "tostring", // (e) -> value
    internal class Tostring : LibFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val h = arg.metatag(LuaValue.TOSTRING)
            if (!h.isnil())
                return h.call(arg)
            val v = arg.tostring()
            return if (!v.isnil()) v else LuaValue.valueOf(arg.tojstring())
        }
    }

    // "type",  // (v) -> value
    internal class Type : LibFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(arg.typename())
        }
    }

    // "xpcall", // (f, err) -> result1, ...
    internal inner class Xpcall : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t = globals!!.running
            val preverror = t!!.errorfunc
            t.errorfunc = args.checkvalue(2)
            try {
                if (globals != null && globals!!.debuglib != null)
                    globals!!.debuglib!!.onCall(this)
                try {
                    return LuaValue.varargsOf(LuaValue.BTRUE, args.arg1().invoke(args.subargs(3)))
                } catch (le: LuaError) {
                    val m = le.messageObject
                    return LuaValue.varargsOf(LuaValue.BFALSE, m ?: LuaValue.NIL)
                } catch (e: Exception) {
                    val m = e.message
                    return LuaValue.varargsOf(LuaValue.BFALSE, LuaValue.valueOf(m ?: e.toString()))
                } finally {
                    if (globals != null && globals!!.debuglib != null)
                        globals!!.debuglib!!.onReturn()
                }
            } finally {
                t.errorfunc = preverror
            }
        }
    }

    // "pairs" (t) -> iter-func, t, nil
    internal class Pairs(val next: Next) : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return LuaValue.varargsOf(next, args.checktable(1)!!, LuaValue.NIL)
        }
    }

    // // "ipairs", // (t) -> iter-func, t, 0
    internal class Ipairs : VarArgFunction() {
        var inext = inext()
        override fun invoke(args: Varargs): Varargs {
            return LuaValue.varargsOf(inext, args.checktable(1)!!, LuaValue.ZERO)
        }
    }

    // "next"  ( table, [index] ) -> next-index, next-value
    internal class Next : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return args.checktable(1)!!.next(args.arg(2))
        }
    }

    // "inext" ( table, [int-index] ) -> next-index, next-value
    internal class inext : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return args.checktable(1)!!.inext(args.arg(2))
        }
    }

    /**
     * Load from a named file, returning the chunk or nil,error of can't load
     * @param env
     * @param mode
     * @return Varargs containing chunk, or NIL,error-text on error
     */
    fun loadFile(filename: String?, mode: String?, env: LuaValue?): Varargs {
        val `is` = globals!!.finder!!.findResource(filename!!) ?: return LuaValue.varargsOf(
            LuaValue.NIL,
            LuaValue.valueOf("cannot open $filename: No such file or directory")
        )
        try {
            return loadStream(`is`, "@$filename", mode, env)
        } finally {
            try {
                `is`.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun loadStream(`is`: LuaBinInput?, chunkname: String?, mode: String?, env: LuaValue?): Varargs {
        try {
            return if (`is` == null) LuaValue.varargsOf(
                LuaValue.NIL,
                LuaValue.valueOf("not found: " + chunkname!!)
            ) else globals!!.load(`is`, chunkname!!, mode!!, env!!)
        } catch (e: Exception) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message!!))
        }

    }


    private class StringInputStream internal constructor(internal val func: LuaValue) : LuaBinInput() {
        internal var bytes: ByteArray = byteArrayOf()
        internal var offset: Int = 0
        internal var remaining = 0
        override fun read(): Int {
            if (remaining <= 0) {
                val s = func.call()
                if (s.isnil())
                    return -1
                val ls = s.strvalue()
                bytes = ls!!.m_bytes
                offset = ls.m_offset
                remaining = ls.m_length
                if (remaining <= 0)
                    return -1
            }
            --remaining
            return bytes[offset++].toInt()
        }
    }
}
