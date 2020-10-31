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

/**
 * Raw, compressed audio and video data, which keeps track of a
 * granule position
 */
open class OggStreamAudioVisualData(pkt: OggPacket?, data: ByteArray?) : HighLevelOggStreamPacket(pkt, data) {
	/**
	 * Returns the granule position, or -1 if
	 * this wasn't read from a file
	 */
	var granulePosition: Long = pkt?.granulePosition ?: -1
}
