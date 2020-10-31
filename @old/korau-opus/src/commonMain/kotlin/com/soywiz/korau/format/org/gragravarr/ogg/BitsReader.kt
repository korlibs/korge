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

import com.soywiz.korio.stream.*
import kotlin.math.*

/**
 * Utilities for reading (and hopefully later writing)
 * a stream of arbitrary bits, in big endian encoding, eg
 * "give me the next 3 bits" or "give me bits to the
 * byte boundary"
 */
class BitsReader(private val input: SyncInputStream) {

	private var tmp = 0
	private var remaining = 0
	/**
	 * Has the End-Of-File / End-of-Stream been hit?
	 */
	var isEOF = false
		private set

	fun read(numBits: Int): Int {
		var numBits = numBits
		var res = 0
		while (numBits > 0 && !isEOF) {
			if (remaining == 0) {
				tmp = input.read()
				if (tmp == -1) {
					isEOF = true
					return -1
				}
				remaining = 8
			}
			val toNibble = min(remaining, numBits)
			val toLeave = remaining - toNibble
			val leaveMask = (1 shl toLeave) - 1

			res = res shl toNibble
			res += tmp shr toLeave
			tmp = tmp and leaveMask

			remaining -= toNibble
			numBits -= toNibble
		}
		return if (isEOF) -1 else res
	}

	/**
	 * Counts the number of bits until the next zero (false)
	 * bit is set
	 *
	 * eg 1110 is 3, 0 is 0, 10 is 1.
	 * @return the number of bits until the next zero
	 */
	fun bitsToNextZero(): Int {
		var count = 0
		while (read(1) == 1) {
			count++
		}
		return count
	}

	/**
	 * Counts the number of bits until the next one (true)
	 * bit is set
	 *
	 * eg b1 is 0, b001 is 2, b0000001 is 6
	 * @return the number of bits until the next one
	 */
	fun bitsToNextOne(): Int {
		var count = 0
		while (read(1) == 0) {
			count++
		}
		return count
	}

	/**
	 * Reads the number to the next byte boundary,
	 * or -1 if already there.
	 */
	fun readToByteBoundary(): Int {
		return if (remaining == 0) -1 else read(remaining)
	}
}
