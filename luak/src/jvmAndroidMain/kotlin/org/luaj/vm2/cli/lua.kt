/*******************************************************************************
 * Copyright (c) 2009-2012 Luaj.org. All rights reserved.
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

package org.luaj.vm2.cli

import org.luaj.vm2.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.*
import java.io.*
import java.util.*
import kotlin.jvm.Throws

/**
 * lua command for use in JSE environments.
 */
object lua {
    private val version = Lua._VERSION + " Copyright (c) 2012 Luaj.org.org"

    private val usage = "usage: java -cp luaj-jse.jar lua [options] [script [args]].\n" +
            "Available options are:\n" +
            "  -e stat  execute string 'stat'\n" +
            "  -l name  require library 'name'\n" +
            "  -i       enter interactive mode after executing 'script'\n" +
            "  -v       show version information\n" +
            "  -b      	use luajc bytecode-to-bytecode compiler (requires bcel on class path)\n" +
            "  -n      	nodebug - do not load debug library by default\n" +
            "  -p      	print the prototype\n" +
            "  -c enc  	use the supplied encoding 'enc' for input files\n" +
            "  --       stop handling options\n" +
            "  -        execute stdin and stop handling options"

    private lateinit var globals: Globals
    private var print = false
    private var encoding: String? = null

    private fun usageExit() {
        println(usage)
        System.exit(-1)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // process args
        var interactive = args.size == 0
        var versioninfo = false
        var processing = true
        var nodebug = false
        var libs: Vector<String>? = null
        try {
            // stateful argument processing
            run {
                var i = 0
                while (i < args.size) {
                    if (!processing || !args[i].startsWith("-")) {
                        // input file - defer to last stage
                        break
                    } else if (args[i].length <= 1) {
                        // input file - defer to last stage
                        break
                    } else {
                        when (args[i][1]) {
                            'e' -> if (++i >= args.size)
                                usageExit()
                            'l' -> {
                                if (++i >= args.size)
                                    usageExit()
                                libs = if (libs != null) libs else Vector()
                                libs!!.addElement(args[i])
                            }
                            'i' -> interactive = true
                            'v' -> versioninfo = true
                            'n' -> nodebug = true
                            'p' -> print = true
                            'c' -> {
                                if (++i >= args.size)
                                    usageExit()
                                encoding = args[i]
                            }
                            '-' -> {
                                if (args[i].length > 2)
                                    usageExit()
                                processing = false
                            }
                            else -> usageExit()
                        }// input script - defer to last stage
                    }
                    i++
                }
            }

            // echo version
            if (versioninfo)
                println(version)

            // new lua state
            globals = if (nodebug) JsePlatform.standardGlobals() else JsePlatform.debugGlobals()
            run {
                var i = 0
                val n = if (libs != null) libs!!.size else 0
                while (i < n) {
                    loadLibrary(libs!!.elementAt(i) as String)
                    i++
                }
            }

            // input script processing
            processing = true
            var i = 0
            while (i < args.size) {
                if (!processing || !args[i].startsWith("-")) {
                    processScript(File(args[i]).readBytes().toLuaBinInput(), args[i], args, i)
                    break
                } else if ("-" == args[i]) {
                    processScript(System.`in`.toLua(), "=stdin", args, i)
                    break
                } else {
                    when (args[i][1]) {
                        'l', 'c' -> ++i
                        'e' -> {
                            ++i
                            processScript(args[i].toByteArray().toLuaBinInput(), "string", args, i)
                        }
                        '-' -> processing = false
                    }
                }
                i++
            }

            if (interactive)
                interactiveMode()

        } catch (ioe: IOException) {
            System.err.println(ioe.toString())
            System.exit(-2)
        }

    }

    @Throws(IOException::class)
    private fun loadLibrary(libname: String) {
        val slibname = LuaValue.valueOf(libname)
        try {
            // load via plain require
            globals!!.get("require").call(slibname)
        } catch (e: Exception) {
            try {
                // load as java class
                val v = Class.forName(libname).newInstance() as LuaValue
                v.call(slibname, globals)
            } catch (f: Exception) {
                throw IOException("loadLibrary($libname) failed: $e,$f")
            }

        }

    }

    @Throws(IOException::class)
    private fun processScript(script: LuaBinInput, chunkname: String, args: Array<String>?, firstarg: Int) {
        var script = script
        try {
            val c: LuaValue
            try {
                script = script.buffered()
                c = if (encoding != null)
                    globals!!.load(script.reader(encoding!!), chunkname)
                else
                    globals!!.load(script, chunkname, "bt", globals)
            } finally {
                script.close()
            }
            if (print && c.isclosure())
                Print.print(c.checkclosure()!!.p)
            val scriptargs = setGlobalArg(chunkname, args, firstarg, globals)
            c.invoke(scriptargs)
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }

    }

    private fun setGlobalArg(chunkname: String, args: Array<String>?, i: Int, globals: LuaValue): Varargs {
        if (args == null)
            return LuaValue.NONE
        val arg = LuaValue.tableOf()
        for (j in args.indices)
            arg.set(j - i, LuaValue.valueOf(args[j]))
        arg.set(0, LuaValue.valueOf(chunkname))
        arg.set(-1, LuaValue.valueOf("luaj"))
        globals.set("arg", arg)
        return arg.unpack()
    }

    @Throws(IOException::class)
    private fun interactiveMode() {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            print("> ")
            System.out.flush()
            val line = reader.readLine() ?: return
            processScript(line.toByteArray().toLuaBinInput(), "=stdin", null, 0)
        }
    }
}
