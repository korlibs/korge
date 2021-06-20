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

import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.internal.*
import kotlin.math.*
import kotlin.native.concurrent.*
import kotlin.random.Random

/**
 * Subclass of [LibFunction] which implements the lua standard `math`
 * library.
 *
 *
 * It contains only the math library support that is possible on JME.
 * For a more complete implementation based on math functions specific to JSE
 * use [org.luaj.vm2.lib.jse.JseMathLib].
 * In Particular the following math functions are **not** implemented by this library:
 *
 *  * acos
 *  * asin
 *  * atan
 *  * cosh
 *  * log
 *  * sinh
 *  * tanh
 *  * atan2
 *
 *
 *
 * The implementations of `exp()` and `pow()` are constructed by
 * hand for JME, so will be slower and less accurate than when executed on the JSE platform.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or
 * [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) );
` *  </pre>
 * When using [org.luaj.vm2.lib.jse.JsePlatform] as in this example,
 * the subclass [org.luaj.vm2.lib.jse.JseMathLib] will
 * be included, which also includes this base functionality.
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new MathLib());
 * System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) );
` *  </pre>
 * Doing so will ensure the library is properly initialized
 * and loaded into the globals table.
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see org.luaj.vm2.lib.jse.JseMathLib
 *
 * @see [Lua 5.2 Math Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.6)
 */
open class MathLib : TwoArgFunction() {
    /** Construct a MathLib, which can be initialized by calling it with a
     * modname string, and a global environment table as arguments using
     * [.call].  */
    init {
        MATHLIB = this
    }

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val math = LuaTable(0, 30)
        math.set("abs", abs())
        math.set("ceil", ceil())
        math.set("cos", cos())
        math.set("deg", deg())
        math.set("exp", exp(this))
        math.set("floor", floor())
        math.set("fmod", fmod())
        math.set("frexp", frexp())
        math.set("huge", LuaDouble.POSINF)
        math.set("ldexp", ldexp())
        math.set("max", max())
        math.set("min", min())
        math.set("modf", modf())
        math.set("pi", kotlin.math.PI)
        math.set("pow", pow())
        val r = random()
        math.set("random", r)
        math.set("randomseed", randomseed(r))
        math.set("rad", rad())
        math.set("sin", sin())
        math.set("sqrt", sqrt())
        math.set("tan", tan())
        env.set("math", math)
        env.get("package").get("loaded").set("math", math)
        return math
    }

    abstract class UnaryOp : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(call(arg.checkdouble()))
        }

        protected abstract fun call(d: Double): Double
    }

    abstract class BinaryOp : TwoArgFunction() {
        override fun call(x: LuaValue, y: LuaValue): LuaValue {
            return LuaValue.valueOf(call(x.checkdouble(), y.checkdouble()))
        }

        protected abstract fun call(x: Double, y: Double): Double
    }

    internal class abs : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.abs(d)
        }
    }

    internal class ceil : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.ceil(d)
        }
    }

    internal class cos : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.cos(d)
        }
    }

    internal class deg : UnaryOp() {
        override fun call(d: Double): Double {
            return d * 180.0 / kotlin.math.PI
        }
    }

    internal class floor : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.floor(d)
        }
    }

    internal class rad : UnaryOp() {
        override fun call(d: Double): Double {
            return d / 180.0 * kotlin.math.PI
        }
    }

    internal class sin : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sin(d)
        }
    }

    internal class sqrt : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sqrt(d)
        }
    }

    internal class tan : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.tan(d)
        }
    }

    internal class exp(val mathlib: MathLib) : UnaryOp() {
        override fun call(d: Double): Double {
            return mathlib.dpow_lib(kotlin.math.E, d)
        }
    }

    internal class fmod : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            val q = x / y
            return x - y * if (q >= 0) kotlin.math.floor(q) else kotlin.math.ceil(q)
        }
    }

    internal class ldexp : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            // This is the behavior on os-x, windows differs in rounding behavior.
            return x * Double.fromBits(y.toLong() + 1023 shl 52)
        }
    }

    internal class pow : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            return MathLib.dpow_default(x, y)
        }
    }

    internal class frexp : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1)
            if (x == 0.0) return LuaValue.varargsOf(LuaValue.ZERO, LuaValue.ZERO)
            val bits = (x).toRawBits()
            val m =
                ((bits and (-1L shl 52).inv()) + (1L shl 52)) * if (bits >= 0) .5 / (1L shl 52) else -.5 / (1L shl 52)
            val e = (((bits shr 52).toInt() and 0x7ff) - 1022).toDouble()
            return LuaValue.varargsOf(LuaValue.valueOf(m), LuaValue.valueOf(e))
        }
    }

    internal class max : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var m = args.checkdouble(1)
            var i = 2
            val n = args.narg()
            while (i <= n) {
                m = max(m, args.checkdouble(i))
                ++i
            }
            return LuaValue.valueOf(m)
        }
    }

    internal class min : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var m = args.checkdouble(1)
            var i = 2
            val n = args.narg()
            while (i <= n) {
                m = min(m, args.checkdouble(i))
                ++i
            }
            return LuaValue.valueOf(m)
        }
    }

    internal class modf : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1)
            val intPart = if (x > 0) kotlin.math.floor(x) else kotlin.math.ceil(x)
            val fracPart = x - intPart
            return LuaValue.varargsOf(LuaValue.valueOf(intPart), LuaValue.valueOf(fracPart))
        }
    }

    internal class random : LibFunction() {
        var random: Random = Random.Default
        override fun call(): LuaValue {
            return LuaValue.valueOf(random.nextDouble())
        }

        override fun call(a: LuaValue): LuaValue {
            val m = a.checkint()
            if (m < 1) LuaValue.argerror(1, "interval is empty")
            return LuaValue.valueOf(1 + random.nextInt(m))
        }

        override fun call(a: LuaValue, b: LuaValue): LuaValue {
            val m = a.checkint()
            val n = b.checkint()
            if (n < m) LuaValue.argerror(2, "interval is empty")
            return LuaValue.valueOf(m + random.nextInt(n + 1 - m))
        }

    }

    internal class randomseed(val random: random) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val seed = arg.checklong()
            random.random = Random(seed)
            return LuaValue.NONE
        }
    }

    /**
     * Hook to override default dpow behavior with faster implementation.
     */
    open fun dpow_lib(a: Double, b: Double): Double {
        return dpow_default(a, b)
    }

    companion object {

        /** Pointer to the latest MathLib instance, used only to dispatch
         * math.exp to tha correct platform math library.
         */
        var MATHLIB: MathLib?
            get() = MathLib_MATHLIB
            set(value) {
                MathLib_MATHLIB = value
            }

        /** compute power using installed math library, or default if there is no math library installed  */

        fun dpow(a: Double, b: Double): LuaValue {
            return LuaDouble.valueOf(
                if (MATHLIB != null)
                    MATHLIB!!.dpow_lib(a, b)
                else
                    dpow_default(a, b)
            )
        }


        fun dpow_d(a: Double, b: Double): Double {
            return if (MATHLIB != null)
                MATHLIB!!.dpow_lib(a, b)
            else
                dpow_default(a, b)
        }

        /**
         * Default JME version computes using longhand heuristics.
         */

        protected fun dpow_default(a: Double, b: Double): Double {
            var a = a
            var b = b
            if (b < 0)
                return 1 / dpow_default(a, -b)
            var p = 1.0
            var whole = b.toInt()
            var v = a
            while (whole > 0) {
                if (whole and 1 != 0)
                    p *= v
                whole = whole shr 1
                v *= v
            }
            if ((run {
                    b -= whole.toDouble()
                    b
                }) > 0) {
                var frac = (0x10000 * b).toInt()
                while (frac and 0xffff != 0) {
                    a = kotlin.math.sqrt(a)
                    if (frac and 0x8000 != 0)
                        p *= a
                    frac = frac shl 1
                }
            }
            return p
        }
    }

}

@ThreadLocal
private var MathLib_MATHLIB: MathLib? = null
