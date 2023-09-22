package korlibs.audio.sound.backends

import korlibs.audio.sound.*
import korlibs.datastructure.thread.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

val alsaNativeSoundProvider: ALSANativeSoundProvider? by lazy {
    try {
        ALSANativeSoundProvider()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

class ALSANativeSoundProvider : NativeSoundProvider() {
    init {
        //println("ALSANativeSoundProvider.init")
    }
    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        //println("ALSANativeSoundProvider.createPlatformAudioOutput(freq=$freq)")
        return ALSAPlatformAudioOutput(this, coroutineContext, freq)
    }
}

class ALSAPlatformAudioOutput(
    val soundProvider: ALSANativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int,
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    var nativeThread: NativeThread? = null
    var running = false

    var pcm: Long = 0L

    override suspend fun wait() {
        running = false
        //println("WAITING")
        val time = measureTime {
            while (pcm != 0L) {
                delay(10.milliseconds)
            }
        }
        //println("WAITED: time=$time")
        //super.wait()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun start() {
        if (running) return
        running = true
        nativeThread = NativeThread {

            pcm = ASound2.snd_pcm_open("default", ASound2.SND_PCM_STREAM_PLAYBACK, 0)
            if (pcm == 0L) {
                error("Can't initialize ALSA")
                running = false
                return@NativeThread
            }

            val latency = 50_000
            ASound2.snd_pcm_set_params(
                pcm,
                ASound2.SND_PCM_FORMAT_S16_LE,
                ASound2.SND_PCM_ACCESS_RW_INTERLEAVED,
                nchannels,
                frequency,
                1,
                latency
            )

            val temp = AudioSamplesInterleaved(nchannels, 1024)
            try {
                while (running) {
                    val readCount = readShortsInterleaved(temp)

                    if (readCount == 0) {
                        blockingSleep(1.milliseconds)
                        continue
                    }

                    //println("readCount=$readCount")
                    var offset = 0
                    var pending = readCount
                    while (pending > 0) {
                        val written = ASound2.snd_pcm_writei(pcm, temp.data, offset * nchannels, pending * nchannels, pending)
                        //println("offset=$offset, pending=$pending, written=$written")
                        if (written == -ASound2.EPIPE) {
                            //println("ALSA: EPIPE error")
                            ASound2.snd_pcm_prepare(pcm)
                            offset = 0
                            pending = readCount
                            continue
                            //blockingSleep(1.milliseconds)
                        } else if (written < 0) {
                            println("ALSA: OTHER error: $written")
                            blockingSleep(1.milliseconds)
                            break
                        } else {
                            offset += written
                            pending -= written
                        }
                    }
                }
            } finally {
                //println("!!COMPLETED : pcm=$pcm")
                if (pcm != 0L) {
                    ASound2.snd_pcm_wait(pcm, 1000)
                    ASound2.snd_pcm_drain(pcm)
                    ASound2.snd_pcm_close(pcm)
                    pcm = 0L
                    //println("!!CLOSED = $pcm")
                }
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        super.stop()
    }
}

expect object ASoundImpl : ASound2

interface ASound2 {
    val initialized: Boolean get() = false

    fun snd_pcm_open(name: String, stream: Int, mode: Int): Long = 0L
    fun snd_pcm_set_params(pcm: Long, format: Int, acess: Int, channels: Int, rate: Int, soft_resample: Int, latency: Int): Int = ERROR
    fun snd_pcm_name(pcm: Long): String = ""
    fun snd_pcm_state(pcm: Long): Int = ERROR
    fun snd_pcm_state_name(state: Int): String = ""
    fun snd_pcm_writei(pcm: Long, buffer: ShortArray, offset: Int, size: Int, nframes: Int): Int = ERROR
    fun snd_pcm_prepare(pcm: Long): Int = ERROR
    fun snd_pcm_recover(pcm: Long, err: Int, silent: Int): Int = ERROR
    fun snd_pcm_drain(pcm: Long): Int = ERROR
    fun snd_pcm_wait(pcm: Long, timeout: Int): Int = ERROR
    fun snd_pcm_drop(pcm: Long): Int = ERROR
    fun snd_pcm_delay(pcm: Long): Int = ERROR
    fun snd_pcm_close(pcm: Long): Int = ERROR

    companion object : ASound2 by ASoundImpl {
        const val ERROR = -1

        const val EPIPE = 32	// Broken pipe
        const val EBADFD = 77	// File descriptor in bad state
        const val ESTRPIPE = 86	// Streams pipe error

        const val SND_PCM_STREAM_PLAYBACK = 0
        const val SND_PCM_STREAM_CAPTURE = 1

        const val SND_PCM_ACCESS_MMAP_INTERLEAVED = 0 // mmap access with simple interleaved channels
        const val SND_PCM_ACCESS_MMAP_NONINTERLEAVED = 1 // mmap access with simple non interleaved channels
        const val SND_PCM_ACCESS_MMAP_COMPLEX = 2 // mmap access with complex placement
        const val SND_PCM_ACCESS_RW_INTERLEAVED = 3 // snd_pcm_readi/snd_pcm_writei access
        const val SND_PCM_ACCESS_RW_NONINTERLEAVED = 4 // /snd_pcm_writen access

        const val SND_PCM_FORMAT_S16_LE = 2

        const val SND_PCM_STATE_OPEN = 0 // Open
        const val SND_PCM_STATE_SETUP = 1 // Setup installed
        const val SND_PCM_STATE_PREPARED = 2 // Ready to start
        const val SND_PCM_STATE_RUNNING = 3 // Running
        const val SND_PCM_STATE_XRUN = 4 // Stopped: underrun (playback) or overrun (capture) detected
        const val SND_PCM_STATE_DRAINING = 5 // Draining: running (playback) or stopped (capture)
        const val SND_PCM_STATE_PAUSED = 6 // Paused
        const val SND_PCM_STATE_SUSPENDED = 7 // Hardware is suspended
        const val SND_PCM_STATE_DISCONNECTED = 8 // Hardware is disconnected
    }
}
