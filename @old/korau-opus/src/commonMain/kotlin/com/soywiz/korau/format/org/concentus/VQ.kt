/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
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

internal object VQ {

    private val SPREAD_FACTOR = intArrayOf(15, 10, 5)

    fun exp_rotation1(X: IntArray, X_ptr: Int, len: Int, stride: Int, c: Int, s: Int) {
        var i: Int
        val ms: Int
        var Xptr: Int
        Xptr = X_ptr
        ms = Inlines.NEG16(s)
        i = 0
        while (i < len - stride) {
            val x1: Int
            val x2: Int
            x1 = X[Xptr]
            x2 = X[Xptr + stride]
            X[Xptr + stride] =
                    Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MAC16_16(Inlines.MULT16_16(c, x2), s, x1), 15)).toInt()
            X[Xptr] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MAC16_16(Inlines.MULT16_16(c, x1), ms, x2), 15)).toInt()
            Xptr++
            i++
        }
        Xptr = X_ptr + (len - 2 * stride - 1)
        i = len - 2 * stride - 1
        while (i >= 0) {
            val x1: Int
            val x2: Int
            x1 = X[Xptr]
            x2 = X[Xptr + stride]
            X[Xptr + stride] =
                    Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MAC16_16(Inlines.MULT16_16(c, x2), s, x1), 15)).toInt()
            X[Xptr] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MAC16_16(Inlines.MULT16_16(c, x1), ms, x2), 15)).toInt()
            Xptr--
            i--
        }
    }

    fun exp_rotation(X: IntArray, X_ptr: Int, len: Int, dir: Int, stride: Int, K: Int, spread: Int) {
        var len = len
        var i: Int
        val c: Int
        val s: Int
        val gain: Int
        val theta: Int
        var stride2 = 0
        val factor: Int

        if (2 * K >= len || spread == Spread.SPREAD_NONE) {
            return
        }

        factor = SPREAD_FACTOR[spread - 1]

        gain = Inlines.celt_div(Inlines.MULT16_16(CeltConstants.Q15_ONE.toInt(), len).toInt(), len + factor * K)
        theta = Inlines.HALF16(Inlines.MULT16_16_Q15(gain, gain))

        c = Inlines.celt_cos_norm(Inlines.EXTEND32(theta))
        s = Inlines.celt_cos_norm(Inlines.EXTEND32(Inlines.SUB16(CeltConstants.Q15ONE, theta)))
        /*  sin(theta) */

        if (len >= 8 * stride) {
            stride2 = 1
            /* This is just a simple (equivalent) way of computing sqrt(len/stride) with rounding.
               It's basically incrementing long as (stride2+0.5)^2 < len/stride. */
            while ((stride2 * stride2 + stride2) * stride + (stride shr 2) < len) {
                stride2++
            }
        }

        /*NOTE: As a minor optimization, we could be passing around log2(B), not B, for both this and for
           extract_collapse_mask().*/
        len = Inlines.celt_udiv(len, stride)
        i = 0
        while (i < stride) {
            if (dir < 0) {
                if (stride2 != 0) {
                    exp_rotation1(X, X_ptr + i * len, len, stride2, s, c)
                }

                exp_rotation1(X, X_ptr + i * len, len, 1, c, s)
            } else {
                exp_rotation1(X, X_ptr + i * len, len, 1, c, (0 - s).toShort().toInt())

                if (stride2 != 0) {
                    exp_rotation1(X, X_ptr + i * len, len, stride2, s, (0 - c).toShort().toInt())
                }
            }
            i++
        }
    }

    /**
     * Takes the pitch vector and the decoded residual vector, computes the gain
     * that will give ||p+g*y||=1 and mixes the residual with the pitch.
     */
    fun normalise_residual(
        iy: IntArray, X: IntArray, X_ptr: Int,
        N: Int, Ryy: Int, gain: Int
    ) {
        var i: Int
        val k: Int
        val t: Int
        val g: Int

        k = Inlines.celt_ilog2(Ryy) shr 1
        t = Inlines.VSHR32(Ryy, 2 * (k - 7))
        g = Inlines.MULT16_16_P15(Inlines.celt_rsqrt_norm(t), gain)

        i = 0
        do {
            X[X_ptr + i] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MULT16_16(g, iy[i]), k + 1)).toInt()
        } while (++i < N)
    }

    fun extract_collapse_mask(iy: IntArray, N: Int, B: Int): Int {
        var collapse_mask: Int
        val N0: Int
        var i: Int
        if (B <= 1) {
            return 1
        }
        /*NOTE: As a minor optimization, we could be passing around log2(B), not B, for both this and for
           exp_rotation().*/
        N0 = Inlines.celt_udiv(N, B)
        collapse_mask = 0
        i = 0
        do {
            var j: Int
            var tmp = 0
            j = 0
            do {
                tmp = tmp or iy[i * N0 + j]
            } while (++j < N0)

            collapse_mask = collapse_mask or ((if (tmp != 0) 1 else 0) shl i)
        } while (++i < B)

        return collapse_mask
    }

    fun alg_quant(
        X: IntArray, X_ptr: Int, N: Int, K: Int, spread: Int, B: Int, enc: EntropyCoder
    ): Int {
        val y = IntArray(N)
        val iy = IntArray(N)
        val signx = IntArray(N)
        var i: Int
        var j: Int
        val s: Int
        var pulsesLeft: Int
        var sum: Int
        var xy: Int
        var yy: Int
        val collapse_mask: Int

        Inlines.OpusAssert(K > 0, "alg_quant() needs at least one pulse")
        Inlines.OpusAssert(N > 1, "alg_quant() needs at least two dimensions")

        exp_rotation(X, X_ptr, N, 1, B, K, spread)

        /* Get rid of the sign */
        sum = 0
        j = 0
        do {
            if (X[X_ptr + j] > 0) {
                signx[j] = 1
            } else {
                signx[j] = -1
                X[X_ptr + j] = 0 - X[X_ptr + j]
            }

            iy[j] = 0
            y[j] = 0
        } while (++j < N)

        yy = 0
        xy = yy

        pulsesLeft = K

        /* Do a pre-search by projecting on the pyramid */
        if (K > N shr 1) {
            val rcp: Int
            j = 0
            do {
                sum += X[X_ptr + j]
            } while (++j < N)

            /* If X is too small, just replace it with a pulse at 0 */
            /* Prevents infinities and NaNs from causing too many pulses
               to be allocated. 64 is an approximation of infinity here. */
            if (sum <= K) {
                X[X_ptr] = (0.5 + 1.0f * (1 shl 14)).toShort().toInt()/*Inlines.QCONST16(1.0f, 14)*/
                j = X_ptr + 1
                do {
                    X[j] = 0
                } while (++j < N + X_ptr)

                sum = (0.5 + 1.0f * (1 shl 14)).toShort().toInt()/*Inlines.QCONST16(1.0f, 14)*/
            }

            rcp = Inlines.EXTRACT16(Inlines.MULT16_32_Q16(K - 1, Inlines.celt_rcp(sum))).toInt()
            j = 0

            do {
                /* It's really important to round *towards zero* here */
                iy[j] = Inlines.MULT16_16_Q15(X[X_ptr + j], rcp)
                y[j] = iy[j]
                yy = Inlines.MAC16_16(yy, y[j], y[j])
                xy = Inlines.MAC16_16(xy, X[X_ptr + j], y[j])
                y[j] *= 2
                pulsesLeft -= iy[j]
            } while (++j < N)
        }

        Inlines.OpusAssert(pulsesLeft >= 1, "Allocated too many pulses in the quick pass")

        /* This should never happen, but just in case it does (e.g. on silence)
           we fill the first bin with pulses. */
        if (pulsesLeft > N + 3) {
            val tmp = pulsesLeft
            yy = Inlines.MAC16_16(yy, tmp, tmp)
            yy = Inlines.MAC16_16(yy, tmp, y[0])
            iy[0] += pulsesLeft
            pulsesLeft = 0
        }

        s = 1
        i = 0
        while (i < pulsesLeft) {
            var best_id: Int
            var best_num = 0 - CeltConstants.VERY_LARGE16
            var best_den = 0
            val rshift = 1 + Inlines.celt_ilog2(K - pulsesLeft + i + 1)
            best_id = 0
            /* The squared magnitude term gets added anyway, so we might as well
               add it outside the loop */
            yy = Inlines.ADD16(yy, 1) // opus bug - was add32
            j = 0
            do {
                var Rxy: Int
                val Ryy: Int
                /* Temporary sums of the new pulse(s) */
                Rxy = Inlines.EXTRACT16(Inlines.SHR32(Inlines.ADD32(xy, Inlines.EXTEND32(X[X_ptr + j])), rshift))
                    .toInt()
                /* We're multiplying y[j] by two so we don't have to do it here */
                Ryy = Inlines.ADD16(yy, y[j])

                /* Approximate score: we maximise Rxy/sqrt(Ryy) (we're guaranteed that
                   Rxy is positive because the sign is pre-computed) */
                Rxy = Inlines.MULT16_16_Q15(Rxy, Rxy)
                /* The idea is to check for num/den >= best_num/best_den, but that way
                   we can do it without any division */
                /* OPT: Make sure to use conditional moves here */
                if (Inlines.MULT16_16(best_den, Rxy) > Inlines.MULT16_16(Ryy, best_num)) {
                    best_den = Ryy
                    best_num = Rxy
                    best_id = j
                }
            } while (++j < N)

            /* Updating the sums of the new pulse(s) */
            xy = Inlines.ADD32(xy, Inlines.EXTEND32(X[X_ptr + best_id]))
            /* We're multiplying y[j] by two so we don't have to do it here */
            yy = Inlines.ADD16(yy, y[best_id])

            /* Only now that we've made the final choice, update y/iy */
            /* Multiplying y[j] by 2 so we don't have to do it everywhere else */
            y[best_id] = y[best_id] + 2 * s
            iy[best_id]++
            i++
        }

        /* Put the original sign back */
        j = 0
        do {
            X[X_ptr + j] = Inlines.MULT16_16(signx[j], X[X_ptr + j])
            if (signx[j] < 0) {
                iy[j] = -iy[j]
            }
        } while (++j < N)

        CWRS.encode_pulses(iy, N, K, enc)

        collapse_mask = extract_collapse_mask(iy, N, B)

        return collapse_mask
    }

    /**
     * Decode pulse vector and combine the result with the pitch vector to
     * produce the final normalised signal in the current band.
     */
    fun alg_unquant(
        X: IntArray, X_ptr: Int, N: Int, K: Int, spread: Int, B: Int,
        dec: EntropyCoder, gain: Int
    ): Int {
        val Ryy: Int
        val collapse_mask: Int
        val iy = IntArray(N)
        Inlines.OpusAssert(K > 0, "alg_unquant() needs at least one pulse")
        Inlines.OpusAssert(N > 1, "alg_unquant() needs at least two dimensions")
        Ryy = CWRS.decode_pulses(iy, N, K, dec)
        normalise_residual(iy, X, X_ptr, N, Ryy, gain)
        exp_rotation(X, X_ptr, N, -1, B, K, spread)
        collapse_mask = extract_collapse_mask(iy, N, B)

        return collapse_mask
    }

    fun renormalise_vector(X: IntArray, X_ptr: Int, N: Int, gain: Int) {
        var i: Int
        val k: Int
        val E: Int
        val g: Int
        val t: Int
        var xptr: Int
        E = CeltConstants.EPSILON + Kernels.celt_inner_prod(X, X_ptr, X, X_ptr, N)
        k = Inlines.celt_ilog2(E) shr 1
        t = Inlines.VSHR32(E, 2 * (k - 7))
        g = Inlines.MULT16_16_P15(Inlines.celt_rsqrt_norm(t), gain)

        xptr = X_ptr
        i = 0
        while (i < N) {
            X[xptr] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MULT16_16(g, X[xptr]), k + 1)).toInt()
            xptr++
            i++
        }
        /*return celt_sqrt(E);*/
    }

    fun stereo_itheta(X: IntArray, X_ptr: Int, Y: IntArray, Y_ptr: Int, stereo: Int, N: Int): Int {
        var i: Int
        val itheta: Int
        val mid: Int
        val side: Int
        var Emid: Int
        var Eside: Int

        Eside = CeltConstants.EPSILON
        Emid = Eside
        if (stereo != 0) {
            i = 0
            while (i < N) {
                val m: Int
                val s: Int
                m = Inlines.ADD16(Inlines.SHR16(X[X_ptr + i], 1), Inlines.SHR16(Y[Y_ptr + i], 1))
                s = Inlines.SUB16(Inlines.SHR16(X[X_ptr + i], 1), Inlines.SHR16(Y[Y_ptr + i], 1))
                Emid = Inlines.MAC16_16(Emid, m, m)
                Eside = Inlines.MAC16_16(Eside, s, s)
                i++
            }
        } else {
            Emid += Kernels.celt_inner_prod(X, X_ptr, X, X_ptr, N)
            Eside += Kernels.celt_inner_prod(Y, Y_ptr, Y, Y_ptr, N)
        }
        mid = Inlines.celt_sqrt(Emid)
        side = Inlines.celt_sqrt(Eside)
        /* 0.63662 = 2/pi */
        itheta = Inlines.MULT16_16_Q15(
            (0.5 + 0.63662f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(0.63662f, 15)*/,
            Inlines.celt_atan2p(side, mid)
        )

        return itheta
    }
}
