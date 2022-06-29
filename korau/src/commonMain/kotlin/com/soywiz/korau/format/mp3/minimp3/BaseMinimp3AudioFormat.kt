package com.soywiz.korau.format.mp3.minimp3

import com.soywiz.kds.ByteArrayDeque
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.MP3
import com.soywiz.korau.format.MP3Base
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioSamplesDeque
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readBytesUpTo

abstract class BaseMinimp3AudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        return MP3.tryReadInfo(data, props)
    }

    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
        return createDecoderStream(data, props, null)
    }

    private suspend fun createDecoderStream(data: AsyncStream, props: AudioDecodingProps, table: MP3Base.SeekingTable? = null): AudioStream {
        val decoder = createMp3Decoder()
        val mp3SeekingTable: MP3Base.SeekingTable? = when (props.exactTimings) {
            true -> table ?: (if (data.hasLength()) MP3Base.Parser(data, data.getLength()).getSeekingTable(44100) else null)
            else -> null
        }

        suspend fun readMoreSamples(): Boolean {
            while (true) {
                if (decoder.compressedData.availableRead < decoder.tempBuffer.size && data.hasAvailable()) {
                    decoder.compressedData.write(data.readBytesUpTo(0x1000))
                }
                val result = decoder.step()
                if (decoder.hz == 0 || decoder.nchannels == 0) continue
                return result
            }
        }

        readMoreSamples()

        //println("Minimp3AudioFormat: decoder.hz=${decoder.hz}, decoder.nchannels=${decoder.nchannels}")

        return object : AudioStream(decoder.hz, decoder.nchannels) {
            override var finished: Boolean = false

            var _currentPositionInSamples: Long = 0L

            override var currentPositionInSamples: Long
                get() = _currentPositionInSamples
                set(value) {
                    if (mp3SeekingTable != null) {
                        decoder.pcmDeque!!.clear()
                        decoder.compressedData!!.clear()
                        data.position = mp3SeekingTable.locateSample(value)
                        _currentPositionInSamples = value
                    } else {
                        // @TODO: We should try to estimate by using decoder.bitrate_kbps

                        decoder.pcmDeque!!.clear()
                        decoder.compressedData!!.clear()
                        data.position = 0L
                        _currentPositionInSamples = 0L
                    }
                }

            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                var noMoreSamples = false
                while (decoder.pcmDeque!!.availableRead < length) {
                    if (!readMoreSamples()) {
                        //println("no more samples!")
                        noMoreSamples = true
                        break
                    }
                }
                //println("audioSamplesDeque!!.availableRead=${audioSamplesDeque!!.availableRead}")
                if (noMoreSamples && decoder.pcmDeque!!.availableRead == 0) {
                    finished = true
                    return 0
                }

                return decoder.pcmDeque!!.read(out, offset, length).also {
                    _currentPositionInSamples += length
                    //println(" -> $it")
                }
            }

            override fun close() {
                decoder.close()
            }

            override suspend fun clone(): AudioStream = createDecoderStream(data, props, table)
        }
    }

    protected abstract fun createMp3Decoder(): BaseMp3Decoder

    interface BaseMp3Decoder : Closeable {
        val hz: Int
        val nchannels: Int
        val tempBuffer: ByteArray //= ByteArray(1152 * 2 * 2)
        val compressedData: ByteArrayDeque
        var pcmDeque: AudioSamplesDeque?
        val samples: Int
        val frame_bytes: Int
        fun decodeFrame(availablePeek: Int): ShortArray?
        fun step(): Boolean {
            val availablePeek = compressedData.peek(tempBuffer, 0, tempBuffer.size)
            val buf = decodeFrame(availablePeek)

            //println("samples=$samples, nchannels=$nchannels, hz=$hz, bitrate_kbps=$bitrate_kbps")

            if (nchannels != 0 && pcmDeque == null) {
                pcmDeque = AudioSamplesDeque(nchannels)
            }

            if (samples > 0) {
                pcmDeque!!.writeInterleaved(buf!!, 0, samples * nchannels)
            }

            //println("mp3decFrameInfo: samples=$samples, channels=${struct.channels}, frame_bytes=${struct.frame_bytes}, frame_offset=${struct.frame_offset}, bitrate_kbps=${struct.bitrate_kbps}, hz=${struct.hz}, layer=${struct.layer}")

            compressedData.skip(frame_bytes)

            if (frame_bytes == 0 && samples == 0) {
                return false
            }

            if (pcmDeque != null && pcmDeque!!.availableRead > 0) {
                return true
            }

            return true
        }
    }
}
