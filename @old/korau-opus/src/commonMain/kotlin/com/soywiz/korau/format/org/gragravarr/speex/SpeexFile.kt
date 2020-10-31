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
package com.soywiz.korau.format.org.gragravarr.speex

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

/**
 * This is a wrapper around an OggFile that lets you
 * get at all the interesting bits of a Speex file.
 */
class SpeexFile(val warningProcessor: ((String) -> Unit)?) : OggAudioStream, OggAudioHeaders, Closeable {
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

	override var info: SpeexInfo? = null
		private set
	override var tags: SpeexTags? = null
		private set

	private var writtenPackets: MutableList<SpeexAudioData> = arrayListOf()

	override val nextAudioPacket: SpeexAudioData?
		get() {
			var p: OggPacket? = null
			var sp: SpeexPacket? = null
			while (true) {
				p = r!!.getNextPacketWithSid(sid) ?: break
				sp = SpeexPacketFactory.create(p)
				if (sp is SpeexAudioData) {
					return sp as SpeexAudioData?
				} else {
					warningProcessor?.invoke("Skipping non audio packet $sp mid audio stream")
				}
			}
			return null
		}

	/**
	 * This is a Speex file
	 */
	override val type: OggStreamIdentifier.OggStreamType
		get() = OggStreamIdentifier.SPEEX_AUDIO
	/**
	 * Speex doesn't have setup headers, so this is always null
	 */
	override val setup: OggAudioSetupHeader?
		get() = null

	/**
	 * Opens the given file for reading
	 */
	constructor(ogg: OggFile, warningProcessor: ((String) -> Unit)?) : this(ogg.packetReader, warningProcessor) {
		this.oggFile = ogg
	}

	/**
	 * Loads a Speex File from the given packet reader.
	 */
	constructor(r: OggPacketReader, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		this.r = r

		var p: OggPacket? = null
		while (true) {
			p = r.getNextPacket() ?: break
			if (p!!.isBeginningOfStream && p!!.data.size > 10) {
				if (SpeexPacketFactory.isSpeexStream(p!!)) {
					sid = p!!.sid
					break
				}
			}
		}
		if (sid == -1) {
			throw IllegalArgumentException("Supplied File is not Speex")
		}

		// First two packets are required to be info then tags
		info = SpeexPacketFactory.create(p!!) as SpeexInfo
		tags = SpeexPacketFactory.create(r.getNextPacketWithSid(sid)!!) as SpeexTags

		// Everything else should be audio data
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file. The Steam ID (SID) is
	 * automatically allocated for you.
	 */
	constructor(out: SyncOutputStream, info: SpeexInfo = SpeexInfo(), tags: SpeexTags = SpeexTags(), warningProcessor: ((String) -> Unit)?) : this(
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
	constructor(out: SyncOutputStream, sid: Int, info: SpeexInfo, tags: SpeexTags, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		oggFile = OggFile(out, warningProcessor)

		if (sid > 0) {
			w = oggFile!!.getPacketWriter(sid)
			this.sid = sid
		} else {
			w = oggFile!!.packetWriter
			this.sid = w!!.sid
		}

		writtenPackets = ArrayList<SpeexAudioData>()

		this.info = info
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
	fun writeAudioData(data: SpeexAudioData) {
		writtenPackets.add(data)
	}

	/**
	 * In Reading mode, will close the underlying ogg
	 * file and free its resources.
	 * In Writing mode, will write out the Info and
	 * Tags objects, and then the audio data.
	 */
	override fun close() {
		if (r != null) {
			r = null
			oggFile!!.close()
			oggFile = null
		}
		if (w != null) {
			w!!.bufferPacket(info!!.write(), true)
			w!!.bufferPacket(tags!!.write(), false)

			var lastGranule: Long = 0
			for (vd in writtenPackets) {
				// Update the granule position as we go
				if (vd.granulePosition >= 0 && lastGranule != vd.granulePosition) {
					w!!.flush()
					lastGranule = vd.granulePosition
					w!!.setGranulePosition(lastGranule)
				}

				// Write the data, flushing if needed
				w!!.bufferPacket(vd.write())
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
