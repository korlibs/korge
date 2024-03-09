package korlibs.audio.sound.backend

import korlibs.audio.sound.*
import korlibs.datastructure.thread.*
import korlibs.memory.*
import javax.sound.sampled.*
import kotlin.coroutines.*

private val mixer by lazy { AudioSystem.getMixer(null) }

object AwtNativeSoundProvider : NativeSoundProviderNew() {
    override fun createNewPlatformAudioOutput(
        coroutineContext: CoroutineContext,
        nchannels: Int,
        freq: Int,
        gen: (AudioSamplesInterleaved) -> Unit
    ): NewPlatformAudioOutput {
        return JvmNewPlatformAudioOutput(this, coroutineContext, nchannels, freq, gen)
    }
}

class JvmNewPlatformAudioOutput(
    val provider: AwtNativeSoundProvider,
    coroutineContext: CoroutineContext,
    nchannels: Int,
    freq: Int,
    gen: (AudioSamplesInterleaved) -> Unit
) : NewPlatformAudioOutput(coroutineContext, nchannels, freq, gen) {
    var nativeThread: NativeThread? = null

    val BYTES_PER_SAMPLE = nchannels * Short.SIZE_BYTES

    private fun bytesToSamples(bytes: Int): Int = bytes / BYTES_PER_SAMPLE
    private fun samplesToBytes(samples: Int): Int = samples * BYTES_PER_SAMPLE

    override fun internalStart() {
        //println("TRYING TO START")
        if (nativeThread?.threadSuggestRunning == true) return

        //println("STARTED")

        // SAMPLE -> Short, FRAME -> nchannels * SAMPLE
        nativeThread = nativeThread(isDaemon = true) {
            it.threadSuggestRunning = true
            val nchannels = this.channels
            val format = AudioFormat(frequency.toFloat(), Short.SIZE_BITS, nchannels, true, false)
            //val format = AudioFormat(44100.toFloat(), Short.SIZE_BITS, nchannels, true, false)
            //val line = AudioSystem.getSourceDataLine(format)
            val line = (mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine)
            line.open()
            line.start()
            try {
                val info = AudioSamplesInterleaved(nchannels, 1024)
                val bytes = ByteArray(samplesToBytes(1024))
                while (it.threadSuggestRunning) {
                    if (paused) {
                        Thread.sleep(10L)
                    } else {
                        genSafe(info)
                        bytes.setArrayLE(0, info.data)
                        //println(bytes.count { it == 0.toByte() })
                        line.write(bytes, 0, bytes.size)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                line.drain()
                line.stop()
                line.close()
            }
        }
    }

    override fun internalStop() {
        nativeThread?.threadSuggestRunning = false
        nativeThread = null
        //println("STOPPING")
    }
}
