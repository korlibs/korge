/*******************************************************************************
 * Copyright (c) 2012 Luaj.org. All rights reserved.
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
package org.luaj.vm2

import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.IoLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.ResourceFinder
import kotlin.jvm.*
import kotlin.math.*

/**
 * Global environment used by luaj.  Contains global variables referenced by executing lua.
 *
 *
 *
 * <h3>Constructing and Initializing Instances</h3>
 * Typically, this is constructed indirectly by a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or
 * [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals],
 * and then used to load lua scripts for execution as in the following example.
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.load( new StringReader("print 'hello'"), "main.lua" ).call();
` *  </pre>
 * The creates a complete global environment with the standard libraries loaded.
 *
 *
 * For specialized circumstances, the Globals may be constructed directly and loaded
 * with only those libraries that are needed, for example.
 * <pre> `Globals globals = new Globals();
 * globals.load( new BaseLib() );
` *  </pre>
 *
 * <h3>Loading and Executing Lua Code</h3>
 * Globals contains convenience functions to load and execute lua source code given a Reader.
 * A simple example is:
 * <pre> `globals.load( new StringReader("print 'hello'"), "main.lua" ).call();
` *  </pre>
 *
 * <h3>Fine-Grained Control of Compiling and Loading Lua</h3>
 * Executable LuaFunctions are created from lua code in several steps
 *
 *  * find the resource using the platform's [ResourceFinder]
 *  * compile lua to lua bytecode using [Compiler]
 *  * load lua bytecode to a [Prototype] using [Undumper]
 *  * construct [LuaClosure] from [Prototype] with [Globals] using [Loader]
 *
 *
 *
 * There are alternate flows when the direct lua-to-Java bytecode compiling [org.luaj.vm2.luajc.LuaJC] is used.
 *
 *  * compile lua to lua bytecode using [Compiler] or load precompiled code using [Undumper]
 *  * convert lua bytecode to equivalent Java bytecode using [org.luaj.vm2.luajc.LuaJC] that implements [Loader] directly
 *
 *
 * <h3>Java Field</h3>
 * Certain public fields are provided that contain the current values of important global state:
 *
 *  * [.STDIN] Current value for standard input in the laaded [IoLib], if any.
 *  * [.STDOUT] Current value for standard output in the loaded [IoLib], if any.
 *  * [.STDERR] Current value for standard error in the loaded [IoLib], if any.
 *  * [.finder] Current loaded [ResourceFinder], if any.
 *  * [.compiler] Current loaded [Compiler], if any.
 *  * [.undumper] Current loaded [Undumper], if any.
 *  * [.loader] Current loaded [Loader], if any.
 *
 *
 * <h3>Lua Environment Variables</h3>
 * When using [org.luaj.vm2.lib.jse.JsePlatform] or [org.luaj.vm2.lib.jme.JmePlatform],
 * these environment variables are created within the Globals.
 *
 *  * "_G" Pointer to this Globals.
 *  * "_VERSION" String containing the version of luaj.
 *
 *
 * <h3>Use in Multithreaded Environments</h3>
 * In a multi-threaded server environment, each server thread should create one Globals instance,
 * which will be logically distinct and not interfere with each other, but share certain
 * static immutable resources such as class data and string data.
 *
 *
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see LuaValue
 *
 * @see Compiler
 *
 * @see Loader
 *
 * @see Undumper
 *
 * @see ResourceFinder
 *
 * @see org.luaj.vm2.compiler.LuaC
 *
 * @see org.luaj.vm2.luajc.LuaJC
 */
open class Globals(
    val runtime: LuaRuntime = LuaRuntime()
) : LuaTable() {

    /** The current default input stream.  */
    @kotlin.jvm.JvmField var STDIN: LuaBinInput = JSystem.`in`

    /** The current default output stream.  */
    @kotlin.jvm.JvmField var STDOUT: LuaWriter = JSystem.out

    /** The current default error stream.  */
    @kotlin.jvm.JvmField var STDERR: LuaWriter = JSystem.err

    /** The installed ResourceFinder for looking files by name.  */
    @kotlin.jvm.JvmField var finder: ResourceFinder? = null

    /** The currently running thread.  Should not be changed by non-library code.  */
    @kotlin.jvm.JvmField var running: LuaThread = LuaThread(this)

    /** The BaseLib instance loaded into this Globals  */
    @kotlin.jvm.JvmField var baselib: BaseLib? = null

    /** The PackageLib instance loaded into this Globals  */
    @kotlin.jvm.JvmField var package_: PackageLib? = null

    /** The DebugLib instance loaded into this Globals, or null if debugging is not enabled  */
    @kotlin.jvm.JvmField var debuglib: DebugLib? = null

    /** The installed loader.
     * @see Loader
     */
    @kotlin.jvm.JvmField var loader: Loader? = null

    /** The installed compiler.
     * @see Compiler
     */
    @kotlin.jvm.JvmField var compiler: Compiler? = null

    /** The installed undumper.
     * @see Undumper
     */
    @kotlin.jvm.JvmField var undumper: Undumper? = null

    /** Interface for module that converts a Prototype into a LuaFunction with an environment.  */
    interface Loader {
        /** Convert the prototype into a LuaFunction with the supplied environment.  */
        fun load(prototype: Prototype, chunkname: String, env: LuaValue): LuaFunction
    }

    /** Interface for module that converts lua source text into a prototype.  */
    interface Compiler {
        /** Compile lua source into a Prototype. The InputStream is assumed to be in UTF-8.  */
        fun compile(stream: LuaBinInput, chunkname: String): Prototype
    }

    /** Interface for module that loads lua binary chunk into a prototype.  */
    interface Undumper {
        /** Load the supplied input stream into a prototype.  */
        fun undump(stream: LuaBinInput, chunkname: String): Prototype?
    }

    /** Check that this object is a Globals object, and return it, otherwise throw an error.  */
    override fun checkglobals(): Globals = this

    /** Convenience function for loading a file that is either binary lua or lua source.
     * @param filename Name of the file to load.
     * @return LuaValue that can be call()'ed or invoke()'ed.
     * @com.soywiz.luak.compat.java.Throws LuaError if the file could not be loaded.
     */
    fun loadfile(filename: String): LuaValue = try {
        load(finder!!.findResource(filename)!!, "@$filename", "bt", this)
    } catch (e: Exception) {
        LuaValue.error("load $filename: $e")
    }

    /** Convenience function to load a string value as a script.  Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @com.soywiz.luak.compat.java.Throws LuaError if the script could not be compiled.
     */
    open fun load(script: String, chunkname: String): LuaValue = load(StrLuaReader(script), chunkname)

    /** Convenience function to load a string value as a script.  Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @com.soywiz.luak.compat.java.Throws LuaError if the script could not be compiled.
     */
    open fun load(script: String): LuaValue = load(StrLuaReader(script), script)

    /** Convenience function to load a string value as a script with a custom environment.
     * Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param environment LuaTable to be used as the environment for the loaded function.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @com.soywiz.luak.compat.java.Throws LuaError if the script could not be compiled.
     */
    fun load(script: String, chunkname: String, environment: LuaTable): LuaValue =
        load(StrLuaReader(script), chunkname, environment)

    /** Load the content form a reader as a text file.  Must be lua source.
     * The source is converted to UTF-8, so any characters appearing in quoted literals
     * above the range 128 will be converted into multiple bytes.
     * @param reader Reader containing text of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @com.soywiz.luak.compat.java.Throws LuaError if the script could not be compiled.
     */
    fun load(reader: LuaReader, chunkname: String): LuaValue = load(UTF8Stream(reader), chunkname, "t", this)

    /** Load the content form a reader as a text file, supplying a custom environment.
     * Must be lua source. The source is converted to UTF-8, so any characters
     * appearing in quoted literals above the range 128 will be converted into
     * multiple bytes.
     * @param reader Reader containing text of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param environment LuaTable to be used as the environment for the loaded function.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @com.soywiz.luak.compat.java.Throws LuaError if the script could not be compiled.
     */
    fun load(reader: LuaReader, chunkname: String, environment: LuaTable): LuaValue =
        load(UTF8Stream(reader), chunkname, "t", environment)

    /** Load the content form an input stream as a binary chunk or text file.
     * @param is InputStream containing a lua script or compiled lua"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param mode String containing 'b' or 't' or both to control loading as binary or text or either.
     * @param environment LuaTable to be used as the environment for the loaded function.
     */
    fun load(`is`: LuaBinInput, chunkname: String, mode: String, environment: LuaValue): LuaValue {
        try {
            return loader!!.load(loadPrototype(`is`, chunkname, mode), chunkname, environment)
        } catch (l: LuaError) {
            throw l
        } catch (e: Exception) {
            e.printStackTrace()
            return LuaValue.error("load $chunkname: $e")
        }
    }

    /** Load lua source or lua binary from an input stream into a Prototype.
     * The InputStream is either a binary lua chunk starting with the lua binary chunk signature,
     * or a text input file.  If it is a text input file, it is interpreted as a UTF-8 byte sequence.
     * @param is Input stream containing a lua script or compiled lua"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param mode String containing 'b' or 't' or both to control loading as binary or text or either.
     */

    fun loadPrototype(`is`: LuaBinInput, chunkname: String, mode: String): Prototype {
        var `is` = `is`
        if (mode.indexOf('b') >= 0) {
            if (undumper == null) LuaValue.error("No undumper.")
            if (!`is`.markSupported()) `is` = BufferedStream(`is`)
            `is`.mark(4)
            val p = undumper!!.undump(`is`, chunkname)
            if (p != null) return p
            `is`.reset()
        }
        if (mode.indexOf('t') >= 0) return compilePrototype(`is`, chunkname)
        LuaValue.error("Failed to load prototype $chunkname using mode '$mode'")
        //return null
        kotlin.error("Failed to load prototype $chunkname using mode '$mode'")
    }

    /** Compile lua source from a Reader into a Prototype. The characters in the reader
     * are converted to bytes using the UTF-8 encoding, so a string literal containing
     * characters with codepoints 128 or above will be converted into multiple bytes.
     */
    fun compilePrototype(reader: LuaReader, chunkname: String): Prototype = compilePrototype(UTF8Stream(reader), chunkname)

    /** Compile lua source from an InputStream into a Prototype.
     * The input is assumed to be UTf-8, but since bytes in the range 128-255 are passed along as
     * literal bytes, any ASCII-compatible encoding such as ISO 8859-1 may also be used.
     */
    fun compilePrototype(stream: LuaBinInput, chunkname: String): Prototype {
        if (compiler == null) LuaValue.error("No compiler.")
        return compiler!!.compile(stream, chunkname)
    }

    /** Function which yields the current thread.
     * @param args  Arguments to supply as return values in the resume function of the resuming thread.
     * @return Values supplied as arguments to the resume() call that reactivates this thread.
     */
    fun yield(args: Varargs): Varargs {
        if (running.isMainThread) throw LuaError("cannot yield main thread")
        val s = running.state
        return s.lua_yield(args)
    }

    /* Abstract base class to provide basic buffered input storage and delivery.
	 * This class may be moved to its own package in the future.
	 */
    internal abstract class AbstractBufferedStream protected constructor(buflen: Int) : LuaBinInput() {
        protected var b: ByteArray = ByteArray(buflen)
        protected var i = 0
        protected var j = 0

        protected abstract fun avail(): Int
        override fun read(): Int = avail().let { a -> if (a <= 0) -1 else 0xff and b[i++].toInt() and 0xFF }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val a = avail()
            if (a <= 0) return -1
            val n_read = min(a, len)
            arraycopy(this.b, i, b, off, n_read)
            i += n_read
            return n_read
        }

        override fun skip(n: Long): Long = min(n, (j - i).toLong()).also { i += it.toInt() }
        override fun available(): Int = j - i
    }

    /**  Simple converter from Reader to InputStream using UTF8 encoding that will work
     * on both JME and JSE.
     * This class may be moved to its own package in the future.
     */
    internal class UTF8Stream(private val r: LuaReader) : AbstractBufferedStream(96) {
        private val c = CharArray(32)

        override fun avail(): Int {
            if (i < j) return j - i
            var n = r.read(c)
            if (n < 0) return -1
            if (n == 0) {
                val u = r.read()
                if (u < 0) return -1
                c[0] = u.toChar()
                n = 1
            }
            j = LuaString.encodeToUtf8(c, n, b, run { i = 0; i })
            return j
        }

        override fun close() { r.close() }
    }

    /** Simple buffered InputStream that supports mark.
     * Used to examine an InputStream for a 4-byte binary lua signature,
     * and fall back to text input when the signature is not found,
     * as well as speed up normal compilation and reading of lua scripts.
     * This class may be moved to its own package in the future.
     */
    internal class BufferedStream(buflen: Int, private val s: LuaBinInput) : AbstractBufferedStream(buflen) {
        constructor(s: LuaBinInput) : this(128, s)

        override fun avail(): Int {
            if (i < j) return j - i
            if (j >= b.size) {
                j = 0
                i = j
            }
            // leave previous bytes in place to implement mark()/reset().
            var n = s.read(b, j, b.size - j)
            if (n < 0) return -1
            if (n == 0) {
                val u = s.read()
                if (u < 0) return -1
                b[j] = u.toByte()
                n = 1
            }
            j += n
            return n
        }

        override fun close() { s.close() }

        @Synchronized
        override fun mark(n: Int) {
            if (i > 0 || n > b.size) {
                val dest = if (n > b.size) ByteArray(n) else b
                arraycopy(b, i, dest, 0, j - i)
                j -= i
                i = 0
                b = dest
            }
        }

        override fun markSupported(): Boolean = true

        @Synchronized
        override fun reset() { i = 0 }
    }
}
