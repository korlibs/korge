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
package org.luaj.vm2.lib

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Subclass of [LuaFunction] common to Java functions exposed to lua.
 *
 *
 * To provide for common implementations in JME and JSE,
 * library functions are typically grouped on one or more library classes
 * and an opcode per library function is defined and used to key the switch
 * to the correct function within the library.
 *
 *
 * Since lua functions can be called with too few or too many arguments,
 * and there are overloaded [LuaValue.call] functions with varying
 * number of arguments, a Java function exposed in lua needs to handle  the
 * argument fixup when a function is called with a number of arguments
 * differs from that expected.
 *
 *
 * To simplify the creation of library functions,
 * there are 5 direct subclasses to handle common cases based on number of
 * argument values and number of return return values.
 *
 *  * [ZeroArgFunction]
 *  * [OneArgFunction]
 *  * [TwoArgFunction]
 *  * [ThreeArgFunction]
 *  * [VarArgFunction]
 *
 *
 *
 * To be a Java library that can be loaded via `require`, it should have
 * a public constructor that returns a [LuaValue] that, when executed,
 * initializes the library.
 *
 *
 * For example, the following code will implement a library called "hyperbolic"
 * with two functions, "sinh", and "cosh":
 * <pre> `import org.luaj.vm2.LuaValue;
 * import org.luaj.vm2.lib.*;
 *
 * public class hyperbolic extends TwoArgFunction {
 *
 * public hyperbolic() {}
 *
 * public LuaValue call(LuaValue modname, LuaValue env) {
 * LuaValue library = tableOf();
 * library.set( "sinh", new sinh() );
 * library.set( "cosh", new cosh() );
 * env.set( "hyperbolic", library );
 * return library;
 * }
 *
 * static class sinh extends OneArgFunction {
 * public LuaValue call(LuaValue x) {
 * return LuaValue.valueOf(Math.sinh(x.checkdouble()));
 * }
 * }
 *
 * static class cosh extends OneArgFunction {
 * public LuaValue call(LuaValue x) {
 * return LuaValue.valueOf(Math.cosh(x.checkdouble()));
 * }
 * }
 * }
`</pre> *
 * The default constructor is used to instantiate the library
 * in response to `require 'hyperbolic'` statement,
 * provided it is on Java&quot;s class path.
 * This instance is then invoked with 2 arguments: the name supplied to require(),
 * and the environment for this function.  The library may ignore these, or use
 * them to leave side effects in the global environment, for example.
 * In the previous example, two functions are created, 'sinh', and 'cosh', and placed
 * into a global table called 'hyperbolic' using the supplied 'env' argument.
 *
 *
 * To test it, a script such as this can be used:
 * <pre> `local t = require('hyperbolic')
 * print( 't', t )
 * print( 'hyperbolic', hyperbolic )
 * for k,v in pairs(t) do
 * print( 'k,v', k,v )
 * end
 * print( 'sinh(.5)', hyperbolic.sinh(.5) )
 * print( 'cosh(.5)', hyperbolic.cosh(.5) )
`</pre> *
 *
 *
 * It should produce something like:
 * <pre> `t	table: 3dbbd23f
 * hyperbolic	table: 3dbbd23f
 * k,v	cosh	function: 3dbbd128
 * k,v	sinh	function: 3dbbd242
 * sinh(.5)	0.5210953
 * cosh(.5)	1.127626
`</pre> *
 *
 *
 * See the source code in any of the library functions
 * such as [BaseLib] or [TableLib] for other examples.
 */
abstract class LibFunction
/** Default constructor for use by subclasses  */
protected constructor() : LuaFunction() {

    /** User-defined opcode to differentiate between instances of the library function class.
     *
     *
     * Subclass will typicall switch on this value to provide the specific behavior for each function.
     */
    @kotlin.jvm.JvmField
    protected var opcode: Int = 0

    /** The common name for this function, useful for debugging.
     *
     *
     * Binding functions initialize this to the name to which it is bound.
     */
    @kotlin.jvm.JvmField
    protected var name: String? = null

    override fun tojstring(): String {
        return if (name != null) name!! else super.tojstring()
    }

    /**
     * Bind a set of library functions, with an offset
     *
     *
     * An array of names is provided, and the first name is bound
     * with opcode = `firstopcode`, second with `firstopcode+1`, etc.
     * @param env The environment to apply to each bound function
     * @param factory the Class to instantiate for each bound function
     * @param names array of String names, one for each function.
     * @param firstopcode the first opcode to use
     * @see .bind
     */
    @JvmOverloads
    protected fun bind(env: LuaValue, factory: () -> LibFunction, names: Array<String>, firstopcode: Int = 0) {
        try {
            var i = 0
            val n = names.size
            while (i < n) {
                val f = factory()
                f.opcode = firstopcode + i
                f.name = names[i]
                env[f.name!!] = f
                i++
            }
        } catch (e: Exception) {
            throw LuaError("bind failed: $e")
        }

    }

    override fun call(): LuaValue {
        return LuaValue.argerror(1, "value")
    }

    override fun call(a: LuaValue): LuaValue {
        return call()
    }

    override fun call(a: LuaValue, b: LuaValue): LuaValue {
        return call(a)
    }

    override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
        return call(a, b)
    }

    open fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
        return call(a, b, c)
    }

    override fun invoke(args: Varargs): Varargs {
        when (args.narg()) {
            0 -> return call()
            1 -> return call(args.arg1())
            2 -> return call(args.arg1(), args.arg(2))
            3 -> return call(args.arg1(), args.arg(2), args.arg(3))
            else -> return call(args.arg1(), args.arg(2), args.arg(3), args.arg(4))
        }
    }

    companion object {

        /** Java code generation utility to allocate storage for upvalue, leave it empty  */
        protected fun newupe(): Array<LuaValue> {
            return arrayOfNulls<LuaValue>(1) as Array<LuaValue>
        }

        /** Java code generation utility to allocate storage for upvalue, initialize with nil  */
        protected fun newupn(): Array<LuaValue> {
            return arrayOf(LuaValue.NIL)
        }

        /** Java code generation utility to allocate storage for upvalue, initialize with value  */
        protected fun newupl(v: LuaValue): Array<LuaValue> {
            return arrayOf(v)
        }
    }
}
/**
 * Bind a set of library functions.
 *
 *
 * An array of names is provided, and the first name is bound
 * with opcode = 0, second with 1, etc.
 * @param env The environment to apply to each bound function
 * @param factory the Class to instantiate for each bound function
 * @param names array of String names, one for each function.
 * @see .bind
 */
