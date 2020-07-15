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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.IoLib
import org.luaj.vm2.lib.LibFunction
import java.io.OutputStream
import java.io.RandomAccessFile

/**
 * Subclass of [IoLib] and therefore [LibFunction] which implements the lua standard `io`
 * library for the JSE platform.
 *
 *
 * It uses RandomAccessFile to implement seek on files.
 *
 *
 * Typically, this library is included as part of a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n"));
` *  </pre>
 *
 *
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new JseIoLib());
 * globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n"));
` *  </pre>
 *
 * However, other libraries such as *MathLib* are not loaded in this case.
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * //@see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see IoLib
 * //@see org.luaj.vm2.lib.jme.JmeIoLib
 *
 * @see [Lua 5.2 I/O Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.8)
 */
class JseIoLib : IoLib() {


    override fun wrapStdin(): IoLib.File {
        return StdinFile()
    }


    override fun wrapStdout(): IoLib.File {
        return StdoutFile(IoLib.FTYPE_STDOUT)
    }


    override fun wrapStderr(): IoLib.File {
        return StdoutFile(IoLib.FTYPE_STDERR)
    }


    override fun openFile(
        filename: String?,
        readMode: Boolean,
        appendMode: Boolean,
        updateMode: Boolean,
        binaryMode: Boolean
    ): IoLib.File {
        val f = RandomAccessFile(filename, if (readMode) "r" else "rw")
        if (appendMode) {
            f.seek(f.length())
        } else {
            if (!readMode)
                f.setLength(0)
        }
        return FileImpl(f)
    }


    override fun openProgram(prog: String?, mode: String?): IoLib.File {
        val p = Runtime.getRuntime().exec(prog)
        return if ("w" == mode)
            FileImpl(p.outputStream)
        else
            FileImpl(p.inputStream.toLua())
    }


    override fun tmpFile(): IoLib.File {
        val f = java.io.File.createTempFile(".luaj", "bin")
        f.deleteOnExit()
        return FileImpl(RandomAccessFile(f, "rw"))
    }

    private fun notimplemented(): Nothing {
        throw LuaError("not implemented")
    }


    private inner class FileImpl private constructor(
        private val file: RandomAccessFile?,
        `is`: LuaBinInput?,
        private val os: OutputStream?
    ) : IoLib.File() {
        private val `is`: LuaBinInput?
        private var closed = false
        private var nobuffer = false

        init {
            this.`is` = if (`is` != null) if (`is`.markSupported()) `is` else (`is`.buffered()) else null
        }

        constructor(f: RandomAccessFile) : this(f, null, null) {}
        constructor(i: LuaBinInput) : this(null, i, null) {}
        constructor(o: OutputStream) : this(null, null, o) {}

        override fun tojstring(): String {
            return "file (" + this.hashCode() + ")"
        }

        override fun isstdfile(): Boolean {
            return file == null
        }


        override fun close() {
            closed = true
            file?.close()
        }


        override fun flush() {
            os?.flush()
        }


        override fun write(s: LuaString?) {
            if (os != null)
                os.write(s!!.m_bytes, s.m_offset, s.m_length)
            else if (file != null)
                file.write(s!!.m_bytes, s.m_offset, s.m_length)
            else
                notimplemented()
            if (nobuffer)
                flush()
        }

        override fun isclosed(): Boolean {
            return closed
        }


        override fun seek(option: String?, pos: Int): Int {
            if (file != null) {
                if ("set" == option) {
                    file.seek(pos.toLong())
                } else if ("end" == option) {
                    file.seek(file.length() + pos)
                } else {
                    file.seek(file.filePointer + pos)
                }
                return file.filePointer.toInt()
            }
            notimplemented()
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {
            nobuffer = "no" == mode
        }

        // get length remaining to read

        override fun remaining(): Int {
            return if (file != null) (file.length() - file.filePointer).toInt() else -1
        }

        // peek ahead one character

        override fun peek(): Int {
            if (`is` != null) {
                `is`.mark(1)
                val c = `is`.read()
                `is`.reset()
                return c
            } else if (file != null) {
                val fp = file.filePointer
                val c = file.read()
                file.seek(fp)
                return c
            }
            notimplemented()
            return 0
        }

        // return char if read, -1 if eof, throw IOException on other exception

        override fun read(): Int {
            if (`is` != null)
                return `is`.read()
            else if (file != null) {
                return file.read()
            }
            notimplemented()
            return 0
        }

        // return number of bytes read if positive, -1 if eof, throws IOException

        override fun read(bytes: ByteArray, offset: Int, length: Int): Int = when {
            file != null -> file.read(bytes, offset, length)
            `is` != null -> `is`.read(bytes, offset, length)
            else -> notimplemented()
        }
    }

    inner class StdoutFile constructor(private val file_type: Int) : IoLib.File() {

        private val printStream: LuaWriter get() = if (file_type == IoLib.FTYPE_STDERR) globals!!.STDERR else globals!!.STDOUT

        override fun tojstring(): String {
            return "file (" + this.hashCode() + ")"
        }


        override fun write(string: LuaString?) {
            printStream.write(string!!.m_bytes, string.m_offset, string.m_length)
        }


        override fun flush() {
            printStream.flush()
        }

        override fun isstdfile(): Boolean {
            return true
        }


        override fun close() {
            // do not close std files.
        }

        override fun isclosed(): Boolean {
            return false
        }


        override fun seek(option: String?, bytecount: Int): Int {
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {}


        override fun remaining(): Int {
            return 0
        }


        override fun peek(): Int {
            return 0
        }


        override fun read(): Int {
            return 0
        }


        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            return 0
        }
    }

    inner class StdinFile constructor() : IoLib.File() {

        override fun tojstring(): String {
            return "file (" + this.hashCode() + ")"
        }


        override fun write(string: LuaString?) {
        }


        override fun flush() {
        }

        override fun isstdfile(): Boolean {
            return true
        }


        override fun close() {
            // do not close std files.
        }

        override fun isclosed(): Boolean {
            return false
        }


        override fun seek(option: String?, bytecount: Int): Int {
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {}


        override fun remaining(): Int {
            return 0
        }


        override fun peek(): Int {
            globals!!.STDIN.mark(1)
            val c = globals!!.STDIN.read()
            globals!!.STDIN.reset()
            return c
        }


        override fun read(): Int {
            return globals!!.STDIN.read()
        }


        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            return globals!!.STDIN.read(bytes, offset, length)
        }
    }
}
