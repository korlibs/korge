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
package com.soywiz.korau.format.org.gragravarr.theora

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * Parent of all Theora (video) packets
 */
interface TheoraPacket : OggStreamPacket {
	companion object {
		val TYPE_IDENTIFICATION = 0x80
		val TYPE_COMMENTS = 0x81
		val TYPE_SETUP = 0x82
	}
}
