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

import org.luaj.vm2.lib.MathLib
import kotlin.jvm.*

/**
 * Extension of [LuaNumber] which can hold a Java double as its value.
 *
 *
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions [LuaValue.valueOf] or [LuaValue.valueOf]
 * functions.  This ensures that values which can be represented as int
 * are wrapped in [LuaInteger] instead of [LuaDouble].
 *
 *
 * Almost all API's implemented in LuaDouble are defined and documented in [LuaValue].
 *
 *
 * However the constants [.NAN], [.POSINF], [.NEGINF],
 * [.JSTR_NAN], [.JSTR_POSINF], and [.JSTR_NEGINF] may be useful
 * when dealing with Nan or Infinite values.
 *
 *
 * LuaDouble also defines functions for handling the unique math rules of lua devision and modulo in
 *
 *  * [.ddiv]
 *  * [.ddiv_d]
 *  * [.dmod]
 *  * [.dmod_d]
 *
 *
 *
 * @see LuaValue
 *
 * @see LuaNumber
 *
 * @see LuaInteger
 *
 * @see LuaValue.valueOf
 * @see LuaValue.valueOf
 */
class LuaDouble
/** Don't allow ints to be boxed by DoubleValues   */
private constructor(
    /** The value being held by this instance.  */
    internal val v: Double
) : LuaNumber() {

    override fun hashCode(): Int = (v + 1).toRawBits().let { l -> (l shr 32).toInt() + l.toInt() }
    override fun islong(): Boolean = v == v.toLong().toDouble()
    override fun tobyte(): Byte = v.toLong().toByte()
    override fun tochar(): Char = v.toLong().toChar()
    override fun todouble(): Double = v
    override fun tofloat(): Float = v.toFloat()
    override fun toint(): Int = v.toLong().toInt()
    override fun tolong(): Long = v.toLong()
    override fun toshort(): Short = v.toLong().toShort()
    override fun optdouble(defval: Double): Double = v
    override fun optint(defval: Int): Int = v.toLong().toInt()
    override fun optinteger(defval: LuaInteger?): LuaInteger? = LuaInteger.valueOf(v.toLong().toInt())
    override fun optlong(defval: Long): Long = v.toLong()
    override fun checkinteger(): LuaInteger? = LuaInteger.valueOf(v.toLong().toInt())

    // unary operators
    override fun neg(): LuaValue = valueOf(-v)

    // object equality, used for key comparison
    override fun equals(o: Any?): Boolean = if (o is LuaDouble) o.v == v else false

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue): LuaValue = if (`val`.raweq(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun eq_b(`val`: LuaValue): Boolean = `val`.raweq(v)

    // equality w/o metatable processing
    override fun raweq(`val`: LuaValue): Boolean = `val`.raweq(v)
    override fun raweq(`val`: Double): Boolean = v == `val`
    override fun raweq(`val`: Int): Boolean = v == `val`.toDouble()

    // basic binary arithmetic
    override fun add(rhs: LuaValue): LuaValue = rhs.add(v)
    override fun add(lhs: Double): LuaValue = LuaDouble.valueOf(lhs + v)
    override fun sub(rhs: LuaValue): LuaValue = rhs.subFrom(v)
    override fun sub(rhs: Double): LuaValue = LuaDouble.valueOf(v - rhs)
    override fun sub(rhs: Int): LuaValue = LuaDouble.valueOf(v - rhs)
    override fun subFrom(lhs: Double): LuaValue = LuaDouble.valueOf(lhs - v)
    override fun mul(rhs: LuaValue): LuaValue = rhs.mul(v)
    override fun mul(lhs: Double): LuaValue = LuaDouble.valueOf(lhs * v)
    override fun mul(lhs: Int): LuaValue = LuaDouble.valueOf(lhs * v)
    override fun pow(rhs: LuaValue): LuaValue = rhs.powWith(v)
    override fun pow(rhs: Double): LuaValue = MathLib.dpow(v, rhs)
    override fun pow(rhs: Int): LuaValue = MathLib.dpow(v, rhs.toDouble())
    override fun powWith(lhs: Double): LuaValue = MathLib.dpow(lhs, v)
    override fun powWith(lhs: Int): LuaValue = MathLib.dpow(lhs.toDouble(), v)
    override fun div(rhs: LuaValue): LuaValue = rhs.divInto(v)
    override fun div(rhs: Double): LuaValue = LuaDouble.ddiv(v, rhs)
    override fun div(rhs: Int): LuaValue = LuaDouble.ddiv(v, rhs.toDouble())
    override fun divInto(lhs: Double): LuaValue = LuaDouble.ddiv(lhs, v)
    override fun mod(rhs: LuaValue): LuaValue = rhs.modFrom(v)
    override fun mod(rhs: Double): LuaValue = LuaDouble.dmod(v, rhs)
    override fun mod(rhs: Int): LuaValue = LuaDouble.dmod(v, rhs.toDouble())
    override fun modFrom(lhs: Double): LuaValue = LuaDouble.dmod(lhs, v)

    // relational operators
    override fun lt(rhs: LuaValue): LuaValue = if (rhs.gt_b(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lt(rhs: Double): LuaValue = if (v < rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lt(rhs: Int): LuaValue = if (v < rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lt_b(rhs: LuaValue): Boolean = rhs.gt_b(v)
    override fun lt_b(rhs: Int): Boolean = v < rhs
    override fun lt_b(rhs: Double): Boolean = v < rhs
    override fun lteq(rhs: LuaValue): LuaValue = if (rhs.gteq_b(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lteq(rhs: Double): LuaValue = if (v <= rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lteq(rhs: Int): LuaValue = if (v <= rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lteq_b(rhs: LuaValue): Boolean = rhs.gteq_b(v)
    override fun lteq_b(rhs: Int): Boolean = v <= rhs
    override fun lteq_b(rhs: Double): Boolean = v <= rhs
    override fun gt(rhs: LuaValue): LuaValue = if (rhs.lt_b(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gt(rhs: Double): LuaValue = if (v > rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gt(rhs: Int): LuaValue = if (v > rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gt_b(rhs: LuaValue): Boolean = rhs.lt_b(v)
    override fun gt_b(rhs: Int): Boolean = v > rhs
    override fun gt_b(rhs: Double): Boolean = v > rhs
    override fun gteq(rhs: LuaValue): LuaValue = if (rhs.lteq_b(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gteq(rhs: Double): LuaValue = if (v >= rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gteq(rhs: Int): LuaValue = if (v >= rhs) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gteq_b(rhs: LuaValue): Boolean = rhs.lteq_b(v)
    override fun gteq_b(rhs: Int): Boolean = v >= rhs
    override fun gteq_b(rhs: Double): Boolean = v >= rhs

    // string comparison
    override fun strcmp(rhs: LuaString): Int = typerror("attempt to compare number with string")

    override fun tojstring(): String {
        val l = v.toLong()
        return when {
            l.toDouble() == v -> l.toString()
            v.isNaN() -> JSTR_NAN
            else -> if ((v).isInfinite()) if (v < 0) JSTR_NEGINF else JSTR_POSINF else v.toFloat().toString()
        }
    }

    override fun strvalue(): LuaString? = LuaString.valueOf(tojstring())
    override fun optstring(defval: LuaString?): LuaString? = LuaString.valueOf(tojstring())
    override fun tostring(): LuaValue = LuaString.valueOf(tojstring())
    override fun optjstring(defval: String?): String? = tojstring()
    override fun optnumber(defval: LuaNumber?): LuaNumber? = this
    override fun isnumber(): Boolean = true
    override fun isstring(): Boolean = true
    override fun tonumber(): LuaValue = this
    override fun checkint(): Int = v.toLong().toInt()
    override fun checklong(): Long = v.toLong()
    override fun checknumber(): LuaNumber? = this
    override fun checkdouble(): Double = v
    override fun checkjstring(): String? = tojstring()
    override fun checkstring(): LuaString = LuaString.valueOf(tojstring())
    override fun isvalidkey(): Boolean = !(v.isNaN())

    companion object {

        /** Constant LuaDouble representing NaN (not a number)  */
        @kotlin.jvm.JvmField val NAN = LuaDouble(Double.NaN)

        /** Constant LuaDouble representing positive infinity  */
        @kotlin.jvm.JvmField val POSINF = LuaDouble(Double.POSITIVE_INFINITY)

        /** Constant LuaDouble representing negative infinity  */
        @kotlin.jvm.JvmField val NEGINF = LuaDouble(Double.NEGATIVE_INFINITY)

        /** Constant String representation for NaN (not a number), "nan"  */
        @kotlin.jvm.JvmField val JSTR_NAN = "nan"

        /** Constant String representation for positive infinity, "inf"  */
        @kotlin.jvm.JvmField val JSTR_POSINF = "inf"

        /** Constant String representation for negative infinity, "-inf"  */
        @kotlin.jvm.JvmField val JSTR_NEGINF = "-inf"

        @JvmName("valueOf2")
         fun valueOf(d: Double): LuaNumber =
            d.toInt().let { id -> if (d == id.toDouble()) LuaInteger.valueOf(id) else LuaDouble(d) }


        /** Divide two double numbers according to lua math, and return a [LuaValue] result.
         * @param lhs Left-hand-side of the division.
         * @param rhs Right-hand-side of the division.
         * @return [LuaValue] for the result of the division,
         * taking into account positive and negiative infinity, and Nan
         * @see .ddiv_d
         */
         fun ddiv(lhs: Double, rhs: Double): LuaValue =
            if (rhs != 0.0) valueOf(lhs / rhs) else if (lhs > 0) POSINF else if (lhs == 0.0) NAN else NEGINF

        /** Divide two double numbers according to lua math, and return a double result.
         * @param lhs Left-hand-side of the division.
         * @param rhs Right-hand-side of the division.
         * @return Value of the division, taking into account positive and negative infinity, and Nan
         * @see .ddiv
         */
         fun ddiv_d(lhs: Double, rhs: Double): Double = when {
            rhs != 0.0 -> lhs / rhs
            lhs > 0 -> Double.POSITIVE_INFINITY
            lhs == 0.0 -> Double.NaN
            else -> Double.NEGATIVE_INFINITY
        }

        /** Take modulo double numbers according to lua math, and return a [LuaValue] result.
         * @param lhs Left-hand-side of the modulo.
         * @param rhs Right-hand-side of the modulo.
         * @return [LuaValue] for the result of the modulo,
         * using lua's rules for modulo
         * @see .dmod_d
         */
         fun dmod(lhs: Double, rhs: Double): LuaValue =
            if (rhs != 0.0) valueOf(lhs - rhs * kotlin.math.floor(lhs / rhs)) else NAN

        /** Take modulo for double numbers according to lua math, and return a double result.
         * @param lhs Left-hand-side of the modulo.
         * @param rhs Right-hand-side of the modulo.
         * @return double value for the result of the modulo,
         * using lua's rules for modulo
         * @see .dmod
         */
         fun dmod_d(lhs: Double, rhs: Double): Double = when {
            rhs != 0.0 -> lhs - rhs * kotlin.math.floor(lhs / rhs)
            else -> Double.NaN
        }
    }
}
