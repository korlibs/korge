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
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

/**
 * Abstract base class extending [LibFunction] which implements the
 * core of the lua standard `io` library.
 *
 *
 * It contains the implementation of the io library support that is common to
 * the JSE and JME platforms.
 * In practice on of the concrete IOLib subclasses is chosen:
 * [org.luaj.vm2.lib.jse.JseIoLib] for the JSE platform, and
 * [org.luaj.vm2.lib.jme.JmeIoLib] for the JME platform.
 *
 *
 * The JSE implementation conforms almost completely to the C-based lua library,
 * while the JME implementation follows closely except in the area of random-access files,
 * which are difficult to support properly on JME.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n"));
` *  </pre>
 * In this example the platform-specific [org.luaj.vm2.lib.jse.JseIoLib] library will be loaded, which will include
 * the base functionality provided by this class, whereas the [org.luaj.vm2.lib.jse.JsePlatform] would load the
 * [org.luaj.vm2.lib.jse.JseIoLib].
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new OsLib());
 * globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n"));
` *  </pre>
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see org.luaj.vm2.lib.jse.JseIoLib
 *
 * @see org.luaj.vm2.lib.jme.JmeIoLib
 *
 * @see [http://www.lua.org/manual/5.1/manual.html.5.7](http://www.lua.org/manual/5.1/manual.html.5.7)
 */
abstract class IoLib : TwoArgFunction() {

    private var infile: File? = null
    private var outfile: File? = null
    private var errfile: File? = null

    @kotlin.jvm.JvmField internal var filemethods: LuaTable = LuaTable()

    @kotlin.jvm.JvmField
    protected var globals: Globals? = null

    abstract inner class File : LuaValue() {

        abstract fun write(string: LuaString?)


        abstract fun flush()

        abstract fun isstdfile(): Boolean

        abstract fun close()

        abstract fun isclosed(): Boolean
        // returns new position

        abstract fun seek(option: String?, bytecount: Int): Int

        abstract fun setvbuf(mode: String?, size: Int)
        // get length remaining to read

        abstract fun remaining(): Int

        // peek ahead one character

        abstract fun peek(): Int

        // return char if read, -1 if eof, throw IOException on other exception

        abstract fun read(): Int

        // return number of bytes read if positive, false if eof, throw IOException on other exception

        abstract fun read(bytes: ByteArray, offset: Int, length: Int): Int

        // delegate method access to file methods table
        override fun get(key: LuaValue): LuaValue {
            return filemethods[key]
        }

        // essentially a userdata instance
        override fun type(): Int {
            return LuaValue.TUSERDATA
        }

        override fun typename(): String {
            return "userdata"
        }

        // displays as "file" type
        override fun tojstring(): String {
            return "file: " + hashCode().toHexString()
        }
    }

    /**
     * Wrap the standard input.
     * @return File
     * @com.soywiz.luak.compat.java.Throws IOException
     */

    protected abstract fun wrapStdin(): File

    /**
     * Wrap the standard output.
     * @return File
     * @com.soywiz.luak.compat.java.Throws IOException
     */

    protected abstract fun wrapStdout(): File

    /**
     * Wrap the standard error output.
     * @return File
     * @com.soywiz.luak.compat.java.Throws IOException
     */

    protected abstract fun wrapStderr(): File

    /**
     * Open a file in a particular mode.
     * @param filename
     * @param readMode true if opening in read mode
     * @param appendMode true if opening in append mode
     * @param updateMode true if opening in update mode
     * @param binaryMode true if opening in binary mode
     * @return File object if successful
     * @com.soywiz.luak.compat.java.Throws IOException if could not be opened
     */

    protected abstract fun openFile(
        filename: String?,
        readMode: Boolean,
        appendMode: Boolean,
        updateMode: Boolean,
        binaryMode: Boolean
    ): File

    /**
     * Open a temporary file.
     * @return File object if successful
     * @com.soywiz.luak.compat.java.Throws IOException if could not be opened
     */

    protected abstract fun tmpFile(): File

    /**
     * Start a new process and return a file for input or output
     * @param prog the program to execute
     * @param mode "r" to read, "w" to write
     * @return File to read to or write from
     * @com.soywiz.luak.compat.java.Throws IOException if an i/o exception occurs
     */

    protected abstract fun openProgram(prog: String?, mode: String?): File

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        globals = env.checkglobals()

        // io lib functions
        val t = LuaTable()
        bind(t, { IoLibV() }, IO_NAMES)

        // create file methods table
        filemethods = LuaTable()
        bind(filemethods, { IoLibV() }, FILE_NAMES, FILE_CLOSE)

        // set up file metatable
        val mt = LuaTable()
        bind(mt, { IoLibV() }, arrayOf("__index"), IO_INDEX)
        t.setmetatable(mt)

        // all functions link to library instance
        setLibInstance(t)
        setLibInstance(filemethods)
        setLibInstance(mt)

        // return the table
        env["io"] = t
        env["package"]["loaded"]["io"] = t
        return t
    }

    private fun setLibInstance(t: LuaTable) {
        val k = t.keys()
        var i = 0
        val n = k.size
        while (i < n) {
            (t[k[i]] as IoLibV).iolib = this
            i++
        }
    }

    open class IoLibV : VarArgFunction {
        lateinit private var f: File
        lateinit var iolib: IoLib

        constructor()
        constructor(f: File, name: String, opcode: Int, iolib: IoLib) : super() {
            this.f = f
            this.name = name
            this.opcode = opcode
            this.iolib = iolib
        }

        override fun invoke(args: Varargs): Varargs {
            try {
                when (opcode) {
                    IO_FLUSH -> return iolib._io_flush()
                    IO_TMPFILE -> return iolib._io_tmpfile()
                    IO_CLOSE -> return iolib._io_close(args.arg1())
                    IO_INPUT -> return iolib._io_input(args.arg1())
                    IO_OUTPUT -> return iolib._io_output(args.arg1())
                    IO_TYPE -> return iolib._io_type(args.arg1())
                    IO_POPEN -> return iolib._io_popen(args.checkjstring(1), args.optjstring(2, "r"))
                    IO_OPEN -> return iolib._io_open(args.checkjstring(1), args.optjstring(2, "r"))
                    IO_LINES -> return iolib._io_lines(if (args.isvalue(1)) args.checkjstring(1) else null)
                    IO_READ -> return iolib._io_read(args)
                    IO_WRITE -> return iolib._io_write(args)

                    FILE_CLOSE -> return iolib._file_close(args.arg1())
                    FILE_FLUSH -> return iolib._file_flush(args.arg1())
                    FILE_SETVBUF -> return iolib._file_setvbuf(args.arg1(), args.checkjstring(2), args.optint(3, 1024))
                    FILE_LINES -> return iolib._file_lines(args.arg1())
                    FILE_READ -> return iolib._file_read(args.arg1(), args.subargs(2))
                    FILE_SEEK -> return iolib._file_seek(args.arg1(), args.optjstring(2, "cur"), args.optint(3, 0))
                    FILE_WRITE -> return iolib._file_write(args.arg1(), args.subargs(2))

                    IO_INDEX -> return iolib._io_index(args.arg(2))
                    LINES_ITER -> return iolib._lines_iter(f)
                }
            } catch (ioe: IOException) {
                return errorresult(ioe)
            }

            return LuaValue.NONE
        }
    }

    private fun input(): File {
        return if (infile != null) infile!! else run { infile = ioopenfile(FTYPE_STDIN, "-", "r"); infile!! }
    }

    //	io.flush() -> bool

    fun _io_flush(): Varargs {
        checkopen(output())
        outfile!!.flush()
        return BTRUE
    }

    //	io.tmpfile() -> file

    fun _io_tmpfile(): Varargs {
        return tmpFile()
    }

    //	io.close([file]) -> void

    fun _io_close(file: LuaValue): Varargs {
        val f = if (file.isnil()) output() else checkfile(file)
        checkopen(f)
        return ioclose(f)
    }

    //	io.input([file]) -> file
    fun _io_input(file: LuaValue): Varargs {
        infile = if (file.isnil())
            input()
        else if (file.isstring())
            ioopenfile(FTYPE_NAMED, file.checkjstring(), "r")
        else
            checkfile(file)
        return infile!!
    }

    // io.output(filename) -> file
    fun _io_output(filename: LuaValue): Varargs {
        outfile = if (filename.isnil())
            output()
        else if (filename.isstring())
            ioopenfile(FTYPE_NAMED, filename.checkjstring(), "w")
        else
            checkfile(filename)
        return outfile!!
    }

    //	io.type(obj) -> "file" | "closed file" | nil
    fun _io_type(obj: LuaValue): Varargs {
        val f = optfile(obj)
        return if (f != null)
            if (f.isclosed()) CLOSED_FILE else FILE
        else
            LuaValue.NIL
    }

    // io.popen(prog, [mode]) -> file

    fun _io_popen(prog: String?, mode: String?): Varargs {
        return openProgram(prog, mode)
    }

    //	io.open(filename, [mode]) -> file | nil,err

    fun _io_open(filename: String?, mode: String?): Varargs {
        return rawopenfile(FTYPE_NAMED, filename, mode)
    }

    //	io.lines(filename) -> iterator
    fun _io_lines(filename: String?): Varargs {
        infile = if (filename == null) input() else ioopenfile(FTYPE_NAMED, filename, "r")
        checkopen(infile!!)
        return lines(infile!!)
    }

    //	io.read(...) -> (...)

    fun _io_read(args: Varargs): Varargs {
        checkopen(input())
        return ioread(infile!!, args)
    }

    //	io.write(...) -> void

    fun _io_write(args: Varargs): Varargs {
        checkopen(output())
        return iowrite(outfile, args)
    }

    // file:close() -> void

    fun _file_close(file: LuaValue): Varargs {
        return ioclose(checkfile(file))
    }

    // file:flush() -> void

    fun _file_flush(file: LuaValue): Varargs {
        checkfile(file).flush()
        return BTRUE
    }

    // file:setvbuf(mode,[size]) -> void
    fun _file_setvbuf(file: LuaValue, mode: String?, size: Int): Varargs {
        checkfile(file).setvbuf(mode, size)
        return BTRUE
    }

    // file:lines() -> iterator
    fun _file_lines(file: LuaValue): Varargs {
        return lines(checkfile(file))
    }

    //	file:read(...) -> (...)

    fun _file_read(file: LuaValue, subargs: Varargs): Varargs {
        return ioread(checkfile(file), subargs)
    }

    //  file:seek([whence][,offset]) -> pos | nil,error

    fun _file_seek(file: LuaValue, whence: String?, offset: Int): Varargs {
        return LuaValue.valueOf(checkfile(file).seek(whence, offset))
    }

    //	file:write(...) -> void

    fun _file_write(file: LuaValue, subargs: Varargs): Varargs {
        return iowrite(checkfile(file), subargs)
    }

    // __index, returns a field
    fun _io_index(v: LuaValue): Varargs {
        return if (v == STDOUT)
            output()
        else if (v == STDIN)
            input()
        else if (v == STDERR) errput() else LuaValue.NIL
    }

    //	lines iterator(s,var) -> var'

    fun _lines_iter(file: LuaValue): Varargs {
        return freadline(checkfile(file))
    }

    private fun output(): File {
        return if (outfile != null) outfile!! else run { outfile = ioopenfile(FTYPE_STDOUT, "-", "w"); outfile!! }
    }

    private fun errput(): File {
        return if (errfile != null) errfile!! else run { errfile = ioopenfile(FTYPE_STDERR, "-", "w"); errfile!! }
    }

    private fun ioopenfile(filetype: Int, filename: String?, mode: String): File? {
        try {
            return rawopenfile(filetype, filename, mode)
        } catch (e: Exception) {
            LuaValue.error("io error: " + e.message)
            return null
        }

    }

    private fun lines(f: File): Varargs {
        try {
            return IoLibV(f, "lnext", LINES_ITER, this)
        } catch (e: Exception) {
            return LuaValue.error("lines: $e")
        }

    }


    private fun ioread(f: File, args: Varargs): Varargs {
        var i: Int
        val n = args.narg()
        val v = arrayOfNulls<LuaValue>(n)
        var ai: LuaValue
        var vi: LuaValue
        var fmt: LuaString?
        i = 0
        while (i < n) {
            item@do {
                when ((run { ai = args.arg(i + 1); ai }).type()) {
                    LuaValue.TNUMBER -> {
                        vi = freadbytes(f, ai.toint())
                        break@item
                    }
                    LuaValue.TSTRING -> {
                        fmt = ai.checkstring()
                        if (fmt.m_length == 2 && fmt.m_bytes[fmt.m_offset] == '*'.toByte()) {
                            when (fmt.m_bytes[fmt.m_offset + 1].toChar()) {
                                'n' -> {
                                    vi = freadnumber(f)
                                    break@item
                                }
                                'l' -> {
                                    vi = freadline(f)
                                    break@item
                                }
                                'a' -> {
                                    vi = freadall(f)
                                    break@item
                                }
                            }
                        }
                        return LuaValue.argerror(i + 1, "(invalid format)")
                    }
                    else -> return LuaValue.argerror(i + 1, "(invalid format)")
                }
            } while (false)
            if ((run { v[i++] = vi; vi }).isnil())
                break
        }
        return if (i == 0) LuaValue.NIL else LuaValue.varargsOf(v as Array<LuaValue>, 0, i)
    }


    private fun rawopenfile(filetype: Int, filename: String?, mode: String?): File {
        when (filetype) {
            FTYPE_STDIN -> return wrapStdin()
            FTYPE_STDOUT -> return wrapStdout()
            FTYPE_STDERR -> return wrapStderr()
        }
        val isreadmode = mode!!.startsWith("r")
        val isappend = mode.startsWith("a")
        val isupdate = mode.indexOf("+") > 0
        val isbinary = mode.endsWith("b")
        return openFile(filename, isreadmode, isappend, isupdate, isbinary)
    }

    companion object {

        /** Enumerated value representing stdin  */
        const val FTYPE_STDIN = 0
        /** Enumerated value representing stdout  */
        const val FTYPE_STDOUT = 1
        /** Enumerated value representing stderr  */
        const val FTYPE_STDERR = 2
        /** Enumerated value representing a file type for a named file  */
        const val FTYPE_NAMED = 3

        private val STDIN = LuaValue.valueOf("stdin")
        private val STDOUT = LuaValue.valueOf("stdout")
        private val STDERR = LuaValue.valueOf("stderr")
        private val FILE = LuaValue.valueOf("file")
        private val CLOSED_FILE = LuaValue.valueOf("closed file")

        private const val IO_CLOSE = 0
        private const val IO_FLUSH = 1
        private const val IO_INPUT = 2
        private const val IO_LINES = 3
        private const val IO_OPEN = 4
        private const val IO_OUTPUT = 5
        private const val IO_POPEN = 6
        private const val IO_READ = 7
        private const val IO_TMPFILE = 8
        private const val IO_TYPE = 9
        private const val IO_WRITE = 10

        private const val FILE_CLOSE = 11
        private const val FILE_FLUSH = 12
        private const val FILE_LINES = 13
        private const val FILE_READ = 14
        private const val FILE_SEEK = 15
        private const val FILE_SETVBUF = 16
        private const val FILE_WRITE = 17

        private const val IO_INDEX = 18
        private const val LINES_ITER = 19

        val IO_NAMES =
            arrayOf("close", "flush", "input", "lines", "open", "output", "popen", "read", "tmpfile", "type", "write")

        val FILE_NAMES = arrayOf("close", "flush", "lines", "read", "seek", "setvbuf", "write")


         private fun ioclose(f: File): Varargs {
            if (f.isstdfile())
                return errorresult("cannot close standard file")
            else {
                f.close()
                return successresult()
            }
        }

         private fun successresult(): Varargs {
            return BTRUE
        }

         private fun errorresult(ioe: Exception): Varargs {
            val s = ioe.message
            return errorresult("io error: " + (s ?: ioe.toString()))
        }

         private fun errorresult(errortext: String): Varargs {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(errortext))
        }


         private fun iowrite(f: File?, args: Varargs): Varargs {
            var i = 1
            val n = args.narg()
            while (i <= n) {
                f!!.write(args.checkstring(i))
                i++
            }
            return f!!
        }

         private fun checkfile(`val`: LuaValue): File {
            val f = optfile(`val`)
            if (f == null)
                LuaValue.argerror(1, "file")
            checkopen(f!!)
            return f
        }

         private fun optfile(`val`: LuaValue): File? {
            return if (`val` is File) `val` else null
        }

         private fun checkopen(file: File): File {
            if (file.isclosed())
                LuaValue.error("attempt to use a closed file")
            return file
        }


        // ------------- file reading utilitied ------------------


         fun freadbytes(f: File, count: Int): LuaValue {
            val b = ByteArray(count)
            val r: Int
            return if ((run { r = f.read(b, 0, b.size); r }) < 0) LuaValue.NIL else LuaString.valueUsing(b, 0, r)
        }


         fun freaduntil(f: File, lineonly: Boolean): LuaValue {
            val baos = ByteArrayLuaBinOutput()
            var c: Int
            try {
                if (lineonly) {
                    loop@ while ((run { c = f.read(); c }) > 0) {
                        when (c.toChar()) {
                            '\r' -> Unit
                            '\n' -> break@loop
                            else -> baos.write(c)
                        }
                    }
                } else {
                    while ((run { c = f.read(); c }) > 0)
                        baos.write(c)
                }
            } catch (e: EOFException) {
                c = -1
            }

            return if (c < 0 && baos.size() == 0)
                LuaValue.NIL
            else
                LuaString.valueUsing(baos.toByteArray())
        }


         fun freadline(f: File): LuaValue {
            return freaduntil(f, true)
        }


         fun freadall(f: File): LuaValue {
            val n = f.remaining()
            return if (n >= 0) {
                freadbytes(f, n)
            } else {
                freaduntil(f, false)
            }
        }


         fun freadnumber(f: File?): LuaValue {
            val baos = ByteArrayLuaBinOutput()
            freadchars(f!!, " \t\r\n", null)
            freadchars(f, "-+", baos)
            //freadchars(f,"0",baos);
            //freadchars(f,"xX",baos);
            freadchars(f, "0123456789", baos)
            freadchars(f, ".", baos)
            freadchars(f, "0123456789", baos)
            //freadchars(f,"eEfFgG",baos);
            // freadchars(f,"+-",baos);
            //freadchars(f,"0123456789",baos);
            val s = baos.toString()
            return if (s.length > 0) LuaValue.valueOf(s.toDouble()) else LuaValue.NIL
        }


         private fun freadchars(f: File, chars: String, baos: ByteArrayLuaBinOutput?) {
            var c: Int
            while (true) {
                c = f.peek()
                if (chars.indexOf(c.toChar()) < 0) {
                    return
                }
                f.read()
                baos?.write(c)
            }
        }
    }


}
