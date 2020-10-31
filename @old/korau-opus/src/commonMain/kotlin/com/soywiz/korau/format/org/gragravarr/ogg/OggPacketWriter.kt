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

import com.soywiz.korio.lang.*

class OggPacketWriter(private val file: OggFile, val sid: Int) : Closeable {
	private var closed = false
	private var doneFirstPacket = false
	private var sequenceNumber: Int = 0
	var currentGranulePosition: Long = 0
		private set

	private val buffer = ArrayList<OggPage>()

	/**
	 * Returns the number of bytes (excluding headers)
	 * currently waiting to be written to disk.
	 * RFC 3533 suggests that pages should normally
	 * be in the 4-8kb range.
	 * If this size exceeds just shy of 64kb, then
	 * multiple pages will be needed in the underlying
	 * stream.
	 */
	val sizePendingFlush: Int
		get() {
			var size = 0
			for (p in buffer) {
				size += p.dataSize
			}
			return size
		}

	/**
	 * Returns the size of the page currently being written
	 * to, including its headers.
	 * For a new stream, or a stream that has just been
	 * flushed, will return zero.
	 * @return Current page size, or 27 (the minimum) if no current page
	 */
	val currentPageSize: Int
		get() {
			if (buffer.isEmpty()) return OggPage.minimumPageSize

			val p = buffer[buffer.size - 1]
			return p.pageSize
		}

	init {

		this.sequenceNumber = 0
	}

	/**
	 * Sets the current granule position.
	 * The granule position will be applied to all
	 * un-flushed packets, and all future packets.
	 * As such, you should normally either call a flush
	 * just before or just after this call.
	 */
	fun setGranulePosition(position: Long) {
		currentGranulePosition = position
		for (p in buffer) {
			p.granulePosition = position
		}
	}

	private fun getCurrentPage(forceNew: Boolean): OggPage {
		if (buffer.size == 0 || forceNew) {
			val page = OggPage(sid, sequenceNumber++)
			if (currentGranulePosition > 0) {
				page.granulePosition = currentGranulePosition
			}
			buffer.add(page)
			return page
		}
		return buffer[buffer.size - 1]
	}

	/**
	 * Buffers the given packet up ready for
	 * writing to the stream, but doesn't
	 * write it to disk yet. The granule position
	 * is updated on the page.
	 * If writing the packet requires a new page,
	 * then the updated granule position only
	 * applies to the new page
	 */
	fun bufferPacket(packet: OggPacket, granulePosition: Long = currentGranulePosition) {
		if (closed) {
			throw IllegalStateException("Can't buffer packets on a closed stream!")
		}
		if (!doneFirstPacket) {
			packet.setIsBOS()
			doneFirstPacket = true
		}

		val size = packet.data.size
		var emptyPacket = size == 0

		// Add to pages in turn
		var page = getCurrentPage(false)
		var pos = 0
		while (pos < size || emptyPacket) {
			pos = page.addPacket(packet, pos)
			if (pos < size) {
				page = getCurrentPage(true)
				page.setIsContinuation()
			}
			page.granulePosition = granulePosition
			emptyPacket = false
		}
		currentGranulePosition = granulePosition
		packet.setParent(page)
	}

	/**
	 * Buffers the given packet up ready for
	 * writing to the file, and then writes
	 * it to the stream if indicated.
	 */
	fun bufferPacket(packet: OggPacket, flush: Boolean) {
		bufferPacket(packet)
		if (flush) {
			flush()
		}
	}

	/**
	 * Writes all pending packets to the stream,
	 * splitting across pages as needed.
	 */
	fun flush() {
		if (closed) {
			throw IllegalStateException("Can't flush packets on a closed stream!")
		}

		// Write in one go
		val pages = buffer.toTypedArray<OggPage>()
		file.writePages(pages)

		// Get ready for next time!
		buffer.clear()
	}

	/**
	 * Writes all pending packets to the stream,
	 * with the last one containing the End Of Stream
	 * Flag, and then closes down.
	 */
	override fun close() {
		if (buffer.size > 0) {
			buffer[buffer.size - 1].setIsEOS()
		} else {
			val p = OggPacket(ByteArray(0))
			p.setIsEOS()
			bufferPacket(p)
		}
		flush()

		closed = true
	}
}
/**
 * Buffers the given packet up ready for
 * writing to the stream, but doesn't
 * write it to disk yet. The granule
 * position is unchanged.
 */
