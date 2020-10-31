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

internal object ResidualEnergy {

    /* Calculates residual energies of input subframes where all subframes have LPC_order   */
    /* of preceding samples                                                                 */
    fun silk_residual_energy(
        nrgs: IntArray, /* O    Residual energy per subframe  [MAX_NB_SUBFR]                                              */
        nrgsQ: IntArray, /* O    Q value per subframe   [MAX_NB_SUBFR]                                                     */
        x: ShortArray, /* I    Input signal                                                                */
        a_Q12: Array<ShortArray>, /* I    AR coefs for each frame half [2][MAX_LPC_ORDER]                                                */
        gains: IntArray, /* I    Quantization gains [SilkConstants.MAX_NB_SUBFR]                                                         */
        subfr_length: Int, /* I    Subframe length                                                             */
        nb_subfr: Int, /* I    Number of subframes                                                         */
        LPC_order: Int /* I    LPC order                                                                   */
    ) {
        val offset: Int
        var i: Int
        var j: Int
        var lz1: Int
        var lz2: Int
        val rshift = BoxedValueInt(0)
        val energy = BoxedValueInt(0)
        var LPC_res_ptr: Int
        val LPC_res: ShortArray
        var x_ptr: Int
        var tmp32: Int

        x_ptr = 0
        offset = LPC_order + subfr_length

        /* Filter input to create the LPC residual for each frame half, and measure subframe energies */
        LPC_res = ShortArray((SilkConstants.MAX_NB_SUBFR shr 1) * offset)
        Inlines.OpusAssert((nb_subfr shr 1) * (SilkConstants.MAX_NB_SUBFR shr 1) == nb_subfr)
        i = 0
        while (i < nb_subfr shr 1) {
            /* Calculate half frame LPC residual signal including preceding samples */
            Filters.silk_LPC_analysis_filter(
                LPC_res,
                0,
                x,
                x_ptr,
                a_Q12[i],
                0,
                (SilkConstants.MAX_NB_SUBFR shr 1) * offset,
                LPC_order
            )

            /* Point to first subframe of the just calculated LPC residual signal */
            LPC_res_ptr = LPC_order
            j = 0
            while (j < SilkConstants.MAX_NB_SUBFR shr 1) {
                /* Measure subframe energy */
                SumSqrShift.silk_sum_sqr_shift(energy, rshift, LPC_res, LPC_res_ptr, subfr_length)
                nrgs[i * (SilkConstants.MAX_NB_SUBFR shr 1) + j] = energy.Val

                /* Set Q values for the measured energy */
                nrgsQ[i * (SilkConstants.MAX_NB_SUBFR shr 1) + j] = 0 - rshift.Val

                /* Move to next subframe */
                LPC_res_ptr += offset
                j++
            }
            /* Move to next frame half */
            x_ptr += (SilkConstants.MAX_NB_SUBFR shr 1) * offset
            i++
        }

        /* Apply the squared subframe gains */
        i = 0
        while (i < nb_subfr) {
            /* Fully upscale gains and energies */
            lz1 = Inlines.silk_CLZ32(nrgs[i]) - 1
            lz2 = Inlines.silk_CLZ32(gains[i]) - 1

            tmp32 = Inlines.silk_LSHIFT32(gains[i], lz2)

            /* Find squared gains */
            tmp32 = Inlines.silk_SMMUL(tmp32, tmp32)
            /* Q( 2 * lz2 - 32 )*/

            /* Scale energies */
            nrgs[i] = Inlines.silk_SMMUL(tmp32, Inlines.silk_LSHIFT32(nrgs[i], lz1))
            /* Q( nrgsQ[ i ] + lz1 + 2 * lz2 - 32 - 32 )*/
            nrgsQ[i] += lz1 + 2 * lz2 - 32 - 32
            i++
        }

    }

    /* Residual energy: nrg = wxx - 2 * wXx * c + c' * wXX * c */
    fun silk_residual_energy16_covar(
        c: ShortArray, /* I    Prediction vector                                                           */
        c_ptr: Int,
        wXX: IntArray, /* I    Correlation matrix                                                          */
        wXX_ptr: Int,
        wXx: IntArray, /* I    Correlation vector                                                          */
        wxx: Int, /* I    Signal energy                                                               */
        D: Int, /* I    Dimension                                                                   */
        cQ: Int /* I    Q value for c vector 0 - 15                                                 */
    ): Int {
        var i: Int
        var j: Int
        var lshifts: Int
        var Qxtra: Int
        var c_max: Int
        val w_max: Int
        var tmp: Int
        var tmp2: Int
        var nrg: Int
        val cn = IntArray(D) //SilkConstants.MAX_MATRIX_SIZE
        var pRow: Int

        /* Safety checks */
        Inlines.OpusAssert(D >= 0)
        Inlines.OpusAssert(D <= 16)
        Inlines.OpusAssert(cQ > 0)
        Inlines.OpusAssert(cQ < 16)

        lshifts = 16 - cQ
        Qxtra = lshifts

        c_max = 0
        i = c_ptr
        while (i < c_ptr + D) {
            c_max = Inlines.silk_max_32(c_max, Inlines.silk_abs(c[i].toInt()))
            i++
        }
        Qxtra = Inlines.silk_min_int(Qxtra, Inlines.silk_CLZ32(c_max) - 17)

        w_max = Inlines.silk_max_32(wXX[wXX_ptr], wXX[wXX_ptr + D * D - 1])
        Qxtra = Inlines.silk_min_int(
            Qxtra,
            Inlines.silk_CLZ32(Inlines.silk_MUL(D, Inlines.silk_RSHIFT(Inlines.silk_SMULWB(w_max, c_max), 4))) - 5
        )
        Qxtra = Inlines.silk_max_int(Qxtra, 0)
        i = 0
        while (i < D) {
            cn[i] = Inlines.silk_LSHIFT(c[c_ptr + i].toInt(), Qxtra)
            Inlines.OpusAssert(Inlines.silk_abs(cn[i]) <= Short.MAX_VALUE + 1)
            i++
            /* Check that Inlines.silk_SMLAWB can be used */
        }
        lshifts -= Qxtra

        /* Compute wxx - 2 * wXx * c */
        tmp = 0
        i = 0
        while (i < D) {
            tmp = Inlines.silk_SMLAWB(tmp, wXx[i], cn[i])
            i++
        }
        nrg = Inlines.silk_RSHIFT(wxx, 1 + lshifts) - tmp
        /* Q: -lshifts - 1 */

        /* Add c' * wXX * c, assuming wXX is symmetric */
        tmp2 = 0
        i = 0
        while (i < D) {
            tmp = 0
            pRow = wXX_ptr + i * D
            j = i + 1
            while (j < D) {
                tmp = Inlines.silk_SMLAWB(tmp, wXX[pRow + j], cn[j])
                j++
            }
            tmp = Inlines.silk_SMLAWB(tmp, Inlines.silk_RSHIFT(wXX[pRow + i], 1), cn[i])
            tmp2 = Inlines.silk_SMLAWB(tmp2, tmp, cn[i])
            i++
        }
        nrg = Inlines.silk_ADD_LSHIFT32(nrg, tmp2, lshifts)
        /* Q: -lshifts - 1 */

        /* Keep one bit free always, because we add them for LSF interpolation */
        if (nrg < 1) {
            nrg = 1
        } else if (nrg > Inlines.silk_RSHIFT(Int.MAX_VALUE, lshifts + 2)) {
            nrg = Int.MAX_VALUE shr 1
        } else {
            nrg = Inlines.silk_LSHIFT(nrg, lshifts + 1)
            /* Q0 */
        }
        return nrg

    }
}
