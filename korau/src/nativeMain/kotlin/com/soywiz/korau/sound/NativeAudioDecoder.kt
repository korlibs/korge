package com.soywiz.korau.sound

import com.soywiz.kds.ByteArrayDeque
import com.soywiz.kmem.startAddressOf
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.MP3
import com.soywiz.korau.format.MP3Base
import com.soywiz.korau.format.OGG
import com.soywiz.korau.format.mp3.minimp3.BaseMinimp3AudioFormat
import com.soywiz.korau.format.mp3.minimp3.MiniMp3Program
import com.soywiz.korau.format.mp3.minimp3.mp3dec_frame_info_t__SIZE_BYTES
import com.soywiz.korau.format.mp3.minimp3.mp3dec_t__SIZE_BYTES
import com.soywiz.korau.internal.SampleConvert
import com.soywiz.korio.lang.portableSimpleName
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.read
import kotlinx.cinterop.*
import minimp3.MINIMP3_MAX_SAMPLES_PER_FRAME
import minimp3.mp3dec_decode_frame
import minimp3.mp3dec_frame_info_t
import minimp3.mp3dec_init
import minimp3.mp3dec_t
import stb_vorbis.stb_vorbis
import stb_vorbis.stb_vorbis_alloc
import stb_vorbis.stb_vorbis_close
import stb_vorbis.stb_vorbis_decode_frame_pushdata
import stb_vorbis.stb_vorbis_get_info
import stb_vorbis.stb_vorbis_open_pushdata
import stb_vorbis.stb_vorbis_stream_length_in_samples

@kotlin.native.concurrent.ThreadLocal
actual val knNativeAudioFormats: List<AudioFormat> = listOf(NativeOggVorbisDecoderFormat, NativeMp3DecoderAudioFormat)
//val knNativeAudioFormats: List<AudioFormat> = listOf(NativeOggVorbisDecoderFormat, NativeMp3DecoderAudioFormat)

open class NativeAudioDecoder(val data: AsyncStream, val maxSamples: Int, val maxChannels: Int = 2) {
    val scope = Arena()

    var closed = false

    val frameData = ByteArray(16 * 1024)
    val samplesData = ShortArray(maxSamples)
    val dataBuffer = ByteArrayDeque(14)
    val samplesBuffers = AudioSamplesDeque(maxChannels)

    open fun init() {
    }

    data class DecodeInfo(
        var samplesDecoded: Int = 0,
        var frameBytes: Int = 0,
        var nchannels: Int = 0,
        var hz: Int = 0,
        var totalLengthInSamples: Long? = null
    )

    private val info = DecodeInfo()

    val nchannels: Int get() = info.nchannels
    val hz: Int get() = info.hz
    val totalLengthInSamples: Long? get() = info.totalLengthInSamples

    suspend fun decodeFrame() {
        var n = 0
        while (samplesBuffers.availableRead == 0) {
            memScoped {
                if (dataBuffer.availableRead < 16 * 1024) {
                    val temp = ByteArray(16 * 1024)
                    val tempRead = data.read(temp)
                    dataBuffer.write(temp, 0, tempRead)
                }
                val frameSize = dataBuffer.read(frameData)

                samplesData.usePinned {
                    val samplesDataPtr = it.addressOf(0)
                    frameData.usePinned {
                        val frameDataPtr = it.addressOf(0)
                        decodeFrameBase(samplesDataPtr, frameDataPtr, frameSize, info)
                        dataBuffer.writeHead(frameData, info.frameBytes, frameSize - info.frameBytes)
                        samplesBuffers.writeInterleaved(samplesData, 0, info.samplesDecoded * info.nchannels, channels = info.nchannels)
                    }
                }
            }
            n++
            if (n >= 16) break
        }
    }

    // Must set: samplesDecoded, nchannels, hz and consumedBytes
    protected open fun decodeFrameBase(
        samplesDataPtr: CPointer<ShortVar>,
        frameDataPtr: CPointer<ByteVar>,
        frameSize: Int,
        out: DecodeInfo
    ) {

    }

    open fun close() {
        if (!closed) {
            closed = true
            scope.clear()
        }
    }

    open suspend fun totalSamples(): Long? {
        return null
    }

    open suspend fun seekSamples(sample: Long) {
    }

    open fun clone(): NativeAudioDecoder {
        println("NativeAudioDecoder.clone not implemented")
        return this
    }

    suspend fun createAudioStream(): AudioStream? {
        decodeFrame()

        if (nchannels == 0) {
            return null
        }

        val totalSamples = totalSamples()

        return object : AudioStream(hz, nchannels) {
            var readSamples = 0L
            var seekPosition = -1L

            override val finished: Boolean get() = closed
            override val totalLengthInSamples: Long? get() = totalSamples
            override var currentPositionInSamples: Long
                get() = readSamples
                set(value) {
                    readSamples = value
                    seekPosition = value
                    closed = false
                    dataBuffer.clear()
                    samplesBuffers.clear()
                }

            override suspend fun clone(): AudioStream = this@NativeAudioDecoder.clone().createAudioStream()!!

            override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
                if (seekPosition >= 0L) {
                    seekSamples(seekPosition)
                    seekPosition = -1L
                }

                if (closed) return -1

                if (samplesBuffers.availableRead == 0) {
                    decodeFrame()
                }
                val result = samplesBuffers.read(out, offset, length)
                if (result <= 0) {
                    close()
                }
                readSamples += result
                //println("   AudioStream.read -> result=$result")
                return result
            }

            override fun close() {
                this@NativeAudioDecoder.close()
            }
        }
    }

    override fun toString(): String = this::class.portableSimpleName
}


internal object NativeMp3DecoderAudioFormat : BaseMinimp3AudioFormat() {
    override fun createMp3Decoder(): BaseMp3Decoder = Minimp3Decoder()

    internal class Minimp3Decoder : BaseMp3Decoder {
        val scope = Arena()
        val mp3dec = scope.alloc<mp3dec_t>()
        val mp3decFrameInfo = scope.alloc<mp3dec_frame_info_t>()
        private val pcmData = ShortArray(1152 * 2 * 2)
        override val tempBuffer = ByteArray(1152 * 2 * 2)

        init {
            mp3dec_init(mp3dec.ptr)
        }

        override val compressedData = ByteArrayDeque()
        override var pcmDeque: AudioSamplesDeque? = null
        override var hz = 0
        var bitrate_kbps = 0
        override var nchannels = 0
        override var samples: Int = 0
        override var frame_bytes: Int = 0
        override var skipRemaining: Int = 0
        override var samplesAvailable: Int = 0
        override var samplesRead: Int = 0

        override fun decodeFrame(availablePeek: Int): ShortArray? {
            samples = tempBuffer.usePinned { tempBufferPin ->
                pcmData.usePinned { pcmDataPin ->
                    mp3dec_decode_frame(
                        mp3dec.ptr,
                        tempBufferPin.startAddressOf.reinterpret(),
                        availablePeek,
                        pcmDataPin.startAddressOf,
                        mp3decFrameInfo.ptr
                    )
                }
            }
            nchannels = mp3decFrameInfo.channels
            hz = mp3decFrameInfo.hz
            bitrate_kbps = mp3decFrameInfo.bitrate_kbps
            frame_bytes = mp3decFrameInfo.frame_bytes

            //println("hz=$hz, nchannels=$nchannels, bitrate_kbps=$bitrate_kbps, frame_bytes=$frame_bytes, samples=$samples")

            if (nchannels == 0 || samples <= 0) return null

            return pcmData.copyOf(samples * nchannels)
        }

        override fun close() {
            scope.clear()
        }
    }
}


/*
object NativeMp3DecoderAudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info?
        = MP3.tryReadInfo(data, props)

    class Mp3AudioDecoder(data: AsyncStream, val props: AudioDecodingProps) : NativeAudioDecoder(data, MINIMP3_MAX_SAMPLES_PER_FRAME) {
        val mp3d = scope.alloc<mp3dec_t>()

        override fun init() {
            mp3dec_init(mp3d.ptr)
        }

        override fun decodeFrameBase(
            samplesDataPtr: CPointer<ShortVar>,
            frameDataPtr: CPointer<ByteVar>,
            frameSize: Int,
            out: DecodeInfo
        ) {
            memScoped {
                val info = alloc<mp3dec_frame_info_t>()
                out.samplesDecoded = mp3dec_decode_frame(
                    mp3d.ptr,
                    frameDataPtr.reinterpret(), frameSize,
                    samplesDataPtr,
                    info.ptr
                )
                out.frameBytes = info.frame_bytes
                out.hz = info.hz
                out.nchannels = info.channels
            }
        }

        private var mp3SeekingTable: MP3Base.SeekingTable? = null
        suspend fun getSeekingTable(): MP3Base.SeekingTable {
            if (mp3SeekingTable == null) mp3SeekingTable = MP3Base.Parser(data).getSeekingTable()
            return mp3SeekingTable!!
        }

        override suspend fun totalSamples(): Long? = getSeekingTable().lengthSamples

        override suspend fun seekSamples(sample: Long) {
            dataBuffer.clear()
            samplesBuffers.clear()
            data.position = getSeekingTable().locateSample(sample)
        }

        override fun clone(): NativeAudioDecoder = Mp3AudioDecoder(data.duplicate(), props)
    }

    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? = Mp3AudioDecoder(data, props).createAudioStream()
}

 */

object NativeOggVorbisDecoderFormat : AudioFormat("ogg") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info?
        = OGG.tryReadInfo(data, props)
    //= decodeStream(data)?.use { Info(it.totalLength.microseconds.microseconds, it.channels) }

    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
        return object : NativeAudioDecoder(data, 16 * 1024) {
            override fun init() {
            }

            private var vorbis: CPointer<stb_vorbis>? = null

            override fun decodeFrameBase(
                samplesDataPtr: CPointer<ShortVar>,
                frameDataPtr: CPointer<ByteVar>,
                frameSize: Int,
                out: DecodeInfo
            ) {
                memScoped {
                    if (vorbis == null) {
                        val consumed = alloc<IntVar>()
                        val error = alloc<IntVar>()
                        val alloc = alloc<stb_vorbis_alloc>()
                        vorbis = stb_vorbis_open_pushdata(
                            frameDataPtr.reinterpret(),
                            frameSize,
                            consumed.ptr,
                            error.ptr,
                            alloc.ptr
                        )
                        //println("vorbis: $vorbis")
                        //println("consumed: ${consumed.value}")
                        //println("error: ${error.value}")

                        out.samplesDecoded = 0
                        out.frameBytes = consumed.value
                        out.totalLengthInSamples = stb_vorbis_stream_length_in_samples(vorbis).toLong()

                        stb_vorbis_get_info(vorbis).useContents {
                            out.nchannels = this.channels.toInt()
                            out.hz = sample_rate.toInt()
                            //println("info.channels: ${this.channels}")
                            //println("info.max_frame_size: ${this.max_frame_size}")
                            //println("info.sample_rate: ${this.sample_rate}")
                            //println("info.setup_memory_required: ${this.setup_memory_required}")
                            //println("info.setup_temp_memory_required: ${this.setup_temp_memory_required}")
                            //println("info.temp_memory_required: ${this.temp_memory_required}")
                            //Unit
                        }
                    } else {
                        val nchannels = alloc<IntVar>()
                        val nsamples = alloc<IntVar>()
                        val output = alloc<CPointerVar<CPointerVar<FloatVar>>>()
                        val consumed = stb_vorbis_decode_frame_pushdata(
                            vorbis,
                            frameDataPtr.reinterpret(),
                            frameSize,
                            nchannels.ptr,
                            output.ptr,
                            nsamples.ptr
                        )
                        val outputPtr = output.value
                        val samples = nsamples.value
                        val channels = nchannels.value

                        //println("stb_vorbis_decode_frame_pushdata: frameSize=$frameSize, samples=$samples, channels=$channels, consumed=$consumed")

                        var m = 0
                        for (n in 0 until samples) {
                            for (channel in 0 until channels) {
                                samplesDataPtr[m++] = SampleConvert.floatToShort(outputPtr!![channel]!![n])
                            }
                        }
                        out.frameBytes = consumed
                        out.samplesDecoded = nsamples.value.toInt() * nchannels.value.toInt()
                    }
                }
            }

            override fun close() {
                super.close()
                if (vorbis != null) {
                    stb_vorbis_close(vorbis)
                    vorbis = null
                }
            }
        }.createAudioStream()
    }
}
