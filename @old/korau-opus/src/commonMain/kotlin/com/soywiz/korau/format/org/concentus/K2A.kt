/* Copyright (c) 2006-2011 Skype Limited. All Rights Reserved
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

internal object K2A {

    /* Step up function, converts reflection coefficients to prediction coefficients */
    fun silk_k2a(
        A_Q24: IntArray, /* O    Prediction coefficients [order] Q24                         */
        rc_Q15: ShortArray, /* I    Reflection coefficients [order] Q15                         */
        order: Int /* I    Prediction order                                            */
    ) {
        var k: Int
        var n: Int
        val Atmp = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)

        k = 0
        while (k < order) {
            n = 0
            while (n < k) {
                Atmp[n] = A_Q24[n]
                n++
            }
            n = 0
            while (n < k) {
                A_Q24[n] = Inlines.silk_SMLAWB(A_Q24[n], Inlines.silk_LSHIFT(Atmp[k - n - 1], 1), rc_Q15[k].toInt())
                n++
            }
            A_Q24[k] = 0 - Inlines.silk_LSHIFT(rc_Q15[k].toInt(), 9)
            k++
        }
    }

    /* Step up function, converts reflection coefficients to prediction coefficients */
    fun silk_k2a_Q16(
        A_Q24: IntArray, /* O    Prediction coefficients [order] Q24                         */
        rc_Q16: IntArray, /* I    Reflection coefficients [order] Q16                         */
        order: Int /* I    Prediction order                                            */
    ) {
        var k: Int
        var n: Int
        val Atmp = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)

        k = 0
        while (k < order) {
            n = 0
            while (n < k) {
                Atmp[n] = A_Q24[n]
                n++
            }
            n = 0
            while (n < k) {
                A_Q24[n] = Inlines.silk_SMLAWW(A_Q24[n], Atmp[k - n - 1], rc_Q16[k])
                n++
            }
            A_Q24[k] = 0 - Inlines.silk_LSHIFT(rc_Q16[k], 8)
            k++
        }
    }
}
