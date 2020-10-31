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

import com.soywiz.korio.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.stream.*

class OggPacketReader(private val inp: SyncInputStream, val warningProcessor: ((String) -> Unit)?) {
	private var it: Iterator<OggPacketData>? = null
	private var nextPacket: OggPacket? = null

	/**
	 * Returns the next packet in the file, or
	 * null if no more packets remain.
	 * Call [OggPacket.isBeginningOfStream]
	 * to detect if it is the first packet in the
	 * stream or not, and use
	 * [OggPacket.getSid] to track which
	 * stream it belongs to.
	 */
	fun getNextPacket(): OggPacket? {
		// If we skipped to a point in the stream, and
		//  have a packet waiting, return that
		if (nextPacket != null) {
			val p = nextPacket
			nextPacket = null
			return p
		}

		// If we're already part way through a page,
		//  then fetch the next packet. If it's a
		//  full one, then we're done.
		var leftOver: OggPacketData? = null
		if (it != null && it!!.hasNext()) {
			val packet = it!!.next()
			if (packet is OggPacket) {
				return packet as OggPacket
			}
			leftOver = packet
		}

		// Find the next page, from which
		//  to get our next packet from
		var searched = 0
		var pos = -1
		var found = false
		var r: Int
		while (searched < 65536 && !found) {
			r = inp.read()
			if (r == -1) {
				// No more data
				return null
			}

			when (pos) {
				-1 -> if (r == 'O'.toInt()) {
					pos = 0
				}
				0 -> if (r == 'g'.toInt()) {
					pos = 1
				} else {
					pos = -1
				}
				1 -> if (r == 'g'.toInt()) {
					pos = 2
				} else {
					pos = -1
				}
				2 -> if (r == 'S'.toInt()) {
					found = true
				} else {
					pos = -1
				}
			}

			if (!found) {
				searched++
			}
		}

		if (!found) {
			throw IOException("Next ogg packet header not found after searching $searched bytes")
		}

		searched -= 3 // OggS
		if (searched > 0) {
			warningProcessor?.invoke("Warning - had to skip $searched bytes of junk data before finding the next packet header")
		}

		// Create the page, and prime the iterator on it
		try {
			val page = OggPage(inp)
			if (!page.isChecksumValid) {
				warningProcessor?.invoke("Warning - invalid checksum on page ${page.sequenceNumber} of stream ${page.sid.shex} (${page.sid})")
			}
			it = page.getPacketIterator(leftOver!!)
			return getNextPacket()
		} catch (eof: EOFException) {
			warningProcessor?.invoke("Warning - data ended mid-page: ${eof.message}")
			return null
		}

	}

	/**
	 * Returns the next packet with the given SID (Stream ID), or
	 * null if no more packets remain.
	 * Any packets from other streams will be silently discarded.
	 */
	fun getNextPacketWithSid(sid: Int): OggPacket? {
		var p: OggPacket? = null
		while (true) {
			p = getNextPacket() ?: break
			if (p!!.sid == sid) {
				return p
			}
		}
		return null
	}

	/**
	 * Un-reads a packet, leaving it ready to be feteched by the
	 * next call to [.getNextPacket].
	 * Only one packet may be unread.
	 * Normally used when identifying a stream, to leave the
	 * initial packet ready for a decoder
	 */
	fun unreadPacket(packet: OggPacket) {
		if (nextPacket != null) {
			throw IllegalStateException("Can't un-read twice")
		}
		nextPacket = packet
	}

	/**
	 * Skips forward until the first packet with a Sequence Number
	 * of equal or greater than that specified. Call [.getNextPacket]
	 * to retrieve this packet.
	 * This method advances across all streams, but only searches the
	 * specified one.
	 * @param sid The ID of the stream who's packets we will search
	 * @param sequenceNumber The sequence number we're looking for
	 */
	fun skipToSequenceNumber(sid: Int, sequenceNumber: Int) {
		var p: OggPacket? = null
		while (true) {
			p = getNextPacket() ?: break
			if (p!!.sid == sid && p!!.sequenceNumber >= sequenceNumber) {
				nextPacket = p
				break
			}
		}
	}

	/**
	 * Skips forward until the first packet with a Granule Position
	 * of equal or greater than that specified. Call [.getNextPacket]
	 * to retrieve this packet.
	 * This method advances across all streams, but only searches the
	 * specified one.
	 * @param sid The ID of the stream who's packets we will search
	 * @param granulePosition The granule position we're looking for
	 */
	fun skipToGranulePosition(sid: Int, granulePosition: Long) {
		var p: OggPacket? = null
		while (true) {
			p = getNextPacket() ?: break
			if (p!!.sid == sid && p!!.granulePosition >= granulePosition) {
				nextPacket = p
				break
			}
		}
	}
}
