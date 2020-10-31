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

internal object Sort {

    /// <summary>
    ///
    /// </summary>
    /// <param name="a">(I/O) Unsorted / Sorted vector</param>
    /// <param name="idx">(O) Index vector for the sorted elements</param>
    /// <param name="L">(I) Vector length</param>
    /// <param name="K">(I) Number of correctly sorted positions</param>
    fun silk_insertion_sort_increasing(a: IntArray, idx: IntArray, L: Int, K: Int) {
        var value: Int
        var i: Int
        var j: Int

        // Safety checks
        Inlines.OpusAssert(K > 0)
        Inlines.OpusAssert(L > 0)
        Inlines.OpusAssert(L >= K)

        // Write start indices in index vector
        i = 0
        while (i < K) {
            idx[i] = i
            i++
        }

        // Sort vector elements by value, increasing order
        i = 1
        while (i < K) {
            value = a[i]

            j = i - 1
            while (j >= 0 && value < a[j]) {
                a[j + 1] = a[j]
                /* Shift value */
                idx[j + 1] = idx[j]
                j--
                /* Shift index */
            }

            a[j + 1] = value
            /* Write value */
            idx[j + 1] = i
            i++
            /* Write index */
        }

        // If less than L values are asked for, check the remaining values,
        // but only spend CPU to ensure that the K first values are correct
        i = K
        while (i < L) {
            value = a[i]

            if (value < a[K - 1]) {
                j = K - 2
                while (j >= 0 && value < a[j]) {
                    a[j + 1] = a[j]
                    /* Shift value */
                    idx[j + 1] = idx[j]
                    j--
                    /* Shift index */
                }

                a[j + 1] = value
                /* Write value */
                idx[j + 1] = i
                /* Write index */
            }
            i++
        }
    }

    /// <summary>
    /// Insertion sort (fast for already almost sorted arrays):
    /// Best case:  O(n)   for an already sorted array
    /// Worst case: O(n^2) for an inversely sorted array
    /// </summary>
    /// <param name="a">(I/O) Unsorted / Sorted vector</param>
    /// <param name="L">(I) Vector length</param>
    fun silk_insertion_sort_increasing_all_values_int16(a: ShortArray, L: Int) {
        // FIXME: Could just use Array.Sort(a.Array, a.Offset, L);

        var value: Short
        var i: Int
        var j: Int

        // Safety checks
        Inlines.OpusAssert(L > 0)

        // Sort vector elements by value, increasing order
        i = 1
        while (i < L) {
            value = a[i]
            j = i - 1
            while (j >= 0 && value < a[j]) {
                a[j + 1] = a[j] // Shift value
                j--
            }

            a[j + 1] = value // Write value
            i++
        }
    }

    /* This function is only used by the fixed-point build */
    fun silk_insertion_sort_decreasing_int16(
        a: ShortArray, /* I/O   Unsorted / Sorted vector                                   */
        idx: IntArray, /* O     Index vector for the sorted elements                       */
        L: Int, /* I     Vector length                                              */
        K: Int /* I     Number of correctly sorted positions                       */
    ) {
        var i: Int
        var j: Int
        var value: Short

        /* Safety checks */
        Inlines.OpusAssert(K > 0)
        Inlines.OpusAssert(L > 0)
        Inlines.OpusAssert(L >= K)

        /* Write start indices in index vector */
        i = 0
        while (i < K) {
            idx[i] = i
            i++
        }

        /* Sort vector elements by value, decreasing order */
        i = 1
        while (i < K) {
            value = a[i]
            j = i - 1
            while (j >= 0 && value > a[j]) {
                a[j + 1] = a[j]
                /* Shift value */
                idx[j + 1] = idx[j]
                j--
                /* Shift index */
            }
            a[j + 1] = value
            /* Write value */
            idx[j + 1] = i
            i++
            /* Write index */
        }

        /* If less than L values are asked for, check the remaining values, */
        /* but only spend CPU to ensure that the K first values are correct */
        i = K
        while (i < L) {
            value = a[i]
            if (value > a[K - 1]) {
                j = K - 2
                while (j >= 0 && value > a[j]) {
                    a[j + 1] = a[j]
                    /* Shift value */
                    idx[j + 1] = idx[j]
                    j--
                    /* Shift index */
                }
                a[j + 1] = value
                /* Write value */
                idx[j + 1] = i
                /* Write index */
            }
            i++
        }
    }
}
