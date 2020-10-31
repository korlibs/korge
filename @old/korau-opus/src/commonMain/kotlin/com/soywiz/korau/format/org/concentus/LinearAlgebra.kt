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

internal object LinearAlgebra {

    /* Solves Ax = b, assuming A is symmetric */
    fun silk_solve_LDL(
        A: IntArray, /* I    Pointer to symetric square matrix A                                         */
        A_ptr: Int,
        M: Int, /* I    Size of matrix                                                              */
        b: IntArray, /* I    Pointer to b vector                                                         */
        x_Q16: IntArray /* O    Pointer to x solution vector                                                */
    ) {
        Inlines.OpusAssert(M <= SilkConstants.MAX_MATRIX_SIZE)
        val L_Q16 = IntArray(M * M)
        val Y = IntArray(SilkConstants.MAX_MATRIX_SIZE)

        // [Porting note] This is an interleaved array. Formerly it was an array of data structures laid out thus:
        //private struct inv_D_t
        //{
        //    int Q36_part;
        //    int Q48_part;
        //}
        val inv_D = IntArray(SilkConstants.MAX_MATRIX_SIZE * 2)

        /**
         * *************************************************
         * Factorize A by LDL such that A = L*D*L', where L is lower triangular
         * with ones on diagonal
         *
         */
        silk_LDL_factorize(A, A_ptr, M, L_Q16, inv_D)

        /**
         * **************************************************
         * substitute D*L'*x = Y. ie: L*D*L'*x = b => L*Y = b <=> Y = inv(L)*b
         *
         */
        silk_LS_SolveFirst(L_Q16, M, b, Y)

        /**
         * **************************************************
         * D*L'*x = Y <=> L'*x = inv(D)*Y, because D is diagonal just multiply
         * with 1/d_i
         *
         */
        silk_LS_divide_Q16(Y, inv_D, M)

        /**
         * **************************************************
         * x = inv(L') * inv(D) * Y
         *
         */
        silk_LS_SolveLast(L_Q16, M, Y, x_Q16)

    }

    /* Factorize square matrix A into LDL form */
    private fun silk_LDL_factorize(
        A: IntArray, /* I/O Pointer to Symetric Square Matrix                            */
        A_ptr: Int,
        M: Int, /* I   Size of Matrix                                               */
        L_Q16: IntArray, /* I/O Pointer to Square Upper triangular Matrix                    */
        inv_D: IntArray /* I/O Pointer to vector holding inverted diagonal elements of D    */
    ) {
        var i: Int
        var j: Int
        var k: Int
        var status: Int
        var loop_count: Int
        var scratch1: IntArray
        var scratch1_ptr: Int
        var scratch2: IntArray
        var scratch2_ptr: Int
        val diag_min_value: Int
        var tmp_32: Int
        var err: Int
        val v_Q0 = IntArray(M)
        /*SilkConstants.MAX_MATRIX_SIZE*/
        val D_Q0 = IntArray(M)
        /*SilkConstants.MAX_MATRIX_SIZE*/
        var one_div_diag_Q36: Int
        var one_div_diag_Q40: Int
        var one_div_diag_Q48: Int

        Inlines.OpusAssert(M <= SilkConstants.MAX_MATRIX_SIZE)

        status = 1
        diag_min_value = Inlines.silk_max_32(
            Inlines.silk_SMMUL(
                Inlines.silk_ADD_SAT32(
                    A[A_ptr],
                    A[A_ptr + Inlines.silk_SMULBB(M, M) - 1]
                ),
                (TuningParameters.FIND_LTP_COND_FAC * (1.toLong() shl 31) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.FIND_LTP_COND_FAC, 31)*/
            ), 1 shl 9
        )
        loop_count = 0
        while (loop_count < M && status == 1) {
            status = 0
            j = 0
            while (j < M) {
                scratch1 = L_Q16
                scratch1_ptr = Inlines.MatrixGetPointer(j, 0, M)
                tmp_32 = 0
                i = 0
                while (i < j) {
                    v_Q0[i] = Inlines.silk_SMULWW(D_Q0[i], scratch1[scratch1_ptr + i])
                    /* Q0 */
                    tmp_32 = Inlines.silk_SMLAWW(tmp_32, v_Q0[i], scratch1[scratch1_ptr + i])
                    i++
                    /* Q0 */
                }
                tmp_32 = Inlines.silk_SUB32(Inlines.MatrixGet(A, A_ptr, j, j, M), tmp_32)

                if (tmp_32 < diag_min_value) {
                    tmp_32 = Inlines.silk_SUB32(Inlines.silk_SMULBB(loop_count + 1, diag_min_value), tmp_32)
                    /* Matrix not positive semi-definite, or ill conditioned */
                    i = 0
                    while (i < M) {
                        Inlines.MatrixSet(
                            A,
                            A_ptr,
                            i,
                            i,
                            M,
                            Inlines.silk_ADD32(Inlines.MatrixGet(A, A_ptr, i, i, M), tmp_32)
                        )
                        i++
                    }
                    status = 1
                    break
                }
                D_Q0[j] = tmp_32
                /* always < max(Correlation) */

                /* two-step division */
                one_div_diag_Q36 = Inlines.silk_INVERSE32_varQ(tmp_32, 36)
                /* Q36 */
                one_div_diag_Q40 = Inlines.silk_LSHIFT(one_div_diag_Q36, 4)
                /* Q40 */
                err = Inlines.silk_SUB32(1 shl 24, Inlines.silk_SMULWW(tmp_32, one_div_diag_Q40))
                /* Q24 */
                one_div_diag_Q48 = Inlines.silk_SMULWW(err, one_div_diag_Q40)
                /* Q48 */

                /* Save 1/Ds */
                inv_D[j * 2 + 0] = one_div_diag_Q36
                inv_D[j * 2 + 1] = one_div_diag_Q48

                Inlines.MatrixSet(L_Q16, j, j, M, 65536)
                /* 1.0 in Q16 */
                scratch1 = A
                scratch1_ptr = Inlines.MatrixGetPointer(j, 0, M) + A_ptr
                scratch2 = L_Q16
                scratch2_ptr = Inlines.MatrixGetPointer(j + 1, 0, M)
                i = j + 1
                while (i < M) {
                    tmp_32 = 0
                    k = 0
                    while (k < j) {
                        tmp_32 = Inlines.silk_SMLAWW(tmp_32, v_Q0[k], scratch2[scratch2_ptr + k])
                        k++
                        /* Q0 */
                    }
                    tmp_32 = Inlines.silk_SUB32(scratch1[scratch1_ptr + i], tmp_32)
                    /* always < max(Correlation) */

                    /* tmp_32 / D_Q0[j] : Divide to Q16 */
                    Inlines.MatrixSet(
                        L_Q16, i, j, M, Inlines.silk_ADD32(
                            Inlines.silk_SMMUL(tmp_32, one_div_diag_Q48),
                            Inlines.silk_RSHIFT(Inlines.silk_SMULWW(tmp_32, one_div_diag_Q36), 4)
                        )
                    )

                    /* go to next column */
                    scratch2_ptr += M
                    i++
                }
                j++
            }
            loop_count++
        }

        Inlines.OpusAssert(status == 0)
    }

    private fun silk_LS_divide_Q16(
        T: IntArray, /* I/O  Numenator vector                                            */
        inv_D: IntArray, /* I    1 / D vector                                                */
        M: Int /* I    dimension                                                   */
    ) {
        var i: Int
        var tmp_32: Int
        var one_div_diag_Q36: Int
        var one_div_diag_Q48: Int

        i = 0
        while (i < M) {
            one_div_diag_Q36 = inv_D[i * 2 + 0]
            one_div_diag_Q48 = inv_D[i * 2 + 1]

            tmp_32 = T[i]
            T[i] = Inlines.silk_ADD32(
                Inlines.silk_SMMUL(tmp_32, one_div_diag_Q48),
                Inlines.silk_RSHIFT(Inlines.silk_SMULWW(tmp_32, one_div_diag_Q36), 4)
            )
            i++
        }
    }

    /* Solve Lx = b, when L is lower triangular and has ones on the diagonal */
    private fun silk_LS_SolveFirst(
        L_Q16: IntArray, /* I    Pointer to Lower Triangular Matrix                          */
        M: Int, /* I    Dim of Matrix equation                                      */
        b: IntArray, /* I    b Vector                                                    */
        x_Q16: IntArray /* O    x Vector                                                    */
    ) {
        var i: Int
        var j: Int
        var ptr32: Int
        var tmp_32: Int

        i = 0
        while (i < M) {
            ptr32 = Inlines.MatrixGetPointer(i, 0, M)
            tmp_32 = 0
            j = 0
            while (j < i) {
                tmp_32 = Inlines.silk_SMLAWW(tmp_32, L_Q16[ptr32 + j], x_Q16[j])
                j++
            }
            x_Q16[i] = Inlines.silk_SUB32(b[i], tmp_32)
            i++
        }
    }

    /* Solve L^t*x = b, where L is lower triangular with ones on the diagonal */
    private fun silk_LS_SolveLast(
        L_Q16: IntArray, /* I    Pointer to Lower Triangular Matrix                          */
        M: Int, /* I    Dim of Matrix equation                                      */
        b: IntArray, /* I    b Vector                                                    */
        x_Q16: IntArray /* O    x Vector                                                    */
    ) {
        var i: Int
        var j: Int
        var ptr32: Int
        var tmp_32: Int

        i = M - 1
        while (i >= 0) {
            ptr32 = Inlines.MatrixGetPointer(0, i, M)
            tmp_32 = 0
            j = M - 1
            while (j > i) {
                tmp_32 = Inlines.silk_SMLAWW(tmp_32, L_Q16[ptr32 + Inlines.silk_SMULBB(j, M)], x_Q16[j])
                j--
            }
            x_Q16[i] = Inlines.silk_SUB32(b[i], tmp_32)
            i--
        }
    }
}
