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
package com.soywiz.korau.format.org.gragravarr.theora

import com.soywiz.kds.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*
import com.soywiz.korau.format.org.gragravarr.skeleton.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

/**
 * This is a wrapper around an OggFile that lets you
 * get at all the interesting bits of a Theora file.
 */
class TheoraFile : HighLevelOggStreamPacket, Closeable {
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
	var sid = -1
		private set

	var info: TheoraInfo? = null
		private set
	var comments: TheoraComments? = null
		private set
	var setup: TheoraSetup? = null
		private set

	/**
	 * Returns the Skeleton data describing all the
	 * streams, or null if the file has no Skeleton stream
	 */
	var skeleton: SkeletonStream? = null
		private set
	private var soundtracks: MutableMap<Int, OggAudioStreamHeaders>? = null
	private var soundtrackWriters: MutableMap<OggAudioStreamHeaders, OggPacketWriter> = LinkedHashMap()

	private var pendingPackets: Deque<AudioVisualDataAndSid> = Deque()
	private var writtenPackets: MutableList<AudioVisualDataAndSid> = arrayListOf()

	/**
	 * Returns the soundtracks and their stream IDs
	 */
	val soundtrackStreams: Map<Int, OggAudioHeaders>?
		get() = soundtracks

	/**
	 * Returns the next audio or video packet across
	 * any supported stream, or null if no more remain
	 */
	val nextAudioVisualPacket: OggStreamAudioVisualData?
		get() = getNextAudioVisualPacket(null)

	/**
	 * Opens the given file for reading
	 */
	constructor(ogg: OggFile) : this(ogg.packetReader) {
		this.oggFile = ogg
	}

	/**
	 * Loads a Theora File from the given packet reader.
	 */
	constructor(r: OggPacketReader) {
		this.r = r
		this.pendingPackets = Deque<AudioVisualDataAndSid>()
		this.soundtracks = HashMap<Int, OggAudioStreamHeaders>()

		val headerCompleteSoundtracks = HashSet<Int>()

		// The start of the file should contain the skeleton
		//  (if there is one), the header packets for the Theora
		//  stream, the header packets for the soundtrack streams,
		//  and the start of any other streams that are time-parallel
		// However, they can all be in pretty much any order, including
		//  coming after the start of the first few video frames, so
		//  process into the video a little bit looking for things
		//  we care about
		var packetsSinceSetup = -1
		var p: OggPacket? = null
		while (true) {
			p = r.getNextPacket() ?: break
			val psid = p!!.sid
			if (p!!.isBeginningOfStream && p!!.data.size > 10) {
				if (TheoraPacketFactory.isTheoraStream(p!!)) {
					sid = psid
					info = TheoraPacketFactory.create(p!!) as TheoraInfo
				} else if (SkeletonPacketFactory.isSkeletonStream(p)) {
					skeleton = SkeletonStream(p)
				} else {
					try {
						soundtracks!![psid] = OggAudioStreamHeaders.create(p)
					} catch (e: IllegalArgumentException) {
						// Not a soundtrack
					}

				}
			} else {
				if (psid == sid) {
					val tp = TheoraPacketFactory.create(p!!)

					// First three packets must be info, comments, setup
					if (comments == null) {
						comments = tp as TheoraComments
					} else if (setup == null) {
						setup = tp as TheoraSetup
						packetsSinceSetup = 0
					} else {
						pendingPackets.add(
							AudioVisualDataAndSid(
								tp as TheoraVideoData, sid
							)
						)
						packetsSinceSetup++

						// Are we, in all likelihood, past all the headers?
						if (packetsSinceSetup > 10) break
					}
				} else if (skeleton != null && skeleton!!.sid === psid) {
					skeleton!!.processPacket(p)
				} else if (soundtracks!!.containsKey(psid)) {
					val audio = soundtracks!![psid]
					if (headerCompleteSoundtracks.contains(psid)) {
						// Onto the data part for this soundtrack
						pendingPackets.add(
							AudioVisualDataAndSid(
								audio!!.createAudio(p)!!, psid
							)
						)
					} else {
						val ongoing = audio!!.populate(p)
						if (!ongoing) {
							headerCompleteSoundtracks.add(psid)
						}
					}
				}
			}
		}
		if (sid == -1) {
			throw IllegalArgumentException("Supplied File is not Theora")
		}
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file. The Steam ID (SID) is
	 * automatically allocated for you.
	 */
	constructor(
		out: SyncOutputStream,
		info: TheoraInfo = TheoraInfo(),
		comments: TheoraComments = TheoraComments(),
		setup: TheoraSetup = TheoraSetup(),
		warningProcessor: ((String) -> Unit)?
	) : this(out, -1, info, comments, setup, warningProcessor) {
	}

	/**
	 * Opens for writing, based on the settings
	 * from a pre-read file, with a specific
	 * Steam ID (SID). You should only set the SID
	 * when copying one file to another!
	 */
	constructor(out: SyncOutputStream, sid: Int, info: TheoraInfo, comments: TheoraComments, setup: TheoraSetup, warningProcessor: ((String) -> Unit)?) {
		oggFile = OggFile(out, warningProcessor)

		if (sid > 0) {
			w = oggFile!!.getPacketWriter(sid)
			this.sid = sid
		} else {
			w = oggFile!!.packetWriter
			this.sid = w!!.sid
		}

		this.writtenPackets = ArrayList()
		this.soundtracks = HashMap<Int, OggAudioStreamHeaders>()
		this.soundtrackWriters = HashMap<OggAudioStreamHeaders, OggPacketWriter>()

		this.info = info
		this.comments = comments
		this.setup = setup
	}

	fun ensureSkeleton() {
		if (skeleton != null) return

		val sids = ArrayList<Int>()
		if (sid != -1) {
			sids.add(sid)
		}
		for (stsid in soundtracks!!.keys) {
			if (stsid != -1) {
				sids.add(stsid)
			}
		}
		val sidsA = IntArray(sids.size)
		for (i in sidsA.indices) {
			sidsA[i] = sids[i]
		}

		skeleton = SkeletonStream(sidsA)
	}

	/**
	 * Returns all the soundtracks
	 */
	fun getSoundtracks(): Collection<OggAudioHeaders> {
		return soundtracks!!.values
	}

	/**
	 * Adds a new soundtrack to the video
	 *
	 * @return the serial id (sid) of the new soundtrack
	 */
	fun addSoundtrack(audio: OggAudioHeaders): Int {
		if (w == null) {
			throw IllegalStateException("Not in write mode")
		}

		// If it doesn't have a sid yet, get it one
		var aw: OggPacketWriter? = null
		if (audio.sid === -1) {
			aw = oggFile!!.packetWriter
			// TODO How to tell the OggAudioHeaders the new SID?
		} else {
			aw = oggFile!!.getPacketWriter(audio.sid)
		}
		val audioSid = aw!!.sid

		// If we have a skeleton, tell it about the new stream
		if (skeleton != null) {
			val bone = skeleton!!.addBoneForStream(audioSid)
			bone.setContentType(audio.type.mimetype)
			// TODO Populate the rest of the bone as best we can
		}

		// Record the new audio stream
		soundtracks!![audioSid] = audio as OggAudioStreamHeaders
		soundtrackWriters[audio as OggAudioStreamHeaders] = aw

		// Report the sid
		return audioSid
	}

	/**
	 * Returns the next audio or video packet from any of
	 * the specified streams, or null if no more remain
	 */
	fun getNextAudioVisualPacket(sids: Set<Int>?): OggStreamAudioVisualData? {
		var data: OggStreamAudioVisualData? = null

		while (data == null && !pendingPackets.isEmpty()) {
			val avd = pendingPackets.removeFirst()
			if (sids == null || sids.contains(avd.sid)) {
				data = avd.data
			}
		}

		if (data == null) {
			var p: OggPacket? = null
			while (true) {
				p = r!!.getNextPacket() ?: break
				if (sids == null || sids.contains(p!!.sid)) {
					if (p!!.sid === sid) {
						data = TheoraPacketFactory.create(p!!) as OggStreamVideoData
						break
					} else if (soundtracks!!.containsKey(p!!.sid)) {
						val audio = soundtracks!![p!!.sid]
						data = audio!!.createAudio(p) as OggStreamAudioData
						break
					} else {
						// We don't know how to handle this stream
						throw IllegalArgumentException("Unsupported stream type with sid " + p!!.sid)
					}
				} else {
					// They're not interested in this stream
					// Proceed on to the next packet
				}
			}
		}

		return data
	}


	/**
	 * Buffers the given video ready for writing
	 * out. Data won't be written out yet, you
	 * need to call [.close] to do that,
	 * because we assume you'll still be populating
	 * the Info/Comment/Setup objects
	 */
	fun writeVideoData(data: TheoraVideoData) {
		writtenPackets.add(AudioVisualDataAndSid(data, sid)!!)
	}

	/**
	 * Buffers the given audio ready for writing
	 * out, to a given (pre-existing) audio stream.
	 * Data won't be written out yet, you
	 * need to call [.close] to do that,
	 * because we assume you'll still be populating
	 * the Info/Comment/Setup objects
	 */
	fun writeAudioData(data: OggStreamAudioData, audioSid: Int) {
		if (!soundtracks!!.containsKey(audioSid)) {
			throw IllegalArgumentException("Unknown audio stream with id $audioSid")
		}

		writtenPackets.add(AudioVisualDataAndSid(data, audioSid))
	}

	/**
	 * In Reading mode, will close the underlying ogg
	 * file and free its resources.
	 * In Writing mode, will write out the Info, Comment
	 * Tags objects, and then the video and audio data.
	 */
	override fun close() {
		if (r != null) {
			r = null
			oggFile!!.close()
			oggFile = null
		}
		if (w != null) {
			// First, write the initial packet of each stream
			// Skeleton (if present) goes first, then video, then audio(s)
			var sw: OggPacketWriter? = null
			if (skeleton != null) {
				sw = oggFile!!.packetWriter
				sw!!.bufferPacket(skeleton!!.fishead!!.write(), true)
			}

			w!!.bufferPacket(info!!.write(), true)

			for (audio in soundtrackWriters.keys) {
				val aw = soundtrackWriters[audio]!!
				aw!!.bufferPacket(audio.info.write(), true)
			}

			// Next, provide the rest of the skeleton information, to
			//  make it easy to work out what's what
			if (skeleton != null) {
				for (bone in skeleton!!.getFisbones()) {
					sw!!.bufferPacket(bone.write(), true)
				}
				for (frame in skeleton!!.getKeyFrames()) {
					sw!!.bufferPacket(frame.write(), true)
				}
			}

			// Next is the rest of the Theora headers
			w!!.bufferPacket(comments!!.write(), true)
			w!!.bufferPacket(setup!!.write(), true)

			// Finish the headers with the soundtrack stream remaining headers
			for (audio in soundtrackWriters.keys) {
				val aw = soundtrackWriters[audio]!!
				aw.bufferPacket(audio.tags!!.write(), true)
				if (audio.setup != null) {
					aw.bufferPacket(audio!!.setup!!.write(), true)
				}
			}

			// Write the audio visual data, along with their granules
			var lastGranule: Long = 0
			for (avData in writtenPackets) {
				var avw: OggPacketWriter = w!!
				if (avData.sid != sid) {
					//avw = soundtrackWriters[avData.sid]!!
					TODO("fix korau")
				}

				// Update the granule position as we go
				// TODO Is this the correct logic for multi-stream writing?
				if (avData.data.granulePosition >= 0 && lastGranule != avData.data.granulePosition) {
					avw.flush()
					lastGranule = avData.data.granulePosition
					avw.setGranulePosition(lastGranule)
				}

				// Write the data, flushing if needed
				avw.bufferPacket(avData.data.write())
				if (avw.sizePendingFlush > 16384) {
					avw.flush()
				}
			}

			// Close down all our writers
			w!!.close()
			w = null

			if (sw != null) {
				sw!!.close()
				sw = null
			}
			for (aw in soundtrackWriters.values) {
				aw.close()
			}

			oggFile!!.close()
			oggFile = null
		}
	}

	// TODO Decide if this can be made generic and non-Theora
	class AudioVisualDataAndSid(var data: OggStreamAudioVisualData, var sid: Int)
}
/**
 * Opens for writing.
 */
