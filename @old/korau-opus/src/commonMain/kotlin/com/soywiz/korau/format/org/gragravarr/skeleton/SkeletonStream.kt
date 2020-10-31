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

import com.soywiz.korau.format.org.gragravarr.ogg.*

/**
 * A Skeleton Stream is made up of a single Fishead,
 * one Fisbone per described stream, and optionally
 * key frame data per stream.
 */
class SkeletonStream {
	/**
	 * Returns the Ogg Stream ID of the Skeleton
	 */
	var sid = -1
	private var hasWholeStream: Boolean = false
	var fishead: SkeletonFishead? = null
		private set
	private var fisbones: MutableList<SkeletonFisbone> = arrayListOf()
	private var bonesByStream: MutableMap<Int, SkeletonFisbone> = LinkedHashMap()
	private var keyFrames: MutableList<SkeletonKeyFramePacket> = arrayListOf()

	/**
	 * Starts tracking a new Skeleton Stream,
	 * from the given packet (which must hold
	 * the fishead)
	 */
	constructor(packet: OggPacket) {
		this.sid = packet.sid
		this.hasWholeStream = false
		this.fisbones = ArrayList<SkeletonFisbone>()
		this.bonesByStream = HashMap<Int, SkeletonFisbone>()
		this.keyFrames = ArrayList<SkeletonKeyFramePacket>()

		processPacket(packet)
	}

	/**
	 * Creates a new Skeleton stream, with empty fisbones
	 * referencing the specified streams (by their stream ids /
	 * serial numbers)
	 */
	constructor(sids: IntArray) {
		this.fishead = SkeletonFishead()
		for (sid in sids) {
			addBoneForStream(sid)
		}
	}

	/**
	 * Processes and tracks the next packet for
	 * the stream
	 */
	fun processPacket(packet: OggPacket) {
		val skel = SkeletonPacketFactory.create(packet)

		// First packet must be the head
		if (packet.isBeginningOfStream) {
			fishead = skel as SkeletonFishead
		} else if (skel is SkeletonFisbone) {
			val bone = skel as SkeletonFisbone
			fisbones.add(bone)
			bonesByStream[bone.serialNumber] = bone
		} else if (skel is SkeletonKeyFramePacket) {
			keyFrames.add(skel as SkeletonKeyFramePacket)
		} else {
			throw IllegalStateException("Unexpected Skeleton $skel")
		}

		if (packet.isEndOfStream) {
			hasWholeStream = true
		}
	}

	/**
	 * Have all the packets in the Skeleton stream
	 * been received and processed yet?
	 */
	fun hasWholeStream(): Boolean {
		return hasWholeStream
	}

	/**
	 * Get all known fisbones
	 */
	fun getFisbones(): List<SkeletonFisbone> {
		return fisbones
	}

	/**
	 * Get the fisbone for a given stream, or null if
	 * the stream isn't described
	 */
	fun getBoneForStream(sid: Int): SkeletonFisbone {
		return bonesByStream[sid]!!
	}

	/**
	 * Adds a new fisbone for the given stream
	 */
	fun addBoneForStream(sid: Int): SkeletonFisbone {
		val bone = SkeletonFisbone()
		bone.serialNumber = sid
		fisbones.add(bone)

		if (sid == -1 || bonesByStream.containsKey(sid)) {
			throw IllegalArgumentException("Invalid / duplicate sid $sid")
		}
		bonesByStream[sid] = bone

		return bone
	}

	/**
	 * Get all known key frames
	 */
	fun getKeyFrames(): List<SkeletonKeyFramePacket> {
		return keyFrames
	}
}
