/* Copyright (c) 2008-2011 Octasic Inc.
   Originally written by Jean-Marc Valin
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

import kotlin.math.*

/// <summary>
/// multi-layer perceptron processor
/// </summary>
internal object MultiLayerPerceptron {

    private val MAX_NEURONS = 100

    fun tansig_approx(x: Float): Float {
        var x = x
        val i: Int
        var y: Float
        val dy: Float
        var sign = 1f
        /* Tests are reversed to catch NaNs */
        if (x >= 8) {
            return 1f
        }
        if (x <= -8) {
            return -1f
        }
        if (x < 0) {
            x = -x
            sign = -1f
        }
        i = floor((.5f + 25 * x).toDouble()).toInt()
        x -= .04f * i
        y = OpusTables.tansig_table[i]
        dy = 1 - y * y
        y = y + x * dy * (1 - y * x)
        return sign * y
    }

    fun mlp_process(m: MLPState, input: FloatArray, output: FloatArray) {
        var j: Int
        val hidden = FloatArray(MAX_NEURONS)
        val W = m.weights
        var W_ptr = 0

        /* Copy to tmp_in */
        j = 0
        while (j < m.topo!![1]) {
            var k: Int
            var sum = W!![W_ptr]
            W_ptr++
            k = 0
            while (k < m.topo!![0]) {
                sum = sum + input[k] * W!![W_ptr]
                W_ptr++
                k++
            }
            hidden[j] = tansig_approx(sum)
            j++
        }

        j = 0
        while (j < m.topo!![2]) {
            var k: Int
            var sum = W!![W_ptr]
            W_ptr++
            k = 0
            while (k < m.topo!![1]) {
                sum = sum + hidden[k] * W!![W_ptr]
                W_ptr++
                k++
            }
            output[j] = tansig_approx(sum)
            j++
        }
    }
}
