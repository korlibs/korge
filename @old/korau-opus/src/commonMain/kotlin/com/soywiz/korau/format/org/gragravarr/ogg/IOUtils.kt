/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soywiz.korau.format.org.gragravarr.ogg

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

/**
 * Utilities for working with IO streams, such
 * as reading and writing.
 *
 * Endian Note - Ogg and Vorbis tend to work in
 * Little Endian format, while FLAC tends to
 * work in Big Endian format.
 */
object IOUtils {
	internal val UTF8 = Charset.forName("UTF-8")
	fun readFully(
		inp: SyncInputStream,
		destination: ByteArray,
		offset: Int = 0,
		length: Int = destination.size
	) {
		var read = 0
		var r: Int
		while (read < length) {
			r = inp.read(destination, offset + read, length - read)
			if (r == -1) {
				throw EOFException("Asked to read $length bytes from $offset but hit EoF at $read")
			}
			read += r
		}
	}


	fun toInt(b: Byte): Int {
		return if (b < 0) b and 0xff else b.toInt()
	}

	fun fromInt(i: Int): Byte {
		if (i > 256) {
			throw IllegalArgumentException("Number $i too big")
		}
		return if (i > 127) {
			(i - 256).toByte()
		} else i.toByte()
	}


	fun readOrEOF(stream: SyncInputStream): Int {
		val data = stream.read()
		if (data == -1) throw EOFException("No data remains")
		return data
	}

	fun getInt2(data: ByteArray, offset: Int = 0): Int {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		return getInt(b0, b1)
	}

	fun getInt3(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		return getInt(b0, b1, b2)
	}

	fun getInt4(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		val b3 = data[i++] and 0xFF
		return getInt(b0, b1, b2, b3)
	}

	fun getInt5(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		val b3 = data[i++] and 0xFF
		val b4 = data[i++] and 0xFF
		return getInt(b0, b1, b2, b3, b4)
	}

	fun getInt8(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		val b3 = data[i++] and 0xFF
		val b4 = data[i++] and 0xFF
		val b5 = data[i++] and 0xFF
		val b6 = data[i++] and 0xFF
		val b7 = data[i++] and 0xFF
		return getInt(b0, b1, b2, b3, b4, b5, b6, b7)
	}

	fun getInt(i0: Int, i1: Int): Int {
		return (i1 shl 8) + (i0 shl 0)
	}

	fun getInt(i0: Int, i1: Int, i2: Int): Long {
		return ((i2 shl 16) + (i1 shl 8) + (i0 shl 0)).toLong()
	}

	fun getInt(i0: Int, i1: Int, i2: Int, i3: Int): Long {
		return ((i3 shl 24) + (i2 shl 16) + (i1 shl 8) + (i0 shl 0)).toLong()
	}

	fun getInt(i0: Int, i1: Int, i2: Int, i3: Int, i4: Int): Long {
		return ((i4 shl 32) + (i3 shl 24) + (i2 shl 16) + (i1 shl 8) + (i0 shl 0)).toLong()
	}

	fun getInt(i0: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int): Long {
		// Special check for all 0xff, to avoid overflowing long
		return if (i0 == 255 && i1 == 255 && i3 == 255 && i4 == 255 && i5 == 255 && i6 == 255 && i7 == 255) -1L else (i7 shl 56).toLong() + (i6 shl 48).toLong() +
				(i5 shl 40).toLong() + (i4 shl 32).toLong() +
				(i3 shl 24).toLong() + (i2 shl 16).toLong() + (i1 shl 8).toLong() + (i0 shl 0).toLong()
		// Otherwise normal convert
	}

	fun getInt2BE(data: ByteArray, offset: Int = 0): Int {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		return getIntBE(b0, b1)
	}

	fun getInt3BE(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		return getIntBE(b0, b1, b2)
	}

	fun getInt4BE(data: ByteArray, offset: Int = 0): Long {
		var i = offset
		val b0 = data[i++] and 0xFF
		val b1 = data[i++] and 0xFF
		val b2 = data[i++] and 0xFF
		val b3 = data[i++] and 0xFF
		return getIntBE(b0, b1, b2, b3)
	}

	fun getIntBE(i0: Int, i1: Int): Int {
		return (i0 shl 8) + (i1 shl 0)
	}

	fun getIntBE(i0: Int, i1: Int, i2: Int): Long {
		return ((i0 shl 16) + (i1 shl 8) + (i2 shl 0)).toLong()
	}

	fun getIntBE(i0: Int, i1: Int, i2: Int, i3: Int): Long {
		return ((i0 shl 24) + (i1 shl 16) + (i2 shl 8) + (i3 shl 0)).toLong()
	}


	fun writeInt2(out: SyncOutputStream, v: Int) {
		val b2 = ByteArray(2)
		putInt2(b2, 0, v)
		out.write(b2, 0, 2)
	}

	fun putInt2(data: ByteArray, offset: Int, v: Int) {
		var i = offset
		data[i++] = (v.ushr(0) and 0xFF).toByte()
		data[i++] = (v.ushr(8) and 0xFF).toByte()
	}

	fun writeInt3(out: SyncOutputStream, v: Long) {
		val b3 = ByteArray(3)
		putInt3(b3, 0, v)
		out.write(b3)
	}

	fun putInt3(data: ByteArray, offset: Int, v: Long) {
		var i = offset
		data[i++] = (v.ushr(0) and 0xFF).toByte()
		data[i++] = (v.ushr(8) and 0xFF).toByte()
		data[i++] = (v.ushr(16) and 0xFF).toByte()
	}

	fun writeInt4(out: SyncOutputStream, v: Long) {
		val b4 = ByteArray(4)
		putInt4(b4, 0, v)
		out.write(b4)
	}

	fun putInt4(data: ByteArray, offset: Int, v: Int) = putInt4(data, offset, v.toLong())

	fun putInt4(data: ByteArray, offset: Int, v: Long) {
		var i = offset
		data[i++] = (v.ushr(0) and 0xFF).toByte()
		data[i++] = (v.ushr(8) and 0xFF).toByte()
		data[i++] = (v.ushr(16) and 0xFF).toByte()
		data[i++] = (v.ushr(24) and 0xFF).toByte()
	}

	fun writeInt5(out: SyncOutputStream, v: Long) {
		val b5 = ByteArray(5)
		putInt5(b5, 0, v)
		out.write(b5)
	}

	fun putInt5(data: ByteArray, offset: Int, v: Long) {
		var i = offset
		data[i++] = (v.ushr(0) and 0xFF).toByte()
		data[i++] = (v.ushr(8) and 0xFF).toByte()
		data[i++] = (v.ushr(16) and 0xFF).toByte()
		data[i++] = (v.ushr(24) and 0xFF).toByte()
		data[i++] = (v.ushr(32) and 0xFF).toByte()
	}

	fun writeInt8(out: SyncOutputStream, v: Long) {
		val b8 = ByteArray(8)
		putInt8(b8, 0, v)
		out.write(b8)
	}

	fun putInt8(data: ByteArray, offset: Int, v: Long) {
		var i = offset
		data[i++] = (v.ushr(0) and 0xFF).toByte()
		data[i++] = (v.ushr(8) and 0xFF).toByte()
		data[i++] = (v.ushr(16) and 0xFF).toByte()
		data[i++] = (v.ushr(24) and 0xFF).toByte()
		data[i++] = (v.ushr(32) and 0xFF).toByte()
		data[i++] = (v.ushr(40) and 0xFF).toByte()
		data[i++] = (v.ushr(48) and 0xFF).toByte()
		data[i++] = (v.ushr(56) and 0xFF).toByte()
	}


	fun writeInt2BE(out: SyncOutputStream, v: Int) {
		val b2 = ByteArray(2)
		putInt2BE(b2, 0, v)
		out.write(b2)
	}

	fun putInt2BE(data: ByteArray, offset: Int, v: Int) {
		data[offset + 1] = (v.ushr(0) and 0xFF).toByte()
		data[offset + 0] = (v.ushr(8) and 0xFF).toByte()
	}

	fun writeInt3BE(out: SyncOutputStream, v: Long) {
		val b3 = ByteArray(3)
		putInt3BE(b3, 0, v)
		out.write(b3)
	}

	fun putInt3BE(data: ByteArray, offset: Int, v: Long) {
		data[offset + 2] = (v.ushr(0) and 0xFF).toByte()
		data[offset + 1] = (v.ushr(8) and 0xFF).toByte()
		data[offset + 0] = (v.ushr(16) and 0xFF).toByte()
	}

	fun writeInt4BE(out: SyncOutputStream, v: Long) {
		val b4 = ByteArray(4)
		putInt4BE(b4, 0, v)
		out.write(b4)
	}

	fun putInt4BE(data: ByteArray, offset: Int, v: Long) {
		data[offset + 3] = (v.ushr(0) and 0xFF).toByte()
		data[offset + 2] = (v.ushr(8) and 0xFF).toByte()
		data[offset + 1] = (v.ushr(16) and 0xFF).toByte()
		data[offset + 0] = (v.ushr(24) and 0xFF).toByte()
	}

	/**
	 * Gets the integer value that is stored in UTF-8 like fashion, in Big Endian
	 * but with the high bit on each number indicating if it continues or not
	 */
	fun readUE7(stream: SyncInputStream): Long {
		var i: Int
		var v: Long = 0
		while (true) {
			i = stream.read()
			if (i < 0) break
			v = v shl 7
			if (i and 128 == 128) {
				// Continues
				v += (i and 127).toLong()
			} else {
				// Last value
				v += i.toLong()
				break
			}
		}
		return v
	}
	//   public static void writeUE7(OutputStream out, long value) throws IOException {
	//       // TODO Implement
	//   }

	/**
	 * @param length The length in BYTES
	 */
	fun getUTF8(data: ByteArray, offset: Int, length: Int): String {
		return data.sliceArray(offset until offset + length).toString(UTF8)
	}

	/**
	 * Strips off any null padding, if any, from the string
	 */
	fun removeNullPadding(str: String): String {
		val idx = str.indexOf('\u0000')
		return if (idx == -1) {
			str
		} else str.substring(0, idx)
	}

	/**
	 * @return The length in BYTES
	 */
	fun putUTF8(data: ByteArray, offset: Int, str: String): Int {
		val s = toUTF8Bytes(str)
		arraycopy(s, 0, data, offset, s.size)
		return s.size
	}

	/**
	 * @return The length in BYTES
	 */
	fun toUTF8Bytes(str: String): ByteArray {
		return str.toByteArray(UTF8)
	}

	/**
	 * Writes the string out as UTF-8
	 */
	fun writeUTF8(out: SyncOutputStream, str: String) {
		val s = str.toByteArray(UTF8)
		out.write(s)
	}

	/**
	 * Writes out a 4 byte integer of the length (in bytes!) of the
	 * String, followed by the String (as UTF-8)
	 */
	fun writeUTF8WithLength(out: SyncOutputStream, str: String) {
		val s = str.toByteArray(UTF8)
		writeInt4(out, s.size.toLong())
		out.write(s)
	}

	/**
	 * Checks to see if the wanted byte pattern is found in the
	 * within bytes from the given offset
	 * @param wanted Byte sequence to look for
	 * @param within Bytes to find in
	 * @param withinOffset Offset to check from
	 */
	fun byteRangeMatches(wanted: ByteArray, within: ByteArray, withinOffset: Int): Boolean {
		for (i in wanted.indices) {
			if (wanted[i] != within[i + withinOffset]) return false
		}
		return true
	}
}
