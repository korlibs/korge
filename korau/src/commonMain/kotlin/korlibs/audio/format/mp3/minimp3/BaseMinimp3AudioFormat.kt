package korlibs.audio.format.mp3.minimp3

import korlibs.datastructure.ByteArrayDeque
import korlibs.memory.hasFlags
import korlibs.memory.indexOf
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.AudioFormat
import korlibs.audio.format.MP3
import korlibs.audio.format.MP3Base
import korlibs.audio.sound.AudioSamples
import korlibs.audio.sound.AudioSamplesDeque
import korlibs.audio.sound.AudioStream
import korlibs.io.lang.Closeable
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.FastByteArrayInputStream
import korlibs.io.stream.readBytesUpTo

abstract class BaseMinimp3AudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        return MP3.tryReadInfo(data, props)
    }

    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
        return createDecoderStream(data, props, null)
    }

    private suspend fun createDecoderStream(data: AsyncStream, props: AudioDecodingProps, table: MP3Base.SeekingTable? = null): AudioStream {
        val dataStartPosition = data.position
        val decoder = createMp3Decoder()
        decoder.info.reset()
        val mp3SeekingTable: MP3Base.SeekingTable? = when (props.exactTimings) {
            true -> table ?: (if (data.hasLength()) MP3Base.Parser(data, data.getLength()).getSeekingTable(44100) else null)
            else -> null
        }

        suspend fun readMoreSamples(): Boolean {
            while (true) {
                if (decoder.info.compressedData.availableRead < decoder.info.tempBuffer.size && data.hasAvailable()) {
                    decoder.info.compressedData.write(data.readBytesUpTo(0x1000))
                }
                val result = decoder.info.step(decoder)
                if (decoder.info.hz == 0 || decoder.info.nchannels == 0) continue
                return result
            }
        }

        readMoreSamples()

        //println("Minimp3AudioFormat: decoder.hz=${decoder.hz}, decoder.nchannels=${decoder.nchannels}")

        return object : AudioStream(decoder.info.hz, decoder.info.nchannels) {
            override var finished: Boolean = false

            override var totalLengthInSamples: Long? = decoder.info.totalSamples.toLong().takeIf { it != 0L }
                ?: mp3SeekingTable?.lengthSamples

            var _currentPositionInSamples: Long = 0L

            override var currentPositionInSamples: Long
                get() = _currentPositionInSamples
                set(value) {
                    finished = false
                    if (mp3SeekingTable != null) {
                        data.position = mp3SeekingTable.locateSample(value)
                        _currentPositionInSamples = value
                    } else {
                        // @TODO: We should try to estimate by using decoder.bitrate_kbps

                        data.position = 0L
                        _currentPositionInSamples = 0L
                    }
                    decoder.info.reset()
                }

            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                var noMoreSamples = false
                while (decoder.info.pcmDeque!!.availableRead < length) {
                    if (!readMoreSamples()) {
                        //println("no more samples!")
                        noMoreSamples = true
                        break
                    }
                }
                //println("audioSamplesDeque!!.availableRead=${audioSamplesDeque!!.availableRead}")
                return if (noMoreSamples && decoder.info.pcmDeque!!.availableRead == 0) {
                    finished = true
                    0
                } else {
                    decoder.info.pcmDeque!!.read(out, offset, length)
                }.also {
                    _currentPositionInSamples += it
                    //println(" -> $it")
                    //println("MP3.read: offset=$offset, length=$length, noMoreSamples=$noMoreSamples, finished=$finished")
                }
            }

            override fun close() {
                decoder.close()
            }

            override suspend fun clone(): AudioStream = createDecoderStream(data.duplicate().also { it.position = dataStartPosition }, props, table)
        }
    }

    protected abstract fun createMp3Decoder(): BaseMp3Decoder

    class BaseMp3DecoderInfo {
        val tempBuffer = ByteArray(1152 * 2 * 2)
        val compressedData = ByteArrayDeque()
        var pcmDeque: AudioSamplesDeque? = null
        var hz = 0
        var bitrate_kbps = 0
        var nchannels = 0
        var samples: Int = 0
        var frame_bytes: Int = 0
        var skipRemaining: Int = 0
        var samplesAvailable: Int = 0
        var totalSamples: Int = 0
        var samplesRead: Int = 0
        var xingFrames: Int = 0
        var xingDelay: Int = 0
        var xingPadding: Int = 0

        fun reset() {
            pcmDeque?.clear()
            compressedData.clear()
            skipRemaining = 0
            samplesAvailable = -1
            samplesRead = 0
        }

        fun step(decoder: BaseMp3Decoder): Boolean {
            val availablePeek = compressedData.peek(tempBuffer, 0, tempBuffer.size)
            val xingIndex = tempBuffer.indexOf(XingTag).takeIf { it >= 0 } ?: tempBuffer.size
            val infoIndex = tempBuffer.indexOf(InfoTag).takeIf { it >= 0 } ?: tempBuffer.size

            if (xingIndex < tempBuffer.size || infoIndex < tempBuffer.size) {
                try {
                    val index = kotlin.math.min(xingIndex, infoIndex)
                    val data = FastByteArrayInputStream(tempBuffer, index)
                    data.skip(7)
                    if (data.available >= 1) {
                        val flags = data.readU8()
                        //println("xing=$xingIndex, infoIndex=$infoIndex, index=$index, flags=$flags")
                        val FRAMES_FLAG = 1
                        val BYTES_FLAG = 2
                        val TOC_FLAG = 4
                        val VBR_SCALE_FLAG = 8
                        if (flags.hasFlags(FRAMES_FLAG) && data.available >= 4) {
                            xingFrames = data.readS32BE()
                            if (flags.hasFlags(BYTES_FLAG)) data.skip(4)
                            if (flags.hasFlags(TOC_FLAG)) data.skip(100)
                            if (flags.hasFlags(VBR_SCALE_FLAG)) data.skip(4)
                            if (data.available >= 1) {
                                val info = data.readU8()
                                if (info != 0) {
                                    data.skip(20)
                                    if (data.available >= 3) {
                                        val t0 = data.readU8()
                                        val t1 = data.readU8()
                                        val t2 = data.readU8()
                                        xingDelay = ((t0 shl 4) or (t1 ushr 4)) + (528 + 1)
                                        xingPadding = (((t1 and 0xF) shl 8) or (t2)) - (528 + 1)
                                    }
                                    //println("frames=$frames, flags=$flags, delay=$delay, padding=$padding, $t0,$t1,$t2")
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            val buf = decoder.decodeFrame(availablePeek)

            if (nchannels != 0 && (xingFrames != 0 || xingDelay != 0 || xingPadding != 0)) {
                val rpadding = xingPadding * nchannels
                val to_skip = xingDelay * nchannels
                var detected_samples = samples * xingFrames * nchannels
                if (detected_samples >= to_skip) detected_samples -= to_skip
                if (rpadding in 1..detected_samples) detected_samples -= rpadding
                skipRemaining = to_skip + (samples * nchannels)
                samplesAvailable = detected_samples
                //println("nchannels=$nchannels")
                totalSamples = detected_samples / nchannels
                //println("frames=$frames, delay=$delay, padding=$padding :: rpadding=$rpadding, to_skip=$to_skip, detected_samples=$detected_samples")

                xingFrames = 0
                xingDelay = 0
                xingPadding = 0
            }

            //println("availablePeek=$availablePeek, frame_bytes=$frame_bytes, samples=$samples, nchannels=$nchannels, hz=$hz, bitrate_kbps=")

            if (nchannels != 0 && pcmDeque == null) {
                pcmDeque = AudioSamplesDeque(nchannels)
            }

            if (samples > 0) {
                var offset = 0
                var toRead = samples * nchannels

                if (skipRemaining > 0) {
                    val skipNow = kotlin.math.min(skipRemaining, toRead)
                    offset += skipNow
                    toRead -= skipNow
                    skipRemaining -= skipNow
                }
                if (samplesAvailable >= 0) {
                    toRead = kotlin.math.min(toRead, samplesAvailable)
                    samplesAvailable -= toRead
                }

                //println("writeInterleaved. offset=$offset, toRead=$toRead")
                pcmDeque!!.writeInterleaved(buf!!, offset, toRead)
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

    interface BaseMp3Decoder : Closeable {
        val info: BaseMp3DecoderInfo
        fun decodeFrame(availablePeek: Int): ShortArray?
    }

    companion object {
        val XingTag = byteArrayOf('X'.code.toByte(), 'i'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte())
        val InfoTag = byteArrayOf('I'.code.toByte(), 'n'.code.toByte(), 'f'.code.toByte(), 'o'.code.toByte())
    }
}