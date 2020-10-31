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
package com.soywiz.korau.format.org.gragravarr.flac

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korio.stream.*

/**
 * This lets you work with FLAC files that
 * are contained in an Ogg Stream
 */
class FlacOggFile : FlacFile, OggAudioHeaders {
	/**
	 * Returns the underlying Ogg File instance
	 * @return
	 */
	var oggFile: OggFile? = null
		private set
	private var r: OggPacketReader? = null
	private var w: OggPacketWriter? = null
	/**
	 * Returns the Ogg Stream ID
	 */
	override var sid = -1
		private set

	/**
	 * Returns the first Ogg Packet, which has some metadata in it
	 */
	var firstPacket: FlacFirstOggPacket? = null
		private set
	private var writtenAudio: MutableList<FlacAudioFrame> = arrayListOf()

	override val nextAudioPacket: FlacAudioFrame?
		get() {
			var p: OggPacket? = null
			while (true) {
				p = r!!.getNextPacketWithSid(sid) ?: break
				return FlacAudioFrame(p!!.data, this.info2!!)
			}
			return null
		}

	/**
	 * This is a Flac-in-Ogg file
	 */
	override val type: OggStreamIdentifier.OggStreamType
		get() = OggStreamIdentifier.OGG_FLAC

	override val info: OggAudioInfoHeader?
		//get() = info2
		get() = TODO("korau FIXME!")

	/**
	 * Flac doesn't have setup packets per-se, so return null
	 */
	override val setup: OggAudioSetupHeader?
		get() = null

	/**
	 * Opens the given file for reading
	 */
	constructor(ogg: OggFile) : this(ogg.packetReader) {
		this.oggFile = ogg
	}

	/**
	 * Loads a Vorbis File from the given packet reader.
	 */
	constructor(r: OggPacketReader) {
		this.r = r

		var p: OggPacket? = null
		while (true) {
			p = r.getNextPacket() ?: break
			if (p!!.isBeginningOfStream && p!!.data.size > 10) {
				if (FlacFirstOggPacket.isFlacStream(p!!)) {
					sid = p!!.sid
					break
				}
			}
		}

		// First packet is special
		firstPacket = FlacFirstOggPacket(p!!)
		this.info2 = firstPacket!!.info

		// Next must be the Tags (Comments)
		tags = FlacTags(r.getNextPacketWithSid(sid)!!)

		// Then continue until the last metadata
		otherMetadata = ArrayList<FlacMetadataBlock>()
		while (true) {
			p = r.getNextPacketWithSid(sid) ?: break
			val block = FlacMetadataBlock.create(p!!.data.openSync())
			otherMetadata!!.add(block)
			if (block.isLastMetadataBlock) {
				break
			}
		}

		// Everything else should be audio data
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file. The Steam ID (SID) is
	 * automatically allocated for you.
	 */
	constructor(out: SyncOutputStream, info: FlacOggInfo = FlacOggInfo(), tags: FlacTags = FlacTags(), warningProcessor: ((String) -> Unit)?) : this(
		out,
		-1,
		info,
		tags,
		warningProcessor
	) {
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file, with a specific
	 * Steam ID (SID). You should only set the SID
	 * when copying one file to another!
	 */
	constructor(out: SyncOutputStream, sid: Int, info: FlacOggInfo, tags: FlacTags, warningProcessor: ((String) -> Unit)?) {
		oggFile = OggFile(out, warningProcessor)

		if (sid > 0) {
			w = oggFile!!.getPacketWriter(sid)
			this.sid = sid
		} else {
			w = oggFile!!.packetWriter
			this.sid = w!!.sid
		}

		writtenAudio = ArrayList<FlacAudioFrame>()

		this.firstPacket = FlacFirstOggPacket(info)
		this.info2 = info
		this.tags = tags
	}

	/**
	 * Skips the audio data to the next packet with a granule
	 * of at least the given granule position.
	 * Note that skipping backwards is not currently supported!
	 */
	override fun skipToGranule(granulePosition: Long) {
		r!!.skipToGranulePosition(sid, granulePosition)
	}

	/**
	 * Buffers the given audio ready for writing
	 * out. Data won't be written out yet, you
	 * need to call [.close] to do that,
	 * because we assume you'll still be populating
	 * the Info/Comment/Setup objects
	 */
	fun writeAudioData(data: FlacAudioFrame) {
		writtenAudio.add(data)
	}

	/**
	 * In Reading mode, will close the underlying ogg
	 * file and free its resources.
	 * In Writing mode, will write out the Info, Comments
	 * and Setup objects, and then the audio data.
	 */
	override fun close() {
		if (r != null) {
			r = null
			oggFile!!.close()
			oggFile = null
		}
		if (w != null) {
			w!!.bufferPacket(firstPacket!!.write(), true)
			w!!.bufferPacket(tags!!.write(), false)
			// TODO Write the others
			//w.bufferPacket(setup.write(), true);

			val lastGranule: Long = 0
			for (fa in writtenAudio) {
				// Update the granule position as we go
				// TODO Track this
				//              if(fa.getGranulePosition() >= 0 &&
				//                 lastGranule != fa.getGranulePosition()) {
				//                 w.flush();
				//                 lastGranule = fa.getGranulePosition();
				//                 w.setGranulePosition(lastGranule);
				//              }

				// Write the data, flushing if needed
				w!!.bufferPacket(OggPacket(fa.data))
				if (w!!.sizePendingFlush > 16384) {
					w!!.flush()
				}
			}

			w!!.close()
			w = null
			oggFile!!.close()
			oggFile = null
		}
	}
}
/**
 * Opens for writing.
 */
