/*******************************************************************************
 * Copyright (c) 2014 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2

import org.luaj.vm2.Globals.BufferedStream
import org.luaj.vm2.io.*
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferedStreamTest {
    private fun NewBufferedStream(buflen: Int, contents: String): BufferedStream =
        BufferedStream(buflen, BytesLuaBinInput(contents.toByteArray()))

    @Test
    fun testReadEmptyStream() {
        val bs = NewBufferedStream(4, "")
        assertEquals(-1, bs.read())
        assertEquals(-1, bs.read(ByteArray(10)))
        assertEquals(-1, bs.read(ByteArray(10), 0, 10))
    }

    @Test
    fun testReadByte() {
        val bs = NewBufferedStream(2, "abc")
        assertEquals('a', bs.read().toChar())
        assertEquals('b', bs.read().toChar())
        assertEquals('c', bs.read().toChar())
        assertEquals(-1, bs.read())
    }

    @Test
    fun testReadByteArray() {
        val array = ByteArray(3)
        val bs = NewBufferedStream(4, "abcdef")
        assertEquals(3, bs.read(array))
        assertEquals("abc", String(array))
        assertEquals(1, bs.read(array))
        assertEquals("d", String(array, 0, 1))
        assertEquals(2, bs.read(array))
        assertEquals("ef", String(array, 0, 2))
        assertEquals(-1, bs.read())
    }

    @Test
    fun testReadByteArrayOffsetLength() {
        val array = ByteArray(10)
        val bs = NewBufferedStream(8, "abcdefghijklmn")
        assertEquals(4, bs.read(array, 0, 4))
        assertEquals("abcd", String(array, 0, 4))
        assertEquals(4, bs.read(array, 2, 8))
        assertEquals("efgh", String(array, 2, 4))
        assertEquals(6, bs.read(array, 0, 10))
        assertEquals("ijklmn", String(array, 0, 6))
        assertEquals(-1, bs.read())
    }

    @Test
    fun testMarkOffsetBeginningOfStream() {
        val array = ByteArray(4)
        val bs = NewBufferedStream(8, "abcdefghijkl")
        assertEquals(true, bs.markSupported())
        bs.mark(4)
        assertEquals(4, bs.read(array))
        assertEquals("abcd", String(array))
        bs.reset()
        assertEquals(4, bs.read(array))
        assertEquals("abcd", String(array))
        assertEquals(4, bs.read(array))
        assertEquals("efgh", String(array))
        assertEquals(4, bs.read(array))
        assertEquals("ijkl", String(array))
        assertEquals(-1, bs.read())
    }

    @Test
    fun testMarkOffsetMiddleOfStream() {
        val array = ByteArray(4)
        val bs = NewBufferedStream(8, "abcdefghijkl")
        assertEquals(true, bs.markSupported())
        assertEquals(4, bs.read(array))
        assertEquals("abcd", String(array))
        bs.mark(4)
        assertEquals(4, bs.read(array))
        assertEquals("efgh", String(array))
        bs.reset()
        assertEquals(4, bs.read(array))
        assertEquals("efgh", String(array))
        assertEquals(4, bs.read(array))
        assertEquals("ijkl", String(array))
        assertEquals(-1, bs.read())
    }
}
