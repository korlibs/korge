package com.soywiz.korau.sound.backends

import com.soywiz.kds.lock.*
import com.soywiz.kds.thread.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import kotlinx.cinterop.*
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
    val arena = Arena()
    val channels = 2
    var pcm: COpaquePointer? = null
    private val lock = Lock()
    val sdeque = AudioSamplesDeque(channels)
    var running = true
    var thread: NativeThread? = null

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

        arena.clear()
        val cmpPtr: CPointer<CPointerVarOf<CPointer<out CPointed>>> = arena.allocArray<COpaquePointerVar>(16)
        val params: CPointer<CPointed> = arena.allocArray<COpaquePointerVar>(16).reinterpret<CPointed>()
        val temp: CPointer<IntVarOf<Int>> = arena.allocArray<IntVar>(16)

        //cmpPtr.clear()
        //cmpPtr.setLong(0L, 0L)
        //println("ALSANativeSoundProvider.snd_pcm_open")
        ASound2.snd_pcm_open(cmpPtr, "default".cstr.placeTo(arena), ASound2.SND_PCM_STREAM_PLAYBACK, 0).also { if (it != 0) error("Can't initialize ALSA") }
        pcm = cmpPtr[0]
        //println("ALSANativeSoundProvider.snd_pcm_open: pcm=$pcm")
        ASound2.snd_pcm_hw_params_any(pcm, params)
        ASound2.snd_pcm_hw_params_set_access(pcm, params, ASound2.SND_PCM_ACCESS_RW_INTERLEAVED).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_access=$it") }
        ASound2.snd_pcm_hw_params_set_format(pcm, params, ASound2.SND_PCM_FORMAT_S16_LE).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_format=$it") }
        ASound2.snd_pcm_hw_params_set_channels(pcm, params, channels).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_channels=$it") }
        ASound2.snd_pcm_hw_params_set_rate(pcm, params, frequency, +1).also { if (it != 0) error("Error calling snd_pcm_hw_params_set_rate=$it") }
        ASound2.snd_pcm_hw_params(pcm, params).also { if (it != 0) error("Error calling snd_pcm_hw_params=$it") }

        //println(ASound2.snd_pcm_name(pcm))
        //println(ASound2.snd_pcm_state_name(ASound2.snd_pcm_state(pcm)))
        ASound2.snd_pcm_hw_params_get_channels(params, temp).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_channels=$it") }
        val cchannels = temp[0]
        ASound2.snd_pcm_hw_params_get_rate(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_rate=$it") }
        val crate = temp[0]
        ASound2.snd_pcm_hw_params_get_period_size(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        val frames = temp[0]
        //println("cchannels: $cchannels, rate=$crate, frames=$frames")
        ASound2.snd_pcm_hw_params_get_period_time(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        //val random = Random(0L)
        //println("ALSANativeSoundProvider: Before starting Sound thread!")
        thread = NativeThread {
            //println("ALSANativeSoundProvider: Started Sound thread!")
            memScoped {
                val buff = allocArray<ShortVar>(frames * channels)
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

        ASound2.snd_pcm_drain(pcm)
        ASound2.snd_pcm_close(pcm)
        arena.clear()
    }
}

internal object ASound2 : DynamicLibrary("libasound.so.2") {
    inline val initialized: Boolean get() = isAvailable
    val snd_pcm_open by func<(pcmPtr: COpaquePointer?, name: CPointer<ByteVar>, stream: Int, mode: Int) -> Int>()
    val snd_pcm_hw_params_any by func<(pcm: COpaquePointer?, params: COpaquePointer) -> Int>()
    val snd_pcm_hw_params_set_access by func<(pcm: COpaquePointer?, params: COpaquePointer, access: Int) -> Int>()
    val snd_pcm_hw_params_set_format by func<(pcm: COpaquePointer?, params: COpaquePointer, format: Int) -> Int>()
    val snd_pcm_hw_params_set_channels by func<(pcm: COpaquePointer?, params: COpaquePointer, channels: Int) -> Int>()
    val snd_pcm_hw_params_set_rate by func<(pcm: COpaquePointer?, params: COpaquePointer, rate: Int, dir: Int) -> Int>()
    val snd_pcm_hw_params by func<(pcm: COpaquePointer?, params: COpaquePointer) -> Int>()
    val snd_pcm_name by func<(pcm: COpaquePointer?) -> CPointer<ByteVar>>()
    val snd_pcm_state by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_state_name by func<(state: Int) -> CPointer<ByteVar>>()
    val snd_pcm_hw_params_get_channels by func<(params: COpaquePointer, out: COpaquePointer) -> Int>()
    val snd_pcm_hw_params_get_rate by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_get_period_size by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_get_period_time by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_writei by func<(pcm: COpaquePointer?, buffer: COpaquePointer, size: Int) -> Int>()
    val snd_pcm_prepare by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_drain by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_close by func<(pcm: COpaquePointer?) -> Int>()

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
