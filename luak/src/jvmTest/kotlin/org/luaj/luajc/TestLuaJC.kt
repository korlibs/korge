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

import java.io.FileOutputStream
import java.io.InputStream
import java.util.Enumeration
import java.util.Hashtable

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Print
import org.luaj.vm2.Prototype
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.jse.JsePlatform

object TestLuaJC {
    // This file will be loaded using the finder as a resource, provided it is in the
    // build path.  This allows the debugger to find the file when stepping into the function.
    var filename = "perf/nsieve.lua"

    lateinit internal var globals: Globals

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isNotEmpty())
            filename = args[0]
        println("filename: $filename")
        try {

            // create an environment to run in
            globals = JsePlatform.standardGlobals()

            // print the chunk as a closure, and pretty-print the closure.
            var f = globals.loadfile(filename).arg1()
            val p = f.checkclosure()!!.p
            Print.print(p)

            // load into a luajc java-bytecode based chunk by installing the LuaJC compiler first
            if (!(args.size > 0 && args[0] == "nocompile")) {
                //LuaJC.install(globals);
                f = globals.loadfile(filename).arg1()
            }

            // call with arguments
            val v = f.invoke(LuaValue.NONE)

            // print the result
            println("result: $v")

            // Write out the files.
            // saveClasses();

        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    @Throws(Exception::class)
    private fun saveClasses() {
        throw RuntimeException("No JME")

        /*
        // create the chunk
		String destdir = ".";

		InputStream is = globals.finder.findResource(filename);
		Hashtable t = LuaJC.instance.compileAll(is, filename, filename, globals, true);

        // write out the chunk
    	for ( Enumeration e = t.keys(); e.hasMoreElements(); ) {
    		String key = (String) e.nextElement();
    		byte[] bytes = (byte[]) t.get(key);
    		String destpath = (destdir!=null? destdir+"/": "") + key + ".class";
    		System.out.println(
						"chunk "+filename+
						" from "+filename+
						" written to "+destpath
						+" length="+bytes.length+" bytes");
        	FileOutputStream fos = new FileOutputStream( destpath );
        	fos.write( bytes );
        	fos.close();
        }
         */

    }

}
