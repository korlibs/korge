/* Copyright (c) 2006-2011 Skype Limited. 
   Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Ported to Java by Logan Stromberg

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of Internet Society, IETF or IETF Trust, nor the
   names of specific contributors, may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soywiz.korau.format.org.concentus

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.Inlines.CapToUInt32
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.lang.*

internal object Inlines {

    private val sqrt_C = shortArrayOf(23175, 11561, -3011, 1699, -664)

    private val log2_C0 = (-6801 + (1 shl 3)).toShort()

    fun OpusAssert(condition: Boolean) {
        if (!condition) {
            throw AssertionError()
        }
        //Debug.Assert(condition);
    }

    fun OpusAssert(condition: Boolean, message: String) {
        if (!condition) {
            throw AssertionError(message)
        }
        //Debug.Assert(condition, message);
    }

    fun CapToUInt32(`val`: Long): Long {
        return 0xFFFFFFFFL and `val`.toInt().toLong()
    }

    fun CapToUInt32(`val`: Int): Long {
        return `val`.toLong()
    }

    // CELT-SPECIFIC INLINES
    //        /** Multiply a 16-bit signed value by a 16-bit unsigned value. The result is a 32-bit signed value */
    //#define MULT16_16SU(a,b) ((opus_val32)(opus_val16)(a)*(opus_val32)(opus_uint16)(b))
    fun MULT16_16SU(a: Int, b: Int): Int {
        return a.toShort().toInt() * (b and 0xFFFF)
    }

    //        /** 16x32 multiplication, followed by a 16-bit shift right. Results fits in 32 bits */
    //#define MULT16_32_Q16(a,b) ADD32(MULT16_16((a),SHR((b),16)), SHR(MULT16_16SU((a),((b)&0x0000ffff)),16))
    fun MULT16_32_Q16(a: Short, b: Int): Int {
        return ADD32(MULT16_16(a.toInt(), SHR(b, 16)), SHR(MULT16_16SU(a.toInt(), b and 0x0000ffff), 16))
    }

    fun MULT16_32_Q16(a: Int, b: Int): Int {
        return ADD32(MULT16_16(a, SHR(b, 16)), SHR(MULT16_16SU(a, b and 0x0000ffff), 16))
    }

    //        /** 16x32 multiplication, followed by a 16-bit shift right (round-to-nearest). Results fits in 32 bits */
    //#define MULT16_32_P16(a,b) ADD32(MULT16_16((a),SHR((b),16)), PSHR(MULT16_16SU((a),((b)&0x0000ffff)),16))
    fun MULT16_32_P16(a: Short, b: Int): Int {
        return ADD32(MULT16_16(a.toInt(), SHR(b, 16)), PSHR(MULT16_16SU(a.toInt(), b and 0x0000ffff), 16))
    }

    fun MULT16_32_P16(a: Int, b: Int): Int {
        return ADD32(MULT16_16(a, SHR(b, 16)), PSHR(MULT16_16SU(a, b and 0x0000ffff), 16))
    }

    //        /** 16x32 multiplication, followed by a 15-bit shift right. Results fits in 32 bits */
    fun MULT16_32_Q15(a: Short, b: Int): Int {
        return (a * (b shr 16) shl 1) + (a * (b and 0xFFFF) shr 15)
        //return ADD32(SHL(MULT16_16((a), SHR((b), 16)), 1), SHR(MULT16_16SU((a), (ushort)((b) & 0x0000ffff)), 15));
    }

    fun MULT16_32_Q15(a: Int, b: Int): Int {
        return (a * (b shr 16) shl 1) + (a * (b and 0xFFFF) shr 15)
        //return ADD32(SHL(MULT16_16((a), SHR((b), 16)), 1), SHR(MULT16_16SU((a), (uint)((b) & 0x0000ffff)), 15));
    }

    //        /** 32x32 multiplication, followed by a 31-bit shift right. Results fits in 32 bits */
    //#define MULT32_32_Q31(a,b) ADD32(ADD32(SHL(MULT16_16(SHR((a),16),SHR((b),16)),1), SHR(MULT16_16SU(SHR((a),16),((b)&0x0000ffff)),15)), SHR(MULT16_16SU(SHR((b),16),((a)&0x0000ffff)),15))
    fun MULT32_32_Q31(a: Int, b: Int): Int {
        return ADD32(
            ADD32(
                SHL(MULT16_16(SHR(a, 16), SHR(b, 16)), 1),
                SHR(MULT16_16SU(SHR(a, 16), b and 0x0000ffff), 15)
            ), SHR(MULT16_16SU(SHR(b, 16), a and 0x0000ffff), 15)
        )
    }

    // "Compile-time" (not really) conversion of float constant to 16-bit value
    fun QCONST16(x: Float, bits: Int): Short = (0.5 + x * (1 shl bits)).toShort()

    // "Compile-time" (not really) conversion of float constant to 32-bit value
    fun QCONST32(x: Float, bits: Int): Int = (0.5 + x * (1 shl bits)).toInt()

    //        /** Negate a 16-bit value */
    fun NEG16(x: Short): Short = (0 - x).toShort()

    fun NEG16(x: Int): Int = 0 - x

    //        /** Negate a 32-bit value */
    fun NEG32(x: Int): Int = 0 - x

    //        /** Change a 32-bit value into a 16-bit value. The value is assumed to fit in 16-bit, otherwise the result is undefined */
    fun EXTRACT16(x: Int): Short = x.toShort()

    //        /** Change a 16-bit value into a 32-bit value */
    fun EXTEND32(x: Short): Int = x.toInt()

    fun EXTEND32(x: Int): Int = x

    //        /** Arithmetic shift-right of a 16-bit value */
    fun SHR16(a: Short, shift: Int): Short = (a shr shift).toShort()

    fun SHR16(a: Int, shift: Int): Int = a shr shift

    //        /** Arithmetic shift-left of a 16-bit value */
    fun SHL16(a: Short, shift: Int): Short = (a and 0xFFFF shl shift).toShort()

    fun SHL16(a: Int, shift: Int): Int = -0x1 and ((a.toLong() shl shift).toInt())

    //        /** Arithmetic shift-right of a 32-bit value */
    fun SHR32(a: Int, shift: Int): Int {
        return a shr shift
    }

    //        /** Arithmetic shift-left of a 32-bit value */
    fun SHL32(a: Int, shift: Int): Int = -0x1 and ((a.toLong() shl shift).toInt())

    //        /** 32-bit arithmetic shift right with rounding-to-nearest instead of rounding down */
    fun PSHR32(a: Int, shift: Int): Int = SHR32(a + (EXTEND32(1) shl shift shr 1), shift)

    fun PSHR16(a: Short, shift: Int): Short = SHR16((a + (1 shl shift shr 1)).toShort(), shift)
    fun PSHR16(a: Int, shift: Int): Int = SHR32(a + (1 shl shift shr 1), shift)

    //        /** 32-bit arithmetic shift right where the argument can be negative */
    fun VSHR32(a: Int, shift: Int): Int = if (shift > 0) SHR32(a, shift) else SHL32(a, -shift)

    //        /** "RAW" macros, should not be used outside of this header file */
    private fun SHR(a: Int, shift: Int): Int = a shr shift
    private fun SHL(a: Int, shift: Int): Int = SHL32(a, shift)
    private fun SHR(a: Short, shift: Int): Int = a shr shift

    private fun SHL(a: Short, shift: Int): Int {
        return SHL32(a.toInt(), shift)
    }

    private fun PSHR(a: Int, shift: Int): Int {
        return SHR(a + (EXTEND32(1) shl shift shr 1), shift)
    }

    fun SATURATE(x: Int, a: Int): Int {
        return if (x > a) a else if (x < -a) -a else x
    }

    fun SATURATE16(x: Int): Short {
        return EXTRACT16(if (x > 32767) 32767 else if (x < -32768) -32768 else x)
    }

    //        /** Shift by a and round-to-neareast 32-bit value. Result is a 16-bit value */
    fun ROUND16(x: Short, a: Short): Short {
        return EXTRACT16(PSHR32(x.toInt(), a.toInt()))
    }

    fun ROUND16(x: Int, a: Int): Int {
        return PSHR32(x, a)
    }

    fun PDIV32(a: Int, b: Int): Int {
        return a / b
    }

    //        /** Divide by two */
    // fixme: can this be optimized?
    fun HALF16(x: Short): Short {
        return SHR16(x, 1)
    }

    fun HALF16(x: Int): Int {
        return SHR32(x, 1)
    }

    fun HALF32(x: Int): Int {
        return SHR32(x, 1)
    }

    //        /** Add two 16-bit values */
    fun ADD16(a: Short, b: Short): Short {
        return (a + b).toShort()
    }

    fun ADD16(a: Int, b: Int): Int {
        return a + b
    }

    //        /** Subtract two 16-bit values */
    fun SUB16(a: Short, b: Short): Short {
        return (a - b).toShort()
    }

    fun SUB16(a: Int, b: Int): Int {
        return a - b
    }

    //        /** Add two 32-bit values */
    fun ADD32(a: Int, b: Int): Int {
        return a + b
    }

    //        /** Subtract two 32-bit values */
    fun SUB32(a: Int, b: Int): Int {
        return a - b
    }

    //        /** 16x16 multiplication where the result fits in 16 bits */
    //#define MULT16_16_16(a,b)     ((((opus_val16)(a))*((opus_val16)(b))))
    fun MULT16_16_16(a: Short, b: Short): Short {
        return (a * b).toShort()
    }

    fun MULT16_16_16(a: Int, b: Int): Int {
        return a * b
    }

    //        /* (opus_val32)(opus_val16) gives TI compiler a hint that it's 16x16->32 multiply */
    //        /** 16x16 multiplication where the result fits in 32 bits */
    //#define MULT16_16(a,b)     (((opus_val32)(opus_val16)(a))*((opus_val32)(opus_val16)(b)))
    fun MULT16_16(a: Int, b: Int): Int {
        return a * b
    }

    fun MULT16_16(a: Short, b: Short): Int {
        return a * b
    }

    //        /** 16x16 multiply-add where the result fits in 32 bits */
    //#define MAC16_16(c,a,b) (ADD32((c),MULT16_16((a),(b))))
    fun MAC16_16(c: Short, a: Short, b: Short): Int {
        return c + a * b
    }

    fun MAC16_16(c: Int, a: Short, b: Short): Int {
        return c + a * b
    }

    fun MAC16_16(c: Int, a: Int, b: Int): Int {
        return c + a * b
    }

    //        /** 16x32 multiply, followed by a 15-bit shift right and 32-bit add.
    //            b must fit in 31 bits.
    //            Result fits in 32 bits. */
    //#define MAC16_32_Q15(c,a,b) ADD32((c),ADD32(MULT16_16((a),SHR((b),15)), SHR(MULT16_16((a),((b)&0x00007fff)),15)))
    fun MAC16_32_Q15(c: Int, a: Short, b: Short): Int {
        return ADD32(c, ADD32(MULT16_16(a.toInt(), SHR(b, 15)), SHR(MULT16_16(a.toInt(), b and 0x00007fff), 15)))
    }

    fun MAC16_32_Q15(c: Int, a: Int, b: Int): Int {
        return ADD32(c, ADD32(MULT16_16(a, SHR(b, 15)), SHR(MULT16_16(a, b and 0x00007fff), 15)))
    }

    //        /** 16x32 multiplication, followed by a 16-bit shift right and 32-bit add.
    //            Results fits in 32 bits */
    //#define MAC16_32_Q16(c,a,b) ADD32((c),ADD32(MULT16_16((a),SHR((b),16)), SHR(MULT16_16SU((a),((b)&0x0000ffff)),16)))
    fun MAC16_32_Q16(c: Int, a: Short, b: Short): Int {
        return ADD32(c, ADD32(MULT16_16(a.toInt(), SHR(b, 16)), SHR(MULT16_16SU(a.toInt(), b and 0x0000ffff), 16)))
    }

    fun MAC16_32_Q16(c: Int, a: Int, b: Int): Int {
        return ADD32(c, ADD32(MULT16_16(a, SHR(b, 16)), SHR(MULT16_16SU(a, b and 0x0000ffff), 16)))
    }

    //#define MULT16_16_Q11_32(a,b) (SHR(MULT16_16((a),(b)),11))
    fun MULT16_16_Q11_32(a: Short, b: Short): Int {
        return SHR(MULT16_16(a, b), 11)
    }

    fun MULT16_16_Q11_32(a: Int, b: Int): Int {
        return SHR(MULT16_16(a, b), 11)
    }

    //#define MULT16_16_Q11(a,b) (SHR(MULT16_16((a),(b)),11))
    fun MULT16_16_Q11(a: Short, b: Short): Short {
        return SHR(MULT16_16(a, b), 11).toShort()
    }

    fun MULT16_16_Q11(a: Int, b: Int): Int {
        return SHR(MULT16_16(a, b), 11)
    }

    //#define MULT16_16_Q13(a,b) (SHR(MULT16_16((a),(b)),13))
    fun MULT16_16_Q13(a: Short, b: Short): Short {
        return SHR(MULT16_16(a, b), 13).toShort()
    }

    fun MULT16_16_Q13(a: Int, b: Int): Int {
        return SHR(MULT16_16(a, b), 13)
    }

    //#define MULT16_16_Q14(a,b) (SHR(MULT16_16((a),(b)),14))
    fun MULT16_16_Q14(a: Short, b: Short): Short {
        return SHR(MULT16_16(a, b), 14).toShort()
    }

    fun MULT16_16_Q14(a: Int, b: Int): Int {
        return SHR(MULT16_16(a, b), 14)
    }

    //#define MULT16_16_Q15(a,b) (SHR(MULT16_16((a),(b)),15))
    fun MULT16_16_Q15(a: Short, b: Short): Short {
        return SHR(MULT16_16(a, b), 15).toShort()
    }

    fun MULT16_16_Q15(a: Int, b: Int): Int {
        return SHR(MULT16_16(a, b), 15)
    }

    //#define MULT16_16_P13(a,b) (SHR(ADD32(4096,MULT16_16((a),(b))),13))
    fun MULT16_16_P13(a: Short, b: Short): Short {
        return SHR(ADD32(4096, MULT16_16(a, b)), 13).toShort()
    }

    fun MULT16_16_P13(a: Int, b: Int): Int {
        return SHR(ADD32(4096, MULT16_16(a, b)), 13)
    }

    //#define MULT16_16_P14(a,b) (SHR(ADD32(8192,MULT16_16((a),(b))),14))
    fun MULT16_16_P14(a: Short, b: Short): Short {
        return SHR(ADD32(8192, MULT16_16(a, b)), 14).toShort()
    }

    fun MULT16_16_P14(a: Int, b: Int): Int {
        return SHR(ADD32(8192, MULT16_16(a, b)), 14)
    }

    //#define MULT16_16_P15(a,b) (SHR(ADD32(16384,MULT16_16((a),(b))),15))
    fun MULT16_16_P15(a: Short, b: Short): Short {
        return SHR(ADD32(16384, MULT16_16(a, b)), 15).toShort()
    }

    fun MULT16_16_P15(a: Int, b: Int): Int {
        return SHR(ADD32(16384, MULT16_16(a, b)), 15)
    }

    //        /** Divide a 32-bit value by a 16-bit value. Result fits in 16 bits */
    //#define DIV32_16(a,b) ((opus_val16)(((opus_val32)(a))/((opus_val16)(b))))
    fun DIV32_16(a: Int, b: Short): Short {
        return (a / b).toShort()
    }

    fun DIV32_16(a: Int, b: Int): Int {
        return a / b
    }

    //        /** Divide a 32-bit value by a 32-bit value. Result fits in 32 bits */
    //#define DIV32(a,b) (((opus_val32)(a))/((opus_val32)(b)))
    fun DIV32(a: Int, b: Int): Int {
        return a / b
    }

    // identical to silk_SAT16 - saturate operation
    fun SAT16(x: Int): Short {
        return (if (x > 32767) 32767 else if (x < -32768) -32768 else x.toShort()).toShort()
    }

    fun SIG2WORD16(x: Int): Short {
        var x = x
        x = PSHR32(x, 12)
        x = MAX32(x, -32768)
        x = MIN32(x, 32767)
        return EXTRACT16(x)
    }

    fun MIN(a: Short, b: Short): Short {
        return if (a < b) a else b
    }

    fun MAX(a: Short, b: Short): Short {
        return if (a > b) a else b
    }

    fun MIN16(a: Short, b: Short): Short {
        return if (a < b) a else b
    }

    fun MAX16(a: Short, b: Short): Short {
        return if (a > b) a else b
    }

    fun MIN16(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun MAX16(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun MIN16(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    fun MAX16(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    fun MIN(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun MAX(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun IMIN(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun IMIN(a: Long, b: Long): Long {
        return if (a < b) a else b
    }

    fun IMAX(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun MIN32(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun MAX32(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun MIN32(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    fun MAX32(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    fun ABS16(x: Int): Int {
        return if (x < 0) -x else x
    }

    fun ABS16(x: Float): Float {
        return if (x < 0) -x else x
    }

    fun ABS16(x: Short): Short {
        return (if (x < 0) (-x).toShort() else x)
    }

    fun ABS32(x: Int): Int {
        return if (x < 0) -x else x
    }

    fun celt_udiv(n: Int, d: Int): Int {
        Inlines.OpusAssert(d > 0)
        return n / d
    }

    fun celt_sudiv(n: Int, d: Int): Int {
        Inlines.OpusAssert(d > 0)
        return n / d
    }

    //#define celt_div(a,b) MULT32_32_Q31((opus_val32)(a),celt_rcp(b))
    fun celt_div(a: Int, b: Int): Int {
        return MULT32_32_Q31(a, celt_rcp(b))
    }

    /**
     * Integer log in base2. Undefined for zero and negative numbers
     */
    fun celt_ilog2(x: Int): Int {
        Inlines.OpusAssert(x > 0, "celt_ilog2() only defined for strictly positive numbers")
        return EC_ILOG(x.toLong()) - 1
    }

    /**
     * Integer log in base2. Defined for zero, but not for negative numbers
     */
    fun celt_zlog2(x: Int): Int {
        return if (x <= 0) 0 else celt_ilog2(x)
    }

    fun celt_maxabs16(x: IntArray, x_ptr: Int, len: Int): Int {
        var i: Int
        var maxval = 0
        var minval = 0
        i = x_ptr
        while (i < len + x_ptr) {
            maxval = MAX32(maxval, x[i])
            minval = MIN32(minval, x[i])
            i++
        }
        return MAX32(EXTEND32(maxval), -EXTEND32(minval))
    }

    fun celt_maxabs32(x: IntArray, x_ptr: Int, len: Int): Int {
        var i: Int
        var maxval = 0
        var minval = 0
        i = x_ptr
        while (i < x_ptr + len) {
            maxval = MAX32(maxval, x[i])
            minval = MIN32(minval, x[i])
            i++
        }
        return MAX32(maxval, 0 - minval)
    }

    fun celt_maxabs32(x: ShortArray, x_ptr: Int, len: Int): Short {
        var i: Int
        var maxval: Short = 0
        var minval: Short = 0
        i = x_ptr
        while (i < x_ptr + len) {
            maxval = MAX16(maxval, x[i])
            minval = MIN16(minval, x[i])
            i++
        }
        return MAX(maxval, (0 - minval).toShort())
    }

    /// <summary>
    /// Multiplies two 16-bit fractional values. Bit-exactness of this macro is important
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun FRAC_MUL16(a: Int, b: Int): Int {
        return 16384 + a.toShort() * b.toShort() shr 15
    }

    /// <summary>
    /// Compute floor(sqrt(_val)) with exact arithmetic.
    /// This has been tested on all possible 32-bit inputs.
    /// </summary>
    /// <param name="_val"></param>
    /// <returns></returns>
    fun isqrt32(_val: Long): Int {
        var _val = _val
        var b: Int
        var g: Int
        var bshift: Int
        /*Uses the second method from
           http://www.azillionmonkeys.com/qed/sqroot.html
          The main idea is to search for the largest binary digit b such that
           (g+b)*(g+b) <= _val, and add it to the solution g.*/
        g = 0
        bshift = EC_ILOG(_val) - 1 shr 1
        b = 1 shl bshift
        do {
            val t: Long
            t = ((g shl 1) + b shl bshift).toLong()
            if (t <= _val) {
                g += b
                _val -= t
            }
            b = b shr 1
            bshift--
        } while (bshift >= 0)
        return g
    }

    /**
     * Sqrt approximation (QX input, QX/2 output)
     */
    fun celt_sqrt(x: Int): Int {
        var x = x
        val k: Int
        val n: Short
        var rt: Int

        if (x == 0) {
            return 0
        } else if (x >= 1073741824) {
            return 32767
        }
        k = (celt_ilog2(x) shr 1) - 7
        x = VSHR32(x, 2 * k)
        n = (x - 32768).toShort()
        rt = ADD16(
            sqrt_C[0], MULT16_16_Q15(
                n, ADD16(
                    sqrt_C[1], MULT16_16_Q15(
                        n, ADD16(
                            sqrt_C[2],
                            MULT16_16_Q15(n, ADD16(sqrt_C[3], MULT16_16_Q15(n, sqrt_C[4])))
                        )
                    )
                )
            )
        ).toInt()
        rt = VSHR32(rt, 7 - k)
        return rt
    }

    /**
     * Reciprocal approximation (Q15 input, Q16 output)
     */
    fun celt_rcp(x: Int): Int {
        val i: Int
        val n: Int
        var r: Int
        Inlines.OpusAssert(x > 0, "celt_rcp() only defined for positive values")
        i = celt_ilog2(x)
        /* n is Q15 with range [0,1). */
        n = VSHR32(x, i - 15) - 32768
        /* Start with a linear approximation:
           r = 1.8823529411764706-0.9411764705882353*n.
           The coefficients and the result are Q14 in the range [15420,30840].*/
        r = ADD16(30840, MULT16_16_Q15(-15420, n))
        /* Perform two Newton iterations:
           r -= r*((r*n)-1.Q15)
              = r*((r*n)+(r-1.Q15)). */
        r = SUB16(
            r, MULT16_16_Q15(
                r,
                ADD16(MULT16_16_Q15(r, n), ADD16(r, -32768))
            )
        )
        /* We subtract an extra 1 in the second iteration to avoid overflow; it also
            neatly compensates for truncation error in the rest of the process. */
        r = SUB16(
            r, ADD16(
                1, MULT16_16_Q15(
                    r,
                    ADD16(MULT16_16_Q15(r, n), ADD16(r, -32768))
                )
            )
        )
        /* r is now the Q15 solution to 2/(n+1), with a maximum relative error
            of 7.05346E-5, a (relative) RMSE of 2.14418E-5, and a peak absolute
            error of 1.24665/32768. */
        return VSHR32(EXTEND32(r), i - 16)
    }

    /**
     * Reciprocal sqrt approximation in the range [0.25,1) (Q16 in, Q14 out)
     */
    fun celt_rsqrt_norm(x: Int): Int {
        val n: Int
        val r: Int
        val r2: Int
        val y: Int
        /* Range of n is [-16384,32767] ([-0.5,1) in Q15). */
        n = x - 32768
        /* Get a rough initial guess for the root.
           The optimal minimax quadratic approximation (using relative error) is
            r = 1.437799046117536+n*(-0.823394375837328+n*0.4096419668459485).
           Coefficients here, and the final result r, are Q14.*/
        r = ADD16(23557, MULT16_16_Q15(n, ADD16(-13490, MULT16_16_Q15(n, 6713))))
        /* We want y = x*r*r-1 in Q15, but x is 32-bit Q16 and r is Q14.
           We can compute the result from n and r using Q15 multiplies with some
            adjustment, carefully done to avoid overflow.
           Range of y is [-1564,1594]. */
        r2 = MULT16_16_Q15(r, r)
        y = SHL16(SUB16(ADD16(MULT16_16_Q15(r2, n), r2), 16384), 1)
        /* Apply a 2nd-order Householder iteration: r += r*y*(y*0.375-0.5).
           This yields the Q14 reciprocal square root of the Q16 x, with a maximum
            relative error of 1.04956E-4, a (relative) RMSE of 2.80979E-5, and a
            peak absolute error of 2.26591/16384. */
        return ADD16(
            r, MULT16_16_Q15(
                r, MULT16_16_Q15(
                    y,
                    SUB16(MULT16_16_Q15(y, 12288), 16384)
                )
            )
        )
    }

    fun frac_div32(a: Int, b: Int): Int {
        var a = a
        var b = b
        val rcp: Int
        var result: Int
        val rem: Int
        val shift = celt_ilog2(b) - 29
        a = VSHR32(a, shift)
        b = VSHR32(b, shift)
        /* 16-bit reciprocal */
        rcp = ROUND16(celt_rcp(ROUND16(b, 16)), 3)
        result = MULT16_32_Q15(rcp, a)
        rem = PSHR32(a, 2) - MULT32_32_Q31(result, b)
        result = ADD32(result, SHL32(MULT16_32_Q15(rcp, rem), 2))
        return if (result >= 536870912)
        /*  2^29 */ {
            2147483647          /*  2^31 - 1 */
        } else if (result <= -536870912)
        /* -2^29 */ {
            -2147483647         /* -2^31 */
        } else {
            SHL32(result, 2)
        }
    }

    /**
     * Base-2 logarithm approximation (log2(x)). (Q14 input, Q10 output)
     */
    fun celt_log2(x: Int): Int {
        val i: Int
        val n: Int
        val frac: Int
        /* -0.41509302963303146, 0.9609890551383969, -0.31836011537636605,
            0.15530808010959576, -0.08556153059057618 */
        if (x == 0) {
            return -32767
        }
        i = celt_ilog2(x)
        n = VSHR32(x, i - 15) - 32768 - 16384
        frac = ADD16(
            log2_C0.toInt(),
            MULT16_16_Q15(
                n,
                ADD16(15746, MULT16_16_Q15(n, ADD16(-5217, MULT16_16_Q15(n, ADD16(2545, MULT16_16_Q15(n, -1401))))))
            )
        )
        return SHL16((i - 13).toShort(), 10) + SHR16(frac, 4)
    }

    fun celt_exp2_frac(x: Int): Int {
        val frac: Int
        frac = SHL16(x, 4)
        return ADD16(
            16383,
            MULT16_16_Q15(frac, ADD16(22804, MULT16_16_Q15(frac, ADD16(14819, MULT16_16_Q15(10204, frac)))))
        )
    }

    /**
     * Base-2 exponential approximation (2^x). (Q10 input, Q16 output)
     */
    fun celt_exp2(x: Int): Int {
        val integer: Int
        val frac: Int
        integer = SHR16(x, 10)
        if (integer > 14) {
            return 0x7f000000
        } else if (integer < -15) {
            return 0
        }
        frac = celt_exp2_frac((x - SHL16(integer.toShort(), 10)).toShort().toInt()).toShort().toInt()
        return VSHR32(EXTEND32(frac), -integer - 2)
    }

    /* Atan approximation using a 4th order polynomial. Input is in Q15 format
       and normalized by pi/4. Output is in Q15 format */
    fun celt_atan01(x: Int): Int {
        return MULT16_16_P15(
            x,
            ADD32(32767, MULT16_16_P15(x, ADD32(-21, MULT16_16_P15(x, ADD32(-11943, MULT16_16_P15(4936, x))))))
        )
    }

    /* atan2() approximation valid for positive input values */
    fun celt_atan2p(y: Int, x: Int): Int {
        if (y < x) {
            var arg: Int
            arg = celt_div(SHL32(EXTEND32(y), 15), x)
            if (arg >= 32767) {
                arg = 32767
            }
            return SHR32(celt_atan01(EXTRACT16(arg).toInt()), 1)
        } else {
            var arg: Int
            arg = celt_div(SHL32(EXTEND32(x), 15), y)
            if (arg >= 32767) {
                arg = 32767
            }
            return 25736 - SHR16(celt_atan01(EXTRACT16(arg).toInt()), 1)
        }
    }

    fun celt_cos_norm(x: Int): Int {
        var x = x
        x = x and 0x0001ffff
        if (x > SHL32(EXTEND32(1), 16)) {
            x = SUB32(SHL32(EXTEND32(1), 17), x)
        }
        return if (x and 0x00007fff != 0) {
            if (x < SHL32(EXTEND32(1), 15)) {
                _celt_cos_pi_2(EXTRACT16(x).toInt())
            } else {
                NEG32(_celt_cos_pi_2(EXTRACT16(65536 - x).toInt())) // opus bug: should be neg32?
            }
        } else if (x and 0x0000ffff != 0) {
            0
        } else if (x and 0x0001ffff != 0) {
            -32767
        } else {
            32767
        }
    }

    fun _celt_cos_pi_2(x: Int): Int {
        val x2: Int

        x2 = MULT16_16_P15(x, x)
        return ADD32(
            1,
            MIN32(
                32766,
                ADD32(
                    SUB16(32767, x2),
                    MULT16_16_P15(x2, ADD32(-7651, MULT16_16_P15(x2, ADD32(8277, MULT16_16_P15(-626, x2)))))
                )
            )
        )
    }

    fun FLOAT2INT16(x: Float): Short {
        var x = x
        x = x * CeltConstants.CELT_SIG_SCALE
        if (x < Short.MIN_VALUE) {
            x = Short.MIN_VALUE.toFloat()
        }
        if (x > Short.MAX_VALUE) {
            x = Short.MAX_VALUE.toFloat()
        }
        return x.toShort()
    }

    // SILK-SPECIFIC INLINES
    /// <summary>
    /// Rotate a32 right by 'rot' bits. Negative rot values result in rotating
    /// left. Output is 32bit int.
    /// </summary>
    /// <param name="a32"></param>
    /// <param name="rot"></param>
    /// <returns></returns>
    fun silk_ROR32(a32: Int, rot: Int): Int {
        val m = 0 - rot
        return if (rot == 0) {
            a32
        } else if (rot < 0) {
            a32 shl m or (a32 shr 32 - m)
        } else {
            a32 shl 32 - rot or (a32 shr rot)
        }
    }

    fun silk_MUL(a32: Int, b32: Int): Int {
        return a32 * b32
    }

    fun silk_MLA(a32: Int, b32: Int, c32: Int): Int {
        val ret = silk_ADD32(a32, b32 * c32)
        Inlines.OpusAssert(ret.toLong() == a32.toLong() + b32.toLong() * c32.toLong())
        return ret
    }

    /// <summary>
    /// ((a32 >> 16)  * (b32 >> 16))
    /// </summary>
    /// <param name="a32"></param>
    /// <param name="b32"></param>
    /// <returns></returns>
    fun silk_SMULTT(a32: Int, b32: Int): Int {
        return (a32 shr 16) * (b32 shr 16)
    }

    fun silk_SMLATT(a32: Int, b32: Int, c32: Int): Int {
        return silk_ADD32(a32, (b32 shr 16) * (c32 shr 16))
    }

    fun silk_SMLALBB(a64: Long, b16: Short, c16: Short): Long {
        return silk_ADD64(a64, (b16.toInt() * c16.toInt()).toLong())
    }

    fun silk_SMULL(a32: Int, b32: Int): Long {
        return a32.toLong() * b32.toLong()
    }

    /// <summary>
    /// Adds two signed 32-bit values in a way that can overflow, while not relying on undefined behaviour
    /// (just standard two's complement implementation-specific behaviour)
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_ADD32_ovflw(a: Int, b: Int): Int {
        return (a.toLong() + b).toInt()
    }

    fun silk_ADD32_ovflw(a: Long, b: Long): Int {
        return (a + b).toInt()
    }

    /// <summary>
    /// Subtracts two signed 32-bit values in a way that can overflow, while not relying on undefined behaviour
    /// (just standard two's complement implementation-specific behaviour)
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_SUB32_ovflw(a: Int, b: Int): Int {
        return (a.toLong() - b).toInt()
    }

    /// <summary>
    /// Multiply-accumulate macros that allow overflow in the addition (ie, no asserts in debug mode)
    /// </summary>
    /// <param name="a32"></param>
    /// <param name="b32"></param>
    /// <param name="c32"></param>
    /// <returns></returns>
    fun silk_MLA_ovflw(a32: Int, b32: Int, c32: Int): Int {
        return silk_ADD32_ovflw(a32.toLong(), b32.toLong() * c32)
    }

    fun silk_SMLABB_ovflw(a32: Int, b32: Int, c32: Int): Int {
        return silk_ADD32_ovflw(a32, b32.toShort().toInt() * c32.toShort().toInt())
    }

    fun silk_SMULBB(a32: Int, b32: Int): Int {
        return a32.toShort().toInt() * b32.toShort().toInt()
    }

    /// <summary>
    /// (a32 * (int)((short)(b32))) >> 16 output have to be 32bit int
    /// </summary>
    /// <param name="a32"></param>
    /// <param name="b32"></param>
    /// <returns></returns>
    fun silk_SMULWB(a32: Int, b32: Int): Int {
        return (a32 * b32.toShort().toLong() shr 16).toInt()
    }

    fun silk_SMLABB(a32: Int, b32: Int, c32: Int): Int {
        return a32 + b32.toShort().toInt() * c32.toShort().toInt()
    }

    fun silk_DIV32_16(a32: Int, b32: Int): Int {
        return a32 / b32
    }

    fun silk_DIV32(a32: Int, b32: Int): Int {
        return a32 / b32
    }

    fun silk_ADD16(a: Short, b: Short): Short {
        return (a + b).toShort()
    }

    fun silk_ADD32(a: Int, b: Int): Int {
        return a + b
    }

    fun silk_ADD64(a: Long, b: Long): Long {
        val ret = a + b
        Inlines.OpusAssert(ret == silk_ADD_SAT64(a, b))
        return ret
    }

    fun silk_SUB16(a: Short, b: Short): Short {
        val ret = (a - b).toShort()
        Inlines.OpusAssert(ret == silk_SUB_SAT16(a, b))
        return ret
    }

    fun silk_SUB32(a: Int, b: Int): Int {
        val ret = a - b
        Inlines.OpusAssert(ret == silk_SUB_SAT32(a, b))
        return ret
    }

    fun silk_SUB64(a: Long, b: Long): Long {
        val ret = a - b
        Inlines.OpusAssert(ret == silk_SUB_SAT64(a, b))
        return ret
    }

    fun silk_SAT8(a: Int): Int {
        return if (a > Byte.MAX_VALUE) Byte.MAX_VALUE.toInt() else if (a < Byte.MIN_VALUE) Byte.MIN_VALUE.toInt() else a
    }

    fun silk_SAT16(a: Int): Int {
        return if (a > Short.MAX_VALUE) Short.MAX_VALUE.toInt() else if (a < Short.MIN_VALUE) Short.MIN_VALUE.toInt() else a
    }

    fun silk_SAT32(a: Long): Int {
        return if (a > Int.MAX_VALUE) Int.MAX_VALUE else if (a < Int.MIN_VALUE) Int.MIN_VALUE else a.toInt()
    }

    /// <summary>
    /// //////////////////
    /// </summary>
    /// <param name="a16"></param>
    /// <param name="b16"></param>
    /// <returns></returns>
    fun silk_ADD_SAT16(a16: Short, b16: Short): Short {
        val res = silk_SAT16(silk_ADD32(a16.toInt(), b16.toInt())).toShort()
        Inlines.OpusAssert(res.toInt() == silk_SAT16(a16.toInt() + b16.toInt()))
        return res
    }

    fun silk_ADD_SAT32(a32: Int, b32: Int): Int {
        val res = if (a32.toLong() + b32.toLong() and -0x80000000 == 0L)
            if (a32 and b32 and -0x80000000 != 0) Int.MIN_VALUE else a32 + b32
        else
            if (a32 or b32 and -0x80000000 == 0) Int.MAX_VALUE else a32 + b32
        Inlines.OpusAssert(res == silk_SAT32(a32.toLong() + b32.toLong()))
        return res
    }

    fun silk_ADD_SAT64(a64: Long, b64: Long): Long {
        val res: Long
        res = if (a64 + b64 and Long.MIN_VALUE == 0L)
            if (a64 and b64 and Long.MIN_VALUE != 0L) Long.MIN_VALUE else a64 + b64
        else
            if (a64 or b64 and Long.MIN_VALUE == 0L) Long.MAX_VALUE else a64 + b64
        return res
    }

    fun silk_SUB_SAT16(a16: Short, b16: Short): Short {
        val res = silk_SAT16(silk_SUB32(a16.toInt(), b16.toInt())).toShort()
        Inlines.OpusAssert(res.toInt() == silk_SAT16(a16.toInt() - b16.toInt()))
        return res
    }

    fun silk_SUB_SAT32(a32: Int, b32: Int): Int {
        val res = if (a32.toLong() - b32 and -0x80000000 == 0L)
            if (a32 and (b32 xor -0x80000000) and -0x80000000 != 0) Int.MIN_VALUE else a32 - b32
        else
            if (a32 xor -0x80000000 and b32 and -0x80000000 != 0) Int.MAX_VALUE else a32 - b32
        Inlines.OpusAssert(res == silk_SAT32(a32.toLong() - b32.toLong()))
        return res
    }

    fun silk_SUB_SAT64(a64: Long, b64: Long): Long {
        val res: Long
        res = if (a64 - b64 and Long.MIN_VALUE == 0L)
            if (a64 and (b64 xor Long.MIN_VALUE) and Long.MIN_VALUE != 0L) Long.MIN_VALUE else a64 - b64
        else
            if (a64 xor Long.MIN_VALUE and b64 and Long.MIN_VALUE != 0L) Long.MAX_VALUE else a64 - b64
        return res
    }

    ///* Saturation for positive input values */
    //#define silk_POS_SAT32(a)                   ((a) > int_MAX ? int_MAX : (a))
    /// <summary>
    /// Add with saturation for positive input values
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_ADD_POS_SAT8(a: Byte, b: Byte): Byte {
        return (if (a + b and 0x80 != 0) Byte.MAX_VALUE else (a + b).toByte())
    }

    /// <summary>
    /// Add with saturation for positive input values
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_ADD_POS_SAT16(a: Short, b: Short): Short {
        return (if (a + b and 0x8000 != 0) Short.MAX_VALUE else (a + b).toShort())
    }

    /// <summary>
    /// Add with saturation for positive input values
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_ADD_POS_SAT32(a: Int, b: Int): Int {
        return if (a + b and -0x80000000 != 0) Int.MAX_VALUE else a + b
    }

    /// <summary>
    /// Add with saturation for positive input values
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun silk_ADD_POS_SAT64(a: Long, b: Long): Long {
        return if (a + b and Long.MIN_VALUE != 0L) Long.MAX_VALUE else a + b
    }

    fun silk_LSHIFT8(a: Byte, shift: Int): Byte {
        return (a shl shift).toByte()
    }

    fun silk_LSHIFT16(a: Short, shift: Int): Short {
        return (a shl shift).toShort()
    }

    fun silk_LSHIFT32(a: Int, shift: Int): Int {
        return a shl shift
    }

    fun silk_LSHIFT64(a: Long, shift: Int): Long {
        return a shl shift
    }

    fun silk_LSHIFT(a: Int, shift: Int): Int {
        return a shl shift
    }

    fun silk_LSHIFT_ovflw(a: Int, shift: Int): Int {
        return a shl shift
    }

    /// <summary>
    /// saturates before shifting
    /// </summary>
    /// <param name="a"></param>
    /// <param name="shift"></param>
    /// <returns></returns>
    fun silk_LSHIFT_SAT32(a: Int, shift: Int): Int {
        return silk_LSHIFT32(
            silk_LIMIT(
                a,
                silk_RSHIFT32(Int.MIN_VALUE, shift),
                silk_RSHIFT32(Int.MAX_VALUE, shift)
            ), shift
        )
    }

    fun silk_RSHIFT8(a: Byte, shift: Int): Byte {
        return (a shr shift).toByte()
    }

    fun silk_RSHIFT16(a: Short, shift: Int): Short {
        return (a shr shift).toShort()
    }

    fun silk_RSHIFT32(a: Int, shift: Int): Int {
        return a shr shift
    }

    fun silk_RSHIFT(a: Int, shift: Int): Int {
        return a shr shift
    }

    fun silk_RSHIFT64(a: Long, shift: Int): Long {
        return a shr shift
    }

    fun silk_RSHIFT_uint(a: Long, shift: Int): Long {
        return CapToUInt32(a) shr shift
    }

    fun silk_ADD_LSHIFT(a: Int, b: Int, shift: Int): Int {
        return a + (b shl shift)
        /* shift >= 0 */
    }

    fun silk_ADD_LSHIFT32(a: Int, b: Int, shift: Int): Int {
        return a + (b shl shift)
        /* shift >= 0 */
    }

    fun silk_ADD_RSHIFT(a: Int, b: Int, shift: Int): Int {
        return a + (b shr shift)
        /* shift  > 0 */
    }

    fun silk_ADD_RSHIFT32(a: Int, b: Int, shift: Int): Int {
        return a + (b shr shift)
        /* shift  > 0 */
    }

    fun silk_ADD_RSHIFT_uint(a: Long, b: Long, shift: Int): Long {
        return CapToUInt32(a + (CapToUInt32(b) shr shift))
        /* shift  > 0 */
    }

    fun silk_SUB_LSHIFT32(a: Int, b: Int, shift: Int): Int {
        val ret: Int
        ret = a - (b shl shift)
        return ret
        /* shift >= 0 */
    }

    fun silk_SUB_RSHIFT32(a: Int, b: Int, shift: Int): Int {
        val ret: Int
        ret = a - (b shr shift)
        return ret
        /* shift  > 0 */
    }

    fun silk_RSHIFT_ROUND(a: Int, shift: Int): Int {
        val ret: Int
        ret = if (shift == 1) (a shr 1) + (a and 1) else (a shr shift - 1) + 1 shr 1
        return ret
    }

    fun silk_RSHIFT_ROUND64(a: Long, shift: Int): Long {
        val ret: Long
        ret = if (shift == 1) (a shr 1) + (a and 1) else (a shr shift - 1) + 1 shr 1
        return ret
    }

    fun silk_min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun silk_max(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun silk_min(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    fun silk_max(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    /// <summary>
    /// Macro to convert floating-point constants to fixed-point by applying a scalar factor
    /// Because of limitations of the JIT, this macro is actually evaluated at runtime and therefore should not be used if you want to maximize performance
    /// </summary>
    fun SILK_CONST(number: Float, scale: Int): Int {
        return (number * (1.toLong() shl scale) + 0.5).toInt()
    }

    /* silk_min() versions with typecast in the function call */
    fun silk_min_int(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun silk_min_16(a: Short, b: Short): Short {
        return if (a < b) a else b
    }

    fun silk_min_32(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun silk_min_64(a: Long, b: Long): Long {
        return if (a < b) a else b
    }

    /* silk_min() versions with typecast in the function call */
    fun silk_max_int(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun silk_max_16(a: Short, b: Short): Short {
        return if (a > b) a else b
    }

    fun silk_max_32(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun silk_max_64(a: Long, b: Long): Long {
        return if (a > b) a else b
    }

    fun silk_LIMIT(a: Float, limit1: Float, limit2: Float): Float {
        return if (limit1 > limit2) if (a > limit1) limit1 else if (a < limit2) limit2 else a else if (a > limit2) limit2 else if (a < limit1) limit1 else a
    }

    fun silk_LIMIT(a: Int, limit1: Int, limit2: Int): Int {
        return silk_LIMIT_32(a, limit1, limit2)
    }

    fun silk_LIMIT_int(a: Int, limit1: Int, limit2: Int): Int {
        return silk_LIMIT_32(a, limit1, limit2)
    }

    fun silk_LIMIT_16(a: Short, limit1: Short, limit2: Short): Short {
        return if (limit1 > limit2) if (a > limit1) limit1 else if (a < limit2) limit2 else a else if (a > limit2) limit2 else if (a < limit1) limit1 else a
    }

    fun silk_LIMIT_32(a: Int, limit1: Int, limit2: Int): Int {
        return if (limit1 > limit2) if (a > limit1) limit1 else if (a < limit2) limit2 else a else if (a > limit2) limit2 else if (a < limit1) limit1 else a
    }

    fun silk_abs(a: Int): Int {
        // Be careful, silk_abs returns wrong when input equals to silk_intXX_MIN
        return if (a > 0) a else -a
    }

    fun silk_abs_int16(a: Int): Int {
        return (a xor (a shr 15)) - (a shr 15)
    }

    fun silk_abs_int32(a: Int): Int {
        return (a xor (a shr 31)) - (a shr 31)
    }

    fun silk_abs_int64(a: Long): Long {
        return if (a > 0) a else -a
    }

    fun silk_sign(a: Int): Long {
        return (if (a > 0) 1 else if (a < 0) -1 else 0).toLong()
    }

    /// <summary>
    /// PSEUDO-RANDOM GENERATOR
    /// Make sure to store the result as the seed for the next call (also in between
    /// frames), otherwise result won't be random at all. When only using some of the
    /// bits, take the most significant bits by right-shifting.
    /// </summary>
    fun silk_RAND(seed: Int): Int {
        return silk_MLA_ovflw(907633515, seed, 196314165)
    }

    /// <summary>
    /// silk_SMMUL: Signed top word multiply.
    /// </summary>
    /// <param name="a32"></param>
    /// <param name="b32"></param>
    /// <returns></returns>
    fun silk_SMMUL(a32: Int, b32: Int): Int {
        return silk_RSHIFT64(silk_SMULL(a32, b32), 32).toInt()
    }

    /* a32 + (b32 * (c32 >> 16)) >> 16 */
    fun silk_SMLAWT(a32: Int, b32: Int, c32: Int): Int {
        return a32 + (b32 shr 16) * (c32 shr 16) + ((b32 and 0x0000FFFF) * (c32 shr 16) shr 16)
    }

    /// <summary>
    /// Divide two int32 values and return result as int32 in a given Q-domain
    /// </summary>
    /// <param name="a32">I    numerator (Q0)</param>
    /// <param name="b32">I    denominator (Q0)</param>
    /// <param name="Qres">I    Q-domain of result (>= 0)</param>
    /// <returns>O    returns a good approximation of "(a32 << Qres) / b32"</returns>
    fun silk_DIV32_varQ(a32: Int, b32: Int, Qres: Int): Int {
        val a_headrm: Int
        val b_headrm: Int
        val lshift: Int
        val b32_inv: Int
        var a32_nrm: Int
        val b32_nrm: Int
        var result: Int

        Inlines.OpusAssert(b32 != 0)
        Inlines.OpusAssert(Qres >= 0)

        /* Compute number of bits head room and normalize inputs */
        a_headrm = silk_CLZ32(silk_abs(a32)) - 1
        a32_nrm = silk_LSHIFT(a32, a_headrm)
        /* Q: a_headrm                  */
        b_headrm = silk_CLZ32(silk_abs(b32)) - 1
        b32_nrm = silk_LSHIFT(b32, b_headrm)
        /* Q: b_headrm                  */

        /* Inverse of b32, with 14 bits of precision */
        b32_inv = silk_DIV32_16(Int.MAX_VALUE shr 2, silk_RSHIFT(b32_nrm, 16))
        /* Q: 29 + 16 - b_headrm        */

        /* First approximation */
        result = silk_SMULWB(a32_nrm, b32_inv)
        /* Q: 29 + a_headrm - b_headrm  */

        /* Compute residual by subtracting product of denominator and first approximation */
        /* It's OK to overflow because the final value of a32_nrm should always be small */
        a32_nrm = silk_SUB32_ovflw(a32_nrm, silk_LSHIFT_ovflw(silk_SMMUL(b32_nrm, result), 3))
        /* Q: a_headrm   */

        /* Refinement */
        result = silk_SMLAWB(result, a32_nrm, b32_inv)
        /* Q: 29 + a_headrm - b_headrm  */

        /* Convert to Qres domain */
        lshift = 29 + a_headrm - b_headrm - Qres
        return if (lshift < 0) {
            silk_LSHIFT_SAT32(result, -lshift)
        } else if (lshift < 32) {
            silk_RSHIFT(result, lshift)
        } else {
            /* Avoid undefined result */
            0
        }
    }

    /// <summary>
    /// Invert int32 value and return result as int32 in a given Q-domain
    /// </summary>
    /// <param name="b32">I    denominator (Q0)</param>
    /// <param name="Qres">I    Q-domain of result (> 0)</param>
    /// <returns>a good approximation of "(1 << Qres) / b32"</returns>
    fun silk_INVERSE32_varQ(b32: Int, Qres: Int): Int {
        val b_headrm: Int
        val lshift: Int
        val b32_inv: Int
        val b32_nrm: Int
        val err_Q32: Int
        var result: Int

        Inlines.OpusAssert(b32 != 0)
        Inlines.OpusAssert(Qres > 0)

        /* Compute number of bits head room and normalize input */
        b_headrm = silk_CLZ32(silk_abs(b32)) - 1
        b32_nrm = silk_LSHIFT(b32, b_headrm)
        /* Q: b_headrm                */

        /* Inverse of b32, with 14 bits of precision */
        b32_inv = silk_DIV32_16(Int.MAX_VALUE shr 2, silk_RSHIFT(b32_nrm, 16).toShort().toInt())
        /* Q: 29 + 16 - b_headrm    */

        /* First approximation */
        result = silk_LSHIFT(b32_inv, 16)
        /* Q: 61 - b_headrm            */

        /* Compute residual by subtracting product of denominator and first approximation from one */
        err_Q32 = silk_LSHIFT((1 shl 29) - silk_SMULWB(b32_nrm, b32_inv), 3)
        /* Q32                        */

        /* Refinement */
        result = silk_SMLAWW(result, err_Q32, b32_inv)
        /* Q: 61 - b_headrm            */

        /* Convert to Qres domain */
        lshift = 61 - b_headrm - Qres
        return if (lshift <= 0) {
            silk_LSHIFT_SAT32(result, -lshift)
        } else if (lshift < 32) {
            silk_RSHIFT(result, lshift)
        } else {
            /* Avoid undefined result */
            0
        }
    }

    //////////////////////// from macros.h /////////////////////////////////////////////
    /// <summary>
    /// a32 + (b32 * (int)((short)(c32))) >> 16 output have to be 32bit int
    /// </summary>
    // fixme: This method should be as optimized as possible
    fun silk_SMLAWB(a32: Int, b32: Int, c32: Int): Int {
        //return (int)(a32 + ((b32 * (long)((short)c32)) >> 16));
        val ret: Int
        ret = a32 + silk_SMULWB(b32, c32)
        return ret
    }

    ///* (a32 * (b32 >> 16)) >> 16 */
    fun silk_SMULWT(a32: Int, b32: Int): Int {
        return (a32 shr 16) * (b32 shr 16) + ((a32 and 0x0000FFFF) * (b32 shr 16) shr 16)
    }

    ///* (int)((short)(a32)) * (b32 >> 16) */
    fun silk_SMULBT(a32: Int, b32: Int): Int {
        return a32.toShort().toInt() * (b32 shr 16)
    }

    ///* a32 + (int)((short)(b32)) * (c32 >> 16) */
    fun silk_SMLABT(a32: Int, b32: Int, c32: Int): Int {
        return a32 + b32.toShort().toInt() * (c32 shr 16)
    }

    ///* a64 + (b32 * c32) */
    fun silk_SMLAL(a64: Long, b32: Int, c32: Int): Long {
        return silk_ADD64(a64, b32.toLong() * c32.toLong())
    }

    fun MatrixGetPointer(row: Int, column: Int, N: Int): Int {
        return row * N + column
    }

    fun MatrixGet(Matrix_base_adr: IntArray, row: Int, column: Int, N: Int): Int {
        return Matrix_base_adr[row * N + column]
    }

    fun MatrixGet(Matrix_base_adr: ShortArray, row: Int, column: Int, N: Int): Short {
        return Matrix_base_adr[row * N + column]
    }

    fun MatrixGet(
        Matrix_base_adr: Array<PitchAnalysisCore.silk_pe_stage3_vals>,
        row: Int,
        column: Int,
        N: Int
    ): PitchAnalysisCore.silk_pe_stage3_vals {
        return Matrix_base_adr[row * N + column]
    }

    fun MatrixGet(Matrix_base_adr: IntArray, matrix_ptr: Int, row: Int, column: Int, N: Int): Int {
        return Matrix_base_adr[matrix_ptr + row * N + column]
    }

    fun MatrixGet(Matrix_base_adr: ShortArray, matrix_ptr: Int, row: Int, column: Int, N: Int): Short {
        return Matrix_base_adr[matrix_ptr + row * N + column]
    }

    fun MatrixSet(Matrix_base_adr: IntArray, matrix_ptr: Int, row: Int, column: Int, N: Int, value: Int) {
        Matrix_base_adr[matrix_ptr + row * N + column] = value
    }

    fun MatrixSet(Matrix_base_adr: ShortArray, matrix_ptr: Int, row: Int, column: Int, N: Int, value: Short) {
        Matrix_base_adr[matrix_ptr + row * N + column] = value
    }

    fun MatrixSet(Matrix_base_adr: IntArray, row: Int, column: Int, N: Int, value: Int) {
        Matrix_base_adr[row * N + column] = value
    }

    fun MatrixSet(Matrix_base_adr: ShortArray, row: Int, column: Int, N: Int, value: Short) {
        Matrix_base_adr[row * N + column] = value
    }

    /// <summary>
    /// (a32 * b32) >> 16
    /// </summary>
    fun silk_SMULWW(a32: Int, b32: Int): Int {
        //return CHOP32(((long)(a32) * (b32)) >> 16);
        return silk_MLA(silk_SMULWB(a32, b32), a32, silk_RSHIFT_ROUND(b32, 16))
    }

    /// <summary>
    /// a32 + ((b32 * c32) >> 16)
    /// </summary>
    fun silk_SMLAWW(a32: Int, b32: Int, c32: Int): Int {
        //return CHOP32(((a32) + (((long)(b32) * (c32)) >> 16)));
        return silk_MLA(silk_SMLAWB(a32, b32, c32), b32, silk_RSHIFT_ROUND(c32, 16))
    }

    /* count leading zeros of opus_int64 */
    fun silk_CLZ64(input: Long): Int {
        val in_upper: Int

        in_upper = silk_RSHIFT64(input, 32).toInt()
        return if (in_upper == 0) {
            /* Search in the lower 32 bits */
            32 + silk_CLZ32(input.toInt())
        } else {
            /* Search in the upper 32 bits */
            silk_CLZ32(in_upper)
        }
    }

    fun silk_CLZ32(in32: Int): Int {
        return if (in32 == 0) 32 else 32 - EC_ILOG(in32.toLong())
    }

    /// <summary>
    /// Get number of leading zeros and fractional part (the bits right after the leading one)
    /// </summary>
    /// <param name="input">input</param>
    /// <param name="lz">number of leading zeros</param>
    /// <param name="frac_Q7">the 7 bits right after the leading one</param>
    fun silk_CLZ_FRAC(input: Int, lz: BoxedValueInt, frac_Q7: BoxedValueInt) {
        val lzeros = silk_CLZ32(input)

        lz.Val = lzeros
        frac_Q7.Val = silk_ROR32(input, 24 - lzeros) and 0x7f
    }

    /// <summary>
    /// Approximation of square root.
    /// Accuracy: +/- 10%  for output values > 15
    ///           +/- 2.5% for output values > 120
    /// </summary>
    /// <param name="x"></param>
    /// <returns></returns>
    fun silk_SQRT_APPROX(x: Int): Int {
        var y: Int
        val lz: Int
        val frac_Q7: Int

        if (x <= 0) {
            return 0
        }

        val boxed_lz = BoxedValueInt(0)
        val boxed_frac_Q7 = BoxedValueInt(0)
        silk_CLZ_FRAC(x, boxed_lz, boxed_frac_Q7)
        lz = boxed_lz.Val
        frac_Q7 = boxed_frac_Q7.Val

        if (lz and 1 != 0) {
            y = 32768
        } else {
            y = 46214        // 46214 = sqrt(2) * 32768
        }

        // get scaling right
        y = y shr silk_RSHIFT(lz, 1)

        // increment using fractional part of input
        y = silk_SMLAWB(y, y, silk_SMULBB(213, frac_Q7))

        return y
    }

    fun MUL32_FRAC_Q(a32: Int, b32: Int, Q: Int): Int {
        return silk_RSHIFT_ROUND64(silk_SMULL(a32, b32), Q).toInt()
    }

    /// <summary>
    /// Approximation of 128 * log2() (very close inverse of silk_log2lin())
    /// Convert input to a log scale
    /// </summary>
    /// <param name="inLin">(I) input in linear scale</param>
    /// <returns></returns>
    fun silk_lin2log(inLin: Int): Int {
        val lz = BoxedValueInt(0)
        val frac_Q7 = BoxedValueInt(0)

        silk_CLZ_FRAC(inLin, lz, frac_Q7)

        // Piece-wise parabolic approximation
        return silk_LSHIFT(31 - lz.Val, 7) + silk_SMLAWB(frac_Q7.Val, silk_MUL(frac_Q7.Val, 128 - frac_Q7.Val), 179)
    }

    /// <summary>
    /// Approximation of 2^() (very close inverse of silk_lin2log())
    /// Convert input to a linear scale
    /// </summary>
    /// <param name="inLog_Q7">input on log scale</param>
    /// <returns>Linearized value</returns>
    fun silk_log2lin(inLog_Q7: Int): Int {
        var output: Int
        val frac_Q7: Int

        if (inLog_Q7 < 0) {
            return 0
        } else if (inLog_Q7 >= 3967) {
            return Int.MAX_VALUE
        }

        output = silk_LSHIFT(1, silk_RSHIFT(inLog_Q7, 7))
        frac_Q7 = inLog_Q7 and 0x7F

        if (inLog_Q7 < 2048) {
            /* Piece-wise parabolic approximation */
            output = silk_ADD_RSHIFT32(
                output,
                silk_MUL(output, silk_SMLAWB(frac_Q7, silk_SMULBB(frac_Q7, 128 - frac_Q7), -174)),
                7
            )
        } else {
            /* Piece-wise parabolic approximation */
            output = silk_MLA(
                output,
                silk_RSHIFT(output, 7),
                silk_SMLAWB(frac_Q7, silk_SMULBB(frac_Q7, 128 - frac_Q7), -174)
            )
        }

        return output
    }

    /// <summary>
    /// Interpolate two vectors
    /// </summary>
    /// <param name="xi">(O) interpolated vector [MAX_LPC_ORDER]</param>
    /// <param name="x0">(I) first vector [MAX_LPC_ORDER]</param>
    /// <param name="x1">(I) second vector [MAX_LPC_ORDER]</param>
    /// <param name="ifact_Q2">(I) interp. factor, weight on 2nd vector</param>
    /// <param name="d">(I) number of parameters</param>
    fun silk_interpolate(
        xi: ShortArray,
        x0: ShortArray,
        x1: ShortArray,
        ifact_Q2: Int,
        d: Int
    ) {
        var i: Int

        Inlines.OpusAssert(ifact_Q2 >= 0)
        Inlines.OpusAssert(ifact_Q2 <= 4)

        i = 0
        while (i < d) {
            xi[i] = silk_ADD_RSHIFT(x0[i].toInt(), silk_SMULBB(x1[i] - x0[i], ifact_Q2), 2).toShort()
            i++
        }
    }

    /// <summary>
    /// Inner product with bit-shift
    /// </summary>
    /// <param name="inVec1">I input vector 1</param>
    /// <param name="inVec2">I input vector 2</param>
    /// <param name="scale">I number of bits to shift</param>
    /// <param name="len">I vector lengths</param>
    /// <returns></returns>
    fun silk_inner_prod_aligned_scale(
        inVec1: ShortArray,
        inVec2: ShortArray,
        scale: Int,
        len: Int
    ): Int {
        var i: Int
        var sum = 0
        i = 0
        while (i < len) {
            sum = silk_ADD_RSHIFT32(sum, silk_SMULBB(inVec1[i].toInt(), inVec2[i].toInt()), scale)
            i++
        }

        return sum
    }

    /* Copy and multiply a vector by a constant */
    fun silk_scale_copy_vector16(
        data_out: ShortArray,
        data_out_ptr: Int,
        data_in: ShortArray,
        data_in_ptr: Int,
        gain_Q16: Int, /* I    Gain in Q16                                                 */
        dataSize: Int /* I    Length                                                      */
    ) {
        for (i in 0 until dataSize) {
            data_out[data_out_ptr + i] = silk_SMULWB(gain_Q16, data_in[data_in_ptr + i].toInt()).toShort()
        }
    }

    /* Multiply a vector by a constant */
    fun silk_scale_vector32_Q26_lshift_18(
        data1: IntArray, /* I/O  Q0/Q18                                                      */
        data1_ptr: Int,
        gain_Q26: Int, /* I    Q26                                                         */
        dataSize: Int /* I    length                                                      */
    ) {
        for (i in data1_ptr until data1_ptr + dataSize) {
            data1[i] = silk_RSHIFT64(silk_SMULL(data1[i], gain_Q26), 8).toInt()
            /* OUTPUT: Q18 */
        }
    }

    /* sum = for(i=0;i<len;i++)inVec1[i]*inVec2[i];      ---        inner product   */
    fun silk_inner_prod(
        inVec1: ShortArray, /*    I input vector 1                                              */
        inVec1_ptr: Int,
        inVec2: ShortArray, /*    I input vector 2                                              */
        inVec2_ptr: Int,
        len: Int /*    I vector lengths                                              */
    ): Int {
        var i: Int
        var xy = 0
        i = 0
        while (i < len) {
            xy = Inlines.MAC16_16(xy, inVec1[inVec1_ptr + i], inVec2[inVec2_ptr + i])
            i++
        }
        return xy
    }

    fun silk_inner_prod_self(
        inVec: ShortArray, /*    I input vector 1 (will be crossed with itself)                                             */
        inVec_ptr: Int,
        len: Int /*    I vector lengths                                              */
    ): Int {
        var i: Int
        var xy = 0
        i = inVec_ptr
        while (i < inVec_ptr + len) {
            xy = Inlines.MAC16_16(xy, inVec[i], inVec[i])
            i++
        }
        return xy
    }

    fun silk_inner_prod16_aligned_64(
        inVec1: ShortArray, /*    I input vector 1                                              */
        inVec1_ptr: Int,
        inVec2: ShortArray, /*    I input vector 2                                              */
        inVec2_ptr: Int,
        len: Int /*    I vector lengths                                              */
    ): Long {
        var i: Int
        var sum: Long = 0
        i = 0
        while (i < len) {
            sum = silk_SMLALBB(sum, inVec1[inVec1_ptr + i], inVec2[inVec2_ptr + i])
            i++
        }
        return sum
    }

    /// <summary>
    /// returns the value that has fewer higher-order bits, ignoring sign bit (? I think?)
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    fun EC_MINI(a: Long, b: Long): Long {
        return a + (b - a and if (b < a) -0x1 else 0)
    }

    fun EC_ILOG(x: Long): Int {
        var x = x
        if (x == 0L) {
            return 1
        }
        x = x or (x shr 1)
        x = x or (x shr 2)
        x = x or (x shr 4)
        x = x or (x shr 8)
        x = x or (x shr 16)
        var y = x - (x shr 1 and 0x55555555)
        y = (y shr 2 and 0x33333333) + (y and 0x33333333)
        y = (y shr 4) + y and 0x0f0f0f0f
        y += y shr 8
        y += y shr 16
        y = y and 0x0000003f
        return y.toInt()
    }

    fun abs(a: Int): Int {
        return if (a < 0) {
            0 - a
        } else a
    }

    fun SignedByteToUnsignedInt(b: Byte): Int {
        return b and 0xFF
    }
}
