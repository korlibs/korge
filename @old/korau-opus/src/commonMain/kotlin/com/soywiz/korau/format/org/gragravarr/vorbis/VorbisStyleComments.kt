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
package com.soywiz.korau.format.org.gragravarr.vorbis

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korio.stream.*

/**
 * General class for all Vorbis-style comments/tags, as used
 * by things like Vorbis, Opus and FLAC.
 */
abstract class VorbisStyleComments : HighLevelOggStreamPacket, OggAudioTagsHeader {

	override var vendor: String? = null
	private var comments = HashMap<String, ArrayList<String>>()


	/**
	 * Returns the (first) Artist, or null if no
	 * Artist tags present.
	 */
	override val artist: String?
		get() = getSingleComment(KEY_ARTIST)
	/**
	 * Returns the (first) Album, or null if no
	 * Album tags present.
	 */
	override val album: String?
		get() = getSingleComment(KEY_ALBUM)
	/**
	 * Returns the (first) Title, or null if no
	 * Title tags present.
	 */
	override val title: String?
		get() = getSingleComment(KEY_TITLE)
	/**
	 * Returns the (first) Genre, or null if no
	 * Genre tags present.
	 */
	override val genre: String?
		get() = getSingleComment(KEY_GENRE)
	/**
	 * Returns the (first) track number as a literal
	 * string, eg "4" or "09", or null if
	 * no track number tags present;
	 */
	override val trackNumber: String?
		get() = getSingleComment(KEY_TRACKNUMBER)
	/**
	 * Returns the track number, as converted into
	 * an integer, or -1 if not available / not numeric
	 */
	override val trackNumberNumeric: Int
		get() {
			val number = trackNumber ?: return -1
			return number.toIntOrNull() ?: -1

		}
	/**
	 * Returns the (first) Date, or null if no
	 * Date tags present. Dates are normally stored
	 * in ISO8601 date format, i.e. YYYY-MM-DD
	 */
	override val date: String?
		get() = getSingleComment("date")


	/**
	 * Returns all the comments
	 */
	override val allComments: Map<String, ArrayList<String>>
		get() = comments

	protected abstract val headerSize: Int

	constructor(pkt: OggPacket, dataBeginsAt: Int) : super(pkt, null) {
		val d = pkt.data!!

		val vlen = getInt4(d, dataBeginsAt)
		vendor = IOUtils.getUTF8(d, dataBeginsAt + 4, vlen)

		var offset = dataBeginsAt + 4 + vlen
		val numComments = getInt4(d, offset)
		offset += 4

		for (i in 0 until numComments) {
			val len = getInt4(d, offset)
			offset += 4
			val c = IOUtils.getUTF8(d, offset, len)
			offset += len

			val equals = c.indexOf('=')
			if (equals == -1) {
				//println("Warning - unable to parse comment '$c'")
			} else {
				val tag = normaliseTag(c.substring(0, equals))
				val value = c.substring(equals + 1)
				addComment(tag, value)
			}
		}

		if (offset < d.size && hasFramingBit()) {
			val framingBit = d[offset]
			if (framingBit.toInt() == 0) {
				throw IllegalArgumentException("Framing bit not set, invalid")
			}
		}
	}

	constructor() : super() {
		vendor = "Gagravarr.org Java Vorbis Tools v0.8 20160217"
	}

	protected fun getSingleComment(normalisedTag: String): String? {
		val c = comments[normalisedTag]
		return if (c != null && c.size > 0) {
			c[0]
		} else null
	}

	/**
	 * Returns all comments for a given tag, in
	 * file order. Will return an empty list for
	 * tags which aren't present.
	 */
	override fun getComments(tag: String): List<String> {
		val c = comments[normaliseTag(tag)]
		return c ?: ArrayList()
	}

	/**
	 * Removes all comments for a given tag.
	 */
	fun removeComments(tag: String) {
		comments.remove(normaliseTag(tag))
	}

	/**
	 * Removes all comments across all tags
	 */
	fun removeAllComments() {
		comments.clear()
	}

	/**
	 * Adds a comment for a given tag
	 */
	fun addComment(tag: String, comment: String) {
		val nt = normaliseTag(tag)
		if (!comments.containsKey(nt)) {
			comments[nt] = ArrayList()
		}
		comments[nt]!!.add(comment)
	}

	/**
	 * Removes any existing comments for a given tag,
	 * and replaces them with the supplied list
	 */
	fun setComments(tag: String, comments: List<String>) {
		val nt = normaliseTag(tag)
		if (this.comments.containsKey(nt)) {
			this.comments.remove(nt)
		}
		this.comments[nt] = ArrayList(comments)
	}

	protected abstract fun hasFramingBit(): Boolean
	protected abstract fun populateMetadataHeader(data: ByteArray, packetLength: Int)
	protected abstract fun populateMetadataFooter(out: SyncOutputStream)

	protected fun getInt4(d: ByteArray, offset: Int): Int {
		return IOUtils.getInt4(d, offset).toInt()
	}

	override fun write(): OggPacket {
		// Serialise the comments
		val data = MemorySyncStreamToByteArray {
			val baos = this
			try {
				// Pad for the header (size isn't known yet, so can't fully write)
				val headerPadding = ByteArray(headerSize)
				baos.write(headerPadding)

				// Do the vendor string
				IOUtils.writeUTF8WithLength(baos, vendor!!)

				// Next is the number of comments
				var numComments = 0
				for (c in comments.values) {
					numComments += c.size
				}
				IOUtils.writeInt4(baos, numComments.toLong())

				// Write out the tags. While the spec doesn't require
				//  an order, unit testing does!
				val tags = comments.keys.toTypedArray()
				tags.sort()
				for (tag in tags) {
					for (value in comments!![tag]!!) {
						val comment = tag + '='.toString() + value

						IOUtils.writeUTF8WithLength(baos, comment)
					}
				}

				// Do a header, if required for the format
				populateMetadataFooter(baos)
			} catch (e: Throwable) {
				// Should never happen!
				throw RuntimeException(e)
			}
		}
		populateMetadataHeader(data, data.size)

		// Record the data
		this.data = data

		// Now write
		return super.write()
	}

	companion object {
		val KEY_ARTIST = "artist"
		val KEY_ALBUM = "album"
		val KEY_TITLE = "title"
		val KEY_GENRE = "genre"
		val KEY_TRACKNUMBER = "tracknumber"
		val KEY_DATE = "date"

		/**
		 * The tag name is case-insensitive and may consist of ASCII 0x20
		 * through 0x7D, 0x3D (’=’) excluded. ASCII 0x41 through 0x5A
		 * inclusive (characters A-Z) is to be considered equivalent to
		 * ASCII 0x61 through 0x7A inclusive (characters a-z).
		 */
		protected fun normaliseTag(tag: String): String {
			val nt = StringBuilder()
			for (c in tag.toLowerCase()) {
				if (c.toInt() in 0x20..0x7d &&
					c.toInt() != 0x3d
				) {
					nt.append(c)
				}
			}
			return nt.toString()
		}
	}
}
