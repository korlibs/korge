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
package org.luaj.vm2

/**
 * Prototype representing compiled lua code.
 *
 *
 *
 * This is both a straight translation of the corresponding C type,
 * and the main data structure for execution of compiled lua bytecode.
 *
 *
 *
 * Generally, the [Prototype] is not constructed directly is an intermediate result
 * as lua code is loaded using [Globals.load]:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * globals.load( new StringReader("print 'hello'"), "main.lua" ).call();
` *  </pre>
 *
 *
 *
 * To create a [Prototype] directly, a compiler such as
 * [org.luaj.vm2.compiler.LuaC] may be used:
 * <pre> `InputStream is = new ByteArrayInputStream("print('hello,world')".getBytes());
 * Prototype p = LuaC.instance.compile(is, "script");
`</pre> *
 *
 * To simplify loading, the [Globals.compilePrototype] method may be used:
 * <pre> `Prototype p = globals.compileProtoytpe(is, "script");
`</pre> *
 *
 * It may also be loaded from a [java.io.Reader] via [Globals.compilePrototype]:
 * <pre> `Prototype p = globals.compileProtoytpe(new StringReader(script), "script");
`</pre> *
 *
 * To un-dump a binary file known to be a binary lua file that has been dumped to a string,
 * the [Globals.Undumper] interface may be used:
 * <pre> `FileInputStream lua_binary_file = new FileInputStream("foo.lc");  // Known to be compiled lua.
 * Prototype p = globals.undumper.undump(lua_binary_file, "foo.lua");
`</pre> *
 *
 * To execute the code represented by the [Prototype] it must be supplied to
 * the constructor of a [LuaClosure]:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * LuaClosure f = new LuaClosure(p, globals);
 * f.call();
`</pre> *
 *
 * To simplify the debugging of prototype values, the contents may be printed using [Print.print]:
 * <pre> `Print.print(p);
`</pre> *
 *
 *
 *
 * @see LuaClosure
 *
 * @see Globals
 *
 * @see Globals.undumper
 *
 * @see Globals.compiler
 *
 * @see Print.print
 */

class Prototype {
    /* constants used by the function */
    @kotlin.jvm.JvmField var k: Array<LuaValue> = arrayOf()
    @kotlin.jvm.JvmField var code: IntArray = IntArray(0)
    /* functions defined inside the function */
    @kotlin.jvm.JvmField var p: Array<Prototype>
    /* map from opcodes to source lines */
    @kotlin.jvm.JvmField var lineinfo: IntArray = IntArray(0)
    /* information about local variables */
    @kotlin.jvm.JvmField var locvars: Array<LocVars> = arrayOf()
    /* upvalue information */
    @kotlin.jvm.JvmField var upvalues: Array<Upvaldesc> = NOUPVALUES
    @kotlin.jvm.JvmField var source: LuaString = LuaString.valueOf("")
    @kotlin.jvm.JvmField var linedefined: Int = 0
    @kotlin.jvm.JvmField var lastlinedefined: Int = 0
    @kotlin.jvm.JvmField var numparams: Int = 0
    @kotlin.jvm.JvmField var is_vararg: Int = 0
    @kotlin.jvm.JvmField var maxstacksize: Int = 0

    constructor() {
        p = NOSUBPROTOS
        upvalues = NOUPVALUES
    }

    constructor(n_upvalues: Int) {
        p = NOSUBPROTOS
        upvalues = arrayOfNulls<Upvaldesc>(n_upvalues) as Array<Upvaldesc>
    }

    override fun toString(): String {
        return "$source:$linedefined-$lastlinedefined"
    }

    /** Get the name of a local variable.
     *
     * @param number the local variable number to look up
     * @param pc the program counter
     * @return the name, or null if not found
     */
    fun getlocalname(number: Int, pc: Int): LuaString? {
        var num = number
        var i = 0
        while (i < locvars.size && locvars[i].startpc <= pc) {
            if (pc < locvars[i].endpc) {  /* is variable active? */
                num--
                if (num == 0) return locvars[i].varname
            }
            i++
        }
        return null  /* not found */
    }

    fun shortsource(): String {
        val name = source.tojstring()
        return when {
            name.startsWith("@") || name.startsWith("=") -> name.substring(1)
            name.startsWith("\u001b") -> "binary string"
            else -> name
        }
    }

    companion object {
        private val NOUPVALUES = arrayOf<Upvaldesc>()
        private val NOSUBPROTOS = arrayOf<Prototype>()
    }
}
