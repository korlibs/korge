/*******************************************************************************
 * Copyright (c) 2010-2011 Luaj.org. All rights reserved.
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

import org.luaj.vm2.*
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

/**
 * Subclass of [LibFunction] which implements the lua standard package and module
 * library functions.
 *
 * <h3>Lua Environment Variables</h3>
 * The following variables are available to lua scrips when this library has been loaded:
 *
 *  * `"package.loaded"` Lua table of loaded modules.
 *  * `"package.path"` Search path for lua scripts.
 *  * `"package.preload"` Lua table of uninitialized preload functions.
 *  * `"package.searchers"` Lua table of functions that search for object to load.
 *
 *
 * <h3>Java Environment Variables</h3>
 * These Java environment variables affect the library behavior:
 *
 *  * `"luaj.package.path"` Initial value for `"package.path"`.  Default value is `"?.lua"`
 *
 *
 * <h3>Loading</h3>
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("require").call"foo") );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * System.out.println( globals.get("require").call("foo") );
` *  </pre>
 * <h3>Limitations</h3>
 * This library has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * However, the default filesystem search semantics are different and delegated to the bas library
 * as outlined in the [BaseLib] and [org.luaj.vm2.lib.jse.JseBaseLib] documentation.
 *
 *
 * @see LibFunction
 *
 * @see BaseLib
 *
 * @see org.luaj.vm2.lib.jse.JseBaseLib
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 Package Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.3)
 */
class PackageLib : TwoArgFunction() {

    /** The globals that were used to load this library.  */
    @kotlin.jvm.JvmField
    internal var globals: Globals? = null

    /** The table for this package.  */
    @kotlin.jvm.JvmField
    internal var package_: LuaTable? = null

    /** Loader that loads from `preload` table if found there  */
    @kotlin.jvm.JvmField
    var preload_searcher: Preload_searcher? = null

    /** Loader that loads as a lua script using the lua path currently in [path]  */
    @kotlin.jvm.JvmField
    var lua_searcher: Lua_searcher? = null

    /** Loader that loads as a Java class.  Class must have public constructor and be a LuaValue.  */
    @kotlin.jvm.JvmField
    var java_searcher: Java_searcher? = null

    /** Perform one-time initialization on the library by adding package functions
     * to the supplied environment, and returning it as the return value.
     * It also creates the package.preload and package.loaded tables for use by
     * other libraries.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        globals!!["require"] = require()
        package_ = LuaTable()
        val package_ = package_!!
        package_[_LOADED] = LuaTable()
        package_[_PRELOAD] = LuaTable()
        package_[_PATH] = valueOf(DEFAULT_LUA_PATH!!)
        package_[_LOADLIB] = loadlib()
        package_[_SEARCHPATH] = searchpath()
        val searchers = LuaTable()
        searchers.set(1, run { preload_searcher = Preload_searcher(); preload_searcher!! })
        searchers.set(2, run { lua_searcher = Lua_searcher(); lua_searcher!! })
        searchers.set(3, run { java_searcher = Java_searcher(); java_searcher!! })
        package_[_SEARCHERS] = searchers
        package_[_LOADED]["package"] = package_
        env["package"] = package_
        globals!!.package_ = this
        return env
    }

    /** Allow packages to mark themselves as loaded  */
    fun setIsLoaded(name: String, value: LuaTable) {
        package_!![_LOADED][name] = value
    }


    /** Set the lua path used by this library instance to a new value.
     * Merely sets the value of [path] to be used in subsequent searches.  */
    fun setLuaPath(newLuaPath: String) {
        package_!![_PATH] = valueOf(newLuaPath)
    }

    override fun tojstring(): String {
        return "package"
    }

    // ======================== Package loading =============================

    /**
     * require (modname)
     *
     * Loads the given module. The function starts by looking into the package.loaded table
     * to determine whether modname is already loaded. If it is, then require returns the value
     * stored at package.loaded[modname]. Otherwise, it tries to find a loader for the module.
     *
     * To find a loader, require is guided by the package.searchers sequence.
     * By changing this sequence, we can change how require looks for a module.
     * The following explanation is based on the default configuration for package.searchers.
     *
     * First require queries package.preload[modname]. If it has a value, this value
     * (which should be a function) is the loader. Otherwise require searches for a Lua loader using
     * the path stored in package.path. If that also fails, it searches for a Java loader using
     * the classpath, using the public default constructor, and casting the instance to LuaFunction.
     *
     * Once a loader is found, require calls the loader with two arguments: modname and an extra value
     * dependent on how it got the loader. If the loader came from a file, this extra value is the file name.
     * If the loader is a Java instance of LuaFunction, this extra value is the environment.
     * If the loader returns any non-nil value, require assigns the returned value to package.loaded[modname].
     * If the loader does not return a non-nil value and has not assigned any value to package.loaded[modname],
     * then require assigns true to this entry.
     * In any case, require returns the final value of package.loaded[modname].
     *
     * If there is any error loading or running the module, or if it cannot find any loader for the module,
     * then require raises an error.
     */
    inner class require : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val name = arg.checkstring()
            val loaded = package_!![_LOADED]
            var result = loaded[name!!]
            if (result.toboolean()) {
                if (result === _SENTINEL)
                    LuaValue.error("loop or previous error loading module '$name'")
                return result
            }

            /* else must load it; iterate over available loaders */
            val tbl = package_!![_SEARCHERS].checktable()
            val sb = StringBuilder()
            var loader: Varargs? = null
            var i = 1
            while (true) {
                val searcher = tbl!![i]
                if (searcher.isnil()) {
                    LuaValue.error("module '$name' not found: $name$sb")
                }

                /* call loader with module name as argument */
                loader = searcher.invoke(name)
                if (loader.isfunction(1))
                    break
                if (loader.isstring(1))
                    sb.append(loader.tojstring(1))
                i++
            }

            // load the module using the loader
            loaded[name] = _SENTINEL
            result = loader!!.arg1().call(name, loader.arg(2))
            if (!result.isnil())
                loaded[name] = result
            else if ((run { result = loaded[name]; result }) === _SENTINEL)
                loaded.set(name, run { result = LuaValue.BTRUE; result!! })
            return result
        }
    }

    class loadlib : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            args.checkstring(1)
            return varargsOf(
                NIL,
                valueOf("dynamic libraries not enabled"),
                valueOf("absent")
            )
        }
    }

    inner class Preload_searcher : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkstring(1)
            val `val` = package_!![_PRELOAD]!![name!!]
            return if (`val`.isnil())
                valueOf("\n\tno field package.preload['$name']")
            else
                `val`
        }
    }

    inner class Lua_searcher : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkstring(1)
            val `is`: LuaBinInput? = null

            // get package path
            val path = package_!![_PATH]!!
            if (!path.isstring())
                return valueOf("package.path is not a string")

            // get the searchpath function.
            var v = package_!![_SEARCHPATH]!!.invoke(varargsOf(name!!, path))

            // Did we get a result?
            if (!v.isstring(1))
                return v.arg(2).tostring()
            val filename = v.arg1().strvalue()

            // Try to load the file.
            v = globals!!.loadfile(filename!!.tojstring())
            return if (v.arg1().isfunction()) varargsOf(v.arg1(), filename) else varargsOf(
                NIL,
                valueOf("'" + filename + "': " + v.arg(2).tojstring())
            )

            // report error
        }
    }

    inner class searchpath : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var name = args.checkjstring(1)
            val path = args.checkjstring(2)
            val sep = args.optjstring(3, ".")
            val rep = args.optjstring(4, FILE_SEP!!)

            // check the path elements
            var e = -1
            val n = path!!.length
            var sb: StringBuilder? = null
            name = name!!.replace(sep!![0], rep!![0])
            while (e < n) {

                // find next template
                val b = e + 1
                e = path.indexOf(';', b)
                if (e < 0)
                    e = path.length
                val template = path.substring(b, e)

                // create filename
                val q = template.indexOf('?')
                var filename = template
                if (q >= 0) {
                    filename = template.substring(0, q) + name + template.substring(q + 1)
                }

                // try opening the file
                val `is` = globals!!.finder!!.findResource(filename)
                if (`is` != null) {
                    try {
                        `is`.close()
                    } catch (ioe: IOException) {
                    }

                    return valueOf(filename)
                }

                // report error
                if (sb == null)
                    sb = StringBuilder()
                sb.append("\n\t" + filename)
            }
            return varargsOf(NIL, valueOf(sb!!.toString()))
        }
    }

    inner class Java_searcher : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val classname = toClassname(name!!)
            try {
                val v = JSystem.InstantiateClassByName(classname) as? LuaValue ?: return valueOf("\n\tno class '$classname'")
                if (v.isfunction()) (v as LuaFunction).initupvalue1(globals!!)
                return varargsOf(v, globals!!)
            } catch (e: Exception) {
                return valueOf("\n\tjava load failed on '$classname', $e")
            }
        }
    }

    companion object {

        /** The default value to use for package.path.  This can be set with the system property
         * `"luaj.package.path"`, and is `"?.lua"` by default.  */
        var DEFAULT_LUA_PATH: String? = null

        init {
            try {
                DEFAULT_LUA_PATH = JSystem.getProperty("luaj.package.path")
            } catch (e: Exception) {
                println(e.toString())
            }

            if (DEFAULT_LUA_PATH == null)
                DEFAULT_LUA_PATH = "?.lua"
        }

        private val _LOADED = valueOf("loaded")
        private val _LOADLIB = valueOf("loadlib")
        private val _PRELOAD = valueOf("preload")
        private val _PATH = valueOf("path")
        private val _SEARCHPATH = valueOf("searchpath")
        private val _SEARCHERS = valueOf("searchers")

        private val _SENTINEL = valueOf("\u0001")

        private val FILE_SEP = JSystem.getProperty("file.separator")

        /** Convert lua filename to valid class name  */
        fun toClassname(filename: String): String {
            val n = filename.length
            var j = n
            if (filename.endsWith(".lua"))
                j -= 4
            for (k in 0 until j) {
                var c = filename[k]
                if (!isClassnamePart(c) || c == '/' || c == '\\') {
                    val sb = StringBuilder(j)
                    for (i in 0 until j) {
                        c = filename[i]
                        sb.append(
                            if (isClassnamePart(c))
                                c
                            else if (c == '/' || c == '\\') '.' else '_'
                        )
                    }
                    return sb.toString()
                }
            }
            return if (n == j) filename else filename.substring(0, j)
        }

        private fun isClassnamePart(c: Char): Boolean {
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')
                return true
            when (c) {
                '.', '$', '_' -> return true
                else -> return false
            }
        }
    }
}
