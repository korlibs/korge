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
import com.soywiz.korio.stream.*
import kotlin.random.Random

/**
 * This class takes care of reading and writing
 * files using the Ogg container format.
 */
class OggFile(val warningProcessor: ((String) -> Unit)?) : Closeable {
	private var inp: SyncInputStream? = null
	private var out: SyncOutputStream? = null
	private var writing = true

	private val seenSIDs = HashSet<Int>()

	/**
	 * Returns a reader that will allow you to read packets
	 * from the file, across all Logical Bit Streams,
	 * in the order that they occur.
	 */
	val packetReader: OggPacketReader
		get() {
			if (writing || inp == null) {
				throw IllegalStateException("Can only read from a file opened with an InputStream")
			}
			return OggPacketReader(inp!!, warningProcessor)
		}

	/**
	 * Creates a new Logical Bit Stream in the file,
	 * and returns a Writer for putting data
	 * into it.
	 */
	val packetWriter: OggPacketWriter
		get() = getPacketWriter(unusedSerialNumber)


	/**
	 * Returns a random, but previously un-used serial
	 * number for use by a new stream
	 */
	protected val unusedSerialNumber: Int
		get() {
			while (true) {
				val sid = (Random.nextDouble() * Short.MAX_VALUE).toInt()
				if (!seenSIDs.contains(sid)) {
					return sid
				}
			}
		}

	/**
	 * Opens a file for writing.
	 * Call [.getPacketWriter] to
	 * begin writing your data.
	 */
	constructor(output: SyncOutputStream, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		this.out = output
		this.writing = true
	}

	/**
	 * Opens a file for reading in
	 * blocking (non event) mode.
	 * Call [.getPacketReader] to
	 * begin reading the file.
	 */
	constructor(input: SyncInputStream, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		this.inp = input
		this.writing = false
	}

	/**
	 * Opens a file for reading in non-blocking
	 * (event) mode.
	 * Will begin processing the file and notifying
	 * your listener immediately.
	 */
	constructor(input: SyncInputStream, listener: OggStreamListener, warningProcessor: ((String) -> Unit)?) : this(input, warningProcessor) {

		val readers = HashMap<Int, Array<OggStreamReader>>()
		val reader = packetReader
		var packet: OggPacket? = null
		while (true) {
			packet = reader.getNextPacket() ?: break
			if (packet!!.isBeginningOfStream) {
				val streams = listener.processNewStream(packet!!.sid, packet!!.data)
				if (streams != null && streams!!.size > 0) {
					readers[packet!!.sid] = streams
				}
			} else {
				val streams = readers[packet!!.sid]
				if (streams != null) {
					for (r in streams) {
						r.processPacket(packet)
					}
				}
			}
			if (packet!!.isEndOfStream) {
				listener.processStreamEnd(packet!!.sid)
			}
		}
	}


	/**
	 * Closes our streams. It's up to you
	 * to close any [OggPacketWriter] instances
	 * first!
	 */
	override fun close() {
		if (inp != null)
			inp!!.close()
		if (out != null)
			out!!.close()
	}

	/**
	 * Creates a new Logical Bit Stream in the file,
	 * and returns a Writer for putting data
	 * into it.
	 */
	fun getPacketWriter(sid: Int): OggPacketWriter {
		if (!writing) {
			throw IllegalStateException("Can only write to a file opened with an OutputStream")
		}
		seenSIDs.add(sid)
		return OggPacketWriter(this, sid)
	}

	/**
	 * Writes a (possibly series) of pages to the
	 * stream in one go.
	 */
	fun writePages(pages: Array<OggPage>) {
		for (page in pages) {
			page.writeHeader(out!!)
			out!!.write(page.getData()!!)
		}
		out!!.flush()
	}
}
