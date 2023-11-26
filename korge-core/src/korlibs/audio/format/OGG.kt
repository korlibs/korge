package korlibs.audio.format

import korlibs.time.microseconds
import korlibs.memory.extract
import korlibs.io.annotations.Keep
import korlibs.io.lang.invalidOp
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readS64LE
import korlibs.io.stream.readS8
import korlibs.io.stream.readStream
import korlibs.io.stream.readString
import korlibs.io.stream.readU8
import kotlin.coroutines.cancellation.CancellationException

@Keep
open class OGG : OggBase() {
    companion object : OGG()
}

open class OggBase : AudioFormat("ogg") {
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = try {
        parse(data)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        //e.printStackTrace()
        null
    }

    suspend fun parse(s: AsyncStream): Info {
        var channels = 2
        var sampleRate = 44100
        var brnom = 160000
        while (s.hasAvailable()) {
            val magic = s.readString(5)
            if (magic != "OggS\u0000") invalidAudioFormat("Not an OGG file")
            val type = s.readS8()
            val cont = type.extract(0);
            val bos = type.extract(1);
            val eos = type.extract(2)
            val gpos = s.readS64LE()
            val sn = s.readS32LE()
            val psn = s.readS32LE()
            val chk = s.readS32LE()
            val pseg = s.readU8()
            val psizs = (0 until pseg).map { s.readU8() }
            val pages = psizs.map { s.readStream(it) }
            if (bos) {
                val info = pages[0]
                val packetType = info.readU8()
                if (packetType > 3) invalidOp("Unsupported OGG vorbis file")
                if (info.readString(6) != "vorbis") invalidOp("Unsupported OGG vorbis file")
                when (packetType) {
                    PacketTypes.ID_HEADER -> {
                        val vver = info.readS32LE()
                        channels = info.readU8()
                        sampleRate = info.readS32LE()
                        val brmax = info.readS32LE()
                        brnom = info.readS32LE()
                        val brmin = info.readS32LE()
                        val bsinfo = info.readU8()
                    }
                    PacketTypes.COMMENT_HEADER -> Unit
                    PacketTypes.SETUP_HEADER -> Unit
                }
            }
            if (eos) return Info(
                duration = ((gpos.toDouble() * 1_000_000.0 / sampleRate.toDouble())).toLong().microseconds,
                channels = channels
            )
        }
        invalidOp("Cannot parse stream")
    }

    object PacketTypes {
        const val ID_HEADER = 1 // 4.2.2. Identification header
        const val COMMENT_HEADER = 3
        const val SETUP_HEADER = 5
    }
}
