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

package org.luaj.vm2.cli

import org.luaj.vm2.*
import org.luaj.vm2.compiler.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.*
import java.io.*

/**
 * Compiler for lua files to lua bytecode.
 */
class luac @Throws(IOException::class)
private constructor(args: Array<String>) {

    private var list = false
    private var output = "luac.out"
    private var parseonly = false
    private var stripdebug = false
    private var littleendian = false
    private var numberformat = DumpState.NUMBER_FORMAT_DEFAULT
    private var versioninfo = false
    private var processing = true
    private var encoding: String? = null

    init {

        // process args
        try {
            // get stateful args
            run {
                var i = 0
                while (i < args.size) {
                    if (!processing || !args[i].startsWith("-")) {
                        // input file - defer to next stage
                    } else if (args[i].length <= 1) {
                        // input file - defer to next stage
                    } else {
                        when (args[i][1]) {
                            'l' -> list = true
                            'o' -> {
                                if (++i >= args.size)
                                    usageExit()
                                output = args[i]
                            }
                            'p' -> parseonly = true
                            's' -> stripdebug = true
                            'e' -> littleendian = true
                            'i' -> {
                                if (args[i].length <= 2)
                                    usageExit()
                                numberformat = Integer.parseInt(args[i].substring(2))
                            }
                            'v' -> versioninfo = true
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
                        }
                    }
                    i++
                }
            }

            // echo version
            if (versioninfo)
                println(version)

            // open output file
            val fos = FileOutputStream(output)

            // process input files
            try {
                val globals = JsePlatform.standardGlobals()
                processing = true
                var i = 0
                while (i < args.size) {
                    if (!processing || !args[i].startsWith("-")) {
                        val chunkname = args[i].substring(0, args[i].length - 4)
                        processScript(globals, File(args[i]).readBytes().toLuaBinInput(), chunkname, fos)
                    } else if (args[i].length <= 1) {
                        processScript(globals, System.`in`.toLua(), "=stdin", fos)
                    } else {
                        when (args[i][1]) {
                            'o', 'c' -> ++i
                            '-' -> processing = false
                        }
                    }
                    i++
                }
            } finally {
                fos.close()
            }

        } catch (ioe: IOException) {
            System.err.println(ioe.toString())
            System.exit(-2)
        }

    }

    @Throws(IOException::class)
    private fun processScript(globals: Globals, script: LuaBinInput, chunkname: String, out: OutputStream) {
        var script = script
        try {
            // create the chunk
            script = (script).buffered()
            val chunk = if (encoding != null)
                globals.compilePrototype(script.reader(encoding!!), chunkname)
            else
                globals.compilePrototype(script, chunkname)

            // list the chunk
            if (list)
                Print.printCode(chunk)

            // write out the chunk
            if (!parseonly) {
                DumpState.dump(chunk, out.toLua(), stripdebug, numberformat, littleendian)
            }

        } catch (e: Exception) {
            e.printStackTrace(System.err)
        } finally {
            script.close()
        }
    }

    companion object {
        private val version = Lua._VERSION + "Copyright (C) 2009 luaj.org"

        private val usage = "usage: java -cp luaj-jse.jar luac [options] [filenames].\n" +
                "Available options are:\n" +
                "  -        process stdin\n" +
                "  -l       list\n" +
                "  -o name  output to file 'name' (default is \"luac.out\")\n" +
                "  -p       parse only\n" +
                "  -s       strip debug information\n" +
                "  -e       little endian format for numbers\n" +
                "  -i<n>    number format 'n', (n=0,1 or 4, default=" + DumpState.NUMBER_FORMAT_DEFAULT + ")\n" +
                "  -v       show version information\n" +
                "  -c enc  	use the supplied encoding 'enc' for input files\n" +
                "  --       stop handling options\n"

        private fun usageExit() {
            println(usage)
            System.exit(-1)
        }

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            luac(args)
        }
    }
}
