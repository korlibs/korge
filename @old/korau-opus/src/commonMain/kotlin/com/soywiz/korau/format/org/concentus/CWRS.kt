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

internal object CWRS {

    /*Although derived separately, the pulse vector coding scheme is equivalent to
a Pyramid Vector Quantizer \cite{Fis86}.
Some additional notes about an early version appear at
http://people.xiph.org/~tterribe/notes/cwrs.html, but the codebook ordering
and the definitions of some terms have evolved since that was written.

The conversion from a pulse vector to an integer index (encoding) and back
(decoding) is governed by two related functions, V(N,K) and U(N,K).

V(N,K) = the number of combinations, with replacement, of N items, taken K
at a time, when a sign bit is added to each item taken at least once (i.e.,
the number of N-dimensional unit pulse vectors with K pulses).
One way to compute this is via
V(N,K) = K>0 ? sum(k=1...K,2**k*choose(N,k)*choose(K-1,k-1)) : 1,
where choose() is the binomial function.
A table of values for N<10 and K<10 looks like:
V[10][10] = {
{1,  0,   0,    0,    0,     0,     0,      0,      0,       0},
{1,  2,   2,    2,    2,     2,     2,      2,      2,       2},
{1,  4,   8,   12,   16,    20,    24,     28,     32,      36},
{1,  6,  18,   38,   66,   102,   146,    198,    258,     326},
{1,  8,  32,   88,  192,   360,   608,    952,   1408,    1992},
{1, 10,  50,  170,  450,  1002,  1970,   3530,   5890,    9290},
{1, 12,  72,  292,  912,  2364,  5336,  10836,  20256,   35436},
{1, 14,  98,  462, 1666,  4942, 12642,  28814,  59906,  115598},
{1, 16, 128,  688, 2816,  9424, 27008,  68464, 157184,  332688},
{1, 18, 162,  978, 4482, 16722, 53154, 148626, 374274,  864146}
};

U(N,K) = the number of such combinations wherein N-1 objects are taken at
most K-1 at a time.
This is given by
U(N,K) = sum(k=0...K-1,V(N-1,k))
       = K>0 ? (V(N-1,K-1) + V(N,K-1))/2 : 0.
The latter expression also makes clear that U(N,K) is half the number of such
combinations wherein the first object is taken at least once.
Although it may not be clear from either of these definitions, U(N,K) is the
natural function to work with when enumerating the pulse vector codebooks,
not V(N,K).
U(N,K) is not well-defined for N=0, but with the extension
U(0,K) = K>0 ? 0 : 1,
the function becomes symmetric: U(N,K) = U(K,N), with a similar table:
U[10][10] = {
{1, 0,  0,   0,    0,    0,     0,     0,      0,      0},
{0, 1,  1,   1,    1,    1,     1,     1,      1,      1},
{0, 1,  3,   5,    7,    9,    11,    13,     15,     17},
{0, 1,  5,  13,   25,   41,    61,    85,    113,    145},
{0, 1,  7,  25,   63,  129,   231,   377,    575,    833},
{0, 1,  9,  41,  129,  321,   681,  1289,   2241,   3649},
{0, 1, 11,  61,  231,  681,  1683,  3653,   7183,  13073},
{0, 1, 13,  85,  377, 1289,  3653,  8989,  19825,  40081},
{0, 1, 15, 113,  575, 2241,  7183, 19825,  48639, 108545},
{0, 1, 17, 145,  833, 3649, 13073, 40081, 108545, 265729}
};

With this extension, V(N,K) may be written in terms of U(N,K):
V(N,K) = U(N,K) + U(N,K+1)
for all N>=0, K>=0.
Thus U(N,K+1) represents the number of combinations where the first element
is positive or zero, and U(N,K) represents the number of combinations where
it is negative.
With a large enough table of U(N,K) values, we could write O(N) encoding
and O(min(N*log(K),N+K)) decoding routines, but such a table would be
prohibitively large for small embedded devices (K may be as large as 32767
for small N, and N may be as large as 200).

Both functions obey the same recurrence relation:
V(N,K) = V(N-1,K) + V(N,K-1) + V(N-1,K-1),
U(N,K) = U(N-1,K) + U(N,K-1) + U(N-1,K-1),
for all N>0, K>0, with different initial conditions at N=0 or K=0.
This allows us to construct a row of one of the tables above given the
previous row or the next row.
Thus we can derive O(NK) encoding and decoding routines with O(K) memory
using only addition and subtraction.

When encoding, we build up from the U(2,K) row and work our way forwards.
When decoding, we need to start at the U(N,K) row and work our way backwards,
which requires a means of computing U(N,K).
U(N,K) may be computed from two previous values with the same N:
U(N,K) = ((2*N-1)*U(N,K-1) - U(N,K-2))/(K-1) + U(N,K-2)
for all N>1, and since U(N,K) is symmetric, a similar relation holds for two
previous values with the same K:
U(N,K>1) = ((2*K-1)*U(N-1,K) - U(N-2,K))/(N-1) + U(N-2,K)
for all K>1.
This allows us to construct an arbitrary row of the U(N,K) table by starting
with the first two values, which are constants.
This saves roughly 2/3 the work in our O(NK) decoding routine, but costs O(K)
multiplications.
Similar relations can be derived for V(N,K), but are not used here.

For N>0 and K>0, U(N,K) and V(N,K) take on the form of an (N-1)-degree
polynomial for fixed N.
The first few are
U(1,K) = 1,
U(2,K) = 2*K-1,
U(3,K) = (2*K-2)*K+1,
U(4,K) = (((4*K-6)*K+8)*K-3)/3,
U(5,K) = ((((2*K-4)*K+10)*K-8)*K+3)/3,
and
V(1,K) = 2,
V(2,K) = 4*K,
V(3,K) = 4*K*K+2,
V(4,K) = 8*(K*K+2)*K/3,
V(5,K) = ((4*K*K+20)*K*K+6)/3,
for all K>0.
This allows us to derive O(N) encoding and O(N*log(K)) decoding routines for
small N (and indeed decoding is also O(N) for N<3).

@ARTICLE{Fis86,
author="Thomas R. Fischer",
title="A Pyramid Vector Quantizer",
journal="IEEE Transactions on Information Theory",
volume="IT-32",
number=4,
pages="568--583",
month=Jul,
year=1986
}*/

    val CELT_PVQ_U_ROW = intArrayOf(0, 176, 351, 525, 698, 870, 1041, 1131, 1178, 1207, 1226, 1240, 1248, 1254, 1257)

    /*U(N,K) = U(K,N) := N>0?K>0?U(N-1,K)+U(N,K-1)+U(N-1,K-1):0:K>0?1:0*/
    private fun CELT_PVQ_U(_n: Int, _k: Int): Long {
        return CeltTables.CELT_PVQ_U_DATA[CELT_PVQ_U_ROW[Inlines.IMIN(_n, _k)] + Inlines.IMAX(_n, _k)]
    }


    /*V(N,K) := U(N,K)+U(N,K+1) = the number of PVQ codewords for a band of size N
       with K pulses allocated to it.*/
    private fun CELT_PVQ_V(_n: Int, _k: Int): Long {
        return CELT_PVQ_U(_n, _k) + CELT_PVQ_U(_n, _k + 1)
    }

    fun icwrs(_n: Int, _y: IntArray): Long {
        var i: Long
        var j: Int
        var k: Int
        Inlines.OpusAssert(_n >= 2)
        j = _n - 1
        i = (if (_y[j] < 0) 1 else 0).toLong()
        k = Inlines.abs(_y[j])
        do {
            j--
            i += CELT_PVQ_U(_n - j, k)
            k += Inlines.abs(_y[j])
            if (_y[j] < 0) {
                i += CELT_PVQ_U(_n - j, k + 1)
            }
        } while (j > 0)
        return i
    }

    fun encode_pulses(_y: IntArray, _n: Int, _k: Int, _enc: EntropyCoder) {
        Inlines.OpusAssert(_k > 0)
        _enc.enc_uint(icwrs(_n, _y), CELT_PVQ_V(_n, _k))
    }

    fun cwrsi(_n: Int, _k: Int, _i: Long, _y: IntArray): Int {
        var _n = _n
        var _k = _k
        var _i = _i
        var p: Long
        var s: Int
        var k0: Int
        var `val`: Short
        var yy = 0
        var y_ptr = 0
        Inlines.OpusAssert(_k > 0)
        Inlines.OpusAssert(_n > 1)

        while (_n > 2) {
            val q: Long
            /*Lots of pulses case:*/
            if (_k >= _n) {
                val row: Int
                row = CELT_PVQ_U_ROW[_n]
                /*Are the pulses in this dimension negative?*/
                p = CeltTables.CELT_PVQ_U_DATA[row + _k + 1]
                s = 0 - if (_i >= p) 1 else 0
                _i = _i - Inlines.CapToUInt32(p and s.toLong())
                /*Count how many pulses were placed in this dimension.*/
                k0 = _k
                q = CeltTables.CELT_PVQ_U_DATA[row + _n]

                if (q > _i) {
                    Inlines.OpusAssert(p > q)
                    _k = _n

                    do {
                        p = CeltTables.CELT_PVQ_U_DATA[CELT_PVQ_U_ROW[--_k] + _n]
                    } while (p > _i)
                } else {
                    p = CeltTables.CELT_PVQ_U_DATA[row + _k]
                    while (p > _i) {
                        _k--
                        p = CeltTables.CELT_PVQ_U_DATA[row + _k]
                    }
                }

                _i -= p
                `val` = (k0 - _k + s xor s).toShort()
                _y[y_ptr++] = `val`.toInt()
                yy = Inlines.MAC16_16(yy, `val`, `val`)
            } /*Lots of dimensions case:*/
            else {
                /*Are there any pulses in this dimension at all?*/
                p = CeltTables.CELT_PVQ_U_DATA[CELT_PVQ_U_ROW[_k] + _n]
                q = CeltTables.CELT_PVQ_U_DATA[CELT_PVQ_U_ROW[_k + 1] + _n]
                if (p <= _i && _i < q) {
                    _i -= p
                    _y[y_ptr++] = 0
                } else {
                    /*Are the pulses in this dimension negative?*/
                    s = 0 - if (_i >= q) 1 else 0
                    _i = _i - Inlines.CapToUInt32(q and s.toLong())
                    /*Count how many pulses were placed in this dimension.*/
                    k0 = _k
                    do {
                        p = CeltTables.CELT_PVQ_U_DATA[CELT_PVQ_U_ROW[--_k] + _n]
                    } while (p > _i)

                    _i -= p
                    `val` = (k0 - _k + s xor s).toShort()
                    _y[y_ptr++] = `val`.toInt()
                    yy = Inlines.MAC16_16(yy, `val`, `val`)
                }
            }
            _n--
        }

        /*_n==2*/
        p = 2L * _k + 1
        s = 0 - if (_i >= p) 1 else 0
        _i = _i - Inlines.CapToUInt32(p and s.toLong())
        k0 = _k
        _k = (_i + 1 shr 1).toInt()
        if (_k != 0) {
            _i -= 2L * _k - 1
        }

        `val` = (k0 - _k + s xor s).toShort()
        _y[y_ptr++] = `val`.toInt()
        yy = Inlines.MAC16_16(yy, `val`, `val`)
        /*_n==1*/
        s = -_i.toInt()
        `val` = (_k + s xor s).toShort()
        _y[y_ptr] = `val`.toInt()
        yy = Inlines.MAC16_16(yy, `val`, `val`)
        return yy
    }

    fun decode_pulses(_y: IntArray, _n: Int, _k: Int, _dec: EntropyCoder): Int {
        return cwrsi(_n, _k, _dec.dec_uint(CELT_PVQ_V(_n, _k)), _y)
    }
}
