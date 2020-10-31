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

internal object Laplace {

    /* The minimum probability of an energy delta (out of 32768). */
    private val LAPLACE_LOG_MINP = 0
    private val LAPLACE_MINP = (1 shl LAPLACE_LOG_MINP).toLong()
    /* The minimum number of guaranteed representable energy deltas (in one
        direction). */
    private val LAPLACE_NMIN = 16

    /* When called, decay is positive and at most 11456. */
    fun ec_laplace_get_freq1(fs0: Long, decay: Int): Long {
        val ft = Inlines.CapToUInt32(32768 - LAPLACE_MINP * (2 * LAPLACE_NMIN) - fs0)
        return Inlines.CapToUInt32(ft * (16384 - decay)) shr 15
    }

    fun ec_laplace_encode(enc: EntropyCoder, value: BoxedValueInt, fs: Long, decay: Int) {
        var fs = fs
        var fl: Long
        var `val` = value.Val
        fl = 0
        if (`val` != 0) {
            val s: Int
            var i: Int
            s = 0 - if (`val` < 0) 1 else 0
            `val` = `val` + s xor s
            fl = fs
            fs = ec_laplace_get_freq1(fs, decay)

            /* Search the decaying part of the PDF.*/
            i = 1
            while (fs > 0 && i < `val`) {
                fs *= 2
                fl = Inlines.CapToUInt32(fl + fs + 2 * LAPLACE_MINP)
                fs = Inlines.CapToUInt32(fs * decay shr 15)
                i++
            }

            /* Everything beyond that has probability LAPLACE_MINP. */
            if (fs == 0L) {
                val di: Int
                var ndi_max: Int
                ndi_max = (32768 - fl + LAPLACE_MINP - 1).toInt() shr LAPLACE_LOG_MINP
                ndi_max = ndi_max - s shr 1
                di = Inlines.IMIN(`val` - i, ndi_max - 1)
                fl = Inlines.CapToUInt32(fl + (2 * di + 1 + s) * LAPLACE_MINP)
                fs = Inlines.IMIN(LAPLACE_MINP, 32768 - fl)
                value.Val = i + di + s xor s
            } else {
                fs += LAPLACE_MINP
                fl = fl + Inlines.CapToUInt32(fs and s.inv().toLong())
            }
            Inlines.OpusAssert(fl + fs <= 32768)
            Inlines.OpusAssert(fs > 0)
        }

        enc.encode_bin(fl, fl + fs, 15)
    }

    fun ec_laplace_decode(dec: EntropyCoder, fs: Long, decay: Int): Int {
        var fs = fs
        var `val` = 0
        var fl: Long
        val fm: Long
        fm = dec.decode_bin(15)
        fl = 0

        if (fm >= fs) {
            `val`++
            fl = fs
            fs = ec_laplace_get_freq1(fs, decay) + LAPLACE_MINP
            /* Search the decaying part of the PDF.*/
            while (fs > LAPLACE_MINP && fm >= fl + 2 * fs) {
                fs *= 2
                fl = Inlines.CapToUInt32(fl + fs)
                fs = Inlines.CapToUInt32((fs - 2 * LAPLACE_MINP) * decay shr 15)
                fs += LAPLACE_MINP
                `val`++
            }
            /* Everything beyond that has probability LAPLACE_MINP. */
            if (fs <= LAPLACE_MINP) {
                val di: Int
                di = (fm - fl).toInt() shr LAPLACE_LOG_MINP + 1
                `val` += di
                fl = Inlines.CapToUInt32(fl + Inlines.CapToUInt32(2 * di.toLong() * LAPLACE_MINP))
            }
            if (fm < fl + fs) {
                `val` = -`val`
            } else {
                fl = Inlines.CapToUInt32(fl + fs)
            }
        }

        Inlines.OpusAssert(fl < 32768)
        Inlines.OpusAssert(fs > 0)
        Inlines.OpusAssert(fl <= fm)
        Inlines.OpusAssert(fm < Inlines.IMIN(fl + fs, 32768))

        dec.dec_update(fl, Inlines.IMIN(fl + fs, 32768), 32768)
        return `val`
    }
}
