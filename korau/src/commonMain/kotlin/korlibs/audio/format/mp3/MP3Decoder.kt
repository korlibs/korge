package korlibs.audio.format.mp3

import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.AudioFormat
import korlibs.audio.format.mp3.minimp3.Minimp3AudioFormat
import korlibs.audio.sound.AudioStream
import korlibs.io.stream.AsyncStream

open class MP3Decoder() : AudioFormat("mp3") {
    companion object : MP3Decoder()

    //internal val format = JavaMp3AudioFormat
    internal val format = Minimp3AudioFormat
    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = format.tryReadInfo(data, props)
    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? = format.decodeStream(data, props)
    override fun toString(): String = "NativeMp3DecoderFormat"
}