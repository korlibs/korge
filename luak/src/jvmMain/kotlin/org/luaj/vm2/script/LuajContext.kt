/*******************************************************************************
 * Copyright (c) 2013 LuaJ. All rights reserved.
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
package org.luaj.vm2.script

import javax.script.ScriptContext
import javax.script.SimpleScriptContext

import org.luaj.vm2.Globals
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.*

/**
 * Context for LuaScriptEngine execution which maintains its own Globals,
 * and manages the input and output redirection.
 */
class LuajContext
/** Construct a LuajContext with its own globals, which
 * which optionally are debug globals, and optionally use the
 * luajc direct lua to java bytecode compiler.
 *
 *
 * If createDebugGlobals is set, the globals
 * created will be a debug globals that includes the debug
 * library.  This may provide better stack traces, but may
 * have negative impact on performance.
 * @param createDebugGlobals true to create debug globals,
 * false for standard globals.
 * @param useLuaJCCompiler true to use the luajc compiler,
 * reqwuires bcel to be on the class path.
 */
@JvmOverloads constructor(
    createDebugGlobals: Boolean = "true" == System.getProperty("org.luaj.debug"),
    useLuaJCCompiler: Boolean = "true" == System.getProperty("org.luaj.luajc")
) : SimpleScriptContext(), ScriptContext {
    init {
        if (useLuaJCCompiler) throw RuntimeException("Can't use useLuaJCCompiler")
    }

    /** Globals for this context instance.  */
    @kotlin.jvm.JvmField
    val globals: Globals = if (createDebugGlobals) JsePlatform.debugGlobals() else JsePlatform.standardGlobals()

    /** The initial value of globals.STDIN  */
    private val stdin: LuaBinInput = globals.STDIN
    /** The initial value of globals.STDOUT  */
    private val stdout: LuaWriter = globals.STDOUT
    /** The initial value of globals.STDERR  */
    private val stderr: LuaWriter = globals.STDERR

    override fun setErrorWriter(writer: Writer?) {
        globals.STDERR = if (writer != null) PrintStream(WriterOutputStream(writer)).toLua() else stderr
    }

    override fun setReader(reader: Reader?) {
        globals.STDIN = reader?.let { ReaderInputStream(it) } ?: stdin
    }

    override fun setWriter(writer: Writer?) {
        globals.STDOUT = if (writer != null) PrintStream(WriterOutputStream(writer), true).toLua() else stdout
    }

    internal class WriterOutputStream(val w: Writer) : OutputStream() {
        override fun write(b: Int) = w.write(String(byteArrayOf(b.toByte())))
        override fun write(b: ByteArray, o: Int, l: Int) = w.write(String(b, o, l))
        override fun write(b: ByteArray) = w.write(String(b))
        override fun close() = w.close()
        override fun flush() = w.flush()
    }

    internal class ReaderInputStream(val r: Reader) : LuaBinInput() {
        override fun read(): Int = r.read()
    }
}
/** Construct a LuajContext with its own globals which may
 * be debug globals depending on the value of the system
 * property 'org.luaj.debug'
 *
 *
 * If the system property 'org.luaj.debug' is set, the globals
 * created will be a debug globals that includes the debug
 * library.  This may provide better stack traces, but may
 * have negative impact on performance.
 */
