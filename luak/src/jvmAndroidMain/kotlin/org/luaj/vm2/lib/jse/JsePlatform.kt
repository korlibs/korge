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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.lib.common.*
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.ResourceFinder
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib

/** The [JsePlatform] class is a convenience class to standardize
 * how globals tables are initialized for the JSE platform.
 *
 *
 * It is used to allocate either a set of standard globals using
 * [.standardGlobals] or debug globals using [.debugGlobals]
 *
 *
 * A simple example of initializing globals and using them from Java is:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.get("print").call(LuaValue.valueOf("hello, world"));
` *  </pre>
 *
 *
 * Once globals are created, a simple way to load and run a script is:
 * <pre> `globals.load( new FileInputStream("main.lua"), "main.lua" ).call();
` *  </pre>
 *
 *
 * although `require` could also be used:
 * <pre> `globals.get("require").call(LuaValue.valueOf("main"));
` *  </pre>
 * For this to succeed, the file "main.lua" must be in the current directory or a resource.
 * See [JseBaseLib] for details on finding scripts using [ResourceFinder].
 *
 *
 * The standard globals will contain all standard libraries plus `luajava`:
 *
 *  * [Globals]
 *  * [JseBaseLib]
 *  * [PackageLib]
 *  * [Bit32Lib]
 *  * [TableLib]
 *  * [StringLib]
 *  * [CoroutineLib]
 *  * [JseMathLib]
 *  * [JseIoLib]
 *  * [JseOsLib]
 *  * [LuajavaLib]
 *
 * In addition, the [LuaC] compiler is installed so lua files may be loaded in their source form.
 *
 *
 * The debug globals are simply the standard globals plus the `debug` library [DebugLib].
 *
 *
 * The class ensures that initialization is done in the correct order.
 *
 * @see Globals
 * //@see org.luaj.vm2.lib.jme.JmePlatform
 */
object JsePlatform : CommonPlatform() {

    /**
     * Create a standard set of globals for JSE including all the libraries.
     *
     * @return Table of globals initialized with the standard JSE libraries
     * @see .debugGlobals
     * @see JsePlatform
     * //@see org.luaj.vm2.lib.jme.JmePlatform
     */
     override fun standardGlobals(): Globals {
        val globals = Globals()
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        globals.load(JseIoLib())
        globals.load(JseOsLib())
        globals.load(LuajavaLib())
        LoadState.install(globals)
        LuaC.install(globals)
        return globals
    }
}
