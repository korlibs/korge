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
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

/**
 * This is a wrapper around an OggFile that lets you
 * get at all the interesting bits of a Vorbis file.
 */
class VorbisFile(val warningProcessor: ((String) -> Unit)?): OggAudioStream, OggAudioHeaders, Closeable {
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

	override var info: VorbisInfo? = null
		private set
	var comment: VorbisComments? = null
		private set
	override var setup: VorbisSetup? = null
		private set

	private var writtenPackets: MutableList<VorbisAudioData> = arrayListOf()

	override val nextAudioPacket: VorbisAudioData?
		get() {
			var p: OggPacket? = null
			var vp: VorbisPacket? = null
			while (true) {
				p = r!!.getNextPacketWithSid(sid) ?: break
				vp = VorbisPacketFactory.create(p)
				if (vp is VorbisAudioData) {
					return vp as VorbisAudioData?
				} else {
					warningProcessor?.invoke("Skipping non audio packet $vp mid audio stream")
				}
			}
			return null
		}

	/**
	 * This is a Vorbis file
	 */
	override val type: OggStreamIdentifier.OggStreamType
		get() = OggStreamIdentifier.OGG_VORBIS

	/**
	 * Opens the given file for reading
	 */
	constructor(ogg: OggFile, warningProcessor: ((String) -> Unit)?) : this(ogg.packetReader, warningProcessor) {
		this.oggFile = ogg
	}

	/**
	 * Loads a Vorbis File from the given packet reader.
	 */
	constructor(r: OggPacketReader, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		this.r = r

		var p: OggPacket? = null
		while (true) {
			p = r.getNextPacket() ?: break
			if (p!!.isBeginningOfStream && p!!.data.size > 10) {
				if (VorbisPacketFactory.isVorbisStream(p!!)) {
					sid = p!!.sid
					break
				}
			}
		}
		if (p == null) {
			throw IllegalArgumentException("Supplied File is not Vorbis")
		}

		// First three packets are required to be info, comments, setup
		info = VorbisPacketFactory.create(p) as VorbisInfo
		comment = VorbisPacketFactory.create(r.getNextPacketWithSid(sid)!!) as VorbisComments
		setup = VorbisPacketFactory.create(r.getNextPacketWithSid(sid)!!) as VorbisSetup

		// Everything else should be audio data
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file. The Steam ID (SID) is
	 * automatically allocated for you.
	 */
	constructor(
		out: SyncOutputStream,
		warningProcessor: ((String) -> Unit)?,
		info: VorbisInfo = VorbisInfo(),
		comments: VorbisComments = VorbisComments(),
		setup: VorbisSetup = VorbisSetup()
	) : this(out, -1, info, comments, setup, warningProcessor) {
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file, with a specific
	 * Steam ID (SID). You should only set the SID
	 * when copying one file to another!
	 */
	constructor(out: SyncOutputStream, sid: Int, info: VorbisInfo, comments: VorbisComments, setup: VorbisSetup, warningProcessor: ((String) -> Unit)?) : this(warningProcessor) {
		oggFile = OggFile(out, warningProcessor)

		if (sid > 0) {
			w = oggFile!!.getPacketWriter(sid)
			this.sid = sid
		} else {
			w = oggFile!!.packetWriter
			this.sid = w!!.sid
		}

		writtenPackets = ArrayList<VorbisAudioData>()

		this.info = info
		this.comment = comments
		this.setup = setup
	}

	/**
	 * Skips the audio data to the next packet with a granule
	 * of at least the given granule position.
	 * Note that skipping backwards is not currently supported!
	 */
	override fun skipToGranule(granulePosition: Long) {
		r!!.skipToGranulePosition(sid, granulePosition)
	}

	override val tags = comment


	/**
	 * Buffers the given audio ready for writing
	 * out. Data won't be written out yet, you
	 * need to call [.close] to do that,
	 * because we assume you'll still be populating
	 * the Info/Comment/Setup objects
	 */
	fun writeAudioData(data: VorbisAudioData) {
		writtenPackets.add(data)
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
			w!!.bufferPacket(info!!.write(), true)
			w!!.bufferPacket(comment!!.write(), false)
			w!!.bufferPacket(setup!!.write(), true)

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
