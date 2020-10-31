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
import com.soywiz.korau.format.org.concentus.internal.*
import kotlin.math.*

/**
 * Represents a logical group of data.
 * RFC3533 suggests that these should usually be
 * around 50-200 bytes long.
 */
class OggPacket : OggPacketData {
	private var parent: OggPage? = null // Last page if split
	/**
	 * Is this the first packet in the stream?
	 * If so, the data should hold the magic
	 * information required to identify which
	 * decoder will be needed.
	 */
	var isBeginningOfStream: Boolean = false
		private set
	/**
	 * Is this the last packet in the stream?
	 */
	var isEndOfStream: Boolean = false
		private set

	/**
	 * Returns the Stream ID (Sid) that
	 * this packet belongs to.
	 */
	val sid: Int
		get() = parent!!.sid
	/**
	 * Returns the granule position of the page
	 * that this packet belongs to. The meaning
	 * of the granule depends on the codec.
	 */
	val granulePosition: Long
		get() = parent!!.granulePosition
	/**
	 * Returns the sequence number within the stream
	 * of the page that this packet belongs to.
	 * You can use this to detect when pages have
	 * been lost.
	 */
	val sequenceNumber: Int
		get() = parent!!.sequenceNumber

	/**
	 * Returns the number of bytes overhead of the [OggPage]
	 * we belong to, if we're the only packet in the page, or
	 * a rough guess if we span multiple pages / share a page.
	 */
	// We don't have a page to ourselves, so we can't come up
	//  with a fully accurate overhead at this stage, so get close
	// Do we span multiple pages?
	// Take a best guess, rounding up
	// We're probably just a part of a larger page
	// Take a rough guess
	// Take the current page's overhead, scale as needed, and return
	val overheadBytes: Int
		get() {
			if (parent == null) return 0

			var ourShare = 1.0
			val ourDataLen = data.size
			val pageDataLen = parent!!.dataSize
			if (pageDataLen != ourDataLen) {
				if (ourDataLen > pageDataLen) {
					val approxPages = ceil((ourDataLen / pageDataLen).toDouble()).toInt()
					ourShare = approxPages.toDouble()
				} else {
					ourShare = ourDataLen.toDouble() / pageDataLen
				}
			}
			return rint(ourShare * (parent!!.pageSize - pageDataLen)).toInt()
		}

	/**
	 * Creates a new Ogg Packet based on data read
	 * from within an Ogg Page.
	 */
	constructor(parent: OggPage, data: ByteArray, bos: Boolean, eos: Boolean) : super(data) {
		this.parent = parent
		this.isBeginningOfStream = bos
		this.isEndOfStream = eos
	}

	/**
	 * Creates a new Ogg Packet filled with data to
	 * be later written.
	 * The Sid, and begin/end flags will be available
	 * after the packet has been flushed.
	 */
	constructor(data: ByteArray) : super(data) {}

	fun setParent(parent: OggPage) {
		this.parent = parent
	}

	fun setIsBOS() {
		this.isBeginningOfStream = true
	}

	fun setIsEOS() {
		this.isEndOfStream = true
	}

	/** Unit tests only!  */
	protected fun _getParent(): OggPage? {
		return parent
	}
}
