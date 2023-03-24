package korlibs.audio.sound.backends

import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.memory.dyn.*
import korlibs.audio.sound.*
import korlibs.io.async.*
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
) : PlatformAudioOutput(coroutineContext, frequency) {
    val channels = 2
    private val lock = Lock()
    val sdeque = AudioSamplesDeque(channels)
    var running = true
    var thread: NativeThread? = null
    var pcm: Long = 0L

    init {
        start()
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        if (!ASound2.initialized) return super.add(samples, offset, size)

        while (running && lock { sdeque.availableRead > 4 * 1024 }) {
            delay(10.milliseconds)
        }
        lock { sdeque.write(samples, offset, size) }
    }

    override fun start() {
        sdeque.clear()
        running = true

        if (!ASound2.initialized) return

        //cmpPtr.clear()
        //cmpPtr.setLong(0L, 0L)
        //println("ALSANativeSoundProvider.snd_pcm_open")
        pcm = ASound2.snd_pcm_open("default", ASound2.SND_PCM_STREAM_PLAYBACK, 0)

        if (pcm == 0L) {
            println("Can't initialize ALSA")
            return
        }

        //println("ALSANativeSoundProvider.snd_pcm_open: pcm=$pcm")
        val params = ASound2.alloc_params()
        ASound2.snd_pcm_hw_params_any(pcm, params)
        ASound2.snd_pcm_hw_params_set_access(pcm, params, ASound2.SND_PCM_ACCESS_RW_INTERLEAVED).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_access=$it") }
        ASound2.snd_pcm_hw_params_set_format(pcm, params, ASound2.SND_PCM_FORMAT_S16_LE).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_format=$it") }
        ASound2.snd_pcm_hw_params_set_channels(pcm, params, channels).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_channels=$it") }
        ASound2.snd_pcm_hw_params_set_rate(pcm, params, frequency, +1).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_rate=$it") }
        ASound2.snd_pcm_hw_params(pcm, params).also { if (it != 0) error("Error calling snd_pcm_hw_params=$it") }
        //println(ASound2.snd_pcm_name(pcm))
        //println(ASound2.snd_pcm_state_name(ASound2.snd_pcm_state(pcm)))
        //val cchannels = ASound2.snd_pcm_hw_params_get_channels(params)
        //val crate = ASound2.snd_pcm_hw_params_get_rate(params)
        val frames = ASound2.snd_pcm_hw_params_get_period_size(params)
        ASound2.free_params(params)
        //val ptime = ASound2.snd_pcm_hw_params_get_period_time(params)
        //val random = Random(0L)
        //println("ALSANativeSoundProvider: Before starting Sound thread!")
        thread = NativeThread {
            //println("ALSANativeSoundProvider: Started Sound thread!")
            val buff = ShortArray(frames * channels)
            val samples = AudioSamplesInterleaved(channels, frames)
            mainLoop@ while (running) {
                while (lock { sdeque.availableRead < frames }) {
                    if (!running) break@mainLoop
                    blockingSleep(1.milliseconds)
                }
                val readCount = lock { sdeque.read(samples, 0, frames) }
                //println("ALSANativeSoundProvider: readCount=$readCount")
                val panning = this@ALSAPlatformAudioOutput.panning.toFloat()
                //val panning = -1f
                //val panning = +0f
                //val panning = +1f
                val volume = this@ALSAPlatformAudioOutput.volume.toFloat().clamp01()
                for (ch in 0 until channels) {
                    val pan = (if (ch == 0) -panning else +panning) + 1f
                    val npan = pan.clamp01()
                    val rscale: Float = npan * volume
                    //println("panning=$panning, volume=$volume, pan=$pan, npan=$npan, rscale=$rscale")
                    for (n in 0 until readCount) {
                        buff[n * channels + ch] = (samples[ch, n] * rscale).toInt().toShort()
                    }
                }
                val result = ASound2.snd_pcm_writei(pcm, buff, frames)
                //println("result=$result")
                if (result == -ASound2.EPIPE) {
                    ASound2.snd_pcm_prepare(pcm)
                }
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        thread?.interrupt()
        if (!ASound2.initialized) return

        if (pcm != null) {
            ASound2.snd_pcm_drain(pcm)
            ASound2.snd_pcm_close(pcm)
        }
    }
}

expect object ASoundImpl : ASound2

interface ASound2 {
    val initialized: Boolean get() = false

    fun alloc_params(): Long = 0L
    fun free_params(value: Long) = Unit

    fun snd_pcm_open(name: String, stream: Int, mode: Int): Long = 0L
    fun snd_pcm_hw_params_any(pcm: Long, params: Long): Int = ERROR
    fun snd_pcm_hw_params_set_access(pcm: Long, params: Long, access: Int): Int = ERROR
    fun snd_pcm_hw_params_set_format(pcm: Long, params: Long, format: Int): Int = ERROR
    fun snd_pcm_hw_params_set_channels(pcm: Long, params: Long, channels: Int): Int = ERROR
    fun snd_pcm_hw_params_set_rate(pcm: Long, params: Long, rate: Int, dir: Int): Int = ERROR
    fun snd_pcm_hw_params(pcm: Long, params: Long): Int = ERROR
    fun snd_pcm_name(pcm: Long): String = ""
    fun snd_pcm_state(pcm: Long): Int = ERROR
    fun snd_pcm_state_name(state: Int): String = ""
    //fun snd_pcm_hw_params_get_channels(params: Long): Int = ERROR
    //fun snd_pcm_hw_params_get_rate(params: Long, dir: Long): Int = ERROR
    fun snd_pcm_hw_params_get_period_size(params: Long): Int = ERROR
    //fun snd_pcm_hw_params_get_period_time(params: Long, dir: Long): Int = ERROR
    fun snd_pcm_writei(pcm: Long, buffer: ShortArray, size: Int): Int = ERROR
    fun snd_pcm_prepare(pcm: Long): Int = ERROR
    fun snd_pcm_drain(pcm: Long): Int = ERROR
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