package com.soywiz.korau.sound.impl.jna

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.internal.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.*
import java.lang.RuntimeException
import java.nio.*
import kotlin.coroutines.*
import kotlin.math.*

class OpenALException(message: String) : RuntimeException(message)

class JnaOpenALNativeSoundProvider : NativeSoundProvider() {
    companion object {
        val MAX_AVAILABLE_SOURCES = 100
    }

    val device = (AL.alcOpenDevice(null) ?: throw OpenALException("Can't open OpenAL device")).also { device ->
        Runtime.getRuntime().addShutdownHook(Thread {
            AL.alcCloseDevice(device)
        })
    }
    val context = (AL.alcCreateContext(device, null) ?: throw OpenALException("Can't get OpenAL context")).also { context ->
        Runtime.getRuntime().addShutdownHook(Thread {
            //alc?.alcDestroyContext(context) // Crashes on mac!
        })
    }

    val sourcePool = Pool {
        alGenSourceAndInitialize()
            //.also { println("CREATED OpenAL source $it") }
    }
    val bufferPool = Pool {
        AL.alGenBuffer()
            //.also { println("CREATED OpenAL buffer $it") }
    }

    fun makeCurrent() {
        AL.alcMakeContextCurrent(context)
    }

    init {
        makeCurrent()

        AL.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
        checkAlErrors("alListener3f", 0)
        AL.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
        checkAlErrors("alListener3f", 0)
        AL.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f))
        checkAlErrors("alListenerfv", 0)
    }

    override val audioFormats = nativeAudioFormats

    override suspend fun createNonStreamingSound(data: AudioData, name: String): Sound {
        if (!AL.loaded) return super.createNonStreamingSound(data, name)
        return OpenALSoundNoStream(this, coroutineContext, data, name = name)
    }

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        if (!AL.loaded) return super.createAudioStream(coroutineContext, freq)
        return OpenALPlatformAudioOutput(this, coroutineContext, freq)
    }
}

class OpenALPlatformAudioOutput(
    val provider: JnaOpenALNativeSoundProvider,
    coroutineContext: CoroutineContext,
    freq: Int,
) : PlatformAudioOutput(coroutineContext, freq) {
    var source = 0
    val sourceProv = JnaSoundPropsProvider { source }
    override var availableSamples: Int = 0

    override var pitch: Double by sourceProv::pitch
    override var volume: Double by sourceProv::volume
    override var panning: Double by sourceProv::panning

    //val source

    //alSourceQueueBuffers

    //val buffersPool = Pool(6) { all.alGenBuffer() }
    //val buffers = IntArray(32)
    //val buffers = IntArray(6)

    init {
        start()
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        //println("OpenALPlatformAudioOutput.add")
        availableSamples += samples.totalSamples
        try {
            provider.makeCurrent()
            val tempBuffers = IntArray(1)
            ensureSource()
            while (true) {
                //val buffer = al.alGetSourcei(source, AL.AL_BUFFER)
                //val sampleOffset = al.alGetSourcei(source, AL.AL_SAMPLE_OFFSET)
                val processed = AL.alGetSourcei(source, AL.AL_BUFFERS_PROCESSED)
                val queued = AL.alGetSourcei(source, AL.AL_BUFFERS_QUEUED)
                val total = processed + queued
                val state = AL.alGetSourceState(source)
                val playing = state == AL.AL_PLAYING

                //println("buffer=$buffer, processed=$processed, queued=$queued, state=$state, playing=$playing, sampleOffset=$sampleOffset")
                //println("Samples.add")

                if (processed <= 0 && total >= 6) {
                    delay(10.milliseconds)
                    continue
                }

                if (total < 6) {
                    AL.alGenBuffers(1, tempBuffers)
                    checkAlErrors("alGenBuffers", tempBuffers[0])
                    //println("alGenBuffers: ${tempBuffers[0]}")
                } else {
                    AL.alSourceUnqueueBuffers(source, 1, tempBuffers)
                    checkAlErrors("alSourceUnqueueBuffers", source)
                    //println("alSourceUnqueueBuffers: ${tempBuffers[0]}")
                }
                //println("samples: $samples - $offset, $size")
                //al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning, volume)
                AL.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning)
                checkAlErrors("alBufferData", tempBuffers[0])
                AL.alSourceQueueBuffers(source, 1, tempBuffers)
                checkAlErrors("alSourceQueueBuffers", tempBuffers[0])

                //val gain = al.alGetSourcef(source, AL.AL_GAIN)
                //val pitch = al.alGetSourcef(source, AL.AL_PITCH)
                //println("gain=$gain, pitch=$pitch")
                if (!playing) {
                    AL.alSourcePlay(source)
                }
                break
            }
        } finally {
            availableSamples -= samples.totalSamples
        }
    }

    fun ensureSource() {
        if (source != 0) return
        provider.makeCurrent()

        source = alGenSourceAndInitialize()
        //al.alGenBuffers(buffers.size, buffers)
    }

    override fun start() {
        ensureSource()
        AL.alSourcePlay(source)
        checkAlErrors("alSourcePlay", source)
        //checkAlErrors()
    }

    //override fun pause() {
    //    al.alSourcePause(source)
    //}

    override fun stop() {
        provider.makeCurrent()

        AL.alSourceStop(source)
        if (source != 0) {
            AL.alDeleteSource(source)
            source = 0
        }
        //for (n in buffers.indices) {
        //    if (buffers[n] != 0) {
        //        al.alDeleteBuffer(buffers[n])
        //        buffers[n] = 0
        //    }
        //}
    }
}

private class MyStopwatch {
    private var running = false
    private var ns = 0L
    private val now get() = System.nanoTime()

    fun resume() {
        if (running) return
        toggle()
    }

    fun pause() {
        if (!running) return
        toggle()
    }

    fun toggle() {
        running = !running
        ns = now - ns
    }

    val elapsedNanoseconds: Long get() = if (running) now - ns else ns
}

// https://ffainelli.github.io/openal-example/
class OpenALSoundNoStream(
    val provider: JnaOpenALNativeSoundProvider,
    coroutineContext: CoroutineContext,
    val data: AudioData?,
    override val name: String = "Unknown"
) : Sound(coroutineContext), SoundProps {
    override suspend fun decode(): AudioData = data ?: AudioData.DUMMY

    override var volume: Double = 1.0
    override var pitch: Double = 1.0
    override var panning: Double = 0.0

    override val length: TimeSpan get() = data?.totalTime ?: 0.seconds
    override val nchannels: Int get() = data?.channels ?: 1

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val data = data ?: return DummySoundChannel(this)
        //println("provider.sourcePool.totalItemsInUse=${provider.sourcePool.totalItemsInUse}, provider.sourcePool.totalAllocatedItems=${provider.sourcePool.totalAllocatedItems}, provider.sourcePool.itemsInPool=${provider.sourcePool.itemsInPool}")
        if (provider.sourcePool.totalItemsInUse >= JnaOpenALNativeSoundProvider.MAX_AVAILABLE_SOURCES) {
            error("OpenAL too many sources in use")
        }
        provider.makeCurrent()
        var buffer = provider.bufferPool.alloc()
        var source = provider.sourcePool.alloc()
        if (source == -1) Console.warn("UNEXPECTED[0] source=-1")

        AL.alBufferData(buffer, data, panning, volume)

        AL.alSourcei(source, AL.AL_BUFFER, buffer)
        checkAlErrors("alSourcei", source)

        var stopped = false

        val sourceProvider: () -> Int = { source }

        val channel = object : SoundChannel(this), SoundProps by JnaSoundPropsProvider(sourceProvider) {
            private val stopWatch = MyStopwatch()
            val totalSamples get() = data.totalSamples
            var currentSampleOffset: Int
                get() {
                    if (source < 0) return 0
                    return AL.alGetSourcei(source, AL.AL_SAMPLE_OFFSET)
                }
                set(value) {
                    if (source < 0) return
                    AL.alSourcei(source, AL.AL_SAMPLE_OFFSET, value)
                }

            val estimatedTotalNanoseconds: Long
                get() = total.nanoseconds.toLong()
            val estimatedCurrentNanoseconds: Long
                get() = stopWatch.elapsedNanoseconds

            override var current: TimeSpan
                get() = data.timeAtSample(currentSampleOffset)
                set(value) {
                    if (source < 0) return
                    AL.alSourcef(source, AL.AL_SEC_OFFSET, value.seconds.toFloat())
                }
            override val total: TimeSpan get() = data.totalTime

            override val state: SoundChannelState get() {
                if (source < 0) return SoundChannelState.STOPPED
                val result = AL.alGetSourceState(source)
                checkAlErrors("alGetSourceState", source)
                return when (result) {
                    AL.AL_INITIAL -> SoundChannelState.INITIAL
                    AL.AL_PLAYING -> SoundChannelState.PLAYING
                    AL.AL_PAUSED -> SoundChannelState.PAUSED
                    AL.AL_STOPPED -> SoundChannelState.STOPPED
                    else -> SoundChannelState.STOPPED
                }
            }

            override fun stop() {
                if (stopped) return
                stopped = true
                if (source == -1) Console.warn("UNEXPECTED[1] source=-1")
                AL.alSourceStop(source)
                AL.alSourcei(source, AL.AL_BUFFER, 0)
                provider.sourcePool.free(source)
                provider.bufferPool.free(buffer)
                source = -1
                buffer = -1
                stopWatch.pause()
                // We reuse them from the pool
                //AL.alDeleteSource(source)
                //AL.alDeleteBuffer(buffer)
            }

            override fun pause() {
                AL.alSourcePause(source)
                stopWatch.pause()
            }

            override fun resume() {
                AL.alSourcePlay(source)
                stopWatch.resume()
            }
        }.also {
            it.copySoundPropsFromCombined(this@OpenALSoundNoStream, params)
        }
        launchImmediately(coroutineContext[ContinuationInterceptor] ?: coroutineContext) {
            var times = params.times
            var startTime = params.startTime
            try {
                while (times.hasMore) {
                    times = times.oneLess
                    channel.reset()
                    AL.alSourcef(source, AL.AL_SEC_OFFSET, startTime.seconds.toFloat())
                    channel.resume()
                    //checkAlErrors("alSourcePlay")
                    startTime = 0.seconds
                    while (channel.playingOrPaused) delay(10L)
                }
            } catch (e: CancellationException) {
                params.onCancel?.invoke()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                channel.stop()
                params.onFinish?.invoke()
            }
        }
        return channel
    }
}

class JnaSoundPropsProvider(val sourceProvider: () -> Int) : SoundProps {
    val source get() = sourceProvider()

    private val temp1 = FloatArray(3)
    private val temp2 = FloatArray(3)
    private val temp3 = FloatArray(3)

    override var pitch: Double
        get() = if (source < 0) 1.0 else AL.alGetSourcef(source, AL.AL_PITCH).toDouble()
        set(value) {
            if (source < 0) return
            AL.alSourcef(source, AL.AL_PITCH, value.toFloat())
        }
    override var volume: Double
        get() = if (source < 0) 1.0 else AL.alGetSourcef(source, AL.AL_GAIN).toDouble()
        set(value) {
            if (source < 0) return
            AL.alSourcef(source, AL.AL_GAIN, value.toFloat())
        }
    override var panning: Double
        get() {
            if (source < 0) return 0.0
            AL.alGetSource3f(source, AL.AL_POSITION, temp1, temp2, temp3)
            return temp1[0].toDouble()
        }
        set(value) {
            if (source < 0) return
            val pan = value.toFloat()
            AL.alSourcef(source, AL.AL_ROLLOFF_FACTOR, 0.0f)
            AL.alSourcei(source, AL.AL_SOURCE_RELATIVE, 1)
            AL.alSource3f(source, AL.AL_POSITION, pan, 0f, -sqrt(1.0f - pan * pan))
            //println("SET PANNING: source=$source, pan=$pan")
        }
}

private fun AL.alBufferData(buffer: Int, data: AudioSamples, freq: Int, panning: Double = 0.0, volume: Double = 1.0) {
    alBufferData(buffer, AudioData(freq, data), panning, volume)
}

private fun applyStereoPanningInline(interleaved: ShortArray, panning: Double = 0.0, volume: Double = 1.0) {
    if (panning == 0.0 || volume != 1.0) return
    val vvolume = volume.clamp01()
    val rratio = (((panning + 1.0) / 2.0).clamp01() * vvolume).toFloat()
    val lratio = ((1.0 - rratio) * vvolume).toFloat()
    //println("panning=$panning, lratio=$lratio, rratio=$rratio, vvolume=$vvolume")
    for (n in interleaved.indices step 2) {
        interleaved[n + 0] = (interleaved[n + 0] * lratio).coerceToShort()
        interleaved[n + 1] = (interleaved[n + 1] * rratio).coerceToShort()
    }
}

private fun AL.alBufferData(buffer: Int, data: AudioData, panning: Double = 0.0, volume: Double = 1.0) {
    val samples = data.samplesInterleaved.data
    if (data.stereo && panning != 0.0) applyStereoPanningInline(samples, panning, volume)
    val bufferData = ShortBuffer.wrap(samples)
    val format = if (data.stereo) AL.AL_FORMAT_STEREO16 else AL.AL_FORMAT_MONO16
    val samplesData = if (samples.isNotEmpty()) bufferData else null
    val bytesSize = samples.size * 2
    val rate = data.rate
    AL.alBufferData(buffer, format, samplesData, bytesSize, rate)
    checkAlErrors("alBufferData", buffer)
}

private fun alGenSourceAndInitialize() = AL.alGenSource().also { source ->
    AL.alSourcef(source, AL.AL_PITCH, 1f)
    AL.alSourcef(source, AL.AL_GAIN, 1f)
    AL.alSource3f(source, AL.AL_POSITION, 0f, 0f, 0f)
    AL.alSource3f(source, AL.AL_VELOCITY, 0f, 0f, 0f)
    AL.alSourcei(source, AL.AL_LOOPING, AL.AL_FALSE)
    AL.alSourceStop(source)
}

fun ALerrorToString(value: Int): String = when (value) {
    AL.AL_INVALID_NAME -> "AL_INVALID_NAME"
    AL.AL_INVALID_ENUM -> "AL_INVALID_ENUM"
    AL.AL_INVALID_VALUE -> "AL_INVALID_VALUE"
    AL.AL_INVALID_OPERATION -> "AL_INVALID_OPERATION"
    AL.AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY"
    else -> "UNKNOWN"
}

//fun checkAlErrors(name: String, value: Int = -1) {
fun checkAlErrors(name: String, value: Int) {
    //AL.alGetError().also { error -> if (error != AL.AL_NO_ERROR) Console.error("OpenAL error ${error.shex} (${ALerrorToString(error)}) '$name' (value=$value)") }
}
