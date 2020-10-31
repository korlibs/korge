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

internal object Downmix {

    /// <summary>
    ///
    /// </summary>
    /// <typeparam name="T">The type of signal being handled (either short or float)</typeparam>
    /// <param name="_x"></param>
    /// <param name="sub"></param>
    /// <param name="subframe"></param>
    /// <param name="offset"></param>
    /// <param name="c1"></param>
    /// <param name="c2"></param>
    /// <param name="C"></param>
    fun downmix_int(
        x: ShortArray,
        x_ptr: Int,
        sub: IntArray,
        sub_ptr: Int,
        subframe: Int,
        offset: Int,
        c1: Int,
        c2: Int,
        C: Int
    ) {
        var scale: Int
        var j: Int
        j = 0
        while (j < subframe) {
            sub[j + sub_ptr] = x[(j + offset) * C + c1].toInt()
            j++
        }
        if (c2 > -1) {
            j = 0
            while (j < subframe) {
                sub[j + sub_ptr] += x[(j + offset) * C + c2].toInt()
                j++
            }
        } else if (c2 == -2) {
            var c: Int
            c = 1
            while (c < C) {
                j = 0
                while (j < subframe) {
                    sub[j + sub_ptr] += x[(j + offset) * C + c].toInt()
                    j++
                }
                c++
            }
        }
        scale = 1 shl CeltConstants.SIG_SHIFT
        if (C == -2) {
            scale /= C
        } else {
            scale /= 2
        }
        j = 0
        while (j < subframe) {
            sub[j + sub_ptr] *= scale
            j++
        }
    }
}
