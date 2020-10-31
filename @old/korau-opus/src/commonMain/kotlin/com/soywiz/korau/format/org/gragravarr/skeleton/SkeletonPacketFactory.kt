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
import com.soywiz.korau.format.org.gragravarr.skeleton.SkeletonPacket.Companion.MAGIC_FISBONE_BYTES
import com.soywiz.korau.format.org.gragravarr.skeleton.SkeletonPacket.Companion.MAGIC_FISHEAD_BYTES

/**
 * Identifies the right kind of [SkeletonPacket] for a given
 * incoming [OggPacket], and creates it
 */
class SkeletonPacketFactory : HighLevelOggStreamPacket() {
	companion object {
		/**
		 * Does this packet (the first in the stream) contain
		 * the magic string indicating that it's a skeleton fis(head|bone)
		 * one?
		 */
		fun isSkeletonStream(firstPacket: OggPacket): Boolean {
			return if (!firstPacket.isBeginningOfStream) {
				false
			} else isSkeletonSpecial(firstPacket)
		}

		protected fun isSkeletonSpecial(packet: OggPacket): Boolean {
			val d = packet.data!!

			// Is it a Skeleton Fishead or Fisbone packet?
			if (d.size < 52) return false
			if (IOUtils.byteRangeMatches(MAGIC_FISHEAD_BYTES, d, 0)) return true
			return if (IOUtils.byteRangeMatches(MAGIC_FISBONE_BYTES, d, 0)) true else false

			// Not a known Skeleton special packet
		}

		/**
		 * Creates the appropriate [SkeletonPacket]
		 * instance based on the type.
		 */
		fun create(packet: OggPacket): SkeletonPacket {
			// Special header types detection
			if (isSkeletonSpecial(packet)) {
				val type = packet.data[3]
				when (type) {
					'h'.toByte() // fishead
					-> return SkeletonFishead(packet)
					'b'.toByte() // fisbone
					-> return SkeletonFisbone(packet)
				}
			}

			// Only Skeleton 4+ has key frames
			// Skeleton 3 just has the two fis* packets
			return SkeletonKeyFramePacket(packet)
		}
	}
}