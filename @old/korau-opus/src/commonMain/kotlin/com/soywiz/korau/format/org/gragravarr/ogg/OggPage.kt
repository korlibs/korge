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

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.gragravarr.ogg.IOUtils.readOrEOF
import com.soywiz.korio.stream.*
import kotlin.math.*

class OggPage {
	var sid: Int = 0
		private set
	var sequenceNumber: Int = 0
		private set
	protected var checksum: Long = 0
		private set
	var granulePosition: Long = 0

	private var isBOS: Boolean = false
	private var isEOS: Boolean = false
	/**
	 * Is this carrying on the packets from
	 * a previous page?
	 */
	var isContinuation: Boolean = false
		private set

	/**
	 * For unit testing only!
	 */
	protected var numLVs = 0
		private set
	private var lvs = ByteArray(255)
	private var data: ByteArray? = null
	private lateinit var tmpData: SyncStream

	/**
	 * Is the checksum for the page valid?
	 */
	val isChecksumValid: Boolean
		get() {
			if (checksum == 0L)
				return true

			var crc = CRCUtils.getCRC(header)
			if (data != null && data!!.size > 0) {
				crc = CRCUtils.getCRC(data!!, crc)
			}

			return checksum == crc.toLong()
		}

	/**
	 * How big is the page, including headers?
	 */
	// Header is 27 bytes + number of headers
	// Data size is given by lvs
	val pageSize: Int
		get() {
			var size = minimumPageSize + numLVs
			size += dataSize
			return size
		}
	/**
	 * How big is the page, excluding headers?
	 */
	// Data size is given by lvs
	val dataSize: Int
		get() {
			var size = 0
			for (i in 0 until numLVs) {
				size += IOUtils.toInt(lvs[i])
			}
			return size
		}
	/**
	 * Gets the header, but with a blank CRC field
	 */
	protected// Version
	// Checksum @ 22 left blank for now
	val header: ByteArray
		get() {
			val header = ByteArray(minimumPageSize + numLVs)
			header[0] = 'O'.toByte()
			header[1] = 'g'.toByte()
			header[2] = 'g'.toByte()
			header[3] = 'S'.toByte()

			header[4] = 0

			var flags: Int = 0
			if (isContinuation) {
				flags += 1
			}
			if (isBOS) {
				flags += 2
			}
			if (isEOS) {
				flags += 4
			}
			header[5] = flags.toByte()

			IOUtils.putInt8(header, 6, granulePosition)
			IOUtils.putInt4(header, 14, sid.toLong())
			IOUtils.putInt4(header, 18, sequenceNumber.toLong())

			header[26] = IOUtils.fromInt(numLVs)
			arraycopy(lvs, 0, header, minimumPageSize, numLVs)

			return header
		}


	val packetIterator: OggPacketIterator
		get() = OggPacketIterator(null)

	constructor(sid: Int, seqNum: Int) {
		this.sid = sid
		this.sequenceNumber = seqNum
		this.tmpData = MemorySyncStream()
	}

	/**
	 * InputStream should be positioned *just after*
	 * the OggS capture pattern.
	 */
	constructor(inp: SyncInputStream) {
		val version = readOrEOF(inp)
		if (version != 0) {
			throw IllegalArgumentException("Found Ogg page in format $version but we only support version 0")
		}

		val flags = readOrEOF(inp)
		if (flags and 0x01 == 0x01) {
			isContinuation = true
		}
		if (flags and 0x02 == 0x02) {
			isBOS = true
		}
		if (flags and 0x04 == 0x04) {
			isEOS = true
		}

		granulePosition = IOUtils.getInt(
			readOrEOF(inp), readOrEOF(inp), readOrEOF(inp), readOrEOF(inp),
			readOrEOF(inp), readOrEOF(inp), readOrEOF(inp), readOrEOF(inp)
		)
		sid = IOUtils.getInt(
			readOrEOF(inp), readOrEOF(inp), readOrEOF(inp), readOrEOF(inp)
		).toInt()
		sequenceNumber = IOUtils.getInt(
			readOrEOF(inp), readOrEOF(inp), readOrEOF(inp), readOrEOF(inp)
		).toInt()
		checksum = IOUtils.getInt(
			readOrEOF(inp), readOrEOF(inp), readOrEOF(inp), readOrEOF(inp)
		)

		numLVs = readOrEOF(inp)
		lvs = ByteArray(numLVs)
		IOUtils.readFully(inp, lvs)

		data = ByteArray(dataSize)
		IOUtils.readFully(inp, data!!)
	}

	/**
	 * Adds as much of the packet's data as
	 * we can do.
	 */
	fun addPacket(packet: OggPacket, offset: Int): Int {
		var offset = offset
		if (packet.isBeginningOfStream) {
			isBOS = true
		}
		if (packet.isEndOfStream) {
			isEOS = true
		}

		// Add on in 255 byte chunks
		val size = packet.data.size
		for (i in numLVs..254) {
			val remains = size - offset

			var toAdd = 255
			if (remains < 255) {
				toAdd = remains
			}
			lvs[i] = IOUtils.fromInt(toAdd)
			tmpData!!.write(packet.data, offset, toAdd)

			numLVs++
			offset += toAdd
			if (toAdd < 255) {
				break
			}
		}

		return offset
	}

	/**
	 * Does this Page have space for the given
	 * number of bytes?
	 */
	protected fun hasSpaceFor(bytes: Int): Boolean {
		// Do we have enough lvs spare?
		// (Each LV holds up to 255 bytes, and we're
		//  not allowed more than 255 of them)
		val reqLVs = ceil(bytes / 255.0).toInt()

		return if (numLVs + reqLVs > 255) {
			false
		} else true
	}

	fun getData(): ByteArray? {
		if (tmpData != null) {
			if (data == null || (tmpData!!.base as MemorySyncStreamBase).data.size != data!!.size) {
				data = tmpData!!.toByteArray()
			}
		}
		return data
	}

	/**
	 * Is there a subsequent page containing the
	 * remainder of the packets?
	 */
	fun hasContinuation(): Boolean {
		// Has a continuation if the last LV
		//  is 255.
		// Normally one would expect to have
		//  the full 255 LVs, with the
		//  last one at 255, but technically
		//  you can force a continue without
		//  using all your LVs up
		if (numLVs == 0) {
			return false
		}
		return if (IOUtils.toInt(lvs[numLVs - 1]) == 255) {
			true
		} else false
	}

	fun setIsContinuation() {
		isContinuation = true
	}

	/**
	 * This should only ever be called by
	 * [OggPacketWriter.close] !
	 */
	fun setIsEOS() {
		isEOS = true
	}


	fun writeHeader(out: SyncOutputStream) {
		val header = header

		// Ensure we've moved from tmpdata to data
		getData()

		// Generate the checksum and store
		var crc = CRCUtils.getCRC(header)
		if (data != null && data!!.size > 0) {
			crc = CRCUtils.getCRC(data!!, crc)
		}
		IOUtils.putInt4(header, 22, crc.toLong())
		checksum = crc.toLong()

		// Write out
		out.write(header)
	}


	override fun toString(): String {
		return "Ogg Page - " + sid + " @ " + sequenceNumber +
				" - " + numLVs + " LVs"
	}

	fun getPacketIterator(previousPart: OggPacketData): OggPacketIterator {
		return OggPacketIterator(previousPart)
	}

	/**
	 * Returns a full [OggPacket] if it can, otherwise
	 * just the [OggPacketData] if the rest of the
	 * packet is in another [OggPage]
	 */
	inner class OggPacketIterator(private var prevPart: OggPacketData?) :
		Iterator<OggPacketData> {
		private var currentLV = 0
		private var currentOffset = 0

		override fun hasNext(): Boolean {
			if (currentLV < numLVs) {
				return true
			}
			// Special case for an empty page
			return if (currentLV == 0 && numLVs == 0) {
				true
			} else false

		}

		override fun next(): OggPacketData {
			var continues = false
			var packetLVs = 0
			var packetSize = 0

			// How much data to we have?
			for (i in currentLV until numLVs) {
				val size = IOUtils.toInt(lvs[i])
				packetSize += size
				packetLVs++

				if (size < 255) {
					break
				}
				if (i == numLVs - 1 && size == 255) {
					continues = true
				}
			}

			// Get the data
			var pd = ByteArray(packetSize)
			for (i in currentLV until currentLV + packetLVs) {
				val size = IOUtils.toInt(lvs[i])
				val offset = (i - currentLV) * 255
				arraycopy(data!!, currentOffset + offset, pd, offset, size)
			}
			// Tack on anything spare from last time too
			if (prevPart != null) {
				val prevSize = prevPart!!.data.size
				val fpd = ByteArray(prevSize + pd.size)
				arraycopy(prevPart!!.data, 0, fpd, 0, prevSize)
				arraycopy(pd, 0, fpd, prevSize, pd.size)

				prevPart = null
				pd = fpd
			}

			// Create
			val packet: OggPacketData
			if (continues) {
				packet = OggPacketData(pd)
			} else {
				var packetBOS = false
				var packetEOS = false
				if (isBOS && currentLV == 0) {
					packetBOS = true
				}
				if (isEOS && currentLV + packetLVs == numLVs) {
					packetEOS = true
				}

				packet = OggPacket(this@OggPage, pd, packetBOS, packetEOS)
			}

			// Wind on
			currentLV += packetLVs
			currentOffset += packetSize
			// Empty page special case wind-on
			if (currentLV == 0)
				currentLV = 1

			// Done!
			return packet
		}

		//override fun remove() {
		//    throw IllegalStateException("Remove not supported")
		//}
	}

	companion object {
		/**
		 * Returns the minimum size of a page, which is 27
		 * bytes for the headers
		 */
		val minimumPageSize = 27
	}
}
