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

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * The Fisbone (note - no h) provides details about
 * what the other streams in the file are.
 * See http://wiki.xiph.org/SkeletonHeaders for a list of
 * the suggested "Message Headers", and what they mean.
 */
class SkeletonFisbone : HighLevelOggStreamPacket, SkeletonPacket {

	private var messageHeaderOffset: Int = 0
	var serialNumber: Int = 0
	var numHeaderPackets: Int = 0
	var granulerateNumerator: Long = 0
	var granulerateDenominator: Long = 0
	var baseGranule: Long = 0
	var preroll: Int = 0
	var granuleShift: Byte = 0

	private var contentType: String? = null
	private val messageHeaders = HashMap<String, String>()

	constructor() : super() {
		messageHeaderOffset = MESSAGE_HEADER_OFFSET
		contentType = OggStreamIdentifier.UNKNOWN.mimetype
	}

	constructor(pkt: OggPacket) : super(pkt, null) {

		// Verify the type
		val data = this.data!!
		if (!IOUtils.byteRangeMatches(SkeletonPacket.MAGIC_FISBONE_BYTES, data, 0)) {
			throw IllegalArgumentException("Invalid type, not a Skeleton Fisbone Header")
		}

		// Parse
		messageHeaderOffset = IOUtils.getInt4(data!!, 8).toInt()
		if (messageHeaderOffset != MESSAGE_HEADER_OFFSET) {
			throw IllegalArgumentException("Unsupported Skeleton message offset $messageHeaderOffset detected")
		}

		serialNumber = IOUtils.getInt4(data, 12).toInt()
		numHeaderPackets = IOUtils.getInt4(data, 16).toInt()
		granulerateNumerator = IOUtils.getInt8(data, 20)
		granulerateDenominator = IOUtils.getInt8(data, 28)
		baseGranule = IOUtils.getInt8(data, 36)
		preroll = IOUtils.getInt4(data, 44).toInt()
		granuleShift = data[48]
		// Next 3 are padding

		// Rest should be the message headers, in html/mime style
		val headers = IOUtils.getUTF8(data, 52, data.size - 52)
		if (!headers.contains(HEADER_CONTENT_TYPE)) {
			throw IllegalArgumentException("No Content Type header found in $headers")
		}
		for (line in headers.split("\r\n")) {
			val splitAt = line.indexOf(": ")
			val k = line.substring(0, splitAt)
			val v = line.substring(splitAt + 2)
			messageHeaders[k] = v

			if (HEADER_CONTENT_TYPE == k) {
				contentType = v
			}
		}
	}

	override fun write(): OggPacket {
		// Encode the message headers first
		val headersStr = StringBuilder()
		for (k in messageHeaders.keys) {
			headersStr.append(k)
			headersStr.append(": ")
			headersStr.append(messageHeaders[k])
			headersStr.append("\r\n")
		}
		val headers = IOUtils.toUTF8Bytes(headersStr.toString())

		// Now calculate the size
		val size = 52 + headers.size

		val data = ByteArray(size)
		arraycopy(SkeletonPacket.MAGIC_FISBONE_BYTES, 0, data, 0, 8)

		IOUtils.putInt4(data, 8, messageHeaderOffset)
		IOUtils.putInt4(data, 12, serialNumber)
		IOUtils.putInt4(data, 16, numHeaderPackets)
		IOUtils.putInt8(data, 20, granulerateNumerator)
		IOUtils.putInt8(data, 28, granulerateDenominator)
		IOUtils.putInt8(data, 36, baseGranule)
		IOUtils.putInt4(data, 44, preroll)
		data[48] = granuleShift
		// Next 3 are zero padding

		// Finally the message headers
		arraycopy(headers, 0, data, 52, headers.size)

		this.data = data
		return super.write()
	}

	fun getContentType(): String? {
		return contentType
	}

	fun setContentType(contentType: String) {
		this.contentType = contentType
		messageHeaders[HEADER_CONTENT_TYPE] = contentType
	}

	/**
	 * Provides read and write access to the Message Headers,
	 * which are used to describe the stream.
	 * http://wiki.xiph.org/SkeletonHeaders provides documentation
	 * on the common headers, and the meaning of their values.
	 */
	fun getMessageHeaders(): Map<String, String> {
		return messageHeaders
	}

	companion object {
		private val MESSAGE_HEADER_OFFSET = 52 - SkeletonPacket.MAGIC_FISBONE_BYTES.size
		private val HEADER_CONTENT_TYPE = "Content-Type"
	}
}
