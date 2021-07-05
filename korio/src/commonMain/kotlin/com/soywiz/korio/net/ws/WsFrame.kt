package com.soywiz.korio.net.ws

import com.soywiz.kmem.extract
import com.soywiz.korio.stream.*
import kotlin.random.Random

open class WsFrame(val data: ByteArray, val type: WsOpcode, val isFinal: Boolean = true, val masked: Boolean = true) {
    override fun toString(): String = "WsFrame(data=#${data.size}, type=$type, isFinal=$isFinal, masked=$masked)"

    fun toByteArray(random: Random = Random): ByteArray = MemorySyncStreamToByteArray {
        val sizeMask = (if (masked) 0x80 else 0x00)

        write8(type.id or (if (isFinal) 0x80 else 0x00))

        when {
            data.size < 126 -> write8(data.size or sizeMask)
            data.size < 65536 -> {
                write8(126 or sizeMask)
                write16BE(data.size)
            }
            else -> {
                write8(127 or sizeMask)
                write32BE(0)
                write32BE(data.size)
            }
        }

        if (masked) {
            val mask = random.nextBytes(4)
            writeBytes(mask)
            writeBytes(applyMask(data, mask))
        } else {
            writeBytes(data)
        }
    }

    companion object {
        fun applyMask(payload: ByteArray, mask: ByteArray?): ByteArray {
            if (mask == null) return payload
            val maskedPayload = ByteArray(payload.size)
            for (n in 0 until payload.size) maskedPayload[n] = (payload[n].toInt() xor mask[n % mask.size].toInt()).toByte()
            return maskedPayload
        }

        suspend fun readWsFrame(s: AsyncInputStream): WsFrame = readWsFrameOrNull(s) ?: error("End of stream")

        suspend fun readWsFrameOrNull(s: AsyncInputStream): WsFrame? {
            val b0 = s.read()
            if (b0 < 0) return null
            val b1 = s.readU8()

            val isFinal = b0.extract(7)
            val opcode = WsOpcode(b0.extract(0, 4))

            val partialLength = b1.extract(0, 7)
            val isMasked = b1.extract(7)

            val length = when (partialLength) {
                126 -> s.readU16BE()
                127 -> {
                    val hi = s.readS32BE()
                    if (hi != 0) error("message too long > 2**32")
                    s.readS32BE()
                }
                else -> partialLength
            }
            val mask = if (isMasked) s.readBytesExact(4) else null
            val unmaskedData = s.readBytesExact(length)
            val finalData = WsFrame.applyMask(unmaskedData, mask)
            return WsFrame(finalData, opcode, isFinal, isMasked)
        }

    }
}
