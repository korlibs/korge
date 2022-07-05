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
package org.luaj.luajc

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Print
import org.luaj.vm2.Prototype
import org.luaj.vm2.lib.jse.JsePlatform

/** Test the plain old bytecode interpreter  */
object TestLuaJ {
    // create the script
    var name = "script"
    var script = "function r(q,...)\n" +
            "	local a=arg\n" +
            "	return a and a[2]\n" +
            "end\n" +
            "function s(q,...)\n" +
            "	local a=arg\n" +
            "	local b=...\n" +
            "	return a and a[2],b\n" +
            "end\n" +
            "print( r(111,222,333),s(111,222,333) )"

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println(script)

        // create an environment to run in
        val globals = JsePlatform.standardGlobals()

        // compile into a chunk, or load as a class
        val chunk = globals.load(script, "script")

        // The loaded chunk should be a closure, which contains the prototype.
        print(chunk.checkclosure()!!.p)

        // The chunk can be called with arguments as desired.
        chunk.call(LuaValue.ZERO, LuaValue.ONE)
    }

    private fun print(p: Prototype) {
        println("--- $p")
        Print.printCode(p)
        if (p.p != null) {
            var i = 0
            val n = p.p.size
            while (i < n) {
                print(p.p[i])
                i++
            }
        }
    }

}
