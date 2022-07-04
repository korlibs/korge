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

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.MathLib

/**
 * Subclass of [LibFunction] which implements the lua standard `math`
 * library.
 *
 *
 * It contains all lua math functions, including those not available on the JME platform.
 * See [org.luaj.vm2.lib.MathLib] for the exception list.
 *
 *
 * Typically, this library is included as part of a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) );
` *  </pre>
 *
 *
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new JseMathLib());
 * System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) );
` *  </pre>
 *
 * However, other libraries such as *CoroutineLib* are not loaded in this case.
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see JseMathLib
 *
 * @see [Lua 5.2 Math Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.6)
 */
class JseMathLib : org.luaj.vm2.lib.MathLib() {


    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * <P>Specifically, adds all library functions that can be implemented directly
     * in JSE but not JME: acos, asin, atan, atan2, cosh, exp, log, pow, sinh, and tanh.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
    </P> */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        super.call(modname, env)
        val math = env["math"]
        math.set("acos", acos())
        math.set("asin", asin())
        math.set("atan", atan())
        math.set("atan2", atan2())
        math.set("cosh", cosh())
        math.set("exp", exp())
        math.set("log", log())
        math.set("pow", pow())
        math.set("sinh", sinh())
        math.set("tanh", tanh())
        return math
    }

    internal class acos : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.acos(d)
        }
    }

    internal class asin : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.asin(d)
        }
    }

    internal class atan : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.atan(d)
        }
    }

    internal class atan2 : MathLib.BinaryOp() {
        override fun call(y: Double, x: Double): Double {
            return Math.atan2(y, x)
        }
    }

    internal class cosh : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.cosh(d)
        }
    }

    internal class exp : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.exp(d)
        }
    }

    internal class log : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.log(d)
        }
    }

    internal class pow : MathLib.BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            return Math.pow(x, y)
        }
    }

    internal class sinh : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.sinh(d)
        }
    }

    internal class tanh : MathLib.UnaryOp() {
        override fun call(d: Double): Double {
            return Math.tanh(d)
        }
    }

    /** Faster, better version of pow() used by arithmetic operator ^  */
    override fun dpow_lib(a: Double, b: Double): Double {
        return Math.pow(a, b)
    }


}
