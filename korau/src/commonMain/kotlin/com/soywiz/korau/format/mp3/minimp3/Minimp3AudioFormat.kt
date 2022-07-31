@file:JvmName("Minimp3AudioFormatKt")

package com.soywiz.korau.format.mp3.minimp3

import kotlin.jvm.JvmName

internal object Minimp3AudioFormat : BaseMinimp3AudioFormat() {
    override fun createMp3Decoder(): BaseMp3Decoder = Minimp3Decoder()

    internal class Minimp3Decoder : MiniMp3Program(), BaseMp3Decoder {
        private val pcmData = ShortArray(1152 * 2 * 2 * 2)
        private val mp3dec = Mp3Dec()
        private val mp3decFrameInfo = Mp3FrameInfo()

        init {
            mp3dec_init(mp3dec)
        }

        override val info: BaseMp3DecoderInfo = BaseMp3DecoderInfo()

        override fun decodeFrame(availablePeek: Int): ShortArray? {
            val samples = mp3dec_decode_frame(
                mp3dec,
                UByteArrayPtr(info.tempBuffer.asUByteArray()),
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
