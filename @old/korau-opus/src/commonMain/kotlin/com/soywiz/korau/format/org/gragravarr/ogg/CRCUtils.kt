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

object CRCUtils {
	internal val CRC_POLYNOMIAL = 0x04c11db7
	private val CRC_TABLE = IntArray(256)

	init {
		var crc: Int
		for (i in 0..255) {
			crc = i shl 24
			for (j in 0..7) {
				if (crc and -0x80000000 != 0) {
					crc = crc shl 1 xor CRC_POLYNOMIAL
				} else {
					crc = crc shl 1
				}
			}
			CRC_TABLE[i] = crc
		}
	}

	fun getCRC(data: ByteArray, previous: Int = 0): Int {
		var crc = previous
		var a: Int
		var b: Int

		for (i in data.indices) {
			a = crc shl 8
			b = CRC_TABLE[crc.ushr(24) and 0xff xor (data[i] and 0xff)]
			crc = a xor b
		}

		return crc
	}
}
