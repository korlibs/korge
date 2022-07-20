/*******************************************************************************
 * Copyright (c) 2007-2011 LuaJ. All rights reserved.
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
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/**
 * Subclass of [LibFunction] which implements the lua standard `coroutine`
 * library.
 *
 *
 * The coroutine library in luaj has the same behavior as the
 * coroutine library in C, but is implemented using Java Threads to maintain
 * the call state between invocations.  Therefore it can be yielded from anywhere,
 * similar to the "Coco" yield-from-anywhere patch available for C-based lua.
 * However, coroutines that are yielded but never resumed to complete their execution
 * may not be collected by the garbage collector.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("coroutine").get("running").call() );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new CoroutineLib());
 * System.out.println( globals.get("coroutine").get("running").call() );
` *  </pre>
 *
 *
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 Coroutine Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.2)
 */
class CoroutineLib : TwoArgFunction() {

    @kotlin.jvm.JvmField
    internal var globals: Globals? = null

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        val coroutine = LuaTable()
        coroutine["create"] = create()
        coroutine["resume"] = resume()
        coroutine["running"] = running()
        coroutine["status"] = status()
        coroutine["yield"] = yield()
        coroutine["wrap"] = wrap()
        env["coroutine"] = coroutine
        env["package"]["loaded"]["coroutine"] = coroutine
        return coroutine
    }

    internal inner class create : LibFunction() {
        override fun call(f: LuaValue): LuaValue {
            return LuaThread(globals!!, f.checkfunction())
        }
    }

    internal inner class resume : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t = args.checkthread(1)
            return t!!.resume(args.subargs(2))
        }
    }

    internal inner class running : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val r = globals!!.running
            return LuaValue.varargsOf(r!!, LuaValue.valueOf(r.isMainThread))
        }
    }

    internal class status : LibFunction() {
        override fun call(t: LuaValue): LuaValue {
            val lt = t.checkthread()
            return LuaValue.valueOf(lt!!.status)
        }
    }

    internal inner class yield : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return globals!!.yield(args)
        }
    }

    internal inner class wrap : LibFunction() {
        override fun call(f: LuaValue): LuaValue {
            val func = f.checkfunction()
            val thread = LuaThread(globals!!, func)
            return wrapper(thread)
        }
    }

    internal inner class wrapper(val luathread: LuaThread) : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val result = luathread.resume(args)
            return if (result.arg1().toboolean()) {
                result.subargs(2)
            } else {
                LuaValue.error(result.arg(2).tojstring())
            }
        }
    }

    companion object {

        internal var coroutine_count = 0
    }
}
