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

internal object Rate {

    private val LOG2_FRAC_TABLE =
        byteArrayOf(0, 8, 13, 16, 19, 21, 23, 24, 26, 27, 28, 29, 30, 31, 32, 32, 33, 34, 34, 35, 36, 36, 37, 37)

    private val ALLOC_STEPS = 6

    fun get_pulses(i: Int): Int {
        return if (i < 8) i else 8 + (i and 7) shl (i shr 3) - 1
    }

    fun bits2pulses(m: CeltMode, band: Int, LM: Int, bits: Int): Int {
        var LM = LM
        var bits = bits
        var i: Int
        var lo: Int
        var hi: Int

        LM++
        val cache = m.cache.bits
        val cache_ptr = m.cache.index!![LM * m.nbEBands + band].toInt()

        lo = 0
        hi = cache!![cache_ptr].toInt()
        bits--
        i = 0
        while (i < CeltConstants.LOG_MAX_PSEUDO) {
            val mid = lo + hi + 1 shr 1
            /* OPT: Make sure this is implemented with a conditional move */
            if (cache!![cache_ptr + mid].toInt() >= bits) {
                hi = mid
            } else {
                lo = mid
            }
            i++
        }
        return if (bits - (if (lo == 0) -1 else cache!![cache_ptr + lo].toInt()) <= cache!![cache_ptr + hi].toInt() - bits) {
            lo
        } else {
            hi
        }
    }

    fun pulses2bits(m: CeltMode, band: Int, LM: Int, pulses: Int): Int {
        var LM = LM
        LM++
        return if (pulses == 0) 0 else m.cache.bits!![m.cache.index!![LM * m.nbEBands + band] + pulses] + 1
    }

    fun interp_bits2pulses(
        m: CeltMode,
        start: Int,
        end: Int,
        skip_start: Int,
        bits1: IntArray,
        bits2: IntArray,
        thresh: IntArray,
        cap: IntArray,
        total: Int,
        _balance: BoxedValueInt,
        skip_rsv: Int,
        intensity: BoxedValueInt,
        intensity_rsv: Int,
        dual_stereo: BoxedValueInt,
        dual_stereo_rsv: Int,
        bits: IntArray,
        ebits: IntArray,
        fine_priority: IntArray,
        C: Int,
        LM: Int,
        ec: EntropyCoder,
        encode: Int,
        prev: Int,
        signalBandwidth: Int
    ): Int {
        var total = total
        var intensity_rsv = intensity_rsv
        var dual_stereo_rsv = dual_stereo_rsv
        var psum: Int
        var lo: Int
        var hi: Int
        var i: Int
        var j: Int
        val logM: Int
        val stereo: Int
        var codedBands = -1
        val alloc_floor: Int
        var left: Int
        var percoeff: Int
        var done: Int
        var balance: Int

        alloc_floor = C shl EntropyCoder.BITRES
        stereo = if (C > 1) 1 else 0

        logM = LM shl EntropyCoder.BITRES
        lo = 0
        hi = 1 shl ALLOC_STEPS
        i = 0
        while (i < ALLOC_STEPS) {
            val mid = lo + hi shr 1
            psum = 0
            done = 0
            j = end
            while (j-- > start) {
                val tmp = bits1[j] + (mid * bits2[j] shr ALLOC_STEPS)
                if (tmp >= thresh[j] || done != 0) {
                    done = 1
                    /* Don't allocate more than we can actually use */
                    psum += Inlines.IMIN(tmp, cap[j])
                } else if (tmp >= alloc_floor) {
                    psum += alloc_floor
                }
            }
            if (psum > total) {
                hi = mid
            } else {
                lo = mid
            }
            i++
        }
        psum = 0
        /*printf ("interp bisection gave %d\n", lo);*/
        done = 0
        j = end
        while (j-- > start) {
            var tmp = bits1[j] + (lo * bits2[j] shr ALLOC_STEPS)
            if (tmp < thresh[j] && done == 0) {
                if (tmp >= alloc_floor) {
                    tmp = alloc_floor
                } else {
                    tmp = 0
                }
            } else {
                done = 1
            }

            /* Don't allocate more than we can actually use */
            tmp = Inlines.IMIN(tmp, cap[j])
            bits[j] = tmp
            psum += tmp
        }

        /* Decide which bands to skip, working backwards from the end. */
        codedBands = end
        while (true) {
            val band_width: Int
            var band_bits: Int
            val rem: Int
            j = codedBands - 1
            /* Never skip the first band, nor a band that has been boosted by
                dynalloc.
               In the first case, we'd be coding a bit to signal we're going to waste
                all the other bits.
               In the second case, we'd be coding a bit to redistribute all the bits
                we just signaled should be cocentrated in this band. */
            if (j <= skip_start) {
                /* Give the bit we reserved to end skipping back. */
                total += skip_rsv
                break
            }

            /*Figure out how many left-over bits we would be adding to this band.
              This can include bits we've stolen back from higher, skipped bands.*/
            left = total - psum
            percoeff = Inlines.celt_udiv(left, m.eBands!![codedBands] - m.eBands!![start])
            left -= (m.eBands!![codedBands] - m.eBands!![start]) * percoeff
            rem = Inlines.IMAX(left - (m.eBands!![j] - m.eBands!![start]), 0)
            band_width = m.eBands!![codedBands] - m.eBands!![j]
            band_bits = bits[j] + percoeff * band_width + rem
            /*Only code a skip decision if we're above the threshold for this band.
              Otherwise it is force-skipped.
              This ensures that we have enough bits to code the skip flag.*/
            if (band_bits >= Inlines.IMAX(thresh[j], alloc_floor + (1 shl EntropyCoder.BITRES))) {
                if (encode != 0) {
                    /*This if() block is the only part of the allocation function that
                       is not a mandatory part of the bitstream: any bands we choose to
                       skip here must be explicitly signaled.*/
                    /*Choose a threshold with some hysteresis to keep bands from
                       fluctuating in and out.*/
                    if (codedBands <= start + 2 || band_bits > (if (j < prev) 7 else 9) * band_width shl LM shl EntropyCoder.BITRES shr 4 && j <= signalBandwidth) {
                        ec.enc_bit_logp(1, 1)
                        break
                    }
                    ec.enc_bit_logp(0, 1)
                } else if (ec.dec_bit_logp(1) != 0) {
                    break
                }
                /*We used a bit to skip this band.*/
                psum += 1 shl EntropyCoder.BITRES
                band_bits -= 1 shl EntropyCoder.BITRES
            }
            /*Reclaim the bits originally allocated to this band.*/
            psum -= bits[j] + intensity_rsv
            if (intensity_rsv > 0) {
                intensity_rsv = LOG2_FRAC_TABLE[j - start].toInt()
            }
            psum += intensity_rsv
            if (band_bits >= alloc_floor) {
                /*If we have enough for a fine energy bit per channel, use it.*/
                psum += alloc_floor
                bits[j] = alloc_floor
            } else {
                /*Otherwise this band gets nothing at all.*/
                bits[j] = 0
            }
            codedBands--
        }

        Inlines.OpusAssert(codedBands > start)
        /* Code the intensity and dual stereo parameters. */
        if (intensity_rsv > 0) {
            if (encode != 0) {
                intensity.Val = Inlines.IMIN(intensity.Val, codedBands)
                ec.enc_uint((intensity.Val - start).toLong(), (codedBands + 1 - start).toLong())
            } else {
                intensity.Val = start + ec.dec_uint((codedBands + 1 - start).toLong()).toInt()
            }
        } else {
            intensity.Val = 0
        }

        if (intensity.Val <= start) {
            total += dual_stereo_rsv
            dual_stereo_rsv = 0
        }
        if (dual_stereo_rsv > 0) {
            if (encode != 0) {
                ec.enc_bit_logp(dual_stereo.Val, 1)
            } else {
                dual_stereo.Val = ec.dec_bit_logp(1)
            }
        } else {
            dual_stereo.Val = 0
        }

        /* Allocate the remaining bits */
        left = total - psum
        percoeff = Inlines.celt_udiv(left, m.eBands!![codedBands] - m.eBands!![start])
        left -= (m.eBands!![codedBands] - m.eBands!![start]) * percoeff
        j = start
        while (j < codedBands) {
            bits[j] += percoeff * (m.eBands!![j + 1] - m.eBands!![j])
            j++
        }
        j = start
        while (j < codedBands) {
            val tmp = Inlines.IMIN(left, m.eBands!![j + 1] - m.eBands!![j]).toInt()
            bits[j] += tmp
            left -= tmp
            j++
        }
        /*for (j=0;j<end;j++)printf("%d ", bits[j]);printf("\n");*/

        balance = 0
        j = start
        while (j < codedBands) {
            val N0: Int
            val N: Int
            val den: Int
            var offset: Int
            val NClogN: Int
            var excess: Int
            val bit: Int

            Inlines.OpusAssert(bits[j] >= 0)
            N0 = m.eBands!![j + 1] - m.eBands!![j]
            N = N0 shl LM
            bit = bits[j] + balance

            if (N > 1) {
                excess = Inlines.MAX32(bit - cap[j], 0)
                bits[j] = bit - excess

                /* Compensate for the extra DoF in stereo */
                den = C * N + if (C == 2 && N > 2 && dual_stereo.Val == 0 && j < intensity.Val) 1 else 0

                NClogN = den * (m.logN!![j] + logM)

                /* Offset for the number of fine bits by log2(N)/2 + FINE_OFFSET
                   compared to their "fair share" of total/N */
                offset = (NClogN shr 1) - den * CeltConstants.FINE_OFFSET

                /* N=2 is the only point that doesn't match the curve */
                if (N == 2) {
                    offset += den shl EntropyCoder.BITRES shr 2
                }

                /* Changing the offset for allocating the second and third
                    fine energy bit */
                if (bits[j] + offset < den * 2 shl EntropyCoder.BITRES) {
                    offset += NClogN shr 2
                } else if (bits[j] + offset < den * 3 shl EntropyCoder.BITRES) {
                    offset += NClogN shr 3
                }

                /* Divide with rounding */
                ebits[j] = Inlines.IMAX(0, bits[j] + offset + (den shl EntropyCoder.BITRES - 1))
                ebits[j] = Inlines.celt_udiv(ebits[j], den) shr EntropyCoder.BITRES

                /* Make sure not to bust */
                if (C * ebits[j] > bits[j] shr EntropyCoder.BITRES) {
                    ebits[j] = bits[j] shr stereo shr EntropyCoder.BITRES
                }

                /* More than that is useless because that's about as far as PVQ can go */
                ebits[j] = Inlines.IMIN(ebits[j], CeltConstants.MAX_FINE_BITS)

                /* If we rounded down or capped this band, make it a candidate for the
                    final fine energy pass */
                fine_priority[j] = if (ebits[j] * (den shl EntropyCoder.BITRES) >= bits[j] + offset) 1 else 0

                /* Remove the allocated fine bits; the rest are assigned to PVQ */
                bits[j] -= C * ebits[j] shl EntropyCoder.BITRES

            } else {
                /* For N=1, all bits go to fine energy except for a single sign bit */
                excess = Inlines.MAX32(0, bit - (C shl EntropyCoder.BITRES))
                bits[j] = bit - excess
                ebits[j] = 0
                fine_priority[j] = 1
            }

            /* Fine energy can't take advantage of the re-balancing in
                quant_all_bands().
               Instead, do the re-balancing here.*/
            if (excess > 0) {
                val extra_fine: Int
                val extra_bits: Int
                extra_fine =
                        Inlines.IMIN(excess shr stereo + EntropyCoder.BITRES, CeltConstants.MAX_FINE_BITS - ebits[j])
                ebits[j] += extra_fine
                extra_bits = extra_fine * C shl EntropyCoder.BITRES
                fine_priority[j] = if (extra_bits >= excess - balance) 1 else 0
                excess -= extra_bits
            }
            balance = excess

            Inlines.OpusAssert(bits[j] >= 0)
            Inlines.OpusAssert(ebits[j] >= 0)
            j++
        }
        /* Save any remaining bits over the cap for the rebalancing in
            quant_all_bands(). */
        _balance.Val = balance

        /* The skipped bands use all their bits for fine energy. */
        while (j < end) {
            ebits[j] = bits[j] shr stereo shr EntropyCoder.BITRES
            Inlines.OpusAssert(C * ebits[j] shl EntropyCoder.BITRES == bits[j])
            bits[j] = 0
            fine_priority[j] = if (ebits[j] < 1) 1 else 0
            j++
        }

        return codedBands
    }

    fun compute_allocation(
        m: CeltMode,
        start: Int,
        end: Int,
        offsets: IntArray,
        cap: IntArray,
        alloc_trim: Int,
        intensity: BoxedValueInt,
        dual_stereo: BoxedValueInt,
        total: Int,
        balance: BoxedValueInt,
        pulses: IntArray,
        ebits: IntArray,
        fine_priority: IntArray,
        C: Int,
        LM: Int,
        ec: EntropyCoder,
        encode: Int,
        prev: Int,
        signalBandwidth: Int
    ): Int {
        var total = total
        var lo: Int
        var hi: Int
        val len: Int
        var j: Int
        val codedBands: Int
        var skip_start: Int
        val skip_rsv: Int
        var intensity_rsv: Int
        var dual_stereo_rsv: Int

        total = Inlines.IMAX(total, 0)
        len = m.nbEBands
        skip_start = start
        /* Reserve a bit to signal the end of manually skipped bands. */
        skip_rsv = if (total >= 1 shl EntropyCoder.BITRES) 1 shl EntropyCoder.BITRES else 0
        total -= skip_rsv
        /* Reserve bits for the intensity and dual stereo parameters. */
        dual_stereo_rsv = 0
        intensity_rsv = dual_stereo_rsv
        if (C == 2) {
            intensity_rsv = LOG2_FRAC_TABLE[end - start].toInt()
            if (intensity_rsv > total) {
                intensity_rsv = 0
            } else {
                total -= intensity_rsv
                dual_stereo_rsv = if (total >= 1 shl EntropyCoder.BITRES) 1 shl EntropyCoder.BITRES else 0
                total -= dual_stereo_rsv
            }
        }

        val bits1 = IntArray(len)
        val bits2 = IntArray(len)
        val thresh = IntArray(len)
        val trim_offset = IntArray(len)

        j = start
        while (j < end) {
            /* Below this threshold, we're sure not to allocate any PVQ bits */
            thresh[j] = Inlines.IMAX(
                C shl EntropyCoder.BITRES,
                3 * (m.eBands!![j + 1] - m.eBands!![j]) shl LM shl EntropyCoder.BITRES shr 4
            )
            /* Tilt of the allocation curve */
            trim_offset[j] = (C * (m.eBands!![j + 1] - m.eBands!![j]) * (alloc_trim - 5 - LM) * (end - j - 1)
                    * (1 shl LM + EntropyCoder.BITRES)) shr 6
            /* Giving less resolution to single-coefficient bands because they get
               more benefit from having one coarse value per coefficient*/
            if (m.eBands!![j + 1] - m.eBands!![j] shl LM == 1) {
                trim_offset[j] -= C shl EntropyCoder.BITRES
            }
            j++
        }
        lo = 1
        hi = m.nbAllocVectors - 1
        do {
            var done = 0
            var psum = 0
            val mid = lo + hi shr 1
            j = end
            while (j-- > start) {
                var bitsj: Int
                val N = m.eBands!![j + 1] - m.eBands!![j]
                bitsj = C * N * m.allocVectors!![mid * len + j].toInt() shl LM shr 2

                if (bitsj > 0) {
                    bitsj = Inlines.IMAX(0, bitsj + trim_offset[j])
                }

                bitsj += offsets[j]

                if (bitsj >= thresh[j] || done != 0) {
                    done = 1
                    /* Don't allocate more than we can actually use */
                    psum += Inlines.IMIN(bitsj, cap[j])
                } else if (bitsj >= C shl EntropyCoder.BITRES) {
                    psum += C shl EntropyCoder.BITRES
                }
            }
            if (psum > total) {
                hi = mid - 1
            } else {
                lo = mid + 1
            }
            /*printf ("lo = %d, hi = %d\n", lo, hi);*/
        } while (lo <= hi)

        hi = lo--
        /*printf ("interp between %d and %d\n", lo, hi);*/

        j = start
        while (j < end) {
            var bits1j: Int
            var bits2j: Int
            val N = m.eBands!![j + 1] - m.eBands!![j]
            bits1j = C * N * m.allocVectors!![lo * len + j].toInt() shl LM shr 2
            bits2j = if (hi >= m.nbAllocVectors)
                cap[j]
            else
                C * N * m.allocVectors!![hi * len + j].toInt() shl LM shr 2
            if (bits1j > 0) {
                bits1j = Inlines.IMAX(0, bits1j + trim_offset[j])
            }
            if (bits2j > 0) {
                bits2j = Inlines.IMAX(0, bits2j + trim_offset[j])
            }
            if (lo > 0) {
                bits1j += offsets[j]
            }
            bits2j += offsets[j]
            if (offsets[j] > 0) {
                skip_start = j
            }
            bits2j = Inlines.IMAX(0, bits2j - bits1j)
            bits1[j] = bits1j
            bits2[j] = bits2j
            j++
        }

        codedBands = interp_bits2pulses(
            m, start, end, skip_start, bits1, bits2, thresh, cap,
            total, balance, skip_rsv, intensity, intensity_rsv, dual_stereo, dual_stereo_rsv,
            pulses, ebits, fine_priority, C, LM, ec, encode, prev, signalBandwidth
        )

        return codedBands
    }
}
