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

/**
 * Wrapper around an InputStream, which captures all
 * bytes as they are read, and makes those available.
 * Used when you want to read from a stream, but also
 * have a record easily of what it contained
 */

fun BytesCapturingInputStream(input: SyncInputStream, out: SyncOutputStream): SyncInputStream {
	return object : SyncInputStream {
		override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			val res = input.read(buffer, offset, len)
			if (res > 0) {
				out.write(buffer, offset, res)
			}
			return res
		}
	}
}