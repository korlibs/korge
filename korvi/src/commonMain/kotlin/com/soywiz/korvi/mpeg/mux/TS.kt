@file:Suppress("unused", "UNUSED_VARIABLE", "MemberVisibilityCanBePrivate", "LocalVariableName", "SpellCheckingInspection")

package com.soywiz.korvi.mpeg.mux

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.fastArrayListOf
import com.soywiz.klogger.Console
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.subarray
import com.soywiz.korvi.mpeg.util.BitBuffer
import com.soywiz.korvi.mpeg.stream.DecoderBase

class TS : Demuxer {
    lateinit var bits: BitBuffer
    var leftoverBytes: Uint8Buffer? = null
    var guessVideoFrameEnd = true
    var pidsToStreamIds = LinkedHashMap<Int, Int>()
    var pesPacketInfo = LinkedHashMap<Int, PesPacketInfo>()
    override var startTime = 0.0
    override var currentTime = 0.0

    class PesPacketInfo(
        var destination: DecoderBase<*>,
        var currentLength: Int,
        var totalLength: Int,
        var pts: Double,
        var buffers: FastArrayList<Uint8Buffer>,
    )

    override fun connect(streamId: Int, destination: DecoderBase<*>) {
        this.pesPacketInfo[streamId] = PesPacketInfo(
            destination = destination,
            currentLength = 0,
            totalLength = 0,
            pts = 0.0,
            buffers = FastArrayList()
        )
    }

    val Uint8Buffer.byteLength: Int get() = size

    override fun write(buffer: Uint8Buffer) {
        if (this.leftoverBytes != null) {
            val totalLength = buffer.byteLength + this.leftoverBytes!!.byteLength
            this.bits = BitBuffer(totalLength)
            this.bits.write(fastArrayListOf(this.leftoverBytes!!, buffer))
        }
        else {
            this.bits = BitBuffer(buffer)
        }

        while (this.bits.has(188 shl 3) && this.parsePacket()) Unit

        val leftoverCount = this.bits.byteLength - (this.bits.index ushr 3)
        this.leftoverBytes = if (leftoverCount > 0) this.bits.bytes.subarray(this.bits.index ushr 3) else null
    }

    var parsePacketCount = 0
    fun parsePacket(): Boolean {
        println("parsePacket ${parsePacketCount++}")
        // Check if we're in sync with packet boundaries; attempt to resync if not.
        if (this.bits.read(8) != 0x47) {
            if (!this.resync()) {
                // Couldn't resync; maybe next time...
                return false
            }
        }

        val end = (this.bits.index ushr 3) + 187
        var transportError = this.bits.read(1)
        val payloadStart = this.bits.read(1)
        var transportPriority = this.bits.read(1)
        val pid = this.bits.read(13)
        var transportScrambling = this.bits.read(2)
        val adaptationField = this.bits.read(2)
        var continuityCounter = this.bits.read(4)


        // If this is the start of a new payload; signal the end of the previous
        // frame, if we didn't do so already.
        var streamId = this.pidsToStreamIds[pid]
        if (payloadStart != 0 && streamId != null && streamId != 0) {
            val pi = this.pesPacketInfo[streamId]
            if (pi != null && pi.currentLength != 0) {
                this.packetComplete(pi)
            }
        }

        // Extract current payload
        if ((adaptationField and 0x1) != 0) {
            if ((adaptationField and 0x2) != 0) {
                val adaptationFieldLength = this.bits.read(8)
                this.bits.skip(adaptationFieldLength shl 3)
            }

            if (payloadStart != 0 && this.bits.nextBytesAreStartCode()) {
                this.bits.skip(24)
                streamId = this.bits.read(8)
                this.pidsToStreamIds[pid] = streamId

                val packetLength = this.bits.read(16)
                this.bits.skip(8)
                val ptsDtsFlag = this.bits.read(2)
                this.bits.skip(6)
                val headerLength = this.bits.read(8)
                val payloadBeginIndex = this.bits.index + (headerLength shl 3)

                val pi = this.pesPacketInfo[streamId]
                if (pi != null) {
                    var pts = 0.0
                    if ((ptsDtsFlag and 0x2) != 0) {
                        // The Presentation Timestamp is encoded as 33(!) bit
                        // integer, but has a "marker bit" inserted at weird places
                        // in between, making the whole thing 5 bytes in size.
                        // You can't make this shit up...
                        this.bits.skip(4)
                        val p32_30 = this.bits.read(3)
                        this.bits.skip(1)
                        val p29_15 = this.bits.read(15)
                        this.bits.skip(1)
                        val p14_0 = this.bits.read(15)
                        this.bits.skip(1)

                        // Can't use bit shifts here; we need 33 bits of precision,
                        // so we're using JavaScript's double number type. Also
                        // divide by the 90khz clock to get the pts in seconds.
                        pts = (p32_30 * 1073741824 + p29_15 * 32768 + p14_0)/90000.0

                        this.currentTime = pts
                        if (this.startTime == -1.0) {
                            this.startTime = pts
                        }
                    }

                    val payloadLength = if (packetLength != 0) packetLength - headerLength - 3 else 0
                    this.packetStart(pi, pts, payloadLength)
                }

                // Skip the rest of the header without parsing it
                this.bits.index = payloadBeginIndex
            }

            if (streamId != 0) {
                // Attempt to detect if the PES packet is complete. For Audio (and
                // other) packets, we received a total packet length with the PES
                // header, so we can check the current length.

                // For Video packets, we have to guess the end by detecting if this
                // TS packet was padded - there's no good reason to pad a TS packet
                // in between, but it might just fit exactly. If this fails, we can
                // only wait for the next PES header for that stream.

                val pi = this.pesPacketInfo[streamId]
                if (pi != null) {
                    val start = this.bits.index ushr 3

                    val complete = if (end < this.bits.byteLength) this.packetAddData(pi, start, end) else false

                    val hasPadding = payloadStart == 0 && (adaptationField and 0x2) != 0
                    if (complete || (this.guessVideoFrameEnd && hasPadding)) {
                        this.packetComplete(pi)
                    }
                }
            }
        }

        this.bits.index = end shl 3
        return true
    }

    fun resync(): Boolean {
        // Check if we have enough data to attempt a resync. We need 5 full packets.
        if (!this.bits.has((188 * 6) shl 3)) {
            return false
        }

        val byteIndex = this.bits.index ushr 3

        // Look for the first sync token in the first 187 bytes
        for (i in 0 until 187) {
            if (this.bits.bytes[byteIndex + i] == 0x47) {

                // Look for 4 more sync tokens, each 188 bytes appart
                var foundSync = true
                for (j in 1 until 5) {
                    if (this.bits.bytes[byteIndex + i + 188 * j] != 0x47) {
                        foundSync = false
                        break
                    }
                }

                if (foundSync) {
                    this.bits.index = (byteIndex + i + 1) shl 3
                    return true
                }
            }
        }

        // In theory, we shouldn't arrive here. If we do, we had enough data but
        // still didn't find sync - this can only happen if we were fed garbage
        // data. Check your source!
        Console.warn("JSMpeg: Possible garbage data. Skipping.")
        this.bits.skip(187 shl 3)
        return false
    }

    fun packetStart(pi: PesPacketInfo, pts: Double, payloadLength: Int) {
        pi.totalLength = payloadLength
        pi.currentLength = 0
        pi.pts = pts
    }

    fun packetAddData(pi: PesPacketInfo, start: Int, end: Int): Boolean {
        pi.buffers.add(this.bits.bytes.subarray(start, end))
        pi.currentLength += end - start

        return (pi.totalLength != 0 && pi.currentLength >= pi.totalLength) // complete
    }

    fun packetComplete(pi: PesPacketInfo) {
        pi.destination.write(pi.pts, pi.buffers)
        pi.totalLength = 0
        pi.currentLength = 0
        pi.buffers = FastArrayList()
    }

    object STREAM {
        const val PACK_HEADER = 0xBA
        const val SYSTEM_HEADER = 0xBB
        const val PROGRAM_MAP = 0xBC
        const val PRIVATE_1 = 0xBD
        const val PADDING = 0xBE
        const val PRIVATE_2 = 0xBF
        const val AUDIO_1 = 0xC0
        const val VIDEO_1 = 0xE0
        const val DIRECTORY = 0xFF
    }
}
