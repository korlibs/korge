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
package com.soywiz.korau.format.org.gragravarr.opus

import com.soywiz.korau.format.org.gragravarr.ogg.*
import com.soywiz.korau.format.org.gragravarr.ogg.audio.*

class OpusStatistics(headers: OggAudioHeaders, audio: OggAudioStream, val warningProcessor: ((String) -> Unit)?) : OggAudioStatistics(headers, audio) {
	private var total_pages: Int = 0
	private var total_packets: Int = 0
	private var total_samples: Int = 0
	private var max_packet_duration: Int = 0
	private var min_packet_duration: Int = 0
	private var max_page_duration: Int = 0
	private var min_page_duration: Int = 0
	var maxPacketBytes: Int = 0
		private set
	var minPacketBytes: Int = 0
		private set
	private var sid: Int = 0

	private var lastlastgranulepos: Long = -1
	private var lastgranulepos: Long = 0
	private var firstgranulepos: Long = -1
	private var page_samples = 0
	private var page_count = 0

	private var info: OpusInfo? = null


	val maxPacketDuration: Double
		get() = max_packet_duration / 48.0
	val avgPacketDuration: Double
		get() = if (total_packets > 0) {
			total_samples.toDouble() / total_packets.toDouble() / 48.0
		} else 0.0
	val minPacketDuration: Double
		get() = min_packet_duration / 48.0
	val maxPageDuration: Double
		get() = max_page_duration / 48.0
	val avgPageDuration: Double
		get() = if (total_pages > 0) {
			total_samples.toDouble() / total_pages.toDouble() / 48.0
		} else 0.0
	val minPageDuration: Double
		get() = min_page_duration / 48.0

	init {
		if (headers.info is OpusInfo) {
			info = headers.info as OpusInfo
		} else {
			throw IllegalArgumentException("Non-Opus stream " + headers.info + " supplied")
		}

		init(headers)
	}

	constructor(opus: OpusFile, warningProcessor: ((String) -> Unit)?) : this(opus, opus, warningProcessor) {
		this.info = opus.info
		init(opus)
	}

	private fun init(headers: OggAudioHeaders) {
		sid = headers.sid
		max_packet_duration = 0
		min_packet_duration = 5760
		total_samples = 0
		total_packets = 0
		max_page_duration = -1
		min_page_duration = 5760 * 255
		maxPacketBytes = 0
		minPacketBytes = 2147483647
	}

	override fun calculate() {
		super.calculate()

		if (max_page_duration < page_samples) max_page_duration = page_samples
		if (min_page_duration > page_samples) min_page_duration = page_samples

		total_pages = page_count
	}

	override protected fun handleAudioData(audioData: OggStreamAudioData) {
		handleAudioData(audioData as OpusAudioData)
	}

	protected fun handleAudioData(audioData: OpusAudioData) {
		super.handleAudioData(audioData)

		val gp = audioData.granulePosition
		if (gp != lastgranulepos) {
			page_count++

			if (gp > 0) {
				if (gp < lastgranulepos) {
					warningProcessor?.invoke("WARNING: granulepos in stream $sid decreases from $lastgranulepos to $gp")
				}
				if (lastgranulepos == 0L && firstgranulepos == -1L) {
					/*First timed page, now we can recover the start time.*/
					firstgranulepos = gp
					if (firstgranulepos < 0) {
						if (!audioData.isEndOfStream) {
							warningProcessor?.invoke("WARNING:Samples with negative granpos in stream $sid")
						} else {
							firstgranulepos = 0
						}
					}
				}
				if (lastlastgranulepos == 0L) {
					firstgranulepos = firstgranulepos - page_samples
				}
				if (total_samples < lastgranulepos - firstgranulepos) {
					warningProcessor?.invoke("WARNING: Sample count behind granule ($total_samples<${lastgranulepos - firstgranulepos}) in stream $sid")
				}
				if (!audioData.isEndOfStream && total_samples > gp - firstgranulepos) {
					warningProcessor?.invoke("WARNING: Sample count ahead granule ($total_samples<$firstgranulepos) in stream$sid")
				}
				lastlastgranulepos = lastgranulepos
				lastgranulepos = gp
				if (audioPacketsCount == 0) {
					warningProcessor?.invoke("WARNING: Page with positive granpos ($gp) on a page with no completed packets in stream $sid")
				}
			} // gp
			else if (audioPacketsCount == 0) {
				warningProcessor?.invoke("Negative or zero granulepos ($gp) on Opus stream outside of headers. This file was created by a buggy encoder")
			}

			//last_page_duration = page_samples;
			if (max_page_duration < page_samples) max_page_duration = page_samples
			if (page_count > 1) {
				if (min_page_duration > page_samples) min_page_duration = page_samples
			}
			page_samples = 0
		}
		//if (p.getSid() != sid) {
		//    System.err.println("WARNING: Ignoring sid "+p.getSid());
		//    continue;
		//}
		val d = audioData.data!!
		if (d.isEmpty()) {
			warningProcessor?.invoke("WARNING: Invalid packet TOC in stream with sid $sid")
			return
		}

		val samples = audioData.numberOfSamples
		if (samples < 120 || samples > 5760 || samples % 120 != 0) {
			warningProcessor?.invoke("WARNING: Invalid packet TOC in stream with sid $sid")
			return
		}
		total_samples += samples
		page_samples += samples
		total_packets++
		//last_packet_duration = spp;
		if (max_packet_duration < samples) max_packet_duration = samples
		if (min_packet_duration > samples) min_packet_duration = samples
		if (maxPacketBytes < d.size) maxPacketBytes = d.size
		if (minPacketBytes > d.size) minPacketBytes = d.size
	}
}
