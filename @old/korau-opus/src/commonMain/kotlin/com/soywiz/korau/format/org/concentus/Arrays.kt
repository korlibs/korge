/* Copyright (c) 2016 Logan Stromberg

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

import com.soywiz.kmem.*

internal object Arrays {
    fun InitTwoDimensionalArrayInt(x: Int, y: Int): Array<IntArray> = Array(x) { IntArray(y) }
    fun InitTwoDimensionalArrayFloat(x: Int, y: Int): Array<FloatArray> = Array(x) { FloatArray(y) }
    fun InitTwoDimensionalArrayShort(x: Int, y: Int): Array<ShortArray> = Array(x) { ShortArray(y) }
    fun InitTwoDimensionalArrayByte(x: Int, y: Int): Array<ByteArray> = Array(x) { ByteArray(y) }
    fun InitThreeDimensionalArrayByte(x: Int, y: Int, z: Int): Array<Array<ByteArray>> =
		Array(x) { Array(y) { ByteArray(z) } }
    fun MemSet(array: ByteArray, value: Byte) = run { array.fill(value) }
    fun MemSet(array: ShortArray, value: Short) = run { array.fill(value) }
    fun MemSet(array: IntArray, value: Int) = run { array.fill(value) }
    fun MemSet(array: FloatArray, value: Float) = run { array.fill(value) }
    fun MemSet(array: ByteArray, value: Byte, length: Int) = run { array.fill(value, 0, length) }
    fun MemSet(array: ShortArray, value: Short, length: Int) = run { array.fill(value, 0, length) }
    fun MemSet(array: IntArray, value: Int, length: Int) = run { array.fill(value, 0, length) }
    fun MemSet(array: FloatArray, value: Float, length: Int) = run { array.fill(value, 0, length) }

    fun MemSetWithOffset(array: ByteArray, value: Byte, offset: Int, length: Int) {
		array.fill(value, offset, offset + length)
    }

    fun MemSetWithOffset(array: ShortArray, value: Short, offset: Int, length: Int) {
		array.fill(value, offset, offset + length)
    }

    fun MemSetWithOffset(array: IntArray, value: Int, offset: Int, length: Int) {
        array.fill(value, offset, offset + length)
    }

    fun MemMove(array: ByteArray, src_idx: Int, dst_idx: Int, length: Int) {
        arraycopy(array, src_idx, array, dst_idx, length)
    }

    fun MemMove(array: ShortArray, src_idx: Int, dst_idx: Int, length: Int) {
        arraycopy(array, src_idx, array, dst_idx, length)
    }

    fun MemMove(array: IntArray, src_idx: Int, dst_idx: Int, length: Int) {
        arraycopy(array, src_idx, array, dst_idx, length)
    }

}
