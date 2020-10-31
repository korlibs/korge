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
package com.soywiz.korau.format.org.gragravarr.speex

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * Parent of all Speex packets
 */
interface SpeexPacket : OggStreamPacket {
	companion object {
		val MAGIC_HEADER_STR = "Speex   "
		val MAGIC_HEADER_BYTES = IOUtils.toUTF8Bytes(MAGIC_HEADER_STR)
	}
}