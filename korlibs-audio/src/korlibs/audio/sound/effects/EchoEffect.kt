package korlibs.audio.sound.effects

import korlibs.audio.sound.AudioSamples
import korlibs.audio.sound.AudioSamplesProcessor
import korlibs.audio.sound.AudioStreamable
import korlibs.audio.sound.Sound
import korlibs.audio.sound.withProcessor

data class AudioStreamEchoConfig(
    var volume: Double = 1.0
)

suspend fun AudioStreamable.withEcho(bufferLen: Int = 2048, config: AudioStreamEchoConfig = AudioStreamEchoConfig()): Sound {
    val buffer2 = AudioSamples(2, bufferLen)
    val buffer = AudioSamples(2, bufferLen)
    val processor = AudioSamplesProcessor(2, bufferLen)
    return withProcessor { inp, outp ->
        val len = inp.read(buffer)

        processor.reset()
        processor.add(buffer)
        processor.add(buffer2, config.volume.toFloat())
        buffer2.setTo(buffer)
        processor.normalize()
        processor.copyTo(buffer)

        outp.write(buffer, 0, len)
    }
}
