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
package com.soywiz.korau.format.org.gragravarr.ogg.audio

import com.soywiz.klock.*
import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.opus.*

/**
 * For computing statistics around an [OggAudioStream],
 * such as how long it lasts.
 * Format specific subclasses may be able to also identify
 * additional statistics beyond these.
 */
open class OggAudioStatistics constructor(private val headers: OggAudioHeaders, private val audio: OggAudioStream) {

	/**
	 * The number of audio packets in the stream
	 */
	var audioPacketsCount = 0
		private set
	/**
	 * The last granule (time position) in the audio stream
	 */
	var lastGranule: Long = -1
		private set
	/**
	 * Returns the duration of the audio, in seconds.
	 */
	var durationSeconds = 0.0
		private set

	/**
	 * The size, in bytes, of the ogg page overhead of all
	 * the packets (audio data and audio headers)
	 */
	var oggOverheadSize: Long = 0
		private set
	/**
	 * The size, in bytes, of the audio headers at the
	 * start of the file
	 */
	var headerOverheadSize: Long = 0
		private set
	/**
	 * The size, in bytes, of all the audio data
	 */
	var audioDataSize: Long = 0
		private set
	/**
	 * Returns the duration, in Hours:Minutes:Seconds.MS
	 */
	// Output as Hours / Minutes / Seconds / Parts
	val duration: String
		get() {
			return durationSeconds.seconds.toTimeString(3, addMilliseconds = true)
		}
	/**
	 * The percentage, from 0 to 100, of the ogg page overhead
	 * of all the packets (audio data and audio headers)
	 */
	val oggOverheadPercentage: Float
		get() {
			val total = audioDataSize + headerOverheadSize + oggOverheadSize
			return 100f * oggOverheadSize / total
		}

	/**
	 * The average bitrate, in bits per second, of all data
	 * in the file (audio, headers, overhead)
	 */
	val averageOverallBitrate: Double
		get() {
			val total = audioDataSize + headerOverheadSize + oggOverheadSize
			return total * 8.0 / durationSeconds
		}
	/**
	 * The average audio bitrate, in bits per second, of the
	 * audio data, but excluding headers and ogg overhead
	 */
	val averageAudioBitrate: Double
		get() = audioDataSize * 8.0 / durationSeconds

	/**
	 * Calculate the statistics
	 */
	open fun calculate() {
		var data: OggStreamAudioData

		// Calculate the headers sizing
		val info = headers.info
		handleHeader(info)
		handleHeader(headers.tags)
		handleHeader(headers.setup)

		// Have each audio packet handled, tracking at least granules
		while (true) {
			data = audio.nextAudioPacket ?: break
			handleAudioData(data)
		}

		// Calculate the duration from the granules, if found
		if (lastGranule > 0) {
			val samples = lastGranule - info!!.preSkip
			var sampleRate = info.sampleRate.toDouble()
			if (info is OpusInfo) {
				// Opus is a special case - granule *always* runs at 48kHz
				sampleRate = OpusAudioData.OPUS_GRANULE_RATE.toDouble()
			}
			durationSeconds = samples / sampleRate
		}
	}

	protected fun handleHeader(header: OggStreamPacket?) {
		if (header != null) {
			oggOverheadSize += header!!.oggOverheadSize
			headerOverheadSize += header!!.data!!.size
		}
	}

	open protected fun handleAudioData(audioData: OggStreamAudioData) {
		audioPacketsCount++
		audioDataSize += audioData.data!!.size
		oggOverheadSize += audioData.oggOverheadSize

		if (audioData.granulePosition > lastGranule) {
			lastGranule = audioData.granulePosition
		}
	}
}
