package korlibs.audio.format

import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.memory.*
import kotlin.math.*

open class MP3Decoder() : AudioFormat("mp3") {
    companion object : MP3Decoder()

    private val format = Minimp3AudioFormat
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = format.tryReadInfo(data, props)
    override suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps): AudioStream? = format.decodeStream(data, props)
    override fun toString(): String = "NativeMp3DecoderFormat"
}

abstract class BaseMinimp3AudioFormat : AudioFormat("mp3") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        return MP3.tryReadInfo(data, props)
    }

    override suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
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
            //println("currentThreadId=$currentThreadId, currentThreadName=$currentThreadName")
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

private object Minimp3AudioFormat : BaseMinimp3AudioFormat() {
    override fun createMp3Decoder(): BaseMp3Decoder = Minimp3Decoder()

    internal class Minimp3Decoder : MiniMp3Program(), BaseMp3Decoder {
        private val pcmData = ShortArray(MINIMP3_MAX_SAMPLES_PER_FRAME * 2 * 2)
        private val mp3dec = Mp3Dec()
        private val mp3decFrameInfo = Mp3FrameInfo()

        init {
            mp3dec_init(mp3dec)
        }

        override val info: BaseMp3DecoderInfo = BaseMp3DecoderInfo()

        override fun decodeFrame(availablePeek: Int): ShortArray? {
            val samples = mp3dec_decode_frame(
                mp3dec,
                UByteArrayIntPtr(UByteArrayInt(info.tempBuffer)),
                availablePeek,
                ShortArrayPtr(pcmData),
                mp3decFrameInfo
            )
            val struct = mp3decFrameInfo
            info.nchannels = struct.channels
            info.hz = struct.hz
            info.bitrate_kbps = struct.bitrate_kbps
            info.frame_bytes = struct.frame_bytes
            info.samples = samples
            //println("samples=$samples, hz=$hz, nchannels=$nchannels, bitrate_kbps=$bitrate_kbps, frameBytes=$frame_bytes")

            if (info.nchannels == 0 || samples <= 0) return null

            return pcmData.copyOf(samples * info.nchannels)
        }

        override fun close() {
        }
    }
}

//ENTRY Program
//Program.main(arrayOf())
@Suppress("MemberVisibilityCanBePrivate", "NAME_SHADOWING", "ObjectPropertyName", "FunctionName", "LocalVariableName")
@OptIn(ExperimentalUnsignedTypes::class)
private open class MiniMp3Program() {
    companion object {
        internal const val MINIMP3_MAX_SAMPLES_PER_FRAME = 2304
        internal const val MAX_BITRESERVOIR_BYTES = 511

        private fun arrayOfUByte(values: String, size: Int = values.length): UByteArrayInt =
            UByteArrayInt(size) { values.getOrElse(it) { 0.toChar() }.code }

        private fun arrayOfShort(values: String, size: Int = values.length): ShortArray =
            ShortArray(size) { values.getOrElse(it) { 0.toChar() }.code.toShort() }

        private val __STATIC_L3_imdct36_g_twid9: FloatArray = floatArrayOf(
            0.7372773f, 0.7933533f, 0.8433915f, 0.8870108f, 0.9238795f, 0.95371693f, 0.976296f, 0.9914449f, 0.99904823f,
            0.6755902f, 0.6087614f, 0.53729963f, 0.4617486f, 0.38268343f, 0.3007058f, 0.2164396f, 0.13052619f, 0.04361938f
        )

        private val __STATIC_L3_imdct12_g_twid3: FloatArray = floatArrayOf(
            0.7933533f, 0.9238795f, 0.9914449f, 0.6087614f, 0.38268343f, 0.13052619f
        )

        private val __STATIC_mp3d_DCT_II_g_sec: FloatArray = floatArrayOf(
            10.190008f, 0.500603f, 0.5024193f, 3.4076085f, 0.50547093f, 0.5224986f, 2.057781f, 0.5154473f,
            0.56694406f, 1.4841646f, 0.5310426f, 0.6468218f, 1.1694399f, 0.5531039f, 0.7881546f, 0.9725682f,
            0.582935f, 1.0606776f, 0.8393496f, 0.6225041f, 1.7224472f, 0.7445363f, 0.6748083f, 5.1011486f
        )

        private val __STATIC_L3_stereo_process_g_pan: FloatArray = floatArrayOf(
            0f, 1f, 0.21132487f, 0.7886751f, 0.3660254f, 0.6339746f, 0.5f, 0.5f, 0.6339746f, 0.3660254f, 0.7886751f, 0.21132487f, 1f, 0f
        )
        private val __STATIC_mp3d_synth_g_win: FloatArray = floatArrayOf(
            -1f, 26f, -31f, 208f, 218f, 401f, -519f, 2063f, 2000f, 4788f, -5517f, 7134f, 5959f, 35640f, -39336f, 74992f, -1f, 24f, -35f, 202f, 222f, 347f,
            -581f, 2080f, 1952f, 4425f, -5879f, 7640f, 5288f, 33791f, -41176f, 74856f, -1f, 21f, -38f, 196f, 225f, 294f, -645f, 2087f, 1893f, 4063f, -6237f,
            8092f, 4561f, 31947f, -43006f, 74630f, -1f, 19f, -41f, 190f, 227f, 244f, -711f, 2085f, 1822f, 3705f, -6589f, 8492f, 3776f, 30112f, -44821f, 74313f,
            -1f, 17f, -45f, 183f, 228f, 197f, -779f, 2075f, 1739f, 3351f, -6935f, 8840f, 2935f, 28289f, -46617f, 73908f, -1f, 16f, -49f, 176f, 228f, 153f,
            -848f, 2057f, 1644f, 3004f, -7271f, 9139f, 2037f, 26482f, -48390f, 73415f, -2f, 14f, -53f, 169f, 227f, 111f, -919f, 2032f, 1535f, 2663f, -7597f,
            9389f, 1082f, 24694f, -50137f, 72835f, -2f, 13f, -58f, 161f, 224f, 72f, -991f, 2001f, 1414f, 2330f, -7910f, 9592f, 70f, 22929f, -51853f, 72169f,
            -2f, 11f, -63f, 154f, 221f, 36f, -1064f, 1962f, 1280f, 2006f, -8209f, 9750f, -998f, 21189f, -53534f, 71420f, -2f, 10f, -68f, 147f, 215f, 2f,
            -1137f, 1919f, 1131f, 1692f, -8491f, 9863f, -2122f, 19478f, -55178f, 70590f, -3f, 9f, -73f, 139f, 208f, -29f, -1210f, 1870f, 970f, 1388f, -8755f,
            9935f, -3300f, 17799f, -56778f, 69679f, -3f, 8f, -79f, 132f, 200f, -57f, -1283f, 1817f, 794f, 1095f, -8998f, 9966f, -4533f, 16155f, -58333f,
            68692f, -4f, 7f, -85f, 125f, 189f, -83f, -1356f, 1759f, 605f, 814f, -9219f, 9959f, -5818f, 14548f, -59838f, 67629f, -4f, 7f, -91f, 117f, 177f,
            -106f, -1428f, 1698f, 402f, 545f, -9416f, 9916f, -7154f, 12980f, -61289f, 66494f, -5f, 6f, -97f, 111f, 163f, -127f, -1498f, 1634f, 185f, 288f,
            -9585f, 9838f, -8540f, 11455f, -62684f, 65290f
        )
        private val __STATIC_L3_antialias_g_aa: Array<FloatArray> = arrayOf(
            floatArrayOf(0.8574929f, 0.881742f, 0.94962865f, 0.9833146f, 0.9955178f, 0.9991606f, 0.9998992f, 0.99999315f),
            floatArrayOf(0.51449573f, 0.47173196f, 0.31337744f, 0.1819132f, 0.09457419f, 0.04096558f, 0.01419856f, 0.00369997f)
        )
        private val __STATIC_L3_imdct_gr_g_mdct_window: Array<FloatArray> = arrayOf(
            floatArrayOf(
                0.99904823f,
                0.9914449f,
                0.976296f,
                0.95371693f,
                0.9238795f,
                0.8870108f,
                0.8433915f,
                0.7933533f,
                0.7372773f, 0.04361938f, 0.13052619f,
                0.2164396f, 0.3007058f, 0.38268343f,
                0.4617486f,
                0.53729963f,
                0.6087614f,
                0.6755902f
            ),
            floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 0.9914449f, 0.9238795f, 0.7933533f, 0f, 0f, 0f, 0f, 0f, 0f, 0.13052619f, 0.38268343f, 0.6087614f)
        )
        private val __STATIC_L3_ldexp_q2_g_expfrac: FloatArray = floatArrayOf(9.313226E-10f, 7.831458E-10f, 6.585445E-10f, 5.537677E-10f)
        private val __STATIC_L12_read_scalefactors_g_deq_L12: FloatArray = floatArrayOf(
            (9.536743E-7f / 3f), (7.569318E-7f / 3f), (6.007772E-7f / 3f), (9.536743E-7f / 7f), (7.569318E-7f / 7f), (6.007772E-7f / 7f),
            (9.536743E-7f / 15f), (7.569318E-7f / 15f), (6.007772E-7f / 15f), (9.536743E-7f / 31f), (7.569318E-7f / 31f), (6.007772E-7f / 31f),
            (9.536743E-7f / 63f), (7.569318E-7f / 63f), (6.007772E-7f / 63f), (9.536743E-7f / 127f), (7.569318E-7f / 127f), (6.007772E-7f / 127f),
            (9.536743E-7f / 255f), (7.569318E-7f / 255f), (6.007772E-7f / 255f), (9.536743E-7f / 511f), (7.569318E-7f / 511f), (6.007772E-7f / 511f),
            (9.536743E-7f / 1023f), (7.569318E-7f / 1023f), (6.007772E-7f / 1023f), (9.536743E-7f / 2047f), (7.569318E-7f / 2047f),
            (6.007772E-7f / 2047f), (9.536743E-7f / 4095f), (7.569318E-7f / 4095f), (6.007772E-7f / 4095f), (9.536743E-7f / 8191f),
            (7.569318E-7f / 8191f), (6.007772E-7f / 8191f), (9.536743E-7f / 16383f), (7.569318E-7f / 16383f), (6.007772E-7f / 16383f),
            (9.536743E-7f / 32767f), (7.569318E-7f / 32767f), (6.007772E-7f / 32767f), (9.536743E-7f / 65535f), (7.569318E-7f / 65535f),
            (6.007772E-7f / 65535f), (9.536743E-7f / 3f), (7.569318E-7f / 3f), (6.007772E-7f / 3f), (9.536743E-7f / 5f), (7.569318E-7f / 5f),
            (6.007772E-7f / 5f), (9.536743E-7f / 9f), (7.569318E-7f / 9f), (6.007772E-7f / 9f)
        )
        private val __STATIC_hdr_sample_rate_hz_g_hz: IntArray = intArrayOf(44100, 48000, 32000)

        private val __STATIC_hdr_bitrate_kbps_halfrate: Array<Array<UByteArrayInt>> = arrayOf(
            arrayOf(
                arrayOfUByte("\u0000\u0004\u0008\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050"),
                arrayOfUByte("\u0000\u0004\u0008\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050"),
                arrayOfUByte("\u0000\u0010\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0048\u0050\u0058\u0060\u0070\u0080")
            ),
            arrayOf(
                arrayOfUByte("\u0000\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0050\u0060\u0070\u0080\u00a0"),
                arrayOfUByte("\u0000\u0010\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u0050\u0060\u0070\u0080\u00a0\u00c0"),
                arrayOfUByte("\u0000\u0010\u0020\u0030\u0040\u0050\u0060\u0070\u0080\u0090\u00a0\u00b0\u00c0\u00d0\u00e0")
            )
        )

        private val __STATIC_L3_decode_scalefactors_g_scfc_decode: UByteArrayInt =
            arrayOfUByte("\u0000\u0001\u0002\u0003\u000c\u0005\u0006\u0007\u0009\u000a\u000b\u000d\u000e\u000f\u0012\u0013")
        private val __STATIC_L3_decode_scalefactors_g_mod: UByteArrayInt =
            arrayOfUByte("\u0005\u0005\u0004\u0004\u0005\u0005\u0004\u0001\u0004\u0003\u0001\u0001\u0005\u0006\u0006\u0001\u0004\u0004\u0004\u0001\u0004\u0003\u0001\u0001")
        private val __STATIC_L3_decode_scalefactors_g_preamp: UByteArrayInt = arrayOfUByte("\u0001\u0001\u0001\u0001\u0002\u0002\u0003\u0003\u0003\u0002")
        private val __STATIC_L3_huffman_tabs: ShortArray = arrayOfShort(
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0201\u0201\u0201\u0201\u0201\u0201\u0201" +
                    "\u0201\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\uff01\u0521\u0512\u0502\u0311\u0311" +
                    "\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100" +
                    "\u0100\u0100\u0100\u0122\u0120\uff01\u0521\u0512\u0502\u0301\u0301\u0301\u0301\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0210\u0210" +
                    "\u0210\u0210\u0210\u0210\u0210\u0210\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0122\u0120\uff03\ufec2\ufea1\ufe91\u0311\u0311\u0311" +
                    "\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100" +
                    "\u0100\u0100\u0333\u0332\u0223\u0223\u0113\u0113\u0113\u0113\u0231\u0230\u0203\u0222\u0121\u0112\u0120\u0102\uff02\ufee1\u0531\u0513\u0522" +
                    "\u0520\u0421\u0421\u0412\u0412\u0402\u0402\u0310\u0310\u0310\u0310\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0301\u0301\u0301\u0301" +
                    "\u0300\u0300\u0300\u0300\u0233\u0230\u0132\u0132\u0123\u0103\uff04\ufe63\ufe23\ufde2\u0512\ufdc1\u0411\u0411\u0310\u0310\u0310\u0310\u0301" +
                    "\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe81\ufe71\u0453\u0444" +
                    "\u0452\u0425\u0351\u0351\u0315\u0315\u0450\u0443\u0305\u0305\u0434\u0433\u0155\u0154\u0145\u0135\u0342\u0324\u0241\u0241\u0214\u0214\u0204" +
                    "\u0204\u0340\u0332\u0323\u0330\u0231\u0231\u0213\u0213\u0203\u0222\u0121\u0121\u0120\u0102\uff04\ufe53\ufe13\ufdd1\u0421\u0421\u0412\u0412" +
                    "\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0211\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0200\u0200\u0200\u0200\u0200\u0200\u0200" +
                    "\u0200\ufe82\u0435\ufe61\u0452\u0425\u0450\u0351\u0351\u0315\u0315\u0443\u0434\u0405\u0433\u0342\u0342\u0255\u0245\u0154\u0154\u0153\u0144" +
                    "\u0324\u0341\u0214\u0214\u0340\u0304\u0332\u0323\u0331\u0313\u0330\u0303\u0122\u0122\u0122\u0122\u0120\u0102\uff03\ufea3\ufe62\ufe41\ufe31" +
                    "\u0531\u0513\ufe21\u0522\u0520\u0421\u0421\u0412\u0412\u0402\u0402\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301" +
                    "\u0300\u0300\u0300\u0300\ufec1\u0353\u0335\ufeb1\u0344\u0352\u0325\u0351\u0155\u0154\u0145\u0150\u0215\u0215\u0243\u0243\u0234\u0234\u0305" +
                    "\u0340\u0242\u0224\u0233\u0204\u0141\u0114\u0132\u0123\u0130\u0103\uff05\ufdc4\ufd23\ufcc2\ufca1\ufc91\u0411\u0411\u0310\u0310\u0310\u0310" +
                    "\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe01\ufdf1\ufde1" +
                    "\u0574\u0547\u0565\u0556\u0573\u0537\u0564\ufdd1\u0536\u0472\u0472\u0427\u0427\u0546\u0570\u0407\u0407\u0426\u0426\u0554\u0553\u0460\u0460" +
                    "\u0535\u0544\u0371\u0371\u0371\u0371\u0177\u0176\u0167\u0175\u0157\u0166\u0155\u0145\u0317\u0317\u0463\u0462\ufd41\u0451\u0415\ufd31\u0361" +
                    "\u0361\u0316\u0316\u0306\u0306\u0450\u0405\u0152\u0125\u0143\u0134\ufce1\ufcd1\u0341\u0314\u0304\u0332\u0323\u0330\u0142\u0124\u0133\u0140" +
                    "\u0231\u0213\u0203\u0222\u0121\u0112\u0120\u0102\uff05\ufdf3\ufda3\ufd53\ufd03\ufcc1\ufcb2\u0512\u0421\u0421\u0520\u0502\u0311\u0311\u0311" +
                    "\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0200\u0577\u0576\u0567\u0557\u0566\u0574" +
                    "\u0547\ufe01\u0565\u0556\u0473\u0473\u0437\u0437\u0464\u0464\u0554\u0545\u0553\u0535\u0372\u0372\u0372\u0372\u0327\u0327\u0327\u0327\u0446" +
                    "\u0446\u0470\u0470\u0175\u0155\u0217\u0217\u0371\u0307\u0363\u0336\u0306\ufdb1\u0144\u0152\ufd61\u0351\u0226\u0226\u0362\u0360\u0261\u0261" +
                    "\u0125\u0150\u0216\u0216\u0315\u0343\u0305\ufd11\u0342\u0324\u0134\u0133\u0341\u0314\u0340\u0304\u0232\u0232\u0223\u0223\u0131\u0113\u0230" +
                    "\u0203\u0122\u0122\uff04\ufe73\ufe23\ufdd3\ufd92\ufd73\ufd31\ufd21\ufd12\u0531\u0513\u0522\u0421\u0421\u0412\u0412\u0520\u0502\u0400\u0400" +
                    "\u0311\u0311\u0311\u0311\u0310\u0310\u0310\u0310\u0301\u0301\u0301\u0301\ufe81\u0467\u0475\u0457\u0466\u0474\u0447\u0456\u0365\u0365\u0373" +
                    "\u0373\u0437\u0455\u0372\u0372\u0177\u0176\u0327\u0364\u0346\u0371\u0317\ufe31\u0363\u0336\u0170\u0107\u0354\u0345\u0344\ufde1\u0262\u0262" +
                    "\u0226\u0226\u0160\u0150\u0216\u0216\u0361\u0306\u0353\u0335\u0352\u0325\u0251\u0215\u0243\u0234\u0305\u0340\u0242\u0242\u0224\u0224\u0241" +
                    "\u0241\u0133\u0114\u0132\u0123\u0204\u0230\u0103\u0103\uff06\uf7c5\uf635\uf534\uf4a3\uf462\uf441\uf431\u0411\u0411\u0410\u0410\u0301\u0301" +
                    "\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufd01\ufbe4\ufb43\ufb03\ufab2" +
                    "\ufa83\ufa43\ufa01\uf9f2\uf9d2\uf9b2\uf991\uf982\uf962\uf942\uf921\uf912\uf8f1\uf8e2\uf8c2\uf8a2\u061d\uf881\uf871\uf861\uf851\u06c3\u06c2" +
                    "\u062c\u06b5\uf841\u06c1\u061c\uf831\u060c\uf821\uf811\u06b3\u063b\uf801\u06b2\uf7f1\u064a\uf7e1\u0649\uf7d1\u052b\u052b\u05b1\u05b1\u051b" +
                    "\u051b\u06b0\u060b\u0669\u06a4\u06a3\u063a\u0695\u0659\u05a2\u05a2\u052a\u052a\ufcf4\ufc33\ufc72\u04ff\u04fe\u04fd\u04ee\u04fc\u04ed\u04fb" +
                    "\u04bf\u04ec\u04cd\ufc41\u03ce\u03ce\u03dd\u03dd\ufc51\u02df\u01de\u01de\u01ef\u01cf\u01fa\u019e\ufbf1\u03eb\u03be\u03f9\u039f\u03ae\u03db" +
                    "\u03bd\u01af\u01dc\u04f8\u048f\u04cc\ufb61\u04e8\ufb51\u037f\u037f\u03ad\u03ad\u04da\u04cb\u04bc\u046f\u03f6\u03f6\u01ea\u01e9\u01f7\u01e7" +
                    "\u038e\u03f5\u03d9\u039d\u035f\u037e\u03ca\u03bb\u03f4\u034f\ufac1\u033f\u02f3\u02f3\u03d8\u038d\u01ac\u016e\u02f2\u022f\ufa91\u02f0\u01e6" +
                    "\u01c9\u039c\u03e5\u02ba\u02ba\u03d7\u037d\u02e4\u02e4\u038c\u036d\u02e3\u02e3\u029b\u029b\u03b9\u03aa\u01f1\u011f\u010f\u010f\u02ab\u025e" +
                    "\u024e\u02c8\u02d6\u023e\u012e\u012e\u02e2\u02e0\u01e1\u011e\u020e\u02d5\u025d\u02c7\u027c\u02d4\u02b8\u028b\u024d\u02a9\u029a\u02c6\u016c" +
                    "\u01d3\u023d\u02b7\u01d2\u01d2\u012d\u01d1\u017b\u017b\u02c5\u025c\u0299\u02a7\u013c\u013c\u027a\u0279\u01b4\u01b4\u01d0\u010d\u01a8\u018a" +
                    "\u01c4\u014c\u01b6\u016b\u015b\u0198\u0189\u01c0\u014b\u01a6\u016a\u0197\u0188\u01a5\u015a\u0196\u0187\u0178\u0177\u0167\u05a1\u051a\uf6c1" +
                    "\u050a\uf6b1\u0539\uf6a1\uf691\u0592\u0529\uf681\u0583\u0538\uf671\uf661\uf651\u0491\u0491\u0419\u0419\u0590\u0509\u0584\u0548\u0527\uf641" +
                    "\u0482\u0482\u0428\u0428\u0481\u0481\u01a0\u0186\u0168\u0194\u0193\u0185\u0158\u0176\u0175\u0157\u0166\u0174\u0147\u0165\u0156\u0137\u0164" +
                    "\u0146\u0573\u0572\u0471\u0471\u0417\u0417\u0555\u0570\u0507\u0563\u0536\u0554\u0545\u0562\u0526\u0553\u0318\u0318\u0318\u0318\u0480\u0480" +
                    "\u0408\u0408\u0461\u0461\u0416\u0416\u0460\u0460\u0406\u0406\uf4b1\u0452\u0425\u0450\u0351\u0351\u0315\u0315\u0443\u0434\u0405\u0442\u0424" +
                    "\u0433\u0341\u0341\u0135\u0144\u0214\u0214\u0340\u0304\u0332\u0323\u0231\u0231\u0213\u0230\u0203\u0222\u0121\u0112\u0120\u0102\uff06\ufb65" +
                    "\uf9d5\uf8d4\uf834\uf7b4\uf733\uf6e3\uf693\uf653\uf612\uf5f2\uf5d1\uf5c2\uf5a1\u0522\u0521\u0512\u0520\u0502\u0311\u0311\u0311\u0311\u0410" +
                    "\u0410\u0401\u0401\u0300\u0300\u0300\u0300\ufd02\ufce2\ufcc2\ufca2\ufc81\ufc71\ufc61\ufc51\ufc41\ufc31\ufc21\ufc11\ufc01\ufbf1\ufbe1\ufbd2" +
                    "\u06bc\u066f\ufbb1\ufba1\u065f\u06e7\u067e\u06ca\u06ac\u06bb\ufb91\u06f4\u064f\u06f3\u063f\u068d\u066e\u06f2\u062f\ufb81\u06f1\u061f\u06c9" +
                    "\u069c\u06e5\u06ba\u06ab\u065e\u06d7\u067d\u06e4\u064e\u06c8\u068c\u06e3\u06d6\u066d\u063e\u06b9\u069b\u06e2\u06aa\u062e\u06e1\u061e\ufb71" +
                    "\u06d5\u065d\u02ff\u02fe\u02ef\u02fd\u01ee\u01ee\u02df\u02fc\u02cf\u02ed\u02de\u02fb\u01bf\u01bf\u02ec\u02ce\u01dd\u01fa\u01af\u01eb\u01be" +
                    "\u01dc\u01cd\u01f9\u019f\u01ae\u01db\u01bd\u01f8\u018f\u01cc\u01e9\u019e\u01f7\u017f\u01da\u01ad\u01cb\u01f6\u01f6\u02ea\u02f0\u01e8\u018e" +
                    "\u01f5\u01d9\u019d\u01d8\u01e6\u010f\u01e0\u010e\ufa61\ufa51\u054d\ufa41\ufa31\ufa21\u053d\u052d\ufa11\u05d1\u05b7\u057b\u051d\ufa01\u055c\u05a8\u058a\u05c4\u054c\u05b6\u056b\uf9f1\u05c3\u053c\u05a7\u057a\u056a\uf9e1\u042c\u042c\u05c2\u05b5\u01c7\u017c\u01d4\u01b8\u018b\u01a9\u019a\u01c6\u016c\u01d3\u01d2\u01d0\u01c5\u010d\u0199\u01c0\u010c\u01b0\u055b\u05c1\u0598\u0589\u051c\u05b4\u054b\u05a6" +
                    "\u05b3\u0597\u043b\u043b\u0579\u0588\u05b2\u05a5\u042b\u042b\u055a\u05b1\u041b\u041b\u050b\u0596\u0569\u05a4\u054a\u0587\u0578\u05a3\u043a\u043a\u0495\u0459\u04a2\u042a\u04a1\u041a\uf851\u0486\u0468\u0494\u0449\u0493\u0439\uf841\u0485\u0458\u01a0\u010a\u0177\u0190\u0492\u0476\u0467\u0429\u0319\u0319\u0491\u0409\u0484\u0448\u0475\u0457\u0483\u0438\u0466\u0474\u0382\u0382\u0328\u0328\u0381\u0381" +
                    "\u0318\u0318\u0447\u0480\u0408\u0465\u0456\u0473\u0437\u0464\u0372\u0327\u0346\u0371\u0355\u0317\uf6f1\u0363\u0170\u0107\u0336\u0354\u0345\u0362\u0326\u0361\uf6a1\u0353\u0160\u0106" +
                    "\u0216\u0216\u0335\u0344\u0252\u0252\u0225\u0225\u0251\u0251\u0215\u0215\u0350\u0305\u0243\u0243\u0234\u0242\u0224\u0233\u0114\u0114\u0241\u0240\u0132\u0123\u0204\u0230\u0131\u0131\u0113\u0103\uff05\ufc84\uf7f6\uf5c4\uf4f4\uf473\uf431\uf421\u0411\u0411\u0410\u0410\u0301\u0301\u0301\u0301\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\u0100\ufe01\ufdf1" +
                    "\ufde1\ufdd1\u05fa\ufdc1\ufdb1\u05f8\u05f7\u057f\u05f6\u056f\u03ff\u03ff\u03ff\u03ff\u05f5\u055f\u04f4\u04f4\u044f\u044f\u043f\u043f\u040f\u040f\u05f3\ufda4\u032f\u032f\u032f\u032f\u01fe\u01ef\u01fd\u01df\u01fc\u01cf\u01fb\u01bf\u01af\u01f9\u019f\u018f\ufd22\ufcf2\u04ee\ufcd1\u04eb\u04dc\ufcc1\u04ea\u04cc\ufcb1\ufca1\u04ac\ufc91\u04e5\u03db\u03db\u02ec\ufd01\u01ed\u01ed\u01ce\u01dd\u019e\u019e" +
                    "\u02ae\u029d\u01de\u01be\u01cd\u01bd\u01da\u01ad\u01e7\u01ca\u019c\u01d7\u04f2\u04f0\u03f1\u03f1\u031f\u031f\ufc05\ufb04\ufa54\uf9d3\uf973\uf923\uf8e3\uf8a2\uf873\uf833\u04e9\u04e9" +
                    "\u05cb\u05bc\u05e8\u058e\u05d9\u057e\u05bb\u05d8\u058d\u05e6\u046e\u046e\u04c9\u04c9\u05ba\u05ab\u055e\u057d\u04e4\u04e4\u054e\u05c8" +
                    "\u048c\u048c\u04e3\u04e3\u04d6\u04d6\u056d\u05b9\ufa81\u041e\u044d\ufa71\u04b7\ufa61\u033e\u033e\u04e0\u040e\u04d5\u045d\u04c7\u047c\u04d4\u04b8\u019b\u01aa\u018b\u019a\u017b\u010d\u04a9\u04c6\u046c\u04d3\u04c5\u045c\u03d0\u03d0\u04a8\u048a\u0499\u04c4\u046b\u04a7\u03c3\u03c3\uf991\u03c1\u030c\uf981\u022e\u022e\u03e2\u03e1\u01b5\u0198\u0189\u0197\u033d\u03d2\u032d\u031d\u03b3\uf931\u02d1\u02d1" +
                    "\u0179\u0188\u034c\u03b6\u033c\u037a\u02c2\u02c2\u032c\u035b\u031c\u03c0\u03b4\u034b\u03a6\u036a\u023b\u023b\uf881\u02b2\u022b\u02b1\u01a5\u015a\u021b\u021b\u03b0\u030b\u0396\u0369\u03a4\u034a\u0387\u0378\u023a\u023a\u03a3\u0395\u02a2\u02a2\uf5f1\u061a\uf5e1\u0649\uf5d1\u0676\u052a\u052a\u05a1\u05a1\u06a0\u060a\u0693\u0639\u0685\u0658\u0592\u0592\u0529\u0529\u0667\u0690\u0591\u0591\u0519\u0519" +
                    "\u0609\u0684\u0648\u0657\u0683\u0638\u0666\u0682\u0528\u0528\u0674\u0647\u0581\u0581\u0518\u0518\u0508\u0508\u0680\u0665\u0573\u0573\u0537\u0537\u0656\u0664\u0572\u0572\u0527\u0527" +
                    "\u0646\u0655\u0570\u0570\u0471\u0471\u0471\u0471\u0159\u0186\u0168\u0177\u0194\u0175\u0417\uf541\uf531\uf521\u0426\u0461\u0416\uf511\u0435\uf501\u0452\u0425\u0315\u0315\u0451\u0450\u0107\u0163\u0136\u0154\u0145\u0162\u0160\u0106\u0153\u0144\u0443\u0434\u0405\u0442" +
                    "\u0424\u0433\u0341\u0341\u0314\u0314\u0440\u0404\u0332\u0332\u0323\u0323\u0231\u0231\u0213\u0213\u0330\u0303\u0222\u0222\u0121\u0112\u0120\u0102\uff03\ufec3\ufe83\ufe42\ufe22\ufe03\u04ff\u04ff\ufcd5\ufb65\ufa55\uf924\uf894\uf814\uf773\uf733\uf6e3\uf692\uf673\uf631\uf622\u0521\u0512\uf601\u0411\u0411\u0410\u0410\u0401\u0401\u0400\u0400\u03fe\u03ef\u03fd\u03df\u03fc\u03cf\u03fb\u03bf\u02af\u02af" +
                    "\u03fa\u03f9\u029f\u029f\u028f\u028f\u03f8\u03f7\u027f\u027f\u02f6\u02f6\u026f\u026f\u02f5\u025f\u02f4\u024f\u02f3\u023f\u02f2\u022f\u021f\u021f\u03f1\u030f\ufdc1\ufd93\ufd53\ufd13\u01f0\ufdb2\u02ee\u02ed\u02de\u02ec\u03ce\u03dd\u03eb\u03be\u03dc\u03cd\u03ea\u03ae\u03db\u03bd\u03cc\u03e9\u039e\u03da\u03ad\u03cb\u03bc\u03e8\u038e\u03d9\u039d\u03e7\u037e\u03ca\ufbd1\ufbc1\ufbb2\u056e\ufb91\u059c" +
                    "\u05e5\u05ab\u055e\ufb81\u057d\u054e\u05c8\u058c\ufb71\u05e3\u05d6\u056d\u053e\u05b9\u059b\u05aa\u052e\u05e1\u051e\u05d5\u055d\u05c7\u057c\u05d4\u05b8\u058b\u01ac\u01bb\u01d8\u018d" +
                    "\u02e0\u020e\u01d0\u01d0\u01e6\u01c9\u01ba\u01d7\u01e4\u01e2\u054d\u05a9\u059a\u05c6\u056c\u05d3\u053d\u05d2\u052d\u05d1\u05b7\u057b\u051d\u05c5\u055c\u05a8\u058a\u0599\u05c4\u054c\u05b6\u056b\ufa61\u05c3\u053c\u05a7\u057a\u05c2\u052c\u05b5\u055b\u05c1\u010d\u01c0\u0598\u0589\u051c\u05b4\uf951\u05b3\uf941\u05a1\u044b\u044b\u05a6\u056a\u0597\u0579\uf931\u0509\u043b\u043b\u0488\u0488\u05b2\u05a5" +
                    "\u042b\u042b\u055a\u05b1\u051b\u0596\u0469\u0469\u044a\u044a\u010c\u01b0\u010b\u01a0\u010a\u0190\uf8a1\u0478\u04a3\u043a\u0495\u0459\u04a2\u042a\u041a\u0486\u0468\u0477\u0494\u0449\u0493\u0439\u01a4\u0187\u0485\u0458\u0492\u0476\u0467\u0429\u0491\u0419\u0484\u0448\u0475\u0457\u0483\u0438\u0466\u0482\u0428\u0481\u0474\u0447\u0418\uf791\u0465\u0456\u0471\uf781\u0337\u0337\u0473\u0472\u0327\u0327" +
                    "\u0180\u0108\u0170\u0107\u0364\u0346\u0355\u0317\u0363\u0336\u0354\u0345\u0362\u0326\u0361\u0316\uf6f1\u0353\u0335\u0344\u0160\u0106\u0352\u0325\u0351\uf6a1\u0215\u0215\u0343\u0334" +
                    "\u0150\u0105\u0242\u0224\u0233\u0241\u0214\u0214\u0340\u0304\u0232\u0232\u0223\u0223\u0131\u0113\u0230\u0203\u0122\u0122\u0120\u0102"
        )
        private val __STATIC_L3_huffman_tab32: UByteArrayInt =
            arrayOfUByte("\u0082\u00a2\u00c1\u00d1\u002c\u001c\u004c\u008c\u0009\u0009\u0009\u0009\u0009\u0009\u0009\u0009\u00be\u00fe\u00de\u00ee\u007e\u005e\u009d\u009d\u006d\u003d\u00ad\u00cd")
        private val __STATIC_L3_huffman_tab33: UByteArrayInt =
            arrayOfUByte("\u00fc\u00ec\u00dc\u00cc\u00bc\u00ac\u009c\u008c\u007c\u006c\u005c\u004c\u003c\u002c\u001c\u000c")
        private val __STATIC_L3_huffman_tabindex: ShortArray =
            arrayOfShort("\u0000\u0020\u0040\u0062\u0000\u0084\u00b4\u00da\u0124\u016c\u01aa\u021a\u0288\u02ea\u0000\u0466\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u05b4\u0732\u0732\u0732\u0732\u0732\u0732\u0732\u0732")
        private val __STATIC_L3_huffman_g_linbits: UByteArrayInt =
            arrayOfUByte("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0003\u0004\u0006\u0008\u000a\u000d\u0004\u0005\u0006\u0007\u0008\u0009\u000b\u000d")

        private val __STATIC_L12_read_scale_info_g_bitalloc_code_tab: UByteArrayInt = arrayOfUByte(
            "\u0000\u0011\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0010\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0006" +
                    "\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u0010\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0010\u0000\u0011\u0012\u0010\u0000\u0011\u0012" +
                    "\u0013\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0000\u0011\u0012\u0003\u0013\u0004\u0005\u0006\u0007\u0008" +
                    "\u0009\u000a\u000b\u000c\u000d\u000e\u0000\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f\u0010"
        )
        private val __STATIC_L3_read_side_info_g_scf_long: Array<UByteArrayInt> = arrayOf(
            arrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000"),
            arrayOfUByte("\u000c\u000c\u000c\u000c\u000c\u000c\u0010\u0014\u0018\u001c\u0020\u0028\u0030\u0038\u0040\u004c\u005a\u0002\u0002\u0002\u0002\u0002\u0000"),
            arrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000"),
            arrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0012\u0016\u001a\u0020\u0026\u002e\u0036\u003e\u0046\u004c\u0024\u0000"),
            arrayOfUByte("\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u000a\u000c\u000e\u0010\u0014\u0018\u001c\u0020\u0026\u002e\u0034\u003c\u0044\u003a\u0036\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0008\u0008\u000a\u000c\u0010\u0014\u0018\u001c\u0022\u002a\u0032\u0036\u004c\u009e\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u000a\u000c\u0010\u0012\u0016\u001c\u0022\u0028\u002e\u0036\u0036\u00c0\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0008\u000a\u000c\u0010\u0014\u0018\u001e\u0026\u002e\u0038\u0044\u0054\u0066\u001a\u0000")
        )
        private val __STATIC_L3_read_side_info_g_scf_short: Array<UByteArrayInt> = arrayOf(
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000"),
            arrayOfUByte("\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u0018\u0018\u0018\u001c\u001c\u001c\u0024\u0024\u0024\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u001a\u001a\u001a\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000e\u000e\u000e\u0012\u0012\u0012\u001a\u001a\u001a\u0020\u0020\u0020\u002a\u002a\u002a\u0012\u0012\u0012\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u0020\u0020\u0020\u002c\u002c\u002c\u000c\u000c\u000c\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0016\u0016\u0016\u001e\u001e\u001e\u0038\u0038\u0038\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0042\u0042\u0042\u0000"),
            arrayOfUByte("\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0022\u0022\u0022\u002a\u002a\u002a\u000c\u000c\u000c\u0000")
        )
        private val __STATIC_L3_read_side_info_g_scf_mixed: Array<UByteArrayInt> = arrayOf(
            arrayOfUByte(
                "\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000",
                size = 40
            ),
            arrayOfUByte("\u000c\u000c\u000c\u0004\u0004\u0004\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u0018\u0018\u0018\u001c\u001c\u001c\u0024\u0024\u0024\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u001a\u001a\u001a\u0000"),
            arrayOfUByte(
                "\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000e\u000e\u000e\u0012\u0012\u0012\u001a\u001a\u001a\u0020\u0020\u0020\u002a\u002a\u002a\u0012\u0012\u0012\u0000",
                size = 40
            ),
            arrayOfUByte(
                "\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u0020\u0020\u0020\u002c\u002c\u002c\u000c\u000c\u000c\u0000",
                size = 40
            ),
            arrayOfUByte(
                "\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0018\u0018\u0018\u001e\u001e\u001e\u0028\u0028\u0028\u0012\u0012\u0012\u0000",
                size = 40
            ),
            arrayOfUByte(
                "\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0012\u0012\u0012\u0016\u0016\u0016\u001e\u001e\u001e\u0038\u0038\u0038\u0000",
                size = 40
            ),
            arrayOfUByte(
                "\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0006\u0006\u0006\u000a\u000a\u000a\u000c\u000c\u000c\u000e\u000e\u000e\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0042\u0042\u0042\u0000",
                size = 40
            ),
            arrayOfUByte(
                "\u0004\u0004\u0004\u0004\u0004\u0004\u0006\u0006\u0004\u0004\u0004\u0006\u0006\u0006\u0008\u0008\u0008\u000c\u000c\u000c\u0010\u0010\u0010\u0014\u0014\u0014\u001a\u001a\u001a\u0022\u0022\u0022\u002a\u002a\u002a\u000c\u000c\u000c\u0000",
                size = 40
            )
        )
        private val __STATIC_L3_decode_scalefactors_g_scf_partitions: Array<UByteArrayInt> = arrayOf(
            arrayOfUByte("\u0006\u0005\u0005\u0005\u0006\u0005\u0005\u0005\u0006\u0005\u0007\u0003\u000b\u000a\u0000\u0000\u0007\u0007\u0007\u0000\u0006\u0006\u0006\u0003\u0008\u0008\u0005\u0000"),
            arrayOfUByte("\u0008\u0009\u0006\u000c\u0006\u0009\u0009\u0009\u0006\u0009\u000c\u0006\u000f\u0012\u0000\u0000\u0006\u000f\u000c\u0000\u0006\u000c\u0009\u0006\u0006\u0012\u0009\u0000"),
            arrayOfUByte("\u0009\u0009\u0006\u000c\u0009\u0009\u0009\u0009\u0009\u0009\u000c\u0006\u0012\u0012\u0000\u0000\u000c\u000c\u000c\u0000\u000c\u0009\u0009\u0006\u000f\u000c\u0009\u0000")
        )

        private val __STATIC_L12_subband_alloc_table_g_alloc_L1: Array<L12_subband_alloc_tStruct> = arrayOf(
            L12_subband_alloc_tStruct(76u, 4u, 32u),
        )
        private val __STATIC_L12_subband_alloc_table_g_alloc_L2M2: Array<L12_subband_alloc_tStruct> = arrayOf(
            L12_subband_alloc_tStruct(60u, 4u, 4u),
            L12_subband_alloc_tStruct(44u, 3u, 7u),
            L12_subband_alloc_tStruct(44u, 2u, 19u),
        )
        private val __STATIC_L12_subband_alloc_table_g_alloc_L2M1: Array<L12_subband_alloc_tStruct> = arrayOf(
            L12_subband_alloc_tStruct(0u, 4u, 3u),
            L12_subband_alloc_tStruct(16u, 4u, 8u),
            L12_subband_alloc_tStruct(32u, 3u, 12u),
            L12_subband_alloc_tStruct(40u, 2u, 7u),
        )
        private val __STATIC_L12_subband_alloc_table_g_alloc_L2M1_lowrate: Array<L12_subband_alloc_tStruct> = arrayOf(
            L12_subband_alloc_tStruct(44u, 4u, 2u),
            L12_subband_alloc_tStruct(44u, 3u, 10u),
        )

        private val g_pow43: FloatArray = floatArrayOf(
            0f, -1f, -2.519842f, -4.326749f, -6.349604f, -8.54988f, -10.902724f, -13.390518f, -16f, -18.720755f, -21.544348f, -24.463781f, -27.473143f,
            -30.56735f, -33.741993f, -36.99318f, 0f, 1f, 2.519842f, 4.326749f, 6.349604f, 8.54988f, 10.902724f, 13.390518f, 16f, 18.720755f, 21.544348f,
            24.463781f, 27.473143f, 30.56735f, 33.741993f, 36.99318f, 40.317474f, 43.71179f, 47.173344f, 50.69963f, 54.288353f, 57.93741f, 61.644863f,
            65.40894f, 69.22798f, 73.10044f, 77.024895f, 81f, 85.02449f, 89.09719f, 93.21697f, 97.3828f, 101.593666f, 105.84863f, 110.146805f,
            114.48732f, 118.869385f, 123.292206f, 127.755066f, 132.25725f, 136.79808f, 141.3769f, 145.99312f, 150.64612f, 155.33533f, 160.0602f,
            164.8202f, 169.61482f, 174.44357f, 179.30598f, 184.20157f, 189.12991f, 194.09058f, 199.08315f, 204.10721f, 209.16238f, 214.24829f,
            219.36456f, 224.51085f, 229.68678f, 234.89206f, 240.12633f, 245.38928f, 250.6806f, 256f, 261.34717f, 266.72183f, 272.12372f,
            277.55255f, 283.00806f, 288.48996f, 293.99805f, 299.53207f, 305.09177f, 310.6769f, 316.28726f, 321.92258f, 327.5827f, 333.26736f,
            338.97638f, 344.70956f, 350.46664f, 356.24747f, 362.05188f, 367.8796f, 373.73053f, 379.60443f, 385.50113f, 391.4205f, 397.3623f,
            403.32642f, 409.31268f, 415.3209f, 421.3509f, 427.4026f, 433.47574f, 439.57028f, 445.68597f, 451.82275f, 457.98044f, 464.15887f,
            470.35797f, 476.57755f, 482.81744f, 489.0776f, 495.35788f, 501.65808f, 507.97815f, 514.31793f, 520.6773f, 527.0562f, 533.4544f,
            539.8719f, 546.3085f, 552.76404f, 559.2386f, 565.7319f, 572.2439f, 578.7744f, 585.3235f, 591.89087f, 598.47656f, 605.08044f,
            611.70233f, 618.3422f, 625f, 631.67554f, 638.3688f, 645.0796f
        )
    }

    data class L12_subband_alloc_tStruct(val tab_offset: UByte, val code_tab_width: UByte, val band_count: UByte)

    class Mp3Dec(
        var mdct_overlap: Array<FloatArray>,
        var qmf_state: FloatArray,
        var reserv: Int,
        var free_format_bytes_array: IntArray,
        var header: UByteArrayInt,
        var reserv_buf: UByteArrayInt,
    ) {
        constructor() : this(
            mdct_overlap = Array(2) { FloatArray(288) },
            qmf_state = FloatArray(960),
            reserv = 0,
            free_format_bytes_array = IntArray(1),
            header = UByteArrayInt(4),
            reserv_buf = UByteArrayInt(MAX_BITRESERVOIR_BYTES),
        )
    }

    class Bs(
        var buf: UByteArrayIntPtr = UByteArrayIntPtr(UByteArrayInt(0)),
        var pos: Int = 0,
        var limit: Int = 0,
    )

    class Mp3Scratch(
        var bs: Bs,
        var maindata: UByteArrayIntPtr,
        var gr_info: ArrayPtr<GrInfo>,
        var grbuf: Array<FloatArrayPtr>,
        var scf: FloatArrayPtr,
        var syn: Array<FloatArrayPtr>,
        var ist_pos: Array<UByteArrayInt>,
    ) {
        constructor() : this(
            bs = Bs(),
            maindata = UByteArrayIntPtr(UByteArrayInt(2815)),
            gr_info = ArrayPtr(Array(4) { GrInfo() }, 0),
            grbuf = kotlin.run {
                val CSIZE = 576
                val data = FloatArray(2 * CSIZE)
                Array(2) {
                    FloatArrayPtr(data, CSIZE * it)
                }
            },
            scf = FloatArrayPtr(FloatArray(40)),
            syn = kotlin.run {
                val CSIZE = 64
                val data = FloatArray(33 * CSIZE)
                Array(33) { FloatArrayPtr(data, CSIZE * it) }
            },
            ist_pos = Array(2) { UByteArrayInt(39) },
        )
    }

    // mp3dec_frame_info_t
    class Mp3FrameInfo(
        var frame_bytes: Int = 0,
        var frame_offset: Int = 0,
        var channels: Int = 0,
        var hz: Int = 0,
        var layer: Int = 0,
        var bitrate_kbps: Int = 0,
    ) {
        val value get() = this
    }

    class ArrayPtr<T>(val array: Array<T>, val pos: Int) {
        var value: T
            get() = array[pos]
            set(value) {
                array[pos] = value
            }

        operator fun get(index: Int): T = array[pos + index]
        operator fun set(index: Int, value: T) {
            array[pos + index] = value
        }

        operator fun inc(): ArrayPtr<T> = ArrayPtr(array, pos + 1)
        operator fun plus(other: Int): ArrayPtr<T> = ArrayPtr(array, pos + other)
        operator fun minus(other: ArrayPtr<T>): Int = pos - other.pos
        fun fill(value: T, from: Int, to: Int) {
            array.fill(value, this.pos + from, this.pos + to)
        }
    }

    class UByteArrayIntPtr(val array: UByteArrayInt, val pos: Int = 0) {
        var value: Int
            get() = array[pos]
            set(value) {
                array[pos] = value
            }

        operator fun get(index: Int): Int = array[pos + index]
        operator fun set(index: Int, value: Int) {
            array[pos + index] = value
        }

        operator fun inc(): UByteArrayIntPtr = UByteArrayIntPtr(array, pos + 1)
        operator fun plus(other: Int): UByteArrayIntPtr = UByteArrayIntPtr(array, pos + other)
        operator fun minus(other: UByteArrayIntPtr): Int = pos - other.pos
        fun minusPtrUByte(other: UByteArrayIntPtr): Int = pos - other.pos
        fun fill(value: UByte, from: Int, to: Int) {
            array.bytes.fill(value.toByte(), this.pos + from, this.pos + to)
        }
    }

    class FloatArrayPtr(val array: FloatArray, val pos: Int = 0) {
        var value: Float
            get() = array[pos]
            set(value) {
                array[pos] = value
            }

        operator fun get(index: Int): Float = array[pos + index]
        operator fun set(index: Int, value: Float) {
            array[pos + index] = value
        }

        operator fun inc(): FloatArrayPtr = FloatArrayPtr(array, pos + 1)
        operator fun plus(other: Int): FloatArrayPtr = FloatArrayPtr(array, pos + other)
        operator fun minus(other: FloatArrayPtr): Int = pos - other.pos
        fun fill(value: Float, from: Int, to: Int) {
            array.fill(value, this.pos + from, this.pos + to)
        }
    }

    class ShortArrayPtr(val array: ShortArray, val pos: Int = 0) {
        var value: Short
            get() = array[pos]
            set(value) {
                array[pos] = value
            }

        operator fun get(index: Int): Short = array[pos + index]
        operator fun set(index: Int, value: Short) {
            array[pos + index] = value
        }

        operator fun inc(): ShortArrayPtr = ShortArrayPtr(array, pos + 1)
        operator fun plus(other: Int): ShortArrayPtr = ShortArrayPtr(array, pos + other)
        operator fun minus(other: ShortArrayPtr): Int = pos - other.pos
        fun fill(value: Short, from: Int, to: Int) {
            array.fill(value, this.pos + from, this.pos + to)
        }
    }

    fun memcpy(dst: FloatArrayPtr, src: FloatArrayPtr, size: Int) {
        arraycopy(src.array, src.pos, dst.array, dst.pos, size)
    }

    fun memcpy(dst: UByteArrayIntPtr, src: UByteArrayIntPtr, size: Int) {
        arraycopy(src.array, src.pos, dst.array, dst.pos, size)
    }

    class GrInfo(
        var sfbtab: UByteArrayIntPtr = UByteArrayIntPtr(UByteArrayInt(0)),
        var part_23_length: Int = 0,
        var big_values: Int = 0,
        var scalefac_compress: Int = 0,
        var global_gain: Int = 0,
        var block_type: Int = 0,
        var mixed_block_flag: Int = 0,
        var n_long_sfb: Int = 0,
        var n_short_sfb: Int = 0,
        var table_select: UByteArrayInt = UByteArrayInt(3),
        var region_count: UByteArrayInt = UByteArrayInt(3),
        var subblock_gain: UByteArrayInt = UByteArrayInt(3),
        var preflag: Int = 0,
        var scalefac_scale: Int = 0,
        var count1_table: Int = 0,
        var scfsi: Int = 0,
    )

    // CPointer<L12_scale_info>
    class ScaleInfo(
        var scf: FloatArray = FloatArray(192),
        var total_bands: Int = 0,
        var stereo_bands: Int = 0,
        var bitalloc: UByteArrayInt = UByteArrayInt(64),
        var scfcod: UByteArrayInt = UByteArrayInt(64),
    )

    ///////////////////////////////////
    // PROGRAM
    ///////////////////////////////////

    fun Boolean.toInt(): Int = if (this) 1 else 0

    fun bs_init(bs: Bs, data: UByteArrayIntPtr, bytes: Int) {
        bs.buf = data
        bs.pos = 0
        bs.limit = bytes * 8
    }

    fun get_bits(bs: Bs, n: Int): Int {
        var cache = 0
        val s: Int = ((bs.pos and 7))
        var shl: Int = n + (s)
        var p: UByteArrayIntPtr = bs.buf + ((bs.pos ushr 3))
        bs.pos += n
        if (bs.pos > bs.limit) return 0
        var next = p.value and (0xFF ushr s)
        p++
        while (true) {
            shl -= 8
            if (shl <= 0) break
            cache = cache or (next shl shl)
            next = p.value
            p++
        }
        return cache or (next ushr (-shl))
    }

    fun hdr_valid(h: UByteArrayIntPtr): Boolean {
        val h0 = h[0]
        val h1 = h[1]
        val h2 = h[2]
        return h0 == 255 && (h1 and 240 == 240 || (h1 and 254 == 226)) && (h1 shr 1 and 3 != 0) && (h2 shr 4 != 15) && (h2 shr 2 and 3 != 3)
    }

    fun hdr_compare(h1: UByteArrayIntPtr, h2: UByteArrayIntPtr): Int {
        return (hdr_valid(h2) && (h1[1] xor h2[1] and 254) == 0 && (h1[2] xor (h2[2]) and 12) == 0 && (((h1[2]) and 240) == 0).toInt() xor ((h2[2] and 240) == 0).toInt() == 0).toInt()
    }

    fun hdr_bitrate_kbps(h: UByteArrayIntPtr): Int {
        return (2 * __STATIC_hdr_bitrate_kbps_halfrate[(h[1] and 8 != 0).toInt()][(h[1] shr 1 and 3) - 1][h[2] shr 4])
    }

    fun hdr_sample_rate_hz(h: UByteArrayIntPtr): Int {
        val g_hz = __STATIC_hdr_sample_rate_hz_g_hz
        return (g_hz[h[2] shr 2 and 3] shr (h[1] and 8 == 0).toInt() shr (h[1] and 16 == 0).toInt()).toInt()
    }

    fun hdr_frame_samples(h: UByteArrayIntPtr): Int {
        return (if (h[1] and 6 == 6) 384 else 1152 shr (h[1] and 14 == 2).toInt()).toInt()
    }

    fun hdr_frame_bytes(h: UByteArrayIntPtr, free_format_size: Int): Int {
        var frame_bytes: Int = (hdr_frame_samples(h) * hdr_bitrate_kbps(h) * 125 / hdr_sample_rate_hz(h))
        if (h[1] and 6 == 6) {
            frame_bytes = frame_bytes and ((3).inv())
        }
        return if (frame_bytes != 0) frame_bytes else free_format_size
    }

    fun hdr_padding(h: UByteArrayIntPtr): Int = when {
        h[2] and 2 != 0 -> if (h[1] and 6 == 6) 4 else 1
        else -> 0
    }

    fun L12_subband_alloc_table(hdr: UByteArrayIntPtr, sci: ScaleInfo): Array<L12_subband_alloc_tStruct> {
        var alloc: Array<L12_subband_alloc_tStruct> = __STATIC_L12_subband_alloc_table_g_alloc_L1
        val mode: Int = hdr[3] shr 6 and 3
        var nbands: Int
        val stereo_bands: Int = when (mode) {
            3 -> 0
            1 -> (hdr[3] shr 4 and 3 shl 2) + 4
            else -> 32
        }
        when {
            hdr[1] and 6 == 6 -> {
                alloc = __STATIC_L12_subband_alloc_table_g_alloc_L1
                nbands = 32
            }

            hdr[1] and 8 == 0 -> {
                alloc = __STATIC_L12_subband_alloc_table_g_alloc_L2M2
                nbands = 30
            }

            else -> {
                val sample_rate_idx: Int = hdr[2] shr 2 and 3
                var kbps: Int = hdr_bitrate_kbps(hdr) shr (mode != 3).toInt()
                if (kbps == 0) kbps = 192
                alloc = __STATIC_L12_subband_alloc_table_g_alloc_L2M1
                nbands = 27
                when {
                    kbps < 56 -> {
                        alloc = __STATIC_L12_subband_alloc_table_g_alloc_L2M1_lowrate
                        nbands = if (sample_rate_idx == 2) 12 else 8
                    }

                    kbps >= 96 && sample_rate_idx != 1 -> {
                        nbands = 30
                    }
                }
            }
        }
        sci.total_bands = nbands
        sci.stereo_bands = if (stereo_bands > nbands) nbands else stereo_bands
        return alloc
    }

    fun L12_read_scalefactors(bs: Bs, pba: UByteArrayInt, scfcod: UByteArrayInt, bands: Int, scf: FloatArrayPtr) {
        var scfOffset = 0
        val g_deq_L12 = __STATIC_L12_read_scalefactors_g_deq_L12
        for (i in 0 until bands) {
            var s = 0f
            val ba: Int = pba[i]
            val mask: Int = if (ba != 0) 4 + (19 shr scfcod[i] and 3) else 0
            var m = 4
            while (m != 0) {
                if (((mask and m)) != 0) {
                    val b: Int = get_bits(bs, 6)
                    s = g_deq_L12[((ba * 3) - 6) + (b % 3)] * ((((1 shl 21) shr (b / 3))).toFloat())
                }
                scf[scfOffset] = s
                m = m shr 1
                scfOffset++
            }
        }
    }

    fun L12_read_scale_info(hdr: UByteArrayIntPtr, bs: Bs, sci: ScaleInfo) {
        val g_bitalloc_code_tab = UByteArrayIntPtr(__STATIC_L12_read_scale_info_g_bitalloc_code_tab)
        val subband_alloc = L12_subband_alloc_table(hdr, sci)
        var subband_alloc_n = 0
        var k = 0
        var ba_bits: Int = 0
        var ba_code_tab: UByteArrayIntPtr = (g_bitalloc_code_tab)
        for (i in 0 until sci.total_bands) {
            var ba: Int = 0
            if (i == k) {
                k += (subband_alloc[subband_alloc_n].band_count.toInt())
                ba_bits = subband_alloc[subband_alloc_n].code_tab_width.toInt()
                ba_code_tab = g_bitalloc_code_tab + subband_alloc[subband_alloc_n].tab_offset.toInt()
                subband_alloc_n++
            }
            ba = ba_code_tab[get_bits(bs, ba_bits)]
            sci.bitalloc[2 * i] = ba
            if (i < (sci.stereo_bands)) {
                ba = ba_code_tab[get_bits(bs, ba_bits)]
            }
            sci.bitalloc[(2 * i) + 1] = if (sci.stereo_bands != 0) ba else 0
        }
        for (i in 0 until 2 * sci.total_bands) {
            sci.scfcod[i] = when {
                sci.bitalloc[i] != 0 -> when {
                    hdr[1] and 6 == 6 -> 2
                    else -> get_bits(bs, 2)
                }

                else -> 6
            }
        }
        L12_read_scalefactors(
            bs,
            sci.bitalloc,
            sci.scfcod,
            sci.total_bands * 2,
            FloatArrayPtr(sci.scf)
        )
        for (i in sci.stereo_bands until sci.total_bands) {
            sci.bitalloc[(2 * i) + 1] = 0
        }
    }

    fun L12_dequantize_granule(grbuf: FloatArrayPtr, bs: Bs, sci: ScaleInfo, group_size: Int): Int {
        var choff = 576
        for (j in 0 until 4) {
            var dst: FloatArrayPtr = grbuf + ((group_size * j))
            for (i in 0 until (2 * (sci.total_bands))) {
                val ba: Int = sci.bitalloc[i]
                if (ba != 0) {
                    if (ba < 17) {
                        val half: Int = (1 shl (ba - 1)) - 1
                        for (k in 0 until group_size) {
                            dst[k] = (((get_bits(bs, ba)) - half)).toFloat()
                        }
                    } else {
                        val mod = (((2 shl (ba - 17)) + 1))
                        var code = get_bits(bs, mod + 2 - (mod shr 3))
                        for (k in 0 until group_size) {
                            dst[k] = (code % mod - (mod / 2)).toFloat()
                            code /= mod
                        }
                    }
                }
                dst += choff
                choff = 18 - choff
            }
        }
        return group_size * 4
    }

    fun L12_apply_scf_384(sci: ScaleInfo, scf: FloatArrayPtr, dst: FloatArrayPtr) {
        var scf: FloatArrayPtr = scf // Mutating parameter
        var dst: FloatArrayPtr = dst // Mutating parameter
        memcpy(
            dst + 576 + sci.stereo_bands * 18,
            dst + sci.stereo_bands * 18,
            (sci.total_bands - sci.stereo_bands) * 18
        )
        for (i in 0 until sci.total_bands) {
            for (k in 0 until 12) {
                dst[k + 0] = dst[k + 0] * scf[0]
                dst[k + 576] = dst[k + 576] * scf[3]
            }
            dst += 18
            scf += 6
        }
    }

    fun L3_read_side_info(bs: Bs, gr: ArrayPtr<GrInfo>, hdr: UByteArrayIntPtr): Int {
        var gr: ArrayPtr<GrInfo> = gr // Mutating parameter
        val g_scf_long = __STATIC_L3_read_side_info_g_scf_long
        val g_scf_short = __STATIC_L3_read_side_info_g_scf_short
        val g_scf_mixed = __STATIC_L3_read_side_info_g_scf_mixed
        var tables: Int
        var scfsi: Int = 0
        val main_data_begin: Int
        var part_23_sum = 0
        var sr_idx: Int = (hdr[2] shr 2 and 3) + ((hdr[1] shr 3 and 1) + (hdr[1] shr 4 and 1)) * 3
        sr_idx -= (sr_idx != 0).toInt()
        var gr_count: Int = (if (hdr[3] and 192 == 192) 1 else 2)
        if (hdr[1] and 8 != 0) {
            gr_count *= 2
            main_data_begin = get_bits(bs, 9)
            scfsi = get_bits(bs, (7 + gr_count))
        } else {
            main_data_begin = ((get_bits(bs, (8 + gr_count)) shr gr_count))
        }
        do {
            if (hdr[3] and 192 == 192) {
                scfsi = scfsi shl 4
            }
            gr.value.part_23_length = get_bits(bs, 12)
            part_23_sum += (gr.value.part_23_length)
            gr.value.big_values = get_bits(bs, 9)
            if (gr.value.big_values > 288) return -1
            gr.value.global_gain = get_bits(bs, 8)
            gr.value.scalefac_compress = get_bits(bs, if (hdr[1] and 8 != 0) 4 else 9)
            gr.value.sfbtab = UByteArrayIntPtr(g_scf_long[sr_idx])
            gr.value.n_long_sfb = 22
            gr.value.n_short_sfb = 0
            if (get_bits(bs, 1) != 0) {
                gr.value.block_type = get_bits(bs, 2)
                if (gr.value.block_type == 0) return -1
                gr.value.mixed_block_flag = get_bits(bs, 1)
                gr.value.region_count[0] = 7u
                gr.value.region_count[1] = 255
                if (gr.value.block_type == 2) {
                    scfsi = scfsi and 3855
                    if (gr.value.mixed_block_flag == 0) {
                        gr.value.region_count[0] = 8
                        gr.value.sfbtab = UByteArrayIntPtr(g_scf_short[sr_idx])
                        gr.value.n_long_sfb = 0
                        gr.value.n_short_sfb = 39
                    } else {
                        gr.value.sfbtab = UByteArrayIntPtr(g_scf_mixed[sr_idx])
                        gr.value.n_long_sfb = if (hdr[1] and 8 != 0) 8 else 6
                        gr.value.n_short_sfb = 30
                    }
                }
                tables = get_bits(bs, 10)
                tables = tables shl 5
                gr.value.subblock_gain[0] = get_bits(bs, 3)
                gr.value.subblock_gain[1] = get_bits(bs, 3)
                gr.value.subblock_gain[2] = get_bits(bs, 3)
            } else {
                gr.value.block_type = 0
                gr.value.mixed_block_flag = 0
                tables = get_bits(bs, 15)
                gr.value.region_count[0] = get_bits(bs, 4)
                gr.value.region_count[1] = get_bits(bs, 3)
                gr.value.region_count[2] = 255
            }
            gr.value.table_select[0] = tables shr 10
            gr.value.table_select[1] = tables shr 5 and 31
            gr.value.table_select[2] = tables and 31
            gr.value.preflag = if (hdr[1] and 8 != 0) get_bits(bs, 1) else (gr.value.scalefac_compress >= 500).toInt()
            gr.value.scalefac_scale = get_bits(bs, 1)
            gr.value.count1_table = get_bits(bs, 1)
            gr.value.scfsi = (scfsi shr 12) and 15
            scfsi = scfsi shl 4
            gr++
        } while (--gr_count != 0)
        if (part_23_sum + bs.pos > bs.limit + (main_data_begin * 8)) return -1
        return main_data_begin
    }

    fun L3_read_scalefactors(scf: UByteArrayIntPtr, ist_pos: UByteArrayInt, scf_size: UByteArrayInt, scf_count: UByteArrayIntPtr, bitbuf: Bs, scfsi: Int) {
        var scf: UByteArrayIntPtr = scf // Mutating parameter
        var ist_pos = UByteArrayIntPtr(ist_pos) // Mutating parameter
        var scfsi: Int = scfsi // Mutating parameter
        for (i in 0 until 4) {
            if (scf_count[i] == 0) break
            val cnt: Int = scf_count[i]
            if (((scfsi and 8)) != 0) {
                for (n in 0 until cnt) scf[n] = ist_pos[n]
            } else {
                val bits: Int = scf_size[i]
                if (bits == 0) {
                    scf.fill(0u, 0, cnt)
                    ist_pos.fill(0u, 0, cnt)
                } else {
                    val max_scf: Int = ((if (scfsi < 0) ((((1 shl bits) - 1)).toLong()) else -1L)).toInt()
                    for (k in 0 until cnt) {
                        val s: Int = get_bits(bitbuf, bits)
                        ist_pos[k] = ((if (s == max_scf) -1L else (s.toLong()))).toInt()
                        scf[k] = s
                    }
                }
            }
            ist_pos += cnt
            scf += cnt
            scfsi *= 2
        }
        scf[0] = 0
        scf[1] = 0
        scf[2] = 0
    }

    fun L3_ldexp_q2(y: Float, exp_q2: Int): Float {
        var y: Float = y
        var exp_q2: Int = exp_q2 // Mutating parameter
        val g_expfrac: FloatArray = __STATIC_L3_ldexp_q2_g_expfrac
        do {
            val e = (if ((30 * 4) > exp_q2) exp_q2 else (30 * 4))
            y *= (g_expfrac[e and 3] * ((((1 shl 30) shr (e shr 2))).toFloat()))
            exp_q2 -= e
        } while (exp_q2 > 0)
        return y
    }

    fun L3_decode_scalefactors(hdr: UByteArrayInt, ist_pos: UByteArrayInt, bs: Bs, gr: ArrayPtr<GrInfo>, scf: FloatArrayPtr, ch: Int) {
        val g_scf_partitions = __STATIC_L3_decode_scalefactors_g_scf_partitions
        var scf_partition = UByteArrayIntPtr(g_scf_partitions[(gr.value.n_short_sfb != 0).toInt() + (gr.value.n_long_sfb == 0).toInt()])
        val scf_size = UByteArrayInt(4)
        val iscf = UByteArrayInt(40)
        val scf_shift: Int = gr.value.scalefac_scale + 1
        var scfsi: Int = gr.value.scfsi
        if (hdr[1] and 8 != 0) {
            val g_scfc_decode = __STATIC_L3_decode_scalefactors_g_scfc_decode
            val part: Int = g_scfc_decode[gr.value.scalefac_compress]
            scf_size[0] = part shr 2
            scf_size[1] = scf_size[0]
            scf_size[2] = part and 3
            scf_size[3] = scf_size[2]
        } else {
            val g_mod = __STATIC_L3_decode_scalefactors_g_mod
            val ist: Int = (hdr[3] and 16 != 0 && (ch != 0)).toInt()
            var sfc = gr.value.scalefac_compress shr ist
            var k = (ist * 3) * 4
            while (sfc >= 0) {
                var modprod = 1
                for (i in 3 downTo 0) {
                    scf_size[i] = (sfc / modprod) % g_mod[k + i]
                    modprod *= (g_mod[k + i])
                }
                sfc -= modprod
                k += 4
            }
            scf_partition += k
            scfsi = -16
        }
        L3_read_scalefactors(UByteArrayIntPtr(iscf), ist_pos, ((scf_size)), scf_partition, bs, scfsi)
        if (gr.value.n_short_sfb != 0) {
            val sh: Int = 3 - scf_shift
            for (i in 0 until gr.value.n_short_sfb step 3) {
                iscf[gr.value.n_long_sfb + i + 0] = iscf[gr.value.n_long_sfb + i + 0] + (gr.value.subblock_gain[0] shl sh)
                iscf[gr.value.n_long_sfb + i + 1] = iscf[gr.value.n_long_sfb + i + 1] + (gr.value.subblock_gain[1] shl sh)
                iscf[gr.value.n_long_sfb + i + 2] = iscf[gr.value.n_long_sfb + i + 2] + (gr.value.subblock_gain[2] shl sh)
            }
        } else {
            if (gr.value.preflag != 0) {
                val g_preamp = __STATIC_L3_decode_scalefactors_g_preamp
                for (i in 0 until 10) {
                    iscf[11 + i] = iscf[11 + i] + g_preamp[i]
                }
            }
        }
        val gain_exp = (gr.value.global_gain + -1L * 4L).toInt() - 210 - if (hdr[3] and 224 == 96) 2 else 0
        val gain = L3_ldexp_q2(
            (1 shl ((255L + (-1L * 4L) - 210L + 3L and 3.inv().toLong()) / 4L).toInt()).toFloat(),
            ((255L + (-1L * 4L) - 210L + 3L and (3.inv().toLong())) - (gain_exp.toLong())).toInt()
        )
        for (i in 0 until gr.value.n_long_sfb + gr.value.n_short_sfb) {
            scf[i] = L3_ldexp_q2(gain, iscf[i] shl scf_shift)
        }
    }

    fun L3_pow_43(x: Int): Float {
        var x: Int = x // Mutating parameter
        var mult = 256
        if (x < 129) return g_pow43[16 + x]
        if (x < 1024) {
            mult = 16
            x = x shl 3
        }
        val sign = (2 * x) and 64
        val frac = ((x and 63) - sign).toFloat() / ((x and ((63).inv())) + sign).toFloat()
        return (g_pow43[16 + ((x + sign) shr 6)] * (1f + (frac * ((4f / 3f) + (frac * (2f / 9f)))))) * (mult.toFloat())
    }

    fun L3_huffman(dst: FloatArrayPtr, bs: Bs, gr_info: ArrayPtr<GrInfo>, scf: FloatArrayPtr, layer3gr_limit: Int) {
        var dst: FloatArrayPtr = dst // Mutating parameter
        var scf: FloatArrayPtr = scf // Mutating parameter
        val tabs = __STATIC_L3_huffman_tabs
        val tab32 = __STATIC_L3_huffman_tab32
        val tab33 = __STATIC_L3_huffman_tab33
        val tabindex = __STATIC_L3_huffman_tabindex
        val g_linbits = __STATIC_L3_huffman_g_linbits
        var one = 0f
        var ireg = 0
        var big_val_cnt: Int = gr_info.value.big_values
        var sfb: UByteArrayIntPtr = gr_info.value.sfbtab
        var bs_next_ptr: UByteArrayIntPtr = bs.buf + ((bs.pos / 8))
        var bs_cache: UInt = (((bs_next_ptr[0] * 256 + (bs_next_ptr[1])) * 256 + bs_next_ptr[2]) * 256 + bs_next_ptr[3] shl (bs.pos and 7)).toUInt()
        var pairs_to_decode = 0
        var np = 0
        var bs_sh: Int = (bs.pos and 7) - 8
        bs_next_ptr += 4
        while (big_val_cnt > 0) {
            val tab_num: Int = gr_info.value.table_select[ireg]
            var sfb_cnt: Int = gr_info.value.region_count[ireg++]
            val codebook = tabindex[tab_num].toInt()
            val linbits: Int = g_linbits[tab_num]
            if (linbits != 0) {
                do {
                    np = sfb.value / 2
                    sfb++
                    pairs_to_decode = if (big_val_cnt > np) np else big_val_cnt
                    one = scf++.value
                    do {
                        var w: Int = 5
                        var leaf: Int = tabs[codebook + ((bs_cache shr (32 - w))).toInt()].toInt()
                        while (leaf < 0) {
                            bs_cache = bs_cache shl w
                            bs_sh += w
                            w = leaf and 7
                            leaf = tabs[codebook + (((bs_cache shr (32 - w))).toInt()) - (leaf shr 3)].toInt()
                        }
                        bs_cache = bs_cache shl (leaf shr 8)
                        bs_sh += (leaf shr 8)
                        for (j in 0 until 2) {
                            var lsb: Int = leaf and 15
                            if (lsb == 15) {
                                lsb += (((bs_cache shr (32 - linbits))).toInt())
                                bs_cache = bs_cache shl linbits
                                bs_sh += linbits
                                while (bs_sh >= 0) {
                                    bs_cache = bs_cache or (bs_next_ptr.value shl bs_sh).toUInt()
                                    bs_next_ptr++
                                    bs_sh -= 8
                                }
                                dst.value = (one * L3_pow_43(lsb)) * (((if ((bs_cache.toInt()) < 0) -1L else 1L)).toFloat())
                            } else {
                                dst.value = g_pow43[(16 + lsb) - (16 * (((bs_cache shr 31)).toInt()))] * one
                            }
                            bs_cache = bs_cache shl (if (lsb != 0) 1 else 0)
                            bs_sh += (if (lsb != 0) 1 else 0)
                            dst++
                            leaf = leaf shr 4
                        }
                        while (bs_sh >= 0) {
                            bs_cache = bs_cache or (bs_next_ptr.value shl bs_sh).toUInt()
                            bs_next_ptr++
                            bs_sh -= 8
                        }
                    } while (--pairs_to_decode != 0)
                    big_val_cnt -= np
                } while (big_val_cnt > 0 && (--sfb_cnt >= 0))
            } else {
                do {
                    np = sfb.value / 2
                    sfb++
                    pairs_to_decode = if (big_val_cnt > np) np else big_val_cnt
                    one = scf++.value
                    do {
                        var w = 5
                        var leaf: Int = tabs[codebook + ((bs_cache shr (32 - w))).toInt()].toInt()
                        while (leaf < 0) {
                            bs_cache = bs_cache shl w
                            bs_sh += w
                            w = leaf and 7
                            leaf = tabs[codebook + (((bs_cache shr (32 - w))).toInt()) - (leaf shr 3)].toInt()
                        }
                        bs_cache = bs_cache shl (leaf shr 8)
                        bs_sh += (leaf shr 8)
                        for (j in 0 until 2) {
                            val lsb: Int = leaf and 15
                            dst.value = g_pow43[(16 + lsb) - (16 * (((bs_cache shr 31)).toInt()))] * one
                            bs_cache = bs_cache shl (if (lsb != 0) 1 else 0)
                            bs_sh += (if (lsb != 0) 1 else 0)
                            dst++
                            leaf = leaf shr 4
                        }
                        while (bs_sh >= 0) {
                            bs_cache = bs_cache or (bs_next_ptr.value shl bs_sh).toUInt()
                            bs_next_ptr++
                            bs_sh -= 8
                        }
                    } while (((--pairs_to_decode)) != 0)
                    big_val_cnt -= np
                } while (big_val_cnt > 0 && --sfb_cnt >= 0)
            }
        }
        np = 1 - big_val_cnt
        while (true) {
            val codebook_count1 = if (gr_info.value.count1_table != 0) tab33 else tab32
            var leaf: Int = codebook_count1[((bs_cache shr (32 - 4))).toInt()]
            if ((leaf and 8) == 0) leaf = codebook_count1[(leaf shr 3) + (bs_cache shl 4 shr 32 - (leaf and 3)).toInt()]
            bs_cache = bs_cache shl (leaf and 7)
            bs_sh += (leaf and 7)
            if ((bs_next_ptr.minusPtrUByte(bs.buf)) * 8 - 24 + bs_sh > layer3gr_limit) {
                break
            }
            if ((--np) == 0) {
                np = sfb.value / 2
                sfb++
                if (np == 0) break
                one = scf++.value
            }
            if (leaf and (128 shr 0) != 0) {
                dst[0] = if ((bs_cache.toInt()) < 0) (-one) else one
                bs_cache = bs_cache shl 1
                bs_sh++
            }
            if ((leaf and (128 shr 1)) != 0) {
                dst[1] = if ((bs_cache.toInt()) < 0) (-one) else one
                bs_cache = bs_cache shl 1
                bs_sh++
            }
            if (--np == 0) {
                np = sfb.value / 2
                sfb++
                if (np == 0) break
                one = scf++.value
            }
            if ((leaf and (128 shr 2)) != 0) {
                dst[2] = if (bs_cache.toInt() < 0) -one else one
                bs_cache = bs_cache shl 1
                bs_sh++
            }
            if ((leaf and (128 shr 3)) != 0) {
                dst[3] = if (bs_cache.toInt() < 0) -one else one
                bs_cache = bs_cache shl 1
                bs_sh++
            }
            while (bs_sh >= 0) {
                bs_cache = bs_cache or (bs_next_ptr.value shl bs_sh).toUInt()
                bs_next_ptr++
                bs_sh -= 8
            }
            dst += 4
        }
        bs.pos = layer3gr_limit
    }

    fun L3_midside_stereo(left: FloatArrayPtr, n: Int) {
        val right: FloatArrayPtr = left + 576
        for (i in 0 until n) {
            val a: Float = left[i]
            val b: Float = right[i]
            left[i] = a + b
            right[i] = a - b
        }
    }

    fun L3_intensity_stereo_band(left: FloatArrayPtr, n: Int, kl: Float, kr: Float) {
        for (i in 0 until n) {
            left[i + 576] = left[i] * kr
            left[i] = left[i] * kl
        }
    }

    fun L3_stereo_top_band(right: FloatArrayPtr, sfb: UByteArrayIntPtr, nbands: Int, max_band: IntArray) {
        var right: FloatArrayPtr = right // Mutating parameter
        max_band[0] = -1
        max_band[1] = -1
        max_band[2] = -1
        for (i in 0 until nbands) {
            var k = 0
            while (k < (sfb[i])) {
                if (right[k] != 0f || right[k + 1] != 0f) {
                    max_band[i % 3] = i
                    break
                }
                k += 2
            }
            right += (sfb[i])
        }
    }

    fun L3_stereo_process(left: FloatArrayPtr, ist_pos: UByteArrayInt, sfb: UByteArrayIntPtr, hdr: UByteArrayInt, max_band: IntArray, mpeg2_sh: Int) {
        var left: FloatArrayPtr = left // Mutating parameter
        val g_pan: FloatArray = __STATIC_L3_stereo_process_g_pan
        val max_pos: Int = if (hdr[1] and 8 != 0) 7 else 64
        var i = 0
        while (sfb[i] != 0) {
            val ipos: Int = ist_pos[i]
            if ((i > max_band[i % 3]) && ipos < max_pos) {
                var kl: Float
                var kr: Float
                val s: Float = if ((hdr[3] and 32) != 0) 1.4142135f else 1f
                if (hdr[1] and 8 != 0) {
                    kl = g_pan[2 * ipos + 0]
                    kr = g_pan[2 * ipos + 1]
                } else {
                    kl = 1f
                    kr = L3_ldexp_q2(1f, ipos + 1 shr 1 shl mpeg2_sh)
                    if (ipos and 1 != 0) {
                        kl = kr
                        kr = 1f
                    }
                }
                L3_intensity_stereo_band(left, (sfb[i]), (kl * s), (kr * s))
            } else {
                if (hdr[3] and 32 != 0) {
                    L3_midside_stereo(left, (sfb[i]))
                }
            }
            left += sfb[i]
            i++
        }
    }

    val tempInt3 = IntArray(3)
    fun L3_intensity_stereo(left: FloatArrayPtr, ist_pos: UByteArrayInt, gr: ArrayPtr<GrInfo>, hdr: UByteArrayInt) {
        val max_band = tempInt3
        val n_sfb: Int = gr.value.n_long_sfb + gr.value.n_short_sfb
        val max_blocks: Int = if (gr.value.n_short_sfb != 0) 3 else 1
        L3_stereo_top_band(left + 576, gr.value.sfbtab, n_sfb, max_band)
        if (gr.value.n_long_sfb != 0) {
            val v = max(max(max_band[0], max_band[1]), max_band[2])
            max_band[0] = v
            max_band[1] = v
            max_band[2] = v
        }
        for (i in 0 until max_blocks) {
            val default_pos: Int = if (hdr[1] and 8 != 0) 3 else 0
            val itop: Int = (n_sfb - max_blocks) + i
            val prev: Int = itop - max_blocks
            ist_pos[itop] = if (max_band[i] >= prev) default_pos else ist_pos[prev]
        }
        L3_stereo_process(left, ist_pos, gr.value.sfbtab, hdr, max_band, gr[1].scalefac_compress and 1)
    }

    fun L3_reorder(grbuf: FloatArrayPtr, scratch: FloatArrayPtr, sfb: UByteArrayIntPtr) {
        var sfb: UByteArrayIntPtr = sfb // Mutating parameter
        var i: Int = 0
        var len: Int = 0
        var src: FloatArrayPtr = grbuf
        var dst: FloatArrayPtr = scratch
        while (true) {
            len = sfb.value
            if (len == 0) break
            i = 0
            while (i < len) {
                dst++.value = src[0 * len]
                dst++.value = src[1 * len]
                dst++.value = src[2 * len]
                i++
                src++
            }
            sfb += 3
            src += (2 * len)
        }
        memcpy((grbuf), (scratch), (dst - scratch))
    }

    fun L3_antialias(grbuf: FloatArrayPtr, nbands: Int) {
        var grbuf: FloatArrayPtr = grbuf // Mutating parameter
        var nbands: Int = nbands // Mutating parameter
        val g_aa: Array<FloatArray> = __STATIC_L3_antialias_g_aa
        while (nbands > 0) {
            for (i in 0 until 8) {
                val u: Float = grbuf[18 + i]
                val d: Float = grbuf[17 - i]
                grbuf[18 + i] = (u * g_aa[0][i]) - (d * g_aa[1][i])
                grbuf[17 - i] = (u * g_aa[1][i]) + (d * g_aa[0][i])
            }
            nbands--
            grbuf += 18
        }
    }

    fun L3_dct3_9(y: FloatArray) {
        var s0: Float = y[0]
        var s2: Float = y[2]
        var s4: Float = y[4]
        var s6: Float = y[6]
        var s8: Float = y[8]
        var t0 = s0 + (s6 * 0.5f)
        s0 -= s6
        var t4 = (s4 + s2) * 0.9396926f
        var t2 = (s8 + s2) * 0.76604444f
        s6 = (s4 - s8) * 0.17364818f
        s4 += (s8 - s2)
        s2 = s0 - (s4 * 0.5f)
        y[4] = s4 + s0
        s8 = (t0 - t2) + s6
        s0 = (t0 - t4) + t2
        s4 = (t0 + t4) - s6
        var s1: Float = y[1]
        var s3: Float = y[3]
        var s5: Float = y[5]
        var s7: Float = y[7]
        s3 *= 0.8660254f
        t0 = (s5 + s1) * 0.9848077f
        t4 = (s5 - s7) * 0.34202015f
        t2 = (s1 + s7) * 0.64278764f
        s1 = ((s1 - s5) - s7) * 0.8660254f
        s5 = (t0 - s3) - t2
        s7 = (t4 - s3) - t0
        s3 = (t4 + s3) - t2
        y[0] = s4 - s7
        y[1] = s2 + s1
        y[2] = s0 - s3
        y[3] = s8 + s5
        y[5] = s8 - s5
        y[6] = s0 + s3
        y[7] = s2 - s1
        y[8] = s4 + s7
    }

    private val co = FloatArray(9)
    private val si = FloatArray(9)
    fun L3_imdct36(grbuf: FloatArrayPtr, overlap: FloatArrayPtr, window: FloatArray, nbands: Int) {
        var grbuf: FloatArrayPtr = grbuf // Mutating parameter
        var overlap: FloatArrayPtr = overlap // Mutating parameter
        val g_twid9 = __STATIC_L3_imdct36_g_twid9
        for (j in 0 until nbands) {
            val co = this.co
            val si = this.si
            co[0] = -grbuf[0]
            si[0] = grbuf[17]
            for (i in 0 until 4) {
                si[8 - (2 * i)] = grbuf[(4 * i) + 1] - grbuf[(4 * i) + 2]
                co[1 + (2 * i)] = grbuf[(4 * i) + 1] + grbuf[(4 * i) + 2]
                si[7 - (2 * i)] = grbuf[(4 * i) + 4] - grbuf[(4 * i) + 3]
                co[2 + (2 * i)] = -(grbuf[(4 * i) + 3] + grbuf[(4 * i) + 4])
            }
            L3_dct3_9(co)
            L3_dct3_9(si)
            si[1] = -si[1]
            si[3] = -si[3]
            si[5] = -si[5]
            si[7] = -si[7]
            for (i in 0 until 9) {
                val ovl: Float = overlap[i]
                val sum: Float = (co[i] * g_twid9[9 + i]) + (si[i] * g_twid9[0 + i])
                overlap[i] = (co[i] * g_twid9[0 + i]) - (si[i] * g_twid9[9 + i])
                grbuf[i] = (ovl * window[0 + i]) - (sum * window[9 + i])
                grbuf[17 - i] = (ovl * window[9 + i]) + (sum * window[0 + i])
            }
            grbuf += 18
            overlap += 9
        }
    }

    fun L3_idct3(x0: Float, x1: Float, x2: Float, dst: FloatArray) {
        val m1: Float = x1 * 0.8660254f
        val a1: Float = x0 - (x2 * 0.5f)
        dst[1] = x0 + x2
        dst[0] = a1 + m1
        dst[2] = a1 - m1
    }

    private val temp1F3 = FloatArray(4)
    private val temp2F3 = FloatArray(4)
    fun L3_imdct12(x: FloatArrayPtr, dst: FloatArrayPtr, overlap: FloatArrayPtr) {
        val g_twid3: FloatArray = __STATIC_L3_imdct12_g_twid3
        val co = temp1F3
        val si = temp2F3
        L3_idct3((-x[0]), (x[6] + x[3]), (x[12] + x[9]), co)
        L3_idct3(x[15], (x[12] - x[9]), (x[6] - x[3]), si)
        si[1] = -si[1]
        for (i in 0 until 3) {
            val ovl: Float = overlap[i]
            val sum: Float = (co[i] * g_twid3[3 + i]) + (si[i] * g_twid3[0 + i])
            overlap[i] = (co[i] * g_twid3[0 + i]) - (si[i] * g_twid3[3 + i])
            dst[i] = (ovl * g_twid3[2 - i]) - (sum * g_twid3[5 - i])
            dst[5 - i] = (ovl * g_twid3[5 - i]) + (sum * g_twid3[2 - i])
        }
    }

    fun L3_imdct_short(grbuf: FloatArrayPtr, overlap: FloatArrayPtr, nbands: Int) {
        var grbuf: FloatArrayPtr = grbuf // Mutating parameter
        var overlap: FloatArrayPtr = overlap // Mutating parameter
        var nbands: Int = nbands // Mutating parameter
        while (nbands > 0) {
            val tmp = FloatArrayPtr(FloatArray(18))
            memcpy(tmp, grbuf, 72 / 4)
            memcpy(grbuf, overlap, 6)
            L3_imdct12(tmp, grbuf + 6, overlap + 6)
            L3_imdct12(tmp + 1, grbuf + 12, overlap + 6)
            L3_imdct12(tmp + 2, overlap, overlap + 6)
            nbands--
            overlap += 9
            grbuf += 18
        }
    }

    fun L3_change_sign(grbuf: FloatArrayPtr) {
        var p = 18
        for (b in 0 until 32 step 2) {
            for (i in 1 until 18 step 2) {
                val index = p + i
                grbuf[index] = -grbuf[index]
            }
            p += 36
        }
    }

    fun L3_imdct_gr(grbuf: FloatArrayPtr, overlap: FloatArrayPtr, block_type: Int, n_long_bands: Int) {
        var grbuf: FloatArrayPtr = grbuf // Mutating parameter
        var overlap: FloatArrayPtr = (overlap) // Mutating parameter
        val g_mdct_window: Array<FloatArray> = __STATIC_L3_imdct_gr_g_mdct_window
        if (n_long_bands != 0) {
            L3_imdct36(grbuf, overlap, g_mdct_window[0], n_long_bands)
            grbuf += 18 * n_long_bands
            overlap += 9 * n_long_bands
        }
        if (block_type == 2) {
            L3_imdct_short(grbuf, overlap, 32 - n_long_bands)
        } else {
            L3_imdct36(grbuf, overlap, g_mdct_window[(block_type == 3).toInt()], 32 - n_long_bands)
        }
    }

    fun L3_save_reservoir(h: Mp3Dec, s: Mp3Scratch) {
        var pos: Int = (s.bs.pos + 7) / 8
        var remains: Int = (s.bs.limit / 8) - pos
        if (remains > MAX_BITRESERVOIR_BYTES) {
            pos += (remains - MAX_BITRESERVOIR_BYTES)
            remains = MAX_BITRESERVOIR_BYTES
        }
        if (remains > 0) {
            memcpy(UByteArrayIntPtr(h.reserv_buf), s.maindata + pos, remains)
        }
        h.reserv = remains
    }

    fun L3_restore_reservoir(h: Mp3Dec, bs: Bs, s: Mp3Scratch, main_data_begin: Int): Int {
        val frame_bytes: Int = (bs.limit - bs.pos) / 8
        val bytes_have: Int = if (h.reserv > main_data_begin) main_data_begin else h.reserv
        memcpy(
            s.maindata,
            UByteArrayIntPtr(h.reserv_buf) + if (0 < (h.reserv - main_data_begin)) (h.reserv - main_data_begin) else 0,
            if (h.reserv > main_data_begin) main_data_begin else h.reserv
        )
        memcpy(s.maindata + bytes_have, bs.buf + (bs.pos / 8), frame_bytes)
        bs_init(s.bs, s.maindata, (bytes_have + frame_bytes))
        return ((h.reserv >= main_data_begin)).toInt()
    }

    fun L3_decode(h: Mp3Dec, s: Mp3Scratch, gr_info: ArrayPtr<GrInfo>, nch: Int) {
        var gr_info: ArrayPtr<GrInfo> = gr_info // Mutating parameter
        for (ch in 0 until nch) {
            val layer3gr_limit: Int = s.bs.pos + (gr_info[ch].part_23_length)
            L3_decode_scalefactors(((h.header)), s.ist_pos[ch], s.bs, (gr_info + ch), ((s.scf)), ch)
            L3_huffman(s.grbuf[ch], s.bs, gr_info + ch, ((s.scf)), layer3gr_limit)
        }
        when {
            h.header[3] and 16 != 0 -> L3_intensity_stereo(s.grbuf[0], (s.ist_pos[1]), gr_info, ((h.header)))
            h.header[3] and 224 == 96 -> L3_midside_stereo(s.grbuf[0], 576)
        }
        for (ch in 0 until nch) {
            var aa_bands = 31
            val n_long_bands: Int =
                (if (gr_info.value.mixed_block_flag != 0) 2 else 0) shl ((h.header[2] shr 2 and 3) + ((h.header[1] shr 3 and 1) + (h.header[1] shr 4 and 1)) * 3 == 2).toInt()
            if (gr_info.value.n_short_sfb != 0) {
                aa_bands = n_long_bands - 1
                L3_reorder(s.grbuf[ch] + n_long_bands * 18, s.syn[0], gr_info.value.sfbtab + gr_info.value.n_long_sfb)
            }
            L3_antialias(s.grbuf[ch], aa_bands)
            L3_imdct_gr(s.grbuf[ch], FloatArrayPtr(h.mdct_overlap[ch]), gr_info.value.block_type, n_long_bands)
            L3_change_sign(s.grbuf[ch])
            gr_info += 1
        }
    }

    fun mp3d_DCT_II(grbuf: FloatArrayPtr, n: Int) {
        val g_sec: FloatArray = __STATIC_mp3d_DCT_II_g_sec
        val t = Array(4) { FloatArray(8) }
        for (k in 0 until n) {
            var y: FloatArrayPtr = grbuf + k
            for (i in 0 until 8) {
                val x0: Float = y[i * 18]
                val x1: Float = y[(15 - i) * 18]
                val x2: Float = y[(16 + i) * 18]
                val x3: Float = y[(31 - i) * 18]
                val t0: Float = x0 + x3
                val t1: Float = x1 + x2
                val t2: Float = (x1 - x2) * g_sec[(3 * i) + 0]
                val t3: Float = (x0 - x3) * g_sec[(3 * i) + 1]
                t[0][i] = t0 + t1
                t[1][i] = (t0 - t1) * g_sec[(3 * i) + 2]
                t[2][i] = t3 + t2
                t[3][i] = (t3 - t2) * g_sec[(3 * i) + 2]
            }
            for (i in 0 until 4) {
                val x = t[i]
                var x0: Float = x[0]
                var x1: Float = x[1]
                var x2: Float = x[2]
                var x3: Float = x[3]
                var x4: Float = x[4]
                var x5: Float = x[5]
                var x6: Float = x[6]
                var x7: Float = x[7]
                var xt: Float = x0 - x7
                x0 += x7
                x7 = x1 - x6
                x1 += x6
                x6 = x2 - x5
                x2 += x5
                x5 = x3 - x4
                x3 += x4
                x4 = x0 - x3
                x0 += x3
                x3 = x1 - x2
                x1 += x2
                x[0] = x0 + x1
                x[4] = (x0 - x1) * 0.70710677f
                x5 += x6
                x6 = (x6 + x7) * 0.70710677f
                x7 += xt
                x3 = (x3 + x4) * 0.70710677f
                x5 -= (x7 * 0.19891237f)
                x7 += (x5 * 0.38268343f)
                x5 -= (x7 * 0.19891237f)
                x0 = xt - x6
                xt += x6
                x[1] = (xt + x7) * 0.5097956f
                x[2] = (x4 + x3) * 0.5411961f
                x[3] = (x0 - x5) * 0.6013449f
                x[5] = (x0 + x5) * 0.8999762f
                x[6] = (x4 - x3) * 1.306563f
                x[7] = (xt - x7) * 2.5629156f
            }
            for (i in 0 until 7) {
                y[0 * 18] = t[0][i]
                y[1 * 18] = (t[2][i] + t[3][i]) + t[3][i + 1]
                y[2 * 18] = t[1][i] + t[1][i + 1]
                y[3 * 18] = (t[2][i + 1] + t[3][i]) + t[3][i + 1]
                y += 4 * 18
            }
            y[0 * 18] = t[0][7]
            y[1 * 18] = t[2][7] + t[3][7]
            y[2 * 18] = t[1][7]
            y[3 * 18] = t[3][7]
        }
    }

    fun mp3d_scale_pcm(sample: Float): Short {
        if (sample >= 32766.5f) return 32767
        if (sample <= -32767.5f) return -32768
        val s: Short = (sample + 0.5f).toInt().toShort()
        return (s.toInt() - (s < 0).toInt()).toShort()
    }

    fun mp3d_synth_pair(pcm: ShortArrayPtr, nch: Int, z: FloatArrayPtr) {
        var z: FloatArrayPtr = z // Mutating parameter
        var a = 0f
        a += (z[14 * 64] - z[0]) * 29f
        a += ((z[1 * 64] + z[13 * 64]) * 213f)
        a += ((z[12 * 64] - z[2 * 64]) * 459f)
        a += ((z[3 * 64] + z[11 * 64]) * 2037f)
        a += ((z[10 * 64] - z[4 * 64]) * 5153f)
        a += ((z[5 * 64] + z[9 * 64]) * 6574f)
        a += ((z[8 * 64] - z[6 * 64]) * 37489f)
        a += (z[7 * 64] * 75038f)
        pcm[0] = mp3d_scale_pcm(a)
        z += 2
        a = z[14 * 64] * 104f
        a += (z[12 * 64] * 1567f)
        a += (z[10 * 64] * 9727f)
        a += (z[8 * 64] * 64019f)
        a += (z[6 * 64] * -9975f)
        a += (z[4 * 64] * -45f)
        a += (z[2 * 64] * 146f)
        a += (z[0 * 64] * -5f)
        pcm[16 * nch] = mp3d_scale_pcm(a)
    }

    fun mp3d_synth(xl: FloatArrayPtr, dstl: ShortArrayPtr, nch: Int, lins: FloatArrayPtr) {
        val xr: FloatArrayPtr = xl + ((576 * (nch - 1)))
        val dstr: ShortArrayPtr = dstl + ((nch - 1))
        val g_win: FloatArray = __STATIC_mp3d_synth_g_win
        val zlin: FloatArrayPtr = lins + ((15 * 64))
        var w = 0
        zlin[4 * 15] = xl[18 * 16]
        zlin[(4 * 15) + 1] = xr[18 * 16]
        zlin[(4 * 15) + 2] = xl[0]
        zlin[(4 * 15) + 3] = xr[0]
        zlin[4 * 31] = xl[1 + (18 * 16)]
        zlin[(4 * 31) + 1] = xr[1 + (18 * 16)]
        zlin[(4 * 31) + 2] = xl[1]
        zlin[(4 * 31) + 3] = xr[1]
        mp3d_synth_pair(dstr, nch, ((lins + ((4 * 15))) + 1))
        mp3d_synth_pair((dstr + ((32 * nch))), nch, (((lins + ((4 * 15))) + 64) + 1))
        mp3d_synth_pair(dstl, nch, (lins + ((4 * 15))))
        mp3d_synth_pair((dstl + ((32 * nch))), nch, ((lins + ((4 * 15))) + 64))
        for (i in 14 downTo 0) {
            val a = temp1F3
            val b = temp2F3
            zlin[4 * i] = xl[18 * (31 - i)]
            zlin[(4 * i) + 1] = xr[18 * (31 - i)]
            zlin[(4 * i) + 2] = xl[1 + (18 * (31 - i))]
            zlin[(4 * i) + 3] = xr[1 + (18 * (31 - i))]
            zlin[4 * (i + 16)] = xl[1 + (18 * (1 + i))]
            zlin[(4 * (i + 16)) + 1] = xr[1 + (18 * (1 + i))]
            zlin[(4 * (i - 16)) + 2] = xl[18 * (1 + i)]
            zlin[(4 * (i - 16)) + 3] = xr[18 * (1 + i)]

            for (j in 0 until 4) {
                b[j] = 0f
                a[j] = 0f
            }

            for (n in 0 until 8) {
                val w0: Float = g_win[w++]
                val w1: Float = g_win[w++]
                val vz: FloatArrayPtr = zlin + ((4 * i) - (n * 64))
                val vy: FloatArrayPtr = zlin + ((4 * i) - ((15 - n) * 64))
                if (n % 2 == 0) {
                    for (j in 0 until 4) {
                        b[j] = b[j] + (vz[j] * w1) + (vy[j] * w0)
                        a[j] = a[j] + (vz[j] * w0) - (vy[j] * w1)
                    }
                } else {
                    for (j in 0 until 4) {
                        b[j] = b[j] + ((vz[j] * w1) + (vy[j] * w0))
                        a[j] = a[j] + ((vy[j] * w1) - (vz[j] * w0))
                    }
                }
            }

            dstr[(15 - i) * nch] = mp3d_scale_pcm(a[1])
            dstr[(17 + i) * nch] = mp3d_scale_pcm(b[1])
            dstl[(15 - i) * nch] = mp3d_scale_pcm(a[0])
            dstl[(17 + i) * nch] = mp3d_scale_pcm(b[0])
            dstr[(47 - i) * nch] = mp3d_scale_pcm(a[3])
            dstr[(49 + i) * nch] = mp3d_scale_pcm(b[3])
            dstl[(47 - i) * nch] = mp3d_scale_pcm(a[2])
            dstl[(49 + i) * nch] = mp3d_scale_pcm(b[2])
        }
    }

    fun mp3d_synth_granule(qmf_state: FloatArrayPtr, grbuf: FloatArrayPtr, nbands: Int, nch: Int, pcm: ShortArrayPtr, lins: FloatArrayPtr) {
        var i: Int = 0
        i = 0
        while (i < nch) {
            mp3d_DCT_II((grbuf + ((576 * i))), nbands)
            i++
        }
        memcpy(lins, qmf_state, 15 * 64)
        i = 0
        while (i < nbands) {
            mp3d_synth((grbuf + i), (pcm + ((32 * (nch * i)))), nch, (lins + ((i * 64))))
            i += 2
        }
        if (nch == 1) {
            i = 0
            while (i < (15 * 64)) {
                qmf_state[i] = lins[(nbands * 64) + i]
                i += 2
            }
        } else {
            memcpy(qmf_state, lins + nbands * 64, 15 * 64)
        }
    }

    fun mp3d_match_frame(hdr: UByteArrayIntPtr, mp3_bytes: Int, frame_bytes: Int): Int {
        var i: Int = 0
        for (nmatch in 0 until 10) {
            i += hdr_frame_bytes(hdr + i, frame_bytes) + hdr_padding((hdr + i))
            if ((i + 4) > mp3_bytes) return (nmatch > 0).toInt()
            if (hdr_compare(hdr, (hdr + i)) == 0) return 0
        }
        return 1
    }

    fun mp3d_find_frame(mp3: UByteArrayIntPtr, mp3_bytes: Int, free_format_bytes: IntArray, ptr_frame_bytes: IntArray): Int {
        var mp3: UByteArrayIntPtr = mp3 // Mutating parameter
        var i: Int = 0
        var k: Int = 0
        i = 0
        while (i < mp3_bytes - 4) {
            if (hdr_valid(mp3)) {
                var frame_bytes: Int = hdr_frame_bytes(mp3, free_format_bytes[0])
                var frame_and_padding: Int = frame_bytes + hdr_padding(mp3)
                k = 4
                while (((frame_bytes == 0) && (k < 2304)) && (((i + (2 * (((k < (mp3_bytes - 4))).toInt())))) != 0)) {
                    if (hdr_compare(mp3, (mp3 + k)) != 0) {
                        val fb: Int = k - hdr_padding(mp3)
                        val nextfb: Int = fb + hdr_padding((mp3 + k))
                        if (((((i + k) + nextfb) + 4) > mp3_bytes) || (hdr_compare(mp3, ((mp3 + k) + nextfb)) == 0)) {
                            k += 1
                            continue
                        }
                        frame_and_padding = k
                        frame_bytes = fb
                        free_format_bytes[0] = fb
                    }
                    k++
                }
                if ((((frame_bytes != 0) && (((i + (((frame_and_padding <= mp3_bytes)).toInt()))) != 0)) && (mp3d_match_frame(
                        mp3,
                        (mp3_bytes - i),
                        frame_bytes
                    ) != 0)) || ((i == 0) && (frame_and_padding == mp3_bytes))
                ) {
                    ptr_frame_bytes[0] = frame_and_padding
                    return i
                }
                free_format_bytes[0] = 0
            }
            i++
            mp3 += 1
        }
        ptr_frame_bytes[0] = 0
        return mp3_bytes
    }

    fun mp3dec_init(dec: Mp3Dec) {
        dec.header[0] = 0
    }

    private val scratch = Mp3Scratch()
    private val bs_frame = Bs()
    fun mp3dec_decode_frame(dec: Mp3Dec, mp3: UByteArrayIntPtr, mp3_bytes: Int, pcm: ShortArrayPtr, info: Mp3FrameInfo): Int {
        var pcm: ShortArrayPtr = pcm // Mutating parameter
        var i = 0
        val frame_size = IntArray(1)
        var success = 1
        val bs_frame = this.bs_frame
        val scratch = this.scratch
        if (mp3_bytes > 4 && ((dec.header[0]) == 255) && (hdr_compare(((UByteArrayIntPtr(dec.header))), mp3) != 0)) {
            frame_size[0] = hdr_frame_bytes(mp3, dec.free_format_bytes_array[0]) + hdr_padding(mp3)
            if (frame_size[0] != mp3_bytes && (((frame_size[0] + 4) > mp3_bytes) || (hdr_compare(mp3, (mp3 + frame_size[0])) == 0))) {
                frame_size[0] = 0
            }
        }
        if (frame_size[0] == 0) {
            dec.reserv = 0
            dec.free_format_bytes_array[0] = 0
            dec.reserv_buf.fill(0)
            dec.header.fill(0)
            dec.qmf_state.fill(0f)
            dec.mdct_overlap[0].fill(0f)
            dec.mdct_overlap[1].fill(0f)

            i = mp3d_find_frame(mp3, mp3_bytes, dec.free_format_bytes_array, frame_size)
            if ((frame_size[0] == 0) || (((i + (((frame_size[0] > mp3_bytes)).toInt()))) != 0)) {
                info.value.frame_bytes = i
                return 0
            }
        }
        val hdr = mp3 + i
        memcpy(UByteArrayIntPtr(dec.header), hdr, 4)
        info.value.frame_bytes = i + frame_size[0]
        info.value.frame_offset = i
        info.value.channels = if (hdr[3] and 192 == 192) 1 else 2
        info.value.hz = hdr_sample_rate_hz(hdr)
        info.value.layer = 4 - (hdr[1] shr 1 and 3)
        info.value.bitrate_kbps = hdr_bitrate_kbps(hdr)
        if (pcm.array.isEmpty()) {
            return hdr_frame_samples(hdr)
        }
        bs_init(bs_frame, hdr + 4, frame_size[0] - 4)
        if (hdr[1] and 1 == 0) {
            get_bits(bs_frame, 16)
        }
        if (info.value.layer == 3) {
            val main_data_begin: Int = L3_read_side_info(bs_frame, scratch.gr_info, hdr)
            if (main_data_begin < 0 || bs_frame.pos > bs_frame.limit) {
                mp3dec_init(dec)
                return 0
            }
            success = L3_restore_reservoir(dec, bs_frame, scratch, main_data_begin)
            if (success != 0) {
                var igr = 0
                while (igr < if (hdr[1] and 8 != 0) 2 else 1) {
                    scratch.grbuf[0].fill(0f, 0, 576 * 2)
                    L3_decode(dec, scratch, scratch.gr_info + igr * info.value.channels, info.value.channels)
                    mp3d_synth_granule(FloatArrayPtr(dec.qmf_state), scratch.grbuf[0], 18, info.value.channels, pcm, scratch.syn[0])
                    igr++
                    pcm += 576 * info.value.channels
                }
            }
            L3_save_reservoir(dec, scratch)
        } else {
            val sci = ScaleInfo()
            L12_read_scale_info(hdr, bs_frame, sci)
            scratch.grbuf[0].fill(0f, 0, 576 * 2)
            i = 0
            var igr = 0
            while (igr < 3) {
                i += L12_dequantize_granule(
                    scratch.grbuf[0] + i,
                    (bs_frame),
                    (sci),
                    info.value.layer or 1
                )
                if (12 == i) {
                    i = 0
                    L12_apply_scf_384(sci, FloatArrayPtr(sci.scf) + igr, scratch.grbuf[0])
                    mp3d_synth_granule(FloatArrayPtr(dec.qmf_state), scratch.grbuf[0], 12, info.value.channels, pcm, scratch.syn[0])
                    scratch.grbuf[0].fill(0f, 0, 576 * 2)
                    pcm += 384 * info.value.channels
                }
                if (bs_frame.pos > bs_frame.limit) {
                    mp3dec_init(dec)
                    return 0
                }
                igr++
            }
        }
        return success * (hdr_frame_samples(UByteArrayIntPtr(dec.header)))
    }
}
