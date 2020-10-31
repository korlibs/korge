package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.korau.format.*
import com.soywiz.korio.stream.*
import java.io.*

object Mp3DecodeAudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream): Info? {
        return MP3.tryReadInfo(data)
    }

    override suspend fun decodeStream(data: AsyncStream): AudioStream? {
        val temp = ByteArray(16 * 1024)
        val deque = ByteArrayDeque(10)
        val samples = ShortArrayDeque()
        val inputStream = deque.inputStream()

        data.copyChunkTo(deque, temp, deque.availableWriteWithoutAllocating)

        //val sound = fr.delthas.javamp3.Sound(inputStream)
        val sound = Sound(inputStream)

        return object : AudioStream(sound.audioFormat.sampleRate.toInt(), sound.audioFormat.channels) {
            override var finished: Boolean = false

            override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
                println("[a]")
                while (samples.availableRead <= temp.size) {
                    if (data.copyChunkTo(deque, temp, deque.availableWriteWithoutAllocating) <= 0) break
                }
                println("[b]")
                val tmp2 = ByteArray(512)
                val tmp2Size = sound.read(tmp2, 0, tmp2.size)

                println("[c]: $tmp2Size")
                samples.write(tmp2.copyOf(tmp2Size).toShortArrayLE())

                return samples.write(out, offset, length)
            }

            override fun close() {
                super.close()
            }
        }
    }

    override suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String) {
        super.encode(data, out, filename)
    }

    override fun toString(): String = "Mp3DecodeAudioFormat"
}

