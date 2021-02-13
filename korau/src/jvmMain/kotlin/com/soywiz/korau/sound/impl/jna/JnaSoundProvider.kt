package com.soywiz.korau.sound.impl.jna

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.internal.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.nio.*
import kotlin.coroutines.*
import kotlin.math.*

class JnaOpenALNativeSoundProvider : NativeSoundProvider() {
    val device = (AL.alcOpenDevice(null) ?: error("Can't open OpenAL device")).also { device ->
        Runtime.getRuntime().addShutdownHook(Thread {
            AL.alcCloseDevice(device)
        })
    }
    val context = (AL.alcCreateContext(device, null) ?: error("Can't get OpenAL context")).also { context ->
        Runtime.getRuntime().addShutdownHook(Thread {
            //alc?.alcDestroyContext(context) // Crashes on mac!
        })
    }

    fun makeCurrent() {
        AL.alcMakeContextCurrent(context)
    }

    init {
        makeCurrent()

        AL.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
        checkAlErrors("alListener3f")
        AL.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
        checkAlErrors("alListener3f")
        AL.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f))
        checkAlErrors("alListenerfv")
    }

    override val audioFormats = nativeAudioFormats

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound {
        if (!AL.loaded || streaming) return super.createSound(data, streaming, props, name)
        return OpenALSoundNoStream(this, coroutineContext, audioFormats.decode(data, props), name = name)
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
    val sourceProvider: SourceProvider = SourceProvider(0)
) : PlatformAudioOutput(coroutineContext, freq) {
    val sourceProv = JnaSoundPropsProvider(sourceProvider)
    override var availableSamples: Int = 0

    override var pitch: Double by sourceProv::pitch.redirected()
    override var volume: Double by sourceProv::volume.redirected()
    override var panning: Double by sourceProv::panning.redirected()

    var source: Int
        get() = sourceProvider.source
        set(value) = run { sourceProvider.source = value }

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
                    checkAlErrors("alGenBuffers")
                    //println("alGenBuffers: ${tempBuffers[0]}")
                } else {
                    AL.alSourceUnqueueBuffers(source, 1, tempBuffers)
                    checkAlErrors("alSourceUnqueueBuffers")
                    //println("alSourceUnqueueBuffers: ${tempBuffers[0]}")
                }
                //println("samples: $samples - $offset, $size")
                //al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning, volume)
                AL.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning)
                AL.alSourceQueueBuffers(source, 1, tempBuffers)
                checkAlErrors("alSourceQueueBuffers")

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
        checkAlErrors("alSourcePlay")
        //checkAlErrors()
    }

    //override fun pause() {
    //    al.alSourcePause(source)
    //}

    override fun stop() {
        dispose()
    }

    // @TODO: Leaking buffers?
    override fun dispose() {
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

// https://ffainelli.github.io/openal-example/
class OpenALSoundNoStream(
    val provider: JnaOpenALNativeSoundProvider, coroutineContext: CoroutineContext,
    val data: AudioData?, val sourceProvider: SourceProvider = SourceProvider(0),
    override val name: String = "Unknown"
) : Sound(coroutineContext), SoundProps by JnaSoundPropsProvider(sourceProvider) {
    override suspend fun decode(): AudioData = data ?: AudioData.DUMMY

    var source: Int
        get() = sourceProvider.source
        set(value) = run { sourceProvider.source = value }

    override val length: TimeSpan get() = data?.totalTime ?: 0.seconds

    override val nchannels: Int get() = data?.channels ?: 1

    override fun play(params: PlaybackParameters): SoundChannel {
        val data = data ?: return DummySoundChannel(this)
        provider.makeCurrent()
        val buffer = AL.alGenBuffer()
        AL.alBufferData(buffer, data, panning, volume)

        source = alGenSourceAndInitialize()
        AL.alSourcei(source, AL.AL_BUFFER, buffer)
        checkAlErrors("alSourcei")

        var stopped = false

        val channel = object : SoundChannel(this), SoundProps by JnaSoundPropsProvider(sourceProvider) {
            val totalSamples get() = data.totalSamples
            var currentSampleOffset: Int
                get() = AL.alGetSourcei(source, AL.AL_SAMPLE_OFFSET)
                set(value) {
                    AL.alSourcei(source, AL.AL_SAMPLE_OFFSET, value)
                }

            override var current: TimeSpan
                get() = data.timeAtSample(currentSampleOffset)
                set(value) { AL.alSourcef(source, AL.AL_SEC_OFFSET, value.seconds.toFloat())  }
            override val total: TimeSpan get() = data.totalTime

            override val state: SoundChannelState get() {
                val result = AL.alGetSourceState(source)
                checkAlErrors("alGetSourceState")
                return when (result) {
                    AL.AL_INITIAL -> SoundChannelState.INITIAL
                    AL.AL_PLAYING -> SoundChannelState.PLAYING
                    AL.AL_PAUSED -> SoundChannelState.PAUSED
                    AL.AL_STOPPED -> SoundChannelState.STOPPED
                    else -> SoundChannelState.STOPPED
                }
            }

            override fun stop() {
                if (!stopped) {
                    stopped = true
                    AL.alDeleteSource(source)
                    AL.alDeleteBuffer(buffer)
                }
            }

            override fun pause() {
                AL.alSourcePause(source)
            }

            override fun resume() {
                AL.alSourcePlay(source)
            }
        }.also {
            it.copySoundPropsFrom(params)
        }
        launchImmediately(coroutineContext[ContinuationInterceptor] ?: coroutineContext) {
            var times = params.times
            var startTime = params.startTime
            try {
                while (times.hasMore) {
                    times = times.oneLess
                    channel.reset()
                    AL.alSourcef(source, AL.AL_SEC_OFFSET, startTime.seconds.toFloat())
                    AL.alSourcePlay(source)
                    //checkAlErrors("alSourcePlay")
                    startTime = 0.seconds
                    while (channel.playingOrPaused) delay(1L)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                channel.stop()
            }
        }
        return channel
    }
}

data class SourceProvider(var source: Int)

class JnaSoundPropsProvider(val sourceProvider: SourceProvider) : SoundProps {
    val source get() = sourceProvider.source

    private val temp1 = FloatArray(3)
    private val temp2 = FloatArray(3)
    private val temp3 = FloatArray(3)

    override var pitch: Double
        get() = AL.alGetSourcef(source, AL.AL_PITCH).toDouble()
        set(value) { AL.alSourcef(source, AL.AL_PITCH, value.toFloat()) }
    override var volume: Double
        get() = AL.alGetSourcef(source, AL.AL_GAIN).toDouble()
        set(value) { AL.alSourcef(source, AL.AL_GAIN, value.toFloat()) }
    override var panning: Double
        get() {
            AL.alGetSource3f(source, AL.AL_POSITION, temp1, temp2, temp3)
            return temp1[0].toDouble()
        }
        set(value) {
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
    //val bufferData = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
    //bufferData.asShortBuffer().put(samples)

    AL.alBufferData(
        buffer,
        if (data.stereo) AL.AL_FORMAT_STEREO16 else AL.AL_FORMAT_MONO16,
        if (samples.isNotEmpty()) bufferData else null,
        samples.size * 2,
        data.rate
    )
    checkAlErrors("alBufferData")
}

private fun alGenSourceAndInitialize() = AL.alGenSource().also { source ->
    AL.alSourcef(source, AL.AL_PITCH, 1f)
    AL.alSourcef(source, AL.AL_GAIN, 1f)
    AL.alSource3f(source, AL.AL_POSITION, 0f, 0f, 0f)
    AL.alSource3f(source, AL.AL_VELOCITY, 0f, 0f, 0f)
    AL.alSourcei(source, AL.AL_LOOPING, AL.AL_FALSE)
}

fun checkAlErrors(name: String) {
    //val error = al.alGetError()
    //if (error != AL.AL_NO_ERROR) error("OpenAL error ${error.shex} '$name'")
}
