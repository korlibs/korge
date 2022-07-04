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
package org.luaj.vm2

import org.luaj.vm2.io.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.io.*
import java.net.*
import kotlin.test.*

abstract class ScriptDrivenTest protected constructor(private val platform: PlatformType, private val subdir: String) :
    ResourceFinder {
    protected lateinit var globals: Globals

    enum class PlatformType {
        JME, JSE, LUAJIT
    }

    init {
        initGlobals()
        globals.finder = this
    }

    private fun initGlobals() {
        when (platform) {
            ScriptDrivenTest.PlatformType.JSE, ScriptDrivenTest.PlatformType.LUAJIT -> globals =
                org.luaj.vm2.lib.jse.JsePlatform.debugGlobals()
            ScriptDrivenTest.PlatformType.JME ->
                //globals = org.luaj.vm2.lib.jme.JmePlatform.debugGlobals();
                throw RuntimeException("No JME")
            else -> globals = org.luaj.vm2.lib.jse.JsePlatform.debugGlobals()
        }
    }

    // ResourceFinder implementation.
    override fun findResource(filename: String): LuaBinInput? {
        var `is`: LuaBinInput? = findInPlainFile(filename)
        if (`is` != null) return `is`
        `is` = findInPlainFileAsResource("", filename)
        if (`is` != null) return `is`
        `is` = findInPlainFileAsResource("/", filename)
        if (`is` != null) return `is`
        `is` = findInZipFileAsPlainFile(filename)
        if (`is` != null) return `is`
        `is` = findInZipFileAsResource("", filename)
        if (`is` != null) return `is`
        `is` = findInZipFileAsResource("/", filename)
        return `is`
    }

    private fun findInPlainFileAsResource(prefix: String, filename: String): LuaBinInput? {
        return javaClass.getResourceAsStream(prefix + subdir + filename)?.readBytes()?.toLuaBinInput()
    }

    private fun findInPlainFile(filename: String): LuaBinInput? {
        try {
            val f = File(zipdir + subdir + filename)
            if (f.exists())
                return f.readBytes().toLuaBinInput()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return null
    }

    private fun findInZipFileAsPlainFile(filename: String): LuaBinInput? {
        val zip: URL
        val file = File(zipdir + zipfile)
        try {
            if (file.exists()) {
                zip = file.toURI().toURL()
                val path = "jar:" + zip.toExternalForm() + "!/" + subdir + filename
                val url = URL(path)
                return url.openStream().readBytes().toLuaBinInput()
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            // Ignore and return null.
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return null
    }


    private fun findInZipFileAsResource(prefix: String, filename: String): LuaBinInput? {
        var zip: URL? = null
        zip = javaClass.getResource(zipfile)
        if (zip != null)
            try {
                val path = "jar:" + zip.toExternalForm() + "!/" + subdir + filename
                val url = URL(path)
                return url.openStream().readBytes().toLuaBinInput()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }

        return null
    }

    // */
    protected fun runTest(testName: String) {
        try {
            // override print()
            val output = ByteArrayLuaBinOutput()
            val oldps = globals.STDOUT
            val ps = output.toWriter()
            globals.STDOUT = ps

            // run the script
            try {
                val chunk = loadScript(testName, globals)
                chunk.call(LuaValue.valueOf(platform.toString()))

                ps.flush()
                var actualOutput = String(output.toByteArray())
                var expectedOutput = getExpectedOutput(testName)
                actualOutput = actualOutput.replace("\r\n".toRegex(), "\n")
                expectedOutput = expectedOutput.replace("\r\n".toRegex(), "\n")

                assertEquals(expectedOutput, actualOutput)
            } finally {
                globals.STDOUT = oldps
                ps.close()
            }
        } catch (ioe: IOException) {
            throw RuntimeException(ioe.toString())
        } catch (ie: InterruptedException) {
            throw RuntimeException(ie.toString())
        }

    }

    @Throws(IOException::class)
    protected fun loadScript(name: String, globals: Globals): LuaValue {
        val script = this.findResource("$name.lua") ?: throw AssertionError("Could not load script for test case: $name")

        return try {
            when (this.platform) {
                ScriptDrivenTest.PlatformType.LUAJIT -> if (nocompile) {
                    Class.forName(name).newInstance() as LuaValue
                } else {
                    globals.load(script, name, "bt", globals)
                }
                else -> globals.load(script, "@$name.lua", "bt", globals)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException(e.toString())
        } finally {
            script!!.close()
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun getExpectedOutput(name: String): String {
        val output = this.findResource("$name.out")
        if (output != null)
            try {
                return readString(output)
            } finally {
                output.close()
            }
        return executeLuaProcess(name) ?: throw IOException("Failed to get comparison output or run process for $name")
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun executeLuaProcess(name: String): String {
        val script = findResource("$name.lua")
        if (script == null)
            throw IOException("Failed to find source file " + script!!)
        try {
            var luaCommand: String? = System.getProperty("LUA_COMMAND")
            if (luaCommand == null)
                luaCommand = "lua"
            val args = arrayOf(luaCommand, "-", platform.toString())
            return collectProcessOutput(args, script)
        } finally {
            script.close()
        }
    }

    @Throws(IOException::class)
    private fun readString(`is`: LuaBinInput): String {
        val baos = ByteArrayOutputStream()
        copy(`is`, baos)
        return String(baos.toByteArray())
    }

    companion object {
        val nocompile = "true" == System.getProperty("nocompile")

        internal val zipdir = "test/lua/"
        internal val zipfile = "luaj3.0-tests.zip"

        @Throws(IOException::class, InterruptedException::class)
        fun collectProcessOutput(cmd: Array<String>, input: LuaBinInput): String {
            val r = Runtime.getRuntime()
            val baos = ByteArrayOutputStream()
            JseProcess(cmd, input, baos, System.err).waitFor()
            return String(baos.toByteArray())
        }

        @Throws(IOException::class)
        private fun copy(`is`: LuaBinInput, os: OutputStream) {
            val buf = ByteArray(1024)
            var r: Int
            while (run {
                    r = `is`.read(buf)
                    (r) >= 0
                }) {
                os.write(buf, 0, r)
            }
        }
    }

}
