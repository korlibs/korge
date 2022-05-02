package com.soywiz.korau.format.mp3

import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.mp3.javamp3.JavaMp3AudioFormat
import com.soywiz.korau.format.mp3.minimp3.Minimp3AudioFormat
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korio.stream.AsyncStream

open class MP3Decoder() : AudioFormat("mp3") {
    companion object : MP3Decoder()

    //internal val format = JavaMp3AudioFormat
    internal val format = Minimp3AudioFormat
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = format.tryReadInfo(data, props)
    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? = format.decodeStream(data, props)
    override fun toString(): String = "NativeMp3DecoderFormat"
}
