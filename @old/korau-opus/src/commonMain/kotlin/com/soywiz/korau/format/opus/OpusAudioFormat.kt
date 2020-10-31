package com.soywiz.korau.format.opus

import com.soywiz.korau.format.*
import com.soywiz.korau.format.org.concentus.*
import com.soywiz.korio.stream.*

object Opus : OpusAudioFormatBase()

open class OpusAudioFormatBase : AudioFormat("opus") {
	override suspend fun decodeStream(data: AsyncStream): AudioStream? {
		val rate = 48000
		val channels = 2
		OpusMSDecoder
		val decoder = OpusDecoder(rate, channels)
		val dataPacket = ByteArray(4096)
		return object : AudioStream(rate, channels) {
			override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
				val dataLen = data.read(dataPacket, 0, dataPacket.size)
				val read = decoder.decode(dataPacket, 0, dataLen, out, 0, length, false)
				return read
			}
		}
	}
}