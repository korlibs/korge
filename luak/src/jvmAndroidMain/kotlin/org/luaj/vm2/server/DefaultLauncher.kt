/*******************************************************************************
 * Copyright (c) 2015 Luaj.org. All rights reserved.
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
package org.luaj.vm2.server

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

/**
 * Default [Launcher] instance that creates standard globals
 * and runs the supplied scripts with chunk name 'main'.
 * <P>
 * Arguments are coerced into lua using [CoerceJavaToLua.coerce].
</P> * <P>
 * Return values with simple types are coerced into Java simple types.
 * Tables, threads, and functions are returned as lua objects.
 *
 * @see Launcher
 *
 * @see LuajClassLoader
 *
 * @see LuajClassLoader.NewLauncher
 * @see LuajClassLoader.NewLauncher
 * @since luaj 3.0.1
</P> */
class DefaultLauncher : Launcher {
    protected var g: Globals = JsePlatform.standardGlobals()

    /** Launches the script with chunk name 'main'  */
    override fun launch(script: String, arg: Array<Any>?): Array<Any?>? = launchChunk(g.load(script, "main"), arg!!)

    /** Launches the script with chunk name 'main' and loading using modes 'bt'  */
    override fun launch(script: LuaBinInput, arg: Array<Any>?): Array<Any?>? =
        launchChunk(g.load(script, "main", "bt", g), arg!!)

    /** Launches the script with chunk name 'main'  */
    override fun launch(script: LuaReader, arg: Array<Any>?): Array<Any?> = launchChunk(g.load(script, "main"), arg!!)

    private fun launchChunk(chunk: LuaValue, arg: Array<Any>): Array<Any?> {
        val args = Array(arg.size) { CoerceJavaToLua.coerce(arg[it]) }
        val results = chunk.invoke(LuaValue.varargsOf(args))

        return Array(results.narg()) { i ->
            val r = results.arg(i + 1)
            when (r.type()) {
                LuaValue.TBOOLEAN -> r.toboolean()
                LuaValue.TNUMBER -> r.todouble()
                LuaValue.TINT -> r.toint()
                LuaValue.TNIL -> null
                LuaValue.TSTRING -> r.tojstring()
                LuaValue.TUSERDATA -> r.touserdata()
                else -> r
            }
        }
    }
}
