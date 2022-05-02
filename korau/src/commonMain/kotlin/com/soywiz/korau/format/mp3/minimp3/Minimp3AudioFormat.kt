@file:JvmName("Minimp3AudioFormatKt")

package com.soywiz.korau.format.mp3.minimp3

import com.soywiz.kds.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.jvm.*

internal object Minimp3AudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        return MP3.tryReadInfo(data, props)
    }

    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
        return createDecoderStream(data, props, null)
    }

    private suspend fun createDecoderStream(data: AsyncStream, props: AudioDecodingProps, table: MP3Base.SeekingTable? = null): AudioStream {
        val decoder = Minimp3Decoder()
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

    internal class Minimp3Decoder : MiniMp3Program(512 * 1024), Closeable {
        private fun <R> CPointer<*>.reinterpret(): CPointer<R> = CPointer(this.ptr)

        private val inputData = alloca(1152 * 2 * 2).reinterpret<Byte>()
        private val pcmData = alloca(1152 * 2 * 2 * 2).reinterpret<Short>()
        private val mp3dec = alloca(mp3dec_t__SIZE_BYTES).reinterpret<mp3dec_t>()
        private val mp3decFrameInfo = alloca(mp3dec_frame_info_t__SIZE_BYTES).reinterpret<mp3dec_frame_info_t>()
        val tempBuffer = ByteArray(1152 * 2 * 2)

        init {
            mp3dec_init(mp3dec)
        }

        val compressedData = ByteArrayDeque()
        var pcmDeque: AudioSamplesDeque? = null
        var hz = 0
        var bitrate_kbps = 0
        var nchannels = 0

        fun step(): Boolean {
            val availablePeek = compressedData.peek(tempBuffer, 0, tempBuffer.size)
            memWrite(inputData, tempBuffer, 0, availablePeek)

            val samples = mp3dec_decode_frame(
                mp3dec,
                inputData.reinterpret<UByte>().plus(0),
                availablePeek,
                pcmData,
                mp3decFrameInfo
            )
            val struct = mp3dec_frame_info_t(mp3decFrameInfo.ptr)
            nchannels = struct.channels
            hz = struct.hz
            bitrate_kbps = struct.bitrate_kbps

            if (nchannels != 0) {
                if (pcmDeque == null) {
                    pcmDeque = AudioSamplesDeque(nchannels)
                }
                if (samples > 0) {
                    val buf = ShortArray(samples * nchannels)
                    memRead(pcmData, buf, 0, samples * nchannels)
                    pcmDeque!!.writeInterleaved(buf, 0, samples)
                }
            }

            //println("mp3decFrameInfo: samples=$samples, channels=${struct.channels}, frame_bytes=${struct.frame_bytes}, frame_offset=${struct.frame_offset}, bitrate_kbps=${struct.bitrate_kbps}, hz=${struct.hz}, layer=${struct.layer}")

            compressedData.skip(struct.frame_bytes)

            if (struct.frame_bytes == 0 && samples == 0) {
                return false
            }

            if (pcmDeque != null && pcmDeque!!.availableRead > 0) {
                return true
            }

            return true
        }

        override fun close() {
            free(inputData)
            free(pcmData)
            free(mp3dec)
            free(mp3decFrameInfo)
        }
    }
}
