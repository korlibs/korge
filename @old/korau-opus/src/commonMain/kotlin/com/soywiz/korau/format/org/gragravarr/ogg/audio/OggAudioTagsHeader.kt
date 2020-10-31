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
 * Common interface for the Tags (Comments) header near
 * the start of an [OggAudioStream]
 */
interface OggAudioTagsHeader : OggStreamPacket {
	val vendor: String?

	/**
	 * Returns the (first) Artist, or null if no
	 * Artist tags present.
	 */
	val artist: String?
	/**
	 * Returns the (first) Album, or null if no
	 * Album tags present.
	 */
	val album: String?
	/**
	 * Returns the (first) Title, or null if no
	 * Title tags present.
	 */
	val title: String?
	/**
	 * Returns the (first) Genre, or null if no
	 * Genre tags present.
	 */
	val genre: String?
	/**
	 * Returns the (first) track number as a literal
	 * string, eg "4" or "09", or null if
	 * no track number tags present;
	 */
	val trackNumber: String?
	/**
	 * Returns the track number, as converted into
	 * an integer, or -1 if not available / not numeric
	 */
	val trackNumberNumeric: Int
	/**
	 * Returns the (first) Date, or null if no
	 * Date tags present. Dates are normally stored
	 * in ISO8601 date format, i.e. YYYY-MM-DD
	 */
	val date: String?
	/**
	 * Returns all the comments, across all tags
	 */
	val allComments: Map<String, ArrayList<String>>

	/**
	 * Returns all comments for a given tag, in
	 * file order. Will return an empty list for
	 * tags which aren't present.
	 */
	fun getComments(tag: String): List<String>
}
