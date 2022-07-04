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

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.ResourceFinder

/**
 * Subclass of [BaseLib] and [LibFunction] which implements the lua basic library functions
 * and provides a directory based [ResourceFinder] as the [Globals.finder].
 *
 *
 * Since JME has no file system by default, [BaseLib] implements
 * [ResourceFinder] using [Class.getResource].
 * The [JseBaseLib] implements [Globals.finder] by scanning the current directory
 * first, then falling back to   [Class.getResource] if that fails.
 * Otherwise, the behavior is the same as that of [BaseLib].
 *
 *
 * Typically, this library is included as part of a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.get("print").call(LuaValue.valueOf("hello, world"));
` *  </pre>
 *
 *
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.get("print").call(LuaValue.valueOf("hello, world"));
` *  </pre>
 *
 * However, other libraries such as *PackageLib* are not loaded in this case.
 *
 *
 * This is a direct port of the corresponding library in C.
 * @see Globals
 *
 * @see BaseLib
 *
 * @see ResourceFinder
 *
 * @see Globals.finder
 *
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 Base Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.1)
 */

class JseBaseLib : BaseLib() {


    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * <P>Specifically, extend the library loading to set the default value for [Globals.STDIN]
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
    </P> */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        super.call(modname, env)
        env.checkglobals()!!.STDIN = System.`in`.toLua()
        return env
    }


    /**
     * Try to open a file in the current working directory,
     * or fall back to base opener if not found.
     *
     * This implementation attempts to open the file using new File(filename).
     * It falls back to the base implementation that looks it up as a resource
     * in the class path if not found as a plain file.
     *
     * @see BaseLib
     *
     * @see ResourceFinder
     *
     *
     * @param filename
     * @return InputStream, or null if not found.
     */
    override fun findResource(filename: String): LuaBinInput? {
        val f = File(filename)
        if (!f.exists())
            return super.findResource(filename)
        try {
            return f.readBytes().toLuaBinInput()
        } catch (ioe: IOException) {
            return null
        }

    }
}
