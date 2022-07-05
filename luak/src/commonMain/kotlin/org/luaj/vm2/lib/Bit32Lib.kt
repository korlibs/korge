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
package org.luaj.vm2.lib

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/**
 * Subclass of LibFunction that implements the Lua standard `bit32` library.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("bit32").get("bnot").call( LuaValue.valueOf(2) ) );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new Bit32Lib());
 * System.out.println( globals.get("bit32").get("bnot").call( LuaValue.valueOf(2) ) );
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
 * @see [Lua 5.2 Bitwise Operation Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.7)
 */
class Bit32Lib : TwoArgFunction() {

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val t = LuaTable()
        bind(t, { Bit32LibV() }, arrayOf("band", "bnot", "bor", "btest", "bxor", "extract", "replace"))
        bind(t, { Bit32Lib2() }, arrayOf("arshift", "lrotate", "lshift", "rrotate", "rshift"))
        env["bit32"] = t
        env["package"]["loaded"]["bit32"] = t
        return t
    }

    internal class Bit32LibV : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            when (opcode) {
                0 -> return Bit32Lib.band(args)
                1 -> return Bit32Lib.bnot(args)
                2 -> return Bit32Lib.bor(args)
                3 -> return Bit32Lib.btest(args)
                4 -> return Bit32Lib.bxor(args)
                5 -> return Bit32Lib.extract(args.checkint(1), args.checkint(2), args.optint(3, 1))
                6 -> return Bit32Lib.replace(
                    args.checkint(1), args.checkint(2),
                    args.checkint(3), args.optint(4, 1)
                )
            }
            return LuaValue.NIL
        }
    }

    internal class Bit32Lib2 : TwoArgFunction() {

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            when (opcode) {
                0 -> return Bit32Lib.arshift(arg1.checkint(), arg2.checkint())
                1 -> return Bit32Lib.lrotate(arg1.checkint(), arg2.checkint())
                2 -> return Bit32Lib.lshift(arg1.checkint(), arg2.checkint())
                3 -> return Bit32Lib.rrotate(arg1.checkint(), arg2.checkint())
                4 -> return Bit32Lib.rshift(arg1.checkint(), arg2.checkint())
            }
            return LuaValue.NIL
        }

    }

    companion object {

         internal fun arshift(x: Int, disp: Int): LuaValue {
            return if (disp >= 0) {
                bitsToValue(x shr disp)
            } else {
                bitsToValue(x shl -disp)
            }
        }

         internal fun rshift(x: Int, disp: Int): LuaValue {
            return if (disp >= 32 || disp <= -32) {
                LuaValue.ZERO
            } else if (disp >= 0) {
                bitsToValue(x.ushr(disp))
            } else {
                bitsToValue(x shl -disp)
            }
        }

         internal fun lshift(x: Int, disp: Int): LuaValue {
            return if (disp >= 32 || disp <= -32) {
                LuaValue.ZERO
            } else if (disp >= 0) {
                bitsToValue(x shl disp)
            } else {
                bitsToValue(x.ushr(-disp))
            }
        }

         internal fun band(args: Varargs): Varargs {
            var result = -1
            for (i in 1..args.narg()) {
                result = result and args.checkint(i)
            }
            return bitsToValue(result)
        }

         internal fun bnot(args: Varargs): Varargs {
            return bitsToValue(args.checkint(1).inv())
        }

         internal fun bor(args: Varargs): Varargs {
            var result = 0
            for (i in 1..args.narg()) {
                result = result or args.checkint(i)
            }
            return bitsToValue(result)
        }

         internal fun btest(args: Varargs): Varargs {
            var bits = -1
            for (i in 1..args.narg()) {
                bits = bits and args.checkint(i)
            }
            return LuaValue.valueOf(bits != 0)
        }

         internal fun bxor(args: Varargs): Varargs {
            var result = 0
            for (i in 1..args.narg()) {
                result = result xor args.checkint(i)
            }
            return bitsToValue(result)
        }

         internal fun lrotate(x: Int, disp: Int): LuaValue {
            var disp = disp
            if (disp < 0) {
                return rrotate(x, -disp)
            } else {
                disp = disp and 31
                return bitsToValue(x shl disp or x.ushr(32 - disp))
            }
        }

         internal fun rrotate(x: Int, disp: Int): LuaValue {
            var disp = disp
            if (disp < 0) {
                return lrotate(x, -disp)
            } else {
                disp = disp and 31
                return bitsToValue(x.ushr(disp) or (x shl 32 - disp))
            }
        }

         internal fun extract(n: Int, field: Int, width: Int): LuaValue {
            if (field < 0) {
                LuaValue.argerror(2, "field cannot be negative")
            }
            if (width < 0) {
                LuaValue.argerror(3, "width must be postive")
            }
            if (field + width > 32) {
                LuaValue.error("trying to access non-existent bits")
            }
            return bitsToValue(n.ushr(field) and (-1).ushr(32 - width))
        }

         internal fun replace(n: Int, v: Int, field: Int, width: Int): LuaValue {
            var n = n
            if (field < 0) {
                LuaValue.argerror(3, "field cannot be negative")
            }
            if (width < 0) {
                LuaValue.argerror(4, "width must be postive")
            }
            if (field + width > 32) {
                LuaValue.error("trying to access non-existent bits")
            }
            val mask = (-1).ushr(32 - width) shl field
            n = n and mask.inv() or (v shl field and mask)
            return bitsToValue(n)
        }

         private fun bitsToValue(x: Int): LuaValue {
            return if (x < 0) LuaValue.valueOf((x.toLong() and 0xFFFFFFFFL).toDouble()) else LuaValue.valueOf(x)
        }
    }
}
