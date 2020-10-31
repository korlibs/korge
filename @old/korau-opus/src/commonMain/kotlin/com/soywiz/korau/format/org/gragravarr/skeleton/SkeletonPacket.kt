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
package com.soywiz.korau.format.org.gragravarr.skeleton

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * Parent of all Skeleton (Annodex) packets
 */
interface SkeletonPacket : OggStreamPacket {
	companion object {
		val MAGIC_FISHEAD_STR = "fishead\u0000"
		val MAGIC_FISBONE_STR = "fisbone\u0000"
		val MAGIC_FISHEAD_BYTES = IOUtils.toUTF8Bytes(MAGIC_FISHEAD_STR)
		val MAGIC_FISBONE_BYTES = IOUtils.toUTF8Bytes(MAGIC_FISBONE_STR)
	}
}
