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
package com.soywiz.korau.format.org.gragravarr.ogg.audio

import com.soywiz.korau.format.org.gragravarr.ogg.*


/**
 * Interface for reading the headers at the start of an
 * [OggAudioStream]
 */
interface OggAudioHeaders {
	/**
	 * @return The stream id of the overall audio stream
	 */
	val sid: Int
	/**
	 * @return The type of the audio stream
	 */
	val type: OggStreamIdentifier.OggStreamType
	/**
	 * @return The information / identification of the stream and audio encoding
	 */
	val info: OggAudioInfoHeader?
	/**
	 * @return The Tags / Comments describing the stream
	 */
	val tags: OggAudioTagsHeader?
	/**
	 * @return The Setup information for the audio encoding, if used in the format
	 */
	val setup: OggAudioSetupHeader?
}
