@file:JvmName("Minimp3AudioFormatKt")

package com.soywiz.korau.format.mp3.minimp3

import com.soywiz.kds.ByteArrayDeque
import com.soywiz.kds.ShortArrayDeque
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
import kotlin.jvm.JvmName

internal object Minimp3AudioFormat : BaseMinimp3AudioFormat() {
    override fun createMp3Decoder(): BaseMp3Decoder = Minimp3Decoder()

    internal class Minimp3Decoder : MiniMp3Program(512 * 1024), BaseMp3Decoder {
        private fun <R> CPointer<*>.reinterpret(): CPointer<R> = CPointer(this.ptr)

        private val inputData = alloca(1152 * 2 * 2).reinterpret<Byte>()
        private val pcmData = alloca(1152 * 2 * 2 * 2).reinterpret<Short>()
        private val mp3dec = alloca(mp3dec_t__SIZE_BYTES).reinterpret<mp3dec_t>()
        private val mp3decFrameInfo = alloca(mp3dec_frame_info_t__SIZE_BYTES).reinterpret<mp3dec_frame_info_t>()

        init {
            mp3dec_init(mp3dec)
        }

        override val info: BaseMp3DecoderInfo = BaseMp3DecoderInfo()

        override fun decodeFrame(availablePeek: Int): ShortArray? {
            memWrite(inputData, info.tempBuffer, 0, availablePeek)
            val samples = mp3dec_decode_frame(
                mp3dec,
                inputData.reinterpret<UByte>().plus(0),
                availablePeek,
                pcmData,
                mp3decFrameInfo
            )
            val struct = mp3dec_frame_info_t(mp3decFrameInfo.ptr)
            info.nchannels = struct.channels
            info.hz = struct.hz
            info.bitrate_kbps = struct.bitrate_kbps
            info.frame_bytes = struct.frame_bytes
            info.samples = samples
            //println("samples=$samples, hz=$hz, nchannels=$nchannels, bitrate_kbps=$bitrate_kbps, frameBytes=$frame_bytes")

            if (info.nchannels == 0 || samples <= 0) return null

            val buf = ShortArray(samples * info.nchannels)
            memRead(pcmData, buf, 0, samples * info.nchannels)
            return buf
        }

        override fun close() {
            free(inputData)
            free(pcmData)
            free(mp3dec)
            free(mp3decFrameInfo)
        }
    }
}
