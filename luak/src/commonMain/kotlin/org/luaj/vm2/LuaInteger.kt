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

import org.luaj.vm2.lib.MathLib
import kotlin.jvm.*

/**
 * Extension of [LuaNumber] which can hold a Java int as its value.
 *
 *
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions [LuaValue.valueOf] or [LuaValue.valueOf]
 * functions.  This ensures that policies regarding pooling of instances are
 * encapsulated.
 *
 *
 * There are no API's specific to LuaInteger that are useful beyond what is already
 * exposed in [LuaValue].
 *
 * @see LuaValue
 *
 * @see LuaNumber
 *
 * @see LuaDouble
 *
 * @see LuaValue.valueOf
 * @see LuaValue.valueOf
 */
class LuaInteger
/**
 * Package protected constructor.
 * @see LuaValue.valueOf
 */
internal constructor(
    /** The value being held by this instance.  */
    val v: Int
) : LuaNumber() {

    override fun isint(): Boolean = true
    override fun isinttype(): Boolean = true
    override fun islong(): Boolean = true
    override fun tobyte(): Byte = v.toByte()
    override fun tochar(): Char = v.toChar()
    override fun todouble(): Double = v.toDouble()
    override fun tofloat(): Float = v.toFloat()
    override fun toint(): Int = v
    override fun tolong(): Long = v.toLong()
    override fun toshort(): Short = v.toShort()
    override fun optdouble(defval: Double): Double = v.toDouble()
    override fun optint(defval: Int): Int = v
    override fun optinteger(defval: LuaInteger?): LuaInteger? = this
    override fun optlong(defval: Long): Long = v.toLong()
    override fun tojstring(): String = v.toString(10)
    override fun strvalue(): LuaString? = LuaString.valueOf(v.toString(10))
    override fun optstring(defval: LuaString?): LuaString? = LuaString.valueOf(v.toString(10))
    override fun tostring(): LuaValue = LuaString.valueOf(v.toString(10))
    override fun optjstring(defval: String?): String? = v.toString(10)
    override fun checkinteger(): LuaInteger? = this
    override fun isstring(): Boolean = true
    override fun hashCode(): Int = v

    // unary operators
    override fun neg(): LuaValue = valueOf(-v.toLong())

    // object equality, used for key comparison
    override fun equals(o: Any?): Boolean = if (o is LuaInteger) o.v == v else false

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue): LuaValue = if (`val`.raweq(v)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun eq_b(`val`: LuaValue): Boolean = `val`.raweq(v)

    // equality w/o metatable processing
    override fun raweq(`val`: LuaValue): Boolean = `val`.raweq(v)
    override fun raweq(`val`: Double): Boolean = v.toDouble() == `val`
    override fun raweq(`val`: Int): Boolean = v == `val`

    // arithmetic operators
    override fun add(rhs: LuaValue): LuaValue = rhs.add(v)
    override fun add(lhs: Double): LuaValue = LuaDouble.valueOf(lhs + v)
    override fun add(lhs: Int): LuaValue = LuaInteger.valueOf(lhs + v.toLong())
    override fun sub(rhs: LuaValue): LuaValue = rhs.subFrom(v)
    override fun sub(rhs: Double): LuaValue = LuaDouble.valueOf(v - rhs)
    override fun sub(rhs: Int): LuaValue = LuaDouble.valueOf((v - rhs).toDouble())
    override fun subFrom(lhs: Double): LuaValue = LuaDouble.valueOf(lhs - v)
    override fun subFrom(lhs: Int): LuaValue = LuaInteger.valueOf(lhs - v.toLong())
    override fun mul(rhs: LuaValue): LuaValue = rhs.mul(v)
    override fun mul(lhs: Double): LuaValue = LuaDouble.valueOf(lhs * v)
    override fun mul(lhs: Int): LuaValue = LuaInteger.valueOf(lhs * v.toLong())
    override fun pow(rhs: LuaValue): LuaValue = rhs.powWith(v)
    override fun pow(rhs: Double): LuaValue = MathLib.dpow(v.toDouble(), rhs)
    override fun pow(rhs: Int): LuaValue = MathLib.dpow(v.toDouble(), rhs.toDouble())
    override fun powWith(lhs: Double): LuaValue = MathLib.dpow(lhs, v.toDouble())
    override fun powWith(lhs: Int): LuaValue = MathLib.dpow(lhs.toDouble(), v.toDouble())
    override fun div(rhs: LuaValue): LuaValue = rhs.divInto(v.toDouble())
    override fun div(rhs: Double): LuaValue = LuaDouble.ddiv(v.toDouble(), rhs)
    override fun div(rhs: Int): LuaValue = LuaDouble.ddiv(v.toDouble(), rhs.toDouble())
    override fun divInto(lhs: Double): LuaValue = LuaDouble.ddiv(lhs, v.toDouble())
    override fun mod(rhs: LuaValue): LuaValue = rhs.modFrom(v.toDouble())
    override fun mod(rhs: Double): LuaValue = LuaDouble.dmod(v.toDouble(), rhs)
    override fun mod(rhs: Int): LuaValue = LuaDouble.dmod(v.toDouble(), rhs.toDouble())
    override fun modFrom(lhs: Double): LuaValue = LuaDouble.dmod(lhs, v.toDouble())

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
    override fun checkint(): Int = v
    override fun checklong(): Long = v.toLong()
    override fun checkdouble(): Double = v.toDouble()
    override fun checkjstring(): String? = v.toString()
    override fun checkstring(): LuaString = LuaValue.valueOf(v.toString())

    companion object {
        private val intValues = Array(512) { LuaInteger(it - 256) }

        @JvmName("valueOf2")

        fun valueOf(i: Int): LuaInteger = if (i <= 255 && i >= -256) intValues[i + 256]!! else LuaInteger(i)

        // TODO consider moving this to LuaValue
        /** Return a LuaNumber that represents the value provided
         * @param l long value to represent.
         * @return LuaNumber that is eithe LuaInteger or LuaDouble representing l
         * @see LuaValue.valueOf
         * @see LuaValue.valueOf
         */

        fun valueOf(l: Long): LuaNumber =
            l.toInt().let { i -> if (l == i.toLong()) if (i <= 255 && i >= -256) intValues[i + 256] else LuaInteger(i) else LuaDouble.valueOf(l.toDouble()) }


        fun hashCode(x: Int): Int = x
    }

}
