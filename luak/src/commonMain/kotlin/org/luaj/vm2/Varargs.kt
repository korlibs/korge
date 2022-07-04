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
package org.luaj.vm2

import org.luaj.vm2.internal.*
import kotlin.math.*
import kotlin.reflect.*

/**
 * Class to encapsulate varargs values, either as part of a variable argument list, or multiple return values.
 *
 *
 * To construct varargs, use one of the static methods such as
 * `LuaValue.varargsOf(LuaValue,LuaValue)`
 *
 *
 *
 *
 * Any LuaValue can be used as a stand-in for Varargs, for both calls and return values.
 * When doing so, nargs() will return 1 and arg1() or arg(1) will return this.
 * This simplifies the case when calling or implementing varargs functions with only
 * 1 argument or 1 return value.
 *
 *
 * Varargs can also be derived from other varargs by appending to the front with a call
 * such as  `LuaValue.varargsOf(LuaValue,Varargs)`
 * or by taking a portion of the args using `Varargs.subargs(int start)`
 *
 *
 * @see LuaValue.varargsOf
 * @see LuaValue.varargsOf
 * @see LuaValue.varargsOf
 * @see LuaValue.varargsOf
 * @see LuaValue.varargsOf
 * @see LuaValue.varargsOf
 * @see LuaValue.subargs
 */
abstract class Varargs {

    /**
     * Return true if this is a TailcallVarargs
     * @return true if a tail call, false otherwise
     */
    open fun isTailcall(): Boolean = false

    /**
     * Get the n-th argument value (1-based).
     * @param i the index of the argument to get, 1 is the first argument
     * @return Value at position i, or LuaValue.NIL if there is none.
     * @see Varargs.arg1
     * @see LuaValue.NIL
     */
    abstract fun arg(i: Int): LuaValue

    /**
     * Get the number of arguments, or 0 if there are none.
     * @return number of arguments.
     */
    abstract fun narg(): Int

    /**
     * Get the first argument in the list.
     * @return LuaValue which is first in the list, or LuaValue.NIL if there are no values.
     * @see Varargs.arg
     * @see LuaValue.NIL
     */
    abstract fun arg1(): LuaValue

    /**
     * Evaluate any pending tail call and return result.
     * @return the evaluated tail call result
     */
    open fun eval(): Varargs = this

    // -----------------------------------------------------------------------
    // utilities to get specific arguments and type-check them.
    // -----------------------------------------------------------------------

    /** Gets the type of argument `i`
     * @param i the index of the argument to convert, 1 is the first argument
     * @return int value corresponding to one of the LuaValue integer type values
     * @see LuaValue.TNIL
     *
     * @see LuaValue.TBOOLEAN
     *
     * @see LuaValue.TNUMBER
     *
     * @see LuaValue.TSTRING
     *
     * @see LuaValue.TTABLE
     *
     * @see LuaValue.TFUNCTION
     *
     * @see LuaValue.TUSERDATA
     *
     * @see LuaValue.TTHREAD
     *
     */
    fun type(i: Int): Int = arg(i).type()

    /** Tests if argument i is nil.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument is nil or does not exist, false otherwise
     * @see LuaValue.TNIL
     *
     */
    fun isnil(i: Int): Boolean = arg(i).isnil()

    /** Tests if argument i is a function.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a function or closure, false otherwise
     * @see LuaValue.TFUNCTION
     *
     */
    fun isfunction(i: Int): Boolean = arg(i).isfunction()

    /** Tests if argument i is a number.
     * Since anywhere a number is required, a string can be used that
     * is a number, this will return true for both numbers and
     * strings that can be interpreted as numbers.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a number or
     * string that can be interpreted as a number, false otherwise
     * @see LuaValue.TNUMBER
     *
     * @see LuaValue.TSTRING
     *
     */
    fun isnumber(i: Int): Boolean = arg(i).isnumber()

    /** Tests if argument i is a string.
     * Since all lua numbers can be used where strings are used,
     * this will return true for both strings and numbers.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a string or number, false otherwise
     * @see LuaValue.TNUMBER
     *
     * @see LuaValue.TSTRING
     *
     */
    fun isstring(i: Int): Boolean = arg(i).isstring()

    /** Tests if argument i is a table.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a lua table, false otherwise
     * @see LuaValue.TTABLE
     *
     */
    fun istable(i: Int): Boolean = arg(i).istable()

    /** Tests if argument i is a thread.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a lua thread, false otherwise
     * @see LuaValue.TTHREAD
     *
     */
    fun isthread(i: Int): Boolean = arg(i).isthread()

    /** Tests if argument i is a userdata.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a userdata, false otherwise
     * @see LuaValue.TUSERDATA
     *
     */
    fun isuserdata(i: Int): Boolean = arg(i).isuserdata()

    /** Tests if a value exists at argument i.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists, false otherwise
     */
    fun isvalue(i: Int): Boolean = i > 0 && i <= narg()

    /** Return argument i as a boolean value, `defval` if nil, or throw a LuaError if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i is boolean true, false if it is false, or defval if not supplied or nil
     * @exception LuaError if the argument is not a lua boolean
     */
    fun optboolean(i: Int, defval: Boolean): Boolean = arg(i).optboolean(defval)

    /** Return argument i as a closure, `defval` if nil, or throw a LuaError if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaClosure if argument i is a closure, or defval if not supplied or nil
     * @exception LuaError if the argument is not a lua closure
     */
    fun optclosure(i: Int, defval: LuaClosure): LuaClosure? = arg(i).optclosure(defval)

    /** Return argument i as a double, `defval` if nil, or throw a LuaError if it cannot be converted to one.
     * @param i the index of the argument to test, 1 is the first argument
     * @return java double value if argument i is a number or string that converts to a number, or defval if not supplied or nil
     * @exception LuaError if the argument is not a number
     */
    fun optdouble(i: Int, defval: Double): Double = arg(i).optdouble(defval)

    /** Return argument i as a function, `defval` if nil, or throw a LuaError  if an incompatible type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue that can be called if argument i is lua function or closure, or defval if not supplied or nil
     * @exception LuaError if the argument is not a lua function or closure
     */
    fun optfunction(i: Int, defval: LuaFunction?): LuaFunction? = arg(i).optfunction(defval)

    /** Return argument i as a java int value, discarding any fractional part, `defval` if nil, or throw a LuaError  if not a number.
     * @param i the index of the argument to test, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number, or defval if not supplied or nil
     * @exception LuaError if the argument is not a number
     */
    fun optint(i: Int, defval: Int): Int = arg(i).optint(defval)

    /** Return argument i as a java int value, `defval` if nil, or throw a LuaError  if not a number or is not representable by a java int.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaInteger value that fits in a java int without rounding, or defval if not supplied or nil
     * @exception LuaError if the argument cannot be represented by a java int value
     */
    fun optinteger(i: Int, defval: LuaInteger): LuaInteger? = arg(i).optinteger(defval)

    /** Return argument i as a java long value, discarding any fractional part, `defval` if nil, or throw a LuaError  if not a number.
     * @param i the index of the argument to test, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number, or defval if not supplied or nil
     * @exception LuaError if the argument is not a number
     */
    fun optlong(i: Int, defval: Long): Long = arg(i).optlong(defval)

    /** Return argument i as a LuaNumber, `defval` if nil, or throw a LuaError  if not a number or string that can be converted to a number.
     * @param i the index of the argument to test, 1 is the first argument, or defval if not supplied or nil
     * @return LuaNumber if argument i is number or can be converted to a number
     * @exception LuaError if the argument is not a number
     */
    fun optnumber(i: Int, defval: LuaNumber): LuaNumber? = arg(i).optnumber(defval)

    /** Return argument i as a java String if a string or number, `defval` if nil, or throw a LuaError  if any other type
     * @param i the index of the argument to test, 1 is the first argument
     * @return String value if argument i is a string or number, or defval if not supplied or nil
     * @exception LuaError if the argument is not a string or number
     */
    fun optjstring(i: Int, defval: String?): String? = arg(i).optjstring(defval)

    /** Return argument i as a LuaString if a string or number, `defval` if nil, or throw a LuaError  if any other type
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaString value if argument i is a string or number, or defval if not supplied or nil
     * @exception LuaError if the argument is not a string or number
     */
    fun optstring(i: Int, defval: LuaString): LuaString? = arg(i).optstring(defval)

    /** Return argument i as a LuaTable if a lua table, `defval` if nil, or throw a LuaError  if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaTable value if a table, or defval if not supplied or nil
     * @exception LuaError if the argument is not a lua table
     */
    fun opttable(i: Int, defval: LuaTable): LuaTable? = arg(i).opttable(defval)

    /** Return argument i as a LuaThread if a lua thread, `defval` if nil, or throw a LuaError  if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaThread value if a thread, or defval if not supplied or nil
     * @exception LuaError if the argument is not a lua thread
     */
    fun optthread(i: Int, defval: LuaThread): LuaThread? = arg(i).optthread(defval)

    /** Return argument i as a java Object if a userdata, `defval` if nil, or throw a LuaError  if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return java Object value if argument i is a userdata, or defval if not supplied or nil
     * @exception LuaError if the argument is not a userdata
     */
    fun optuserdata(i: Int, defval: Any?): Any? = arg(i).optuserdata(defval)

    /** Return argument i as a java Object if it is a userdata whose instance Class c or a subclass,
     * `defval` if nil, or throw a LuaError  if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass, or defval if not supplied or nil
     * @exception LuaError if the argument is not a userdata or from whose instance c is not assignable
     */
    fun optuserdata(i: Int, c: KClass<*>, defval: Any): Any? = arg(i).optuserdata(c, defval)

    /** Return argument i as a LuaValue if it exists, or `defval`.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument exists, defval if not
     * @exception LuaError if the argument does not exist.
     */
    fun optvalue(i: Int, defval: LuaValue): LuaValue = if (i > 0 && i <= narg()) arg(i) else defval

    /** Return argument i as a boolean value, or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i is boolean true, false if it is false
     * @exception LuaError if the argument is not a lua boolean
     */
    fun checkboolean(i: Int): Boolean = arg(i).checkboolean()

    /** Return argument i as a closure, or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaClosure if argument i is a closure.
     * @exception LuaError if the argument is not a lua closure
     */
    fun checkclosure(i: Int): LuaClosure? = arg(i).checkclosure()

    /** Return argument i as a double, or throw an error if it cannot be converted to one.
     * @param i the index of the argument to test, 1 is the first argument
     * @return java double value if argument i is a number or string that converts to a number
     * @exception LuaError if the argument is not a number
     */
    fun checkdouble(i: Int): Double = arg(i).checknumber()!!.todouble()

    /** Return argument i as a function, or throw an error if an incompatible type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue that can be called if argument i is lua function or closure
     * @exception LuaError if the argument is not a lua function or closure
     */
    fun checkfunction(i: Int): LuaFunction? = arg(i).checkfunction()

    /** Return argument i as a java int value, discarding any fractional part, or throw an error if not a number.
     * @param i the index of the argument to test, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number
     * @exception LuaError if the argument is not a number
     */
    fun checkint(i: Int): Int = arg(i).checknumber()!!.toint()

    /** Return argument i as a java int value, or throw an error if not a number or is not representable by a java int.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaInteger value that fits in a java int without rounding
     * @exception LuaError if the argument cannot be represented by a java int value
     */
    fun checkinteger(i: Int): LuaInteger? = arg(i).checkinteger()

    /** Return argument i as a java long value, discarding any fractional part, or throw an error if not a number.
     * @param i the index of the argument to test, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number
     * @exception LuaError if the argument is not a number
     */
    fun checklong(i: Int): Long = arg(i).checknumber()!!.tolong()

    /** Return argument i as a LuaNumber, or throw an error if not a number or string that can be converted to a number.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaNumber if argument i is number or can be converted to a number
     * @exception LuaError if the argument is not a number
     */
    fun checknumber(i: Int): LuaNumber? = arg(i).checknumber()

    /** Return argument i as a java String if a string or number, or throw an error if any other type
     * @param i the index of the argument to test, 1 is the first argument
     * @return String value if argument i is a string or number
     * @exception LuaError if the argument is not a string or number
     */
    fun checkjstring(i: Int): String? = arg(i).checkjstring()

    /** Return argument i as a LuaString if a string or number, or throw an error if any other type
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaString value if argument i is a string or number
     * @exception LuaError if the argument is not a string or number
     */
    fun checkstring(i: Int): LuaString? = arg(i).checkstring()

    /** Return argument i as a LuaTable if a lua table, or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaTable value if a table
     * @exception LuaError if the argument is not a lua table
     */
    fun checktable(i: Int): LuaTable? = arg(i).checktable()

    /** Return argument i as a LuaThread if a lua thread, or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaThread value if a thread
     * @exception LuaError if the argument is not a lua thread
     */
    fun checkthread(i: Int): LuaThread? = arg(i).checkthread()

    /** Return argument i as a java Object if a userdata, or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @return java Object value if argument i is a userdata
     * @exception LuaError if the argument is not a userdata
     */
    fun checkuserdata(i: Int): Any? = arg(i).checkuserdata()

    /** Return argument i as a java Object if it is a userdata whose instance Class c or a subclass,
     * or throw an error if any other type.
     * @param i the index of the argument to test, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass
     * @exception LuaError if the argument is not a userdata or from whose instance c is not assignable
     */
    fun checkuserdata(i: Int, c: KClass<*>): Any? = arg(i).checkuserdata(c)

    /** Return argument i as a LuaValue if it exists, or throw an error.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument exists
     * @exception LuaError if the argument does not exist.
     */
    fun checkvalue(i: Int): LuaValue = if (i <= narg()) arg(i) else LuaValue.argerror(i, "value expected")

    /** Return argument i as a LuaValue if it is not nil, or throw an error if it is nil.
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument is not nil
     * @exception LuaError if the argument doesn't exist or evaluates to nil.
     */
    fun checknotnil(i: Int): LuaValue = arg(i).checknotnil()

    /** Performs test on argument i as a LuaValue when a user-supplied assertion passes, or throw an error.
     * Returns normally if the value of `test` is `true`, otherwise throws and argument error with
     * the supplied message, `msg`.
     * @param test user supplied assertion to test against
     * @param i the index to report in any error message
     * @param msg the error message to use when the test fails
     * @exception LuaError if the the value of `test` is `false`
     */
    fun argcheck(test: Boolean, i: Int, msg: String) { if (!test) LuaValue.argerror(i, msg) }

    /** Return true if there is no argument or nil at argument i.
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i contains either no argument or nil
     */
    fun isnoneornil(i: Int): Boolean = i > narg() || arg(i).isnil()

    /** Convert argument `i` to java boolean based on lua rules for boolean evaluation.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return `false` if argument i is nil or false, otherwise `true`
     */
    fun toboolean(i: Int): Boolean = arg(i).toboolean()

    /** Return argument i as a java byte value, discarding any fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return byte value with fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun tobyte(i: Int): Byte = arg(i).tobyte()

    /** Return argument i as a java char value, discarding any fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return char value with fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun tochar(i: Int): Char = arg(i).tochar()

    /** Return argument i as a java double value or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return double value if argument i is number, otherwise 0
     */
    fun todouble(i: Int): Double = arg(i).todouble()

    /** Return argument i as a java float value, discarding excess fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return float value with excess fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun tofloat(i: Int): Float = arg(i).tofloat()

    /** Return argument i as a java int value, discarding any fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun toint(i: Int): Int = arg(i).toint()

    /** Return argument i as a java long value, discarding any fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun tolong(i: Int): Long = arg(i).tolong()

    /** Return argument i as a java String based on the type of the argument.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return String value representing the type
     */
    fun tojstring(i: Int): String = arg(i).tojstring()

    /** Return argument i as a java short value, discarding any fractional part and truncating,
     * or 0 if not a number.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return short value with fraction discarded and truncated if necessary if argument i is number, otherwise 0
     */
    fun toshort(i: Int): Short = arg(i).toshort()

    /** Return argument i as a java Object if a userdata, or null.
     * @param i the index of the argument to convert, 1 is the first argument
     * @return java Object value if argument i is a userdata, otherwise null
     */
    fun touserdata(i: Int): Any? = arg(i).touserdata()

    /** Return argument i as a java Object if it is a userdata whose instance Class c or a subclass, or null.
     * @param i the index of the argument to convert, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass, otherwise null
     */
    fun touserdata(i: Int, c: KClass<*>): Any? = arg(i).touserdata(c)

    /** Convert the list of varargs values to a human readable java String.
     * @return String value in human readable form such as {1,2}.
     */
    open fun tojstring(): String {
        val sb = Buffer()
        sb.append("(")
        var i = 1
        val n = narg()
        while (i <= n) {
            if (i > 1) sb.append(",")
            sb.append(arg(i).tojstring())
            i++
        }
        sb.append(")")
        return sb.tojstring()
    }

    /** Convert the value or values to a java String using Varargs.tojstring()
     * @return String value in human readable form.
     * @see Varargs.tojstring
     */
    override fun toString(): String = tojstring()

    /**
     * Create a `Varargs` instance containing arguments starting at index `start`
     * @param start the index from which to include arguments, where 1 is the first argument.
     * @return Varargs containing argument { start, start+1,  ... , narg-start-1 }
     */
    abstract fun subargs(start: Int): Varargs

    /**
     * Implementation of Varargs for use in the Varargs.subargs() function.
     * @see Varargs.subargs
     */
    internal class SubVarargs(private val v: Varargs, private val start: Int, private val end: Int) : Varargs() {
        override fun arg(i: Int): LuaValue {
            var i = i
            i += start - 1
            return if (i >= start && i <= end) v.arg(i) else LuaValue.NIL
        }

        override fun arg1(): LuaValue = v.arg(start)

        override fun narg(): Int = end + 1 - start

        override fun subargs(start: Int): Varargs {
            if (start == 1) return this
            val newstart = this.start + start - 1
            return if (start > 0) {
                when {
                    newstart >= this.end -> LuaValue.NONE
                    newstart == this.end -> v.arg(this.end)
                    else -> if (newstart == this.end - 1) PairVarargs(v.arg(this.end - 1), v.arg(this.end)) else SubVarargs(v, newstart, this.end)
                }
            } else {
                SubVarargs(v, newstart, this.end)
            }
        }
    }

    /** Varargs implemenation backed by two values.
     *
     *
     * This is an internal class not intended to be used directly.
     * Instead use the corresponding static method on LuaValue.
     *
     * @see LuaValue.varargsOf
     */
    /** Construct a Varargs from an two LuaValue.
     *
     *
     * This is an internal class not intended to be used directly.
     * Instead use the corresponding static method on LuaValue.
     *
     * @see LuaValue.varargsOf
     */
    internal class PairVarargs(private val v1: LuaValue, private val v2: Varargs) : Varargs() {
        override fun arg(i: Int): LuaValue = if (i == 1) v1 else v2.arg(i - 1)
        override fun narg(): Int = 1 + v2.narg()
        override fun arg1(): LuaValue = v1

        override fun subargs(start: Int): Varargs = when (start) {
            1 -> this
            2 -> v2
            else -> if (start > 2) v2.subargs(start - 1) else LuaValue.argerror(1, "start must be > 0")
        }
    }

    /** Varargs implemenation backed by an array of LuaValues
     *
     *
     * This is an internal class not intended to be used directly.
     * Instead use the corresponding static methods on LuaValue.
     *
     * @see LuaValue.varargsOf
     * @see LuaValue.varargsOf
     */
    /** Construct a Varargs from an array of LuaValue.
     *
     *
     * This is an internal class not intended to be used directly.
     * Instead use the corresponding static methods on LuaValue.
     *
     * @see LuaValue.varargsOf
     * @see LuaValue.varargsOf
     */
    class ArrayVarargs(private val v: Array<LuaValue>, private val r: Varargs) : Varargs() {
        override fun arg(i: Int): LuaValue = if (i < 1) LuaValue.NIL else if (i <= v.size) v[i - 1] else r.arg(i - v.size)
        override fun narg(): Int = v.size + r.narg()
        override fun arg1(): LuaValue = if (v.size > 0) v[0] else r.arg1()

        override fun subargs(start: Int): Varargs = when {
            start <= 0 -> LuaValue.argerror(1, "start must be > 0")
            start == 1 -> this
            else -> if (start > v.size) r.subargs(start - v.size) else LuaValue.varargsOf(
                v,
                start - 1,
                v.size - (start - 1),
                r
            )
        }

        override fun copyto(dest: Array<LuaValue>, offset: Int, length: Int) {
            val n = min(v.size, length)
            arraycopy(v, 0, dest, offset, n)
            r.copyto(dest, offset + n, length - n)
        }
    }

    /** Varargs implemenation backed by an array of LuaValues
     *
     *
     * This is an internal class not intended to be used directly.
     * Instead use the corresponding static methods on LuaValue.
     *
     * @see LuaValue.varargsOf
     * @see LuaValue.varargsOf
     */
    internal class ArrayPartVarargs : Varargs {
        private val offset: Int
        private val v: Array<LuaValue>
        private val length: Int
        private val more: Varargs

        /** Construct a Varargs from an array of LuaValue.
         *
         *
         * This is an internal class not intended to be used directly.
         * Instead use the corresponding static methods on LuaValue.
         *
         * @see LuaValue.varargsOf
         */
        constructor(v: Array<LuaValue>, offset: Int, length: Int) {
            this.v = v
            this.offset = offset
            this.length = length
            this.more = LuaValue.NONE
        }

        /** Construct a Varargs from an array of LuaValue and additional arguments.
         *
         *
         * This is an internal class not intended to be used directly.
         * Instead use the corresponding static method on LuaValue.
         *
         * @see LuaValue.varargsOf
         */
        constructor(v: Array<LuaValue>, offset: Int, length: Int, more: Varargs) {
            this.v = v
            this.offset = offset
            this.length = length
            this.more = more
        }

        override fun arg(i: Int): LuaValue = if (i < 1) LuaValue.NIL else if (i <= length) v[offset + i - 1] else more.arg(i - length)
        override fun narg(): Int = length + more.narg()
        override fun arg1(): LuaValue = if (length > 0) v[offset] else more.arg1()

        override fun subargs(start: Int): Varargs = when {
            start <= 0 -> LuaValue.argerror(1, "start must be > 0")
            start == 1 -> this
            else -> if (start > length) more.subargs(start - length) else LuaValue.varargsOf(
                v,
                offset + start - 1,
                length - (start - 1),
                more
            )
        }

        override fun copyto(dest: Array<LuaValue>, offset: Int, length: Int) {
            val n = min(this.length, length)
            arraycopy(this.v, this.offset, dest, offset, n)
            more.copyto(dest, offset + n, length - n)
        }
    }

    /** Copy values in a varargs into a destination array.
     * Internal utility method not intended to be called directly from user code.
     * @return Varargs containing same values, but flattened.
     */
    open fun copyto(dest: Array<LuaValue>, offset: Int, length: Int) {
        for (i in 0 until length) dest[offset + i] = arg(i + 1)
    }

    /** Return Varargs that cannot be using a shared array for the storage, and is flattened.
     * Internal utility method not intended to be called directly from user code.
     * @return Varargs containing same values, but flattened and with a new array if needed.
     */
    fun dealias(): Varargs {
        val n = narg()
        return when (n) {
            0 -> LuaValue.NONE
            1 -> arg1()
            2 -> PairVarargs(arg1(), arg(2))
            else -> {
                val v = arrayOfNulls<LuaValue>(n) as Array<LuaValue>
                copyto(v, 0, n)
                ArrayVarargs(v, LuaValue.NONE)
            }
        }
    }
}
