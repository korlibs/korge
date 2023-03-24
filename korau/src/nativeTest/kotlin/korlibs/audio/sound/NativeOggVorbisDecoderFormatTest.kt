package korlibs.audio.sound

/*
class NativeOggVorbisDecoderFormatTest {
    @Test
    fun test() = suspendTest {
        val decoder = NativeOggVorbisDecoderFormat
        val file = resourcesVfs["ogg1.ogg"]
        val stream = decoder.decodeStream(file.open()) ?: error("Can't open decoder")
        val audioData = stream.toData()

        //localCurrentDirVfs["demo.vorbis.raw"].write(MemorySyncStreamToByteArray { writeShortArrayLE(audioData.samples) })

        assertEquals(1, audioData.channels)
        assertEquals(22050, audioData.totalSamples)
        assertEquals(44100, audioData.rate)
        assertEquals(500, audioData.totalTime.millisecondsInt)
    }
}
 */
