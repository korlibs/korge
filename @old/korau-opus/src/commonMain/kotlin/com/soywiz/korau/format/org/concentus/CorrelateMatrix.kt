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

/**
 * ********************************************************************
 * Correlation Matrix Computations for LS estimate.
 *
 */
internal object CorrelateMatrix {

    /* Calculates correlation vector X'*t */
    fun silk_corrVector(
        x: ShortArray, /* I    x vector [L + order - 1] used to form data matrix X                         */
        x_ptr: Int,
        t: ShortArray, /* I    Target vector [L]                                                           */
        t_ptr: Int,
        L: Int, /* I    Length of vectors                                                           */
        order: Int, /* I    Max lag for correlation                                                     */
        Xt: IntArray, /* O    Pointer to X'*t correlation vector [order]                                  */
        rshifts: Int /* I    Right shifts of correlations                                                */
    ) {
        var lag: Int
        var i: Int
        var ptr1: Int
        val ptr2: Int
        var inner_prod: Int

        ptr1 = x_ptr + order - 1
        /* Points to first sample of column 0 of X: X[:,0] */
        ptr2 = t_ptr
        /* Calculate X'*t */
        if (rshifts > 0) {
            /* Right shifting used */
            lag = 0
            while (lag < order) {
                inner_prod = 0
                i = 0
                while (i < L) {
                    inner_prod += Inlines.silk_RSHIFT32(
                        Inlines.silk_SMULBB(x[ptr1 + i].toInt(), t[ptr2 + i].toInt()),
                        rshifts
                    )
                    i++
                }
                Xt[lag] = inner_prod
                /* X[:,lag]'*t */
                ptr1--
                lag++
                /* Go to next column of X */
            }
        } else {
            Inlines.OpusAssert(rshifts == 0)
            lag = 0
            while (lag < order) {
                Xt[lag] = Inlines.silk_inner_prod(x, ptr1, t, ptr2, L)
                /* X[:,lag]'*t */
                ptr1--
                lag++
                /* Go to next column of X */
            }
        }
    }

    /* Calculates correlation matrix X'*X */
    fun silk_corrMatrix(
        x: ShortArray, /* I    x vector [L + order - 1] used to form data matrix X                         */
        x_ptr: Int,
        L: Int, /* I    Length of vectors                                                           */
        order: Int, /* I    Max lag for correlation                                                     */
        head_room: Int, /* I    Desired headroom                                                            */
        XX: IntArray, /* O    Pointer to X'*X correlation matrix [ order x order ]                        */
        XX_ptr: Int,
        rshifts: BoxedValueInt /* I/O  Right shifts of correlations                                                */
    ) {
        var i: Int
        var j: Int
        var lag: Int
        val head_room_rshifts: Int
        var energy: Int
        var rshifts_local: Int
        val ptr1: Int
        var ptr2: Int

        /* Calculate energy to find shift used to fit in 32 bits */
        val boxed_energy = BoxedValueInt(0)
        val boxed_rshifts_local = BoxedValueInt(0)
        SumSqrShift.silk_sum_sqr_shift(boxed_energy, boxed_rshifts_local, x, x_ptr, L + order - 1)
        energy = boxed_energy.Val
        rshifts_local = boxed_rshifts_local.Val

        /* Add shifts to get the desired head room */
        head_room_rshifts = Inlines.silk_max(head_room - Inlines.silk_CLZ32(energy), 0)

        energy = Inlines.silk_RSHIFT32(energy, head_room_rshifts)
        rshifts_local += head_room_rshifts

        /* Calculate energy of first column (0) of X: X[:,0]'*X[:,0] */
        /* Remove contribution of first order - 1 samples */
        i = x_ptr
        while (i < x_ptr + order - 1) {
            energy -= Inlines.silk_RSHIFT32(Inlines.silk_SMULBB(x[i].toInt(), x[i].toInt()), rshifts_local)
            i++
        }
        if (rshifts_local < rshifts.Val) {
            /* Adjust energy */
            energy = Inlines.silk_RSHIFT32(energy, rshifts.Val - rshifts_local)
            rshifts_local = rshifts.Val
        }

        /* Calculate energy of remaining columns of X: X[:,j]'*X[:,j] */
        /* Fill out the diagonal of the correlation matrix */
        Inlines.MatrixSet(XX, XX_ptr, 0, 0, order, energy)
        ptr1 = x_ptr + order - 1
        /* First sample of column 0 of X */
        j = 1
        while (j < order) {
            energy = Inlines.silk_SUB32(
                energy,
                Inlines.silk_RSHIFT32(
                    Inlines.silk_SMULBB(x[ptr1 + L - j].toInt(), x[ptr1 + L - j].toInt()),
                    rshifts_local
                )
            )
            energy = Inlines.silk_ADD32(
                energy,
                Inlines.silk_RSHIFT32(Inlines.silk_SMULBB(x[ptr1 - j].toInt(), x[ptr1 - j].toInt()), rshifts_local)
            )
            Inlines.MatrixSet(XX, XX_ptr, j, j, order, energy)
            j++
        }

        ptr2 = x_ptr + order - 2
        /* First sample of column 1 of X */
        /* Calculate the remaining elements of the correlation matrix */
        if (rshifts_local > 0) {
            /* Right shifting used */
            lag = 1
            while (lag < order) {
                /* Inner product of column 0 and column lag: X[:,0]'*X[:,lag] */
                energy = 0
                i = 0
                while (i < L) {
                    energy += Inlines.silk_RSHIFT32(
                        Inlines.silk_SMULBB(x[ptr1 + i].toInt(), x[ptr2 + i].toInt()),
                        rshifts_local
                    )
                    i++
                }
                /* Calculate remaining off diagonal: X[:,j]'*X[:,j + lag] */
                Inlines.MatrixSet(XX, XX_ptr, lag, 0, order, energy)
                Inlines.MatrixSet(XX, XX_ptr, 0, lag, order, energy)
                j = 1
                while (j < order - lag) {
                    energy = Inlines.silk_SUB32(
                        energy,
                        Inlines.silk_RSHIFT32(
                            Inlines.silk_SMULBB(x[ptr1 + L - j].toInt(), x[ptr2 + L - j].toInt()),
                            rshifts_local
                        )
                    )
                    energy = Inlines.silk_ADD32(
                        energy,
                        Inlines.silk_RSHIFT32(
                            Inlines.silk_SMULBB(x[ptr1 - j].toInt(), x[ptr2 - j].toInt()),
                            rshifts_local
                        )
                    )
                    Inlines.MatrixSet(XX, XX_ptr, lag + j, j, order, energy)
                    Inlines.MatrixSet(XX, XX_ptr, j, lag + j, order, energy)
                    j++
                }
                ptr2--
                lag++
                /* Update pointer to first sample of next column (lag) in X */
            }
        } else {
            lag = 1
            while (lag < order) {
                /* Inner product of column 0 and column lag: X[:,0]'*X[:,lag] */
                energy = Inlines.silk_inner_prod(x, ptr1, x, ptr2, L)
                Inlines.MatrixSet(XX, XX_ptr, lag, 0, order, energy)
                Inlines.MatrixSet(XX, XX_ptr, 0, lag, order, energy)
                /* Calculate remaining off diagonal: X[:,j]'*X[:,j + lag] */
                j = 1
                while (j < order - lag) {
                    energy = Inlines.silk_SUB32(
                        energy,
                        Inlines.silk_SMULBB(x[ptr1 + L - j].toInt(), x[ptr2 + L - j].toInt())
                    )
                    energy = Inlines.silk_SMLABB(energy, x[ptr1 - j].toInt(), x[ptr2 - j].toInt())
                    Inlines.MatrixSet(XX, XX_ptr, lag + j, j, order, energy)
                    Inlines.MatrixSet(XX, XX_ptr, j, lag + j, order, energy)
                    j++
                }
                ptr2--/* Update pointer to first sample of next column (lag) in X */
                lag++
            }
        }
        rshifts.Val = rshifts_local
    }
}
