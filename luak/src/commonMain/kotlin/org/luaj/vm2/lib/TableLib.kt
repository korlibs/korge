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
package org.luaj.vm2.lib

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/**
 * Subclass of [LibFunction] which implements the lua standard `table`
 * library.
 *
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("table").get("length").call( LuaValue.tableOf() ) );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new TableLib());
 * System.out.println( globals.get("table").get("length").call( LuaValue.tableOf() ) );
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
 * @see [Lua 5.2 Table Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.5)
 */
class TableLib : TwoArgFunction() {

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val table = LuaTable()
        table.set("concat", concat())
        table.set("insert", insert())
        table.set("pack", pack())
        table.set("remove", remove())
        table.set("sort", sort())
        table.set("unpack", unpack())
        env.set("table", table)
        env.get("package").get("loaded").set("table", table)
        return LuaValue.NIL
    }

    internal open class TableLibFunction : LibFunction() {
        override fun call(): LuaValue {
            return LuaValue.argerror(1, "table expected, got no value")
        }
    }

    // "concat" (table [, sep [, i [, j]]]) -> string
    internal class concat : TableLibFunction() {
        override fun call(list: LuaValue): LuaValue {
            return list.checktable()!!.concat(LuaValue.EMPTYSTRING, 1, list.length())
        }

        override fun call(list: LuaValue, sep: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring()!!, 1, list.length())
        }

        override fun call(list: LuaValue, sep: LuaValue, i: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring()!!, i.checkint(), list.length())
        }

        override fun call(list: LuaValue, sep: LuaValue, i: LuaValue, j: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring()!!, i.checkint(), j.checkint())
        }
    }

    // "insert" (table, [pos,] value)
    internal class insert : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            when (args.narg()) {
                0, 1 -> {
                    return LuaValue.argerror(2, "value expected")
                }
                2 -> {
                    val table = args.arg1().checktable()
                    table!!.insert(table.length() + 1, args.arg(2))
                    return LuaValue.NONE
                }
                else -> {
                    args.arg1().checktable()!!.insert(args.checkint(2), args.arg(3))
                    return LuaValue.NONE
                }
            }
        }
    }

    // "pack" (...) -> table
    internal class pack : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t = LuaValue.tableOf(args, 1)
            t.set("n", args.narg())
            return t
        }
    }

    // "remove" (table [, pos]) -> removed-ele
    internal class remove : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return args.arg1().checktable()!!.remove(args.optint(2, 0))
        }
    }

    // "sort" (table [, comp])
    internal class sort : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            args.arg1().checktable()!!.sort(
                if (args.arg(2).isnil()) LuaValue.NIL else args.arg(2).checkfunction()!!
            )
            return LuaValue.NONE
        }
    }


    // "unpack", // (list [,i [,j]]) -> result1, ...
    internal class unpack : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t = args.checktable(1)!!
            when (args.narg()) {
                1 -> return t.unpack()
                2 -> return t.unpack(args.checkint(2))
                else -> return t.unpack(args.checkint(2), args.checkint(3))
            }
        }
    }
}
