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

import org.luaj.vm2.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.*
import java.io.*

/**
 * Subclass of [LibFunction] which implements the standard lua `os` library.
 *
 *
 * This contains more complete implementations of the following functions
 * using features that are specific to JSE:
 *
 *  * `execute()`
 *  * `remove()`
 *  * `rename()`
 *  * `tmpname()`
 *
 *
 *
 * Because the nature of the `os` library is to encapsulate
 * os-specific features, the behavior of these functions varies considerably
 * from their counterparts in the C platform.
 *
 *
 * Typically, this library is included as part of a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("os").get("time").call() );
` *  </pre>
 *
 *
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new JseOsLib());
 * System.out.println( globals.get("os").get("time").call() );
` *  </pre>
 *
 * However, other libraries such as *MathLib* are not loaded in this case.
 *
 *
 * @see LibFunction
 *
 * @see OsLib
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * //@see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 OS Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.9)
 */
/** public constructor  */
class JseOsLib : OsLib() {

    override fun getenv(varname: String?): String? {
        val s = System.getenv(varname)
        return s ?: System.getProperty(varname!!)
    }

    override fun execute(command: String?): Varargs {
        val exitValue: Int = try {
            JseProcess(command!!, null, globals?.STDOUT?.toOutputStream(), globals?.STDERR?.toOutputStream()).waitFor()
        } catch (ioe: IOException) {
            EXEC_IOEXCEPTION
        } catch (e: InterruptedException) {
            EXEC_INTERRUPTED
        } catch (t: Throwable) {
            EXEC_ERROR
        }

        return if (exitValue == 0) LuaValue.varargsOf(
            LuaValue.BTRUE,
            LuaValue.valueOf("exit"),
            LuaValue.ZERO
        ) else LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("signal"), LuaValue.valueOf(exitValue))
    }


    override fun remove(filename: String?) {
        val f = File(filename!!)
        if (!f.exists())
            throw IOException("No such file or directory")
        if (!f.delete())
            throw IOException("Failed to delete")
    }


    override fun rename(oldname: String?, newname: String?) {
        val f = File(oldname!!)
        if (!f.exists())
            throw IOException("No such file or directory")
        if (!f.renameTo(File(newname!!)))
            throw IOException("Failed to delete")
    }

    override fun tmpname(): String {
        try {
            val f = File.createTempFile(OsLib.TMP_PREFIX, OsLib.TMP_SUFFIX)
            return f.name
        } catch (ioe: IOException) {
            return super.tmpname()
        }

    }

    companion object {

        /** return code indicating the execute() threw an I/O exception  */
        @kotlin.jvm.JvmField var EXEC_IOEXCEPTION = 1

        /** return code indicating the execute() was interrupted  */
        @kotlin.jvm.JvmField var EXEC_INTERRUPTED = -2

        /** return code indicating the execute() threw an unknown exception  */
        @kotlin.jvm.JvmField var EXEC_ERROR = -3
    }

}
