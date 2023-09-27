package korlibs.audio.sound

interface AudioStreamable {
    suspend fun toStream(): AudioStream
}
