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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.io.*
import java.io.*

/** Analog of Process that pipes input and output to client-specified streams.
 */
class JseProcess private constructor(
    internal val process: Process,
    stdin: LuaBinInput?,
    stdout: OutputStream?,
    stderr: OutputStream?
) {
    @kotlin.jvm.JvmField internal val input: Thread? = if (stdin == null) null else copyBytes(stdin, process.outputStream, null, process.outputStream)
    @kotlin.jvm.JvmField internal val output: Thread? = if (stdout == null) null else copyBytes(process.inputStream.toLua(), stdout, process.inputStream.toLua(), null)
    @kotlin.jvm.JvmField internal val error: Thread? = if (stderr == null) null else copyBytes(process.errorStream.toLua(), stderr, process.errorStream.toLua(), null)

    /** Construct a process around a command, with specified streams to redirect input and output to.
     *
     * @param cmd The command to execute, including arguments, if any
     * @param stdin Optional InputStream to read from as process input, or null if input is not needed.
     * @param stdout Optional OutputStream to copy process output to, or null if output is ignored.
     * @param stderr Optinoal OutputStream to copy process stderr output to, or null if output is ignored.
     * @com.soywiz.luak.compat.java.Throws IOException If the system process could not be created.
     * @see Process
     */

    constructor(
        cmd: Array<String>,
        stdin: LuaBinInput,
        stdout: OutputStream,
        stderr: OutputStream
    ) : this(Runtime.getRuntime().exec(cmd), stdin, stdout, stderr) {
    }

    /** Construct a process around a command, with specified streams to redirect input and output to.
     *
     * @param cmd The command to execute, including arguments, if any
     * @param stdin Optional InputStream to read from as process input, or null if input is not needed.
     * @param stdout Optional OutputStream to copy process output to, or null if output is ignored.
     * @param stderr Optinoal OutputStream to copy process stderr output to, or null if output is ignored.
     * @com.soywiz.luak.compat.java.Throws IOException If the system process could not be created.
     * @see Process
     */

    constructor(
        cmd: String,
        stdin: LuaBinInput?,
        stdout: OutputStream?,
        stderr: OutputStream?
    ) : this(Runtime.getRuntime().exec(cmd), stdin, stdout, stderr) {
    }

    /** Get the exit value of the process.  */
    fun exitValue(): Int {
        return process.exitValue()
    }

    /** Wait for the process to complete, and all pending output to finish.
     * @return The exit status.
     * @com.soywiz.luak.compat.java.Throws InterruptedException
     */

    fun waitFor(): Int {
        val r = process.waitFor()
        input?.join()
        output?.join()
        error?.join()
        process.destroy()
        return r
    }

    /** Create a thread to copy bytes from input to output.  */
    private fun copyBytes(
        input: LuaBinInput,
        output: OutputStream, ownedInput: LuaBinInput?,
        ownedOutput: OutputStream?
    ): Thread {
        val t = CopyThread(output, ownedOutput, ownedInput, input)
        t.start()
        return t
    }

    class CopyThread constructor(
        private val output: OutputStream, private val ownedOutput: OutputStream?,
        private val ownedInput: LuaBinInput?, private val input: LuaBinInput
    ) : Thread() {

        override fun run() {
            try {
                val buf = ByteArray(1024)
                var r: Int
                try {
                    while ((run { r = input.read(buf); r }) >= 0) {
                        output.write(buf, 0, r)
                    }
                } finally {
                    ownedInput?.close()
                    ownedOutput?.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

}
