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
package org.luaj.vm2.lib

import org.luaj.vm2.Globals
import org.luaj.vm2.Lua
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaNil
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Print
import org.luaj.vm2.Prototype
import org.luaj.vm2.Varargs
import org.luaj.vm2.internal.*
import kotlin.jvm.*
import kotlin.math.*

/**
 * Subclass of [LibFunction] which implements the lua standard `debug`
 * library.
 *
 *
 * The debug library in luaj tries to emulate the behavior of the corresponding C-based lua library.
 * To do this, it must maintain a separate stack of calls to [LuaClosure] and [LibFunction]
 * instances.
 * Especially when lua-to-java bytecode compiling is being used
 * via a [org.luaj.vm2.Globals.Compiler] such as [org.luaj.vm2.luajc.LuaJC],
 * this cannot be done in all cases.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.debugGlobals] or
 * [org.luaj.vm2.lib.jme.JmePlatform.debugGlobals]
 * <pre> `Globals globals = JsePlatform.debugGlobals();
 * System.out.println( globals.get("debug").get("traceback").call() );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new DebugLib());
 * System.out.println( globals.get("debug").get("traceback").call() );
` *  </pre>
 *
 *
 * This library exposes the entire state of lua code, and provides method to see and modify
 * all underlying lua values within a Java VM so should not be exposed to client code
 * in a shared server environment.
 *
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 Debug Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.10)
 */
class DebugLib : TwoArgFunction() {

    lateinit internal var globals: Globals

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        globals.debuglib = this
        val debug = LuaTable()
        debug.set("debug", Debug())
        debug["gethook"] = Gethook()
        debug["getinfo"] = Getinfo()
        debug["getlocal"] = Getlocal()
        debug.set("getmetatable", Getmetatable())
        debug["getregistry"] = Getregistry()
        debug["getupvalue"] = Getupvalue()
        debug["getuservalue"] = Getuservalue()
        debug["sethook"] = Sethook()
        debug["setlocal"] = Setlocal()
        debug["setmetatable"] = Setmetatable()
        debug["setupvalue"] = Setupvalue()
        debug["setuservalue"] = Setuservalue()
        debug["traceback"] = Traceback()
        debug["upvalueid"] = Upvalueid()
        debug["upvaluejoin"] = Upvaluejoin()
        env["debug"] = debug
        env["package"]["loaded"]["debug"] = debug
        return debug
    }

    // debug.debug()
    internal class Debug : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.NONE
        }
    }

    // debug.gethook ([thread])
    internal inner class Gethook : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t = if (args.narg() > 0) args.checkthread(1) else globals.running
            val s = t!!.state
            return LuaValue.varargsOf(
                if (s.hookfunc != null) s.hookfunc!! else LuaValue.NIL,
                LuaValue.valueOf((if (s.hookcall) "c" else "") + (if (s.hookline) "l" else "") + if (s.hookrtrn) "r" else ""),
                LuaValue.valueOf(s.hookcount)
            )
        }
    }

    //	debug.getinfo ([thread,] f [, what])
    internal inner class Getinfo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread = if (args.isthread(a)) args.checkthread(a++) else globals.running
            var func: LuaValue? = args.arg(a++)
            val what = args.optjstring(a++, "flnStu")
            val callstack = callstack(thread!!)

            // find the stack info
            val frame: CallFrame?
            if (func!!.isnumber()) {
                frame = callstack.getCallFrame(func.toint())
                if (frame == null)
                    return LuaValue.NONE
                func = frame.f
            } else if (func.isfunction()) {
                frame = callstack.findCallFrame(func)
            } else {
                return LuaValue.argerror(a - 2, "function or level")
            }

            // start a table
            val ar = callstack.auxgetinfo(what!!, func as LuaFunction?, frame)
            val info = LuaTable()
            if (what.indexOf('S') >= 0) {
                info[WHAT] = LUA
                info[SOURCE] = LuaValue.valueOf(ar.source)
                info[SHORT_SRC] = LuaValue.valueOf(ar.short_src)
                info[LINEDEFINED] = LuaValue.valueOf(ar.linedefined)
                info[LASTLINEDEFINED] = LuaValue.valueOf(ar.lastlinedefined)
            }
            if (what.indexOf('l') >= 0) {
                info[CURRENTLINE] = LuaValue.valueOf(ar.currentline)
            }
            if (what.indexOf('u') >= 0) {
                info[NUPS] = LuaValue.valueOf(ar.nups.toInt())
                info[NPARAMS] = LuaValue.valueOf(ar.nparams.toInt())
                info[ISVARARG] = if (ar.isvararg) LuaValue.ONE else LuaValue.ZERO
            }
            if (what.indexOf('n') >= 0) {
                info[NAME] = LuaValue.valueOf(if (ar.name != null) ar.name!! else "?")
                info[NAMEWHAT] = LuaValue.valueOf(ar.namewhat!!)
            }
            if (what.indexOf('t') >= 0) {
                info[ISTAILCALL] = LuaValue.ZERO
            }
            if (what.indexOf('L') >= 0) {
                val lines = LuaTable()
                info[ACTIVELINES] = lines
                var cf: CallFrame?
                var l = 1
                while ((run { cf = callstack.getCallFrame(l); cf }) != null) {
                    if (cf!!.f === func)
                        lines.insert(-1, LuaValue.valueOf(cf!!.currentline()))
                    ++l
                }
            }
            if (what.indexOf('f') >= 0) {
                if (func != null)
                    info[FUNC] = func
            }
            return info
        }
    }

    //	debug.getlocal ([thread,] f, local)
    internal inner class Getlocal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread = if (args.isthread(a)) args.checkthread(a++) else globals.running
            val level = args.checkint(a++)
            val local = args.checkint(a++)
            val f = callstack(thread!!).getCallFrame(level)
            return f?.getLocal(local) ?: LuaValue.NONE
        }
    }

    //	debug.getmetatable (value)
    internal inner class Getmetatable : LibFunction() {
        override fun call(v: LuaValue): LuaValue {
            val mt = v.getmetatable()
            return mt ?: LuaValue.NIL
        }
    }

    //	debug.getregistry ()
    internal inner class Getregistry : ZeroArgFunction() {
        override fun call(): LuaValue {
            return globals!!
        }
    }

    //	debug.getupvalue (f, up)
    internal class Getupvalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func = args.checkfunction(1)
            val up = args.checkint(2)
            if (func is LuaClosure) {
                val c = func as LuaClosure?
                val name = findupvalue(c!!, up)
                if (name != null) {
                    return LuaValue.varargsOf(name, c.upValues[up - 1]!!.value!!)
                }
            }
            return LuaValue.NIL
        }
    }

    //	debug.getuservalue (u)
    internal class Getuservalue : LibFunction() {
        override fun call(u: LuaValue): LuaValue {
            return if (u.isuserdata()) u else LuaValue.NIL
        }
    }


    // debug.sethook ([thread,] hook, mask [, count])
    internal inner class Sethook : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val t = if (args.isthread(a)) args.checkthread(a++) else globals.running
            val func = args.optfunction(a++, null)
            val str = args.optjstring(a++, "")
            val count = args.optint(a++, 0)
            var call = false
            var line = false
            var rtrn = false
            for (i in 0 until str!!.length)
                when (str[i]) {
                    'c' -> call = true
                    'l' -> line = true
                    'r' -> rtrn = true
                }
            val s = t!!.state
            s.hookfunc = func
            s.hookcall = call
            s.hookline = line
            s.hookcount = count
            s.hookrtrn = rtrn
            return LuaValue.NONE
        }
    }

    //	debug.setlocal ([thread,] level, local, value)
    internal inner class Setlocal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread = if (args.isthread(a)) args.checkthread(a++) else globals.running
            val level = args.checkint(a++)
            val local = args.checkint(a++)
            val value = args.arg(a++)
            val f = callstack(thread!!).getCallFrame(level)
            return f?.setLocal(local, value) ?: LuaValue.NONE
        }
    }

    //	debug.setmetatable (value, table)
    internal inner class Setmetatable : TwoArgFunction() {
        override fun call(value: LuaValue, table: LuaValue): LuaValue {
            val mt = table.opttable(null)
            when (value.type()) {
                LuaValue.TNIL -> LuaNil.s_metatable = mt
                LuaValue.TNUMBER -> LuaNumber.s_metatable = mt
                LuaValue.TBOOLEAN -> LuaBoolean.s_metatable = mt
                LuaValue.TSTRING -> LuaString.s_metatable = mt
                LuaValue.TFUNCTION -> LuaFunction.s_metatable = mt
                LuaValue.TTHREAD -> LuaThread.s_metatable = mt
                else -> value.setmetatable(mt)
            }
            return value
        }
    }

    //	debug.setupvalue (f, up, value)
    internal inner class Setupvalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func = args.checkfunction(1)
            val up = args.checkint(2)
            val value = args.arg(3)
            if (func is LuaClosure) {
                val c = func as LuaClosure?
                val name = findupvalue(c!!, up)
                if (name != null) {
                    c.upValues[up - 1]?.value = value
                    return name
                }
            }
            return LuaValue.NIL
        }
    }

    //	debug.setuservalue (udata, value)
    internal inner class Setuservalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val o = args.checkuserdata(1)
            val v = args.checkvalue(2)
            val u = args.arg1() as LuaUserdata
            u.m_instance = v.checkuserdata()!!
            u.m_metatable = v.getmetatable()
            return LuaValue.NONE
        }
    }

    //	debug.traceback ([thread,] [message [, level]])
    internal inner class Traceback : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread = if (args.isthread(a)) args.checkthread(a++) else globals.running
            val message = args.optjstring(a++, null)
            val level = args.optint(a++, 1)
            val tb = callstack(thread!!).traceback(level)
            return LuaValue.valueOf(if (message != null) message + "\n" + tb else tb)
        }
    }

    //	debug.upvalueid (f, n)
    internal inner class Upvalueid : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func = args.checkfunction(1)
            val up = args.checkint(2)
            if (func is LuaClosure) {
                val c = func as LuaClosure?
                if (c!!.upValues != null && up > 0 && up <= c.upValues.size) {
                    return LuaValue.valueOf(c.upValues[up - 1].hashCode())
                }
            }
            return LuaValue.NIL
        }
    }

    //	debug.upvaluejoin (f1, n1, f2, n2)
    internal inner class Upvaluejoin : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val f1 = args.checkclosure(1)
            val n1 = args.checkint(2)
            val f2 = args.checkclosure(3)
            val n2 = args.checkint(4)
            if (n1 < 1 || n1 > f1!!.upValues.size)
                argerror("index out of range")
            if (n2 < 1 || n2 > f2!!.upValues.size)
                argerror("index out of range")
            f1.upValues[n1 - 1] = f2.upValues[n2 - 1]
            return LuaValue.NONE
        }
    }

    fun onCall(f: LuaFunction) {
        val s = globals.running.state
        if (s.inhook) return
        callstack().onCall(f)
        if (s.hookcall) callHook(s, CALL, LuaValue.NIL)
    }

    fun onCall(c: LuaClosure, varargs: Varargs, stack: Array<LuaValue>) {
        val s = globals.running.state
        if (s.inhook) return
        callstack().onCall(c, varargs, stack)
        if (s.hookcall) callHook(s, CALL, LuaValue.NIL)
    }

    fun onInstruction(pc: Int, v: Varargs, top: Int) {
        val s = globals.running.state
        if (s.inhook) return
        callstack().onInstruction(pc, v, top)
        if (s.hookfunc == null) return
        if (s.hookcount > 0)
            if (++s.bytecodes % s.hookcount == 0)
                callHook(s, COUNT, LuaValue.NIL)
        if (s.hookline) {
            val newline = callstack().currentline()
            if (newline != s.lastline) {
                s.lastline = newline
                callHook(s, LINE, LuaValue.valueOf(newline))
            }
        }
    }

    fun onReturn() {
        val s = globals.running.state
        if (s.inhook) return
        callstack().onReturn()
        if (s.hookrtrn) callHook(s, RETURN, LuaValue.NIL)
    }

    fun traceback(level: Int): String {
        return callstack().traceback(level)
    }

    internal fun callHook(s: LuaThread.State, type: LuaValue, arg: LuaValue) {
        if (s.inhook || s.hookfunc == null) return
        s.inhook = true
        try {
            s.hookfunc!!.call(type, arg)
        } catch (e: LuaError) {
            throw e
        } catch (e: RuntimeException) {
            throw LuaError(e)
        } finally {
            s.inhook = false
        }
    }

    @JvmOverloads
    internal fun callstack(t: LuaThread = globals.running): CallStack {
        if (t.callstack == null)
            t.callstack = CallStack()
        return t.callstack as CallStack
    }

    internal class DebugInfo {
        var name: String? = null    /* (n) */
        var namewhat: String? = null    /* (n) 'global', 'local', 'field', 'method' */
        var what: String = ""    /* (S) 'Lua', 'C', 'main', 'tail' */
        var source: String = ""    /* (S) */
        var currentline: Int = 0    /* (l) */
        var linedefined: Int = 0    /* (S) */
        var lastlinedefined: Int = 0    /* (S) */
        var nups: Short = 0    /* (u) number of upvalues */
        var nparams: Short = 0/* (u) number of parameters */
        var isvararg: Boolean = false        /* (u) */
        var istailcall: Boolean = false    /* (t) */
        var short_src: String = "" /* (S) */
        var cf: CallFrame? = null  /* active function */

        fun funcinfo(f: LuaFunction) {
            if (f.isclosure()) {
                val p = f.checkclosure()!!.p
                this.source = if (p.source != null) p.source.tojstring() else "=?"
                this.linedefined = p.linedefined
                this.lastlinedefined = p.lastlinedefined
                this.what = if (this.linedefined == 0) "main" else "Lua"
                this.short_src = p.shortsource()
            } else {
                this.source = "=[Java]"
                this.linedefined = -1
                this.lastlinedefined = -1
                this.what = "Java"
                this.short_src = f.name()
            }
        }
    }

    class CallStack internal constructor() {
        internal var frame = EMPTY
        internal var calls = 0

        @Synchronized
        internal fun currentline(): Int {
            return if (calls > 0) frame[calls - 1].currentline() else -1
        }

        @Synchronized
        private fun pushcall(): CallFrame {
            if (calls >= frame.size) {
                val n = max(4, frame.size * 3 / 2)
                val f = arrayOfNulls<CallFrame>(n)
                arraycopy(frame as Array<CallFrame?>, 0, f, 0, frame.size)
                for (i in frame.size until n)
                    f[i] = CallFrame()
                frame = f as Array<CallFrame>
                for (i in 1 until n)
                    f[i].previous = f[i - 1]
            }
            return frame[calls++]
        }

        @Synchronized
        internal fun onCall(function: LuaFunction) {
            pushcall().set(function)
        }

        @Synchronized
        internal fun onCall(function: LuaClosure, varargs: Varargs, stack: Array<LuaValue>) {
            pushcall()[function, varargs] = stack
        }

        @Synchronized
        internal fun onReturn() {
            if (calls > 0)
                frame[--calls].reset()
        }

        @Synchronized
        internal fun onInstruction(pc: Int, v: Varargs, top: Int) {
            if (calls > 0)
                frame[calls - 1].instr(pc, v, top)
        }

        /**
         * Get the traceback starting at a specific level.
         * @param level
         * @return String containing the traceback.
         */
        @Synchronized
        internal fun traceback(level: Int): String {
            var level = level
            val sb = StringBuilder()
            sb.append("stack traceback:")
            var c: CallFrame?
            while ((run { c = getCallFrame(level++); c }) != null) {
                val c = c!!
                sb.append("\n\t")
                sb.append(c!!.shortsource())
                sb.append(':')
                if (c.currentline() > 0)
                    sb.append(c.currentline().toString() + ":")
                sb.append(" in ")
                val ar = auxgetinfo("n", c.f, c)
                if (c.linedefined() == 0)
                    sb.append("main chunk")
                else if (ar.name != null) {
                    sb.append("function '")
                    sb.append(ar.name!!)
                    sb.append('\'')
                } else {
                    sb.append("function <" + c.shortsource() + ":" + c.linedefined() + ">")
                }
            }
            sb.append("\n\t[Java]: in ?")
            return sb.toString()
        }

        @Synchronized
        internal fun getCallFrame(level: Int): CallFrame? {
            return if (level < 1 || level > calls) null else frame[calls - level]
        }

        @Synchronized
        internal fun findCallFrame(func: LuaValue): CallFrame? {
            for (i in 1..calls)
                if (frame[calls - i].f === func)
                    return frame[i]
            return null
        }


        @Synchronized
        internal fun auxgetinfo(what: String, f: LuaFunction?, ci: CallFrame?): DebugInfo {
            val ar = DebugInfo()
            var i = 0
            val n = what.length
            while (i < n) {
                when (what[i]) {
                    'S' -> ar.funcinfo(f!!)
                    'l' -> ar.currentline = if (ci != null && ci.f!!.isclosure()) ci.currentline() else -1
                    'u' -> if (f != null && f.isclosure()) {
                        val p = f.checkclosure()!!.p
                        ar.nups = p.upvalues.size.toShort()
                        ar.nparams = p.numparams.toShort()
                        ar.isvararg = p.is_vararg != 0
                    } else {
                        ar.nups = 0
                        ar.isvararg = true
                        ar.nparams = 0
                    }
                    't' -> ar.istailcall = false
                    'n' -> {
                        /* calling function is a known Lua function? */
                        if (ci != null && ci.previous != null) {
                            if (ci.previous!!.f!!.isclosure()) {
                                val nw = getfuncname(ci.previous!!)
                                if (nw != null) {
                                    ar.name = nw.name
                                    ar.namewhat = nw.namewhat
                                }
                            }
                        }
                        if (ar.namewhat == null) {
                            ar.namewhat = ""  /* not found */
                            ar.name = null
                        }
                    }
                    'L', 'f' -> {
                    }
                    else -> {
                    }
                }// TODO: return bad status.
                ++i
            }
            return ar
        }

        companion object {
            internal val EMPTY = arrayOf<CallFrame>()
        }

    }

    internal class CallFrame {
        var f: LuaFunction? = null
        var pc: Int = 0
        var top: Int = 0
        var v: Varargs? = null
        var stack: Array<LuaValue>? = null
        var previous: CallFrame? = null
        operator fun set(function: LuaClosure, varargs: Varargs, stack: Array<LuaValue>) {
            this.f = function
            this.v = varargs
            this.stack = stack
        }

        fun shortsource(): String {
            return if (f!!.isclosure()) f!!.checkclosure()!!.p.shortsource() else "[Java]"
        }

        fun set(function: LuaFunction) {
            this.f = function
        }

        fun reset() {
            this.f = null
            this.v = null
            this.stack = null
        }

        fun instr(pc: Int, v: Varargs, top: Int) {
            this.pc = pc
            this.v = v
            this.top = top
            if (TRACE)
                Print.printState(f!!.checkclosure()!!, pc, stack!! as Array<LuaValue?>, top, v)
        }

        fun getLocal(i: Int): Varargs {
            val name = getlocalname(i)
            return if (name != null)
                LuaValue.varargsOf(name, stack!![i - 1])
            else
                LuaValue.NIL
        }

        fun setLocal(i: Int, value: LuaValue): Varargs {
            val name = getlocalname(i)
            if (name != null) {
                stack!![i - 1] = value
                return name
            } else {
                return LuaValue.NIL
            }
        }

        fun currentline(): Int {
            if (!f!!.isclosure()) return -1
            val li = f!!.checkclosure()!!.p.lineinfo
            return if (li == null || pc < 0 || pc >= li.size) -1 else li[pc]
        }

        fun sourceline(): String {
            return if (!f!!.isclosure()) f!!.tojstring() else f!!.checkclosure()!!.p.shortsource() + ":" + currentline()
        }

        fun linedefined(): Int {
            return if (f!!.isclosure()) f!!.checkclosure()!!.p.linedefined else -1
        }

        fun getlocalname(index: Int): LuaString? {
            return if (!f!!.isclosure()) null else f!!.checkclosure()!!.p.getlocalname(index, pc)
        }
    }

    class NameWhat(val name: String, val namewhat: String)

    companion object {
        var CALLS: Boolean = false
        var TRACE: Boolean = false

        init {
            try {
                CALLS = null != JSystem.getProperty("CALLS")
            } catch (e: Exception) {
            }

            try {
                TRACE = null != JSystem.getProperty("TRACE")
            } catch (e: Exception) {
            }

        }

        private val LUA = LuaValue.valueOf("Lua")
        private val QMARK = LuaValue.valueOf("?")
        private val CALL = LuaValue.valueOf("call")
        private val LINE = LuaValue.valueOf("line")
        private val COUNT = LuaValue.valueOf("count")
        private val RETURN = LuaValue.valueOf("return")

        private val FUNC = LuaValue.valueOf("func")
        private val ISTAILCALL = LuaValue.valueOf("istailcall")
        private val ISVARARG = LuaValue.valueOf("isvararg")
        private val NUPS = LuaValue.valueOf("nups")
        private val NPARAMS = LuaValue.valueOf("nparams")
        private val NAME = LuaValue.valueOf("name")
        private val NAMEWHAT = LuaValue.valueOf("namewhat")
        private val WHAT = LuaValue.valueOf("what")
        private val SOURCE = LuaValue.valueOf("source")
        private val SHORT_SRC = LuaValue.valueOf("short_src")
        private val LINEDEFINED = LuaValue.valueOf("linedefined")
        private val LASTLINEDEFINED = LuaValue.valueOf("lastlinedefined")
        private val CURRENTLINE = LuaValue.valueOf("currentline")
        private val ACTIVELINES = LuaValue.valueOf("activelines")

        internal fun findupvalue(c: LuaClosure, up: Int): LuaString? {
            return if (c.upValues != null && up > 0 && up <= c.upValues.size) {
                if (c.p.upvalues != null && up <= c.p.upvalues.size)
                    c.p.upvalues[up - 1].name
                else
                    LuaString.valueOf(".$up")
            } else null
        }

        internal fun lua_assert(x: Boolean) {
            if (!x) throw RuntimeException("lua_assert failed")
        }

        // Return the name info if found, or null if no useful information could be found.
        internal fun getfuncname(frame: CallFrame): NameWhat? {
            if (!frame.f!!.isclosure())
                return NameWhat(frame.f!!.classnamestub(), "Java")
            val p = frame.f!!.checkclosure()!!.p
            val pc = frame.pc
            val i = p.code[pc] /* calling instruction */
            val tm: LuaString
            when (Lua.GET_OPCODE(i)) {
                Lua.OP_CALL, Lua.OP_TAILCALL /* get function name */ -> return getobjname(p, pc, Lua.GETARG_A(i))
                Lua.OP_TFORCALL /* for iterator */ -> return NameWhat("(for iterator)", "(for iterator")
                /* all other instructions can call only through metamethods */
                Lua.OP_SELF, Lua.OP_GETTABUP, Lua.OP_GETTABLE -> tm = LuaValue.INDEX
                Lua.OP_SETTABUP, Lua.OP_SETTABLE -> tm = LuaValue.NEWINDEX
                Lua.OP_EQ -> tm = LuaValue.EQ
                Lua.OP_ADD -> tm = LuaValue.ADD
                Lua.OP_SUB -> tm = LuaValue.SUB
                Lua.OP_MUL -> tm = LuaValue.MUL
                Lua.OP_DIV -> tm = LuaValue.DIV
                Lua.OP_MOD -> tm = LuaValue.MOD
                Lua.OP_POW -> tm = LuaValue.POW
                Lua.OP_UNM -> tm = LuaValue.UNM
                Lua.OP_LEN -> tm = LuaValue.LEN
                Lua.OP_LT -> tm = LuaValue.LT
                Lua.OP_LE -> tm = LuaValue.LE
                Lua.OP_CONCAT -> tm = LuaValue.CONCAT
                else -> return null  /* else no useful name can be found */
            }
            return NameWhat(tm.tojstring(), "metamethod")
        }

        // return NameWhat if found, null if not
        fun getobjname(p: Prototype, lastpc: Int, reg: Int): NameWhat? {
            var pc = lastpc // currentpc(L, ci);
            var name = p.getlocalname(reg + 1, pc)
            if (name != null)
            /* is a local? */
                return NameWhat(name.tojstring(), "local")

            /* else try symbolic execution */
            pc = findsetreg(p, lastpc, reg)
            if (pc != -1) { /* could find instruction? */
                val i = p.code[pc]
                when (Lua.GET_OPCODE(i)) {
                    Lua.OP_MOVE -> {
                        val a = Lua.GETARG_A(i)
                        val b = Lua.GETARG_B(i) /* move from `b' to `a' */
                        if (b < a)
                            return getobjname(p, pc, b) /* get name for `b' */
                    }
                    Lua.OP_GETTABUP, Lua.OP_GETTABLE -> {
                        val k = Lua.GETARG_C(i) /* key index */
                        val t = Lua.GETARG_B(i) /* table index */
                        val vn = if (Lua.GET_OPCODE(i) == Lua.OP_GETTABLE  /* name of indexed variable */)
                            p.getlocalname(t + 1, pc)
                        else
                            if (t < p.upvalues.size) p.upvalues[t].name else QMARK
                        name = kname(p, k)
                        return NameWhat(
                            name!!.tojstring(),
                            if (vn != null && vn.eq_b(LuaValue.ENV)) "global" else "field"
                        )
                    }
                    Lua.OP_GETUPVAL -> {
                        val u = Lua.GETARG_B(i) /* upvalue index */
                        name = if (u < p.upvalues.size) p.upvalues[u].name else QMARK
                        return NameWhat(name!!.tojstring(), "upvalue")
                    }
                    Lua.OP_LOADK, Lua.OP_LOADKX -> {
                        val b = if (Lua.GET_OPCODE(i) == Lua.OP_LOADK)
                            Lua.GETARG_Bx(i)
                        else
                            Lua.GETARG_Ax(p.code[pc + 1])
                        if (p.k[b].isstring()) {
                            name = p.k[b].strvalue()
                            return NameWhat(name!!.tojstring(), "constant")
                        }
                    }
                    Lua.OP_SELF -> {
                        val k = Lua.GETARG_C(i) /* key index */
                        name = kname(p, k)
                        return NameWhat(name!!.tojstring(), "method")
                    }
                    else -> {
                    }
                }
            }
            return null /* no useful name found */
        }

        internal fun kname(p: Prototype, c: Int): LuaString? {
            return if (Lua.ISK(c) && p.k[Lua.INDEXK(c)].isstring())
                p.k[Lua.INDEXK(c)].strvalue()
            else
                QMARK
        }

        /*
	** try to find last instruction before 'lastpc' that modified register 'reg'
	*/
        internal fun findsetreg(p: Prototype, lastpc: Int, reg: Int): Int {
            var pc: Int
            var setreg = -1  /* keep last instruction that changed 'reg' */
            pc = 0
            while (pc < lastpc) {
                val i = p.code[pc]
                val op = Lua.GET_OPCODE(i)
                val a = Lua.GETARG_A(i)
                when (op) {
                    Lua.OP_LOADNIL -> {
                        val b = Lua.GETARG_B(i)
                        if (a <= reg && reg <= a + b)
                        /* set registers from 'a' to 'a+b' */
                            setreg = pc
                    }
                    Lua.OP_TFORCALL -> {
                        if (reg >= a + 2) setreg = pc  /* affect all regs above its base */
                    }
                    Lua.OP_CALL, Lua.OP_TAILCALL -> {
                        if (reg >= a) setreg = pc  /* affect all registers above base */
                    }
                    Lua.OP_JMP -> {
                        val b = Lua.GETARG_sBx(i)
                        val dest = pc + 1 + b
                        /* jump is forward and do not skip `lastpc'? */
                        if (pc < dest && dest <= lastpc)
                            pc += b  /* do the jump */
                    }
                    Lua.OP_TEST -> {
                        if (reg == a) setreg = pc  /* jumped code can change 'a' */
                    }
                    else -> if (Lua.testAMode(op) && reg == a)
                    /* any instruction that set A */
                        setreg = pc
                }
                pc++
            }
            return setreg
        }
    }
}
