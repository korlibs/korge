package com.soywiz.korau.sound

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.WAV
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.util.redirected
import kotlinx.cinterop.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

val openalNativeSoundProvider: OpenALNativeSoundProvider? by lazy {
    try {
        OpenALNativeSoundProvider()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}
actual val nativeSoundProvider: NativeSoundProvider get() = openalNativeSoundProvider ?: DummyNativeSoundProvider

class OpenALNativeSoundProvider : NativeSoundProvider() {
    val device = AL.alcOpenDevice(null)
    //val device: CPointer<ALCdevice>? = null
    val context = device?.let { AL.alcCreateContext(it, null).also {
        AL.alcMakeContextCurrent(it)
        memScoped {
            AL.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
            AL.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
            val listenerOri = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)
            listenerOri.usePinned {
                AL.alListenerfv(AL.AL_ORIENTATION, it.addressOf(0))
            }
        }
    } }

    internal fun makeCurrent() {
        AL.alcMakeContextCurrent(context)
    }

    override val audioFormats: AudioFormats = AudioFormats(WAV, com.soywiz.korau.format.mp3.MP3Decoder, NativeOggVorbisDecoderFormat)

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound {
        return if (streaming) {
            super.createSound(data, streaming, props, name)
        } else {
            OpenALNativeSoundNoStream(this, coroutineContext, audioFormats.decode(data, props), name = name)
        }
    }

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        return OpenALPlatformAudioOutput(this, coroutineContext, freq)
    }
}

class OpenALPlatformAudioOutput(
    val provider: OpenALNativeSoundProvider,
    coroutineContext: CoroutineContext,
    freq: Int,
    val sourceProvider: SourceProvider = SourceProvider(0.convert())
) : PlatformAudioOutput(coroutineContext, freq) {
    val sourceProv = JnaSoundPropsProvider(sourceProvider)
    override var availableSamples: Int = 0

    override var pitch: Double by sourceProv::pitch.redirected()
    override var volume: Double by sourceProv::volume.redirected()
    override var panning: Double by sourceProv::panning.redirected()

    var source: ALuint
        get() = sourceProvider.source
        set(value) { sourceProvider.source = value }

    //val source

    //alSourceQueueBuffers

    //val buffersPool = Pool(6) { all.alGenBuffer() }
    //val buffers = IntArray(32)
    //val nbuffers = 6
    //val buffers = IntArray(nbuffers)

    init {
        start()
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        availableSamples += samples.totalSamples
        try {
            memScoped {
                provider.makeCurrent()
                var tempBuffers = alloc<ALuintVar>()
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
                        tempBuffers.value = AL.alGenBuffer()
                        AL.checkAlErrors("alGenBuffers")
                        //println("alGenBuffers: ${tempBuffers[0]}")
                    } else {
                        AL.alSourceUnqueueBuffers(source, 1, tempBuffers.ptr)
                        AL.checkAlErrors("alSourceUnqueueBuffers")
                        //println("alSourceUnqueueBuffers: ${tempBuffers[0]}")
                    }
                    //println("samples: $samples - $offset, $size")
                    //al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning, volume)
                    AL.alBufferData(tempBuffers.value, samples.copyOfRange(offset, offset + size), frequency, panning)
                    AL.alSourceQueueBuffers(source, 1, tempBuffers.ptr)
                    AL.checkAlErrors("alSourceQueueBuffers")

                    //val gain = al.alGetSourcef(source, AL.AL_GAIN)
                    //val pitch = al.alGetSourcef(source, AL.AL_PITCH)
                    //println("gain=$gain, pitch=$pitch")
                    if (!playing) {
                        AL.alSourcePlay(source)
                    }
                    break
                }
            }
        } finally {
            availableSamples -= samples.totalSamples
        }
    }

    fun ensureSource() {
        if (source.toInt() != 0) return
        provider.makeCurrent()

        source = AL.alGenSource()
        //for (n in buffers.indices) buffers[n] = alGenBuffer() .toInt()
    }

    override fun start() {
        ensureSource()
        AL.alSourcePlay(source)
        AL.checkAlErrors("alSourcePlay")
        //checkAlErrors()
    }

    override fun stop() {
        provider.makeCurrent()

        AL.alSourceStop(source)
        if (source.toInt() != 0) {
            AL.alDeleteSource(source)
            source = 0.convert()
        }
        //for (n in buffers.indices) {
        //    if (buffers[n] != 0) {
        //        alDeleteBuffer(buffers[n])
        //        buffers[n] = 0
        //    }
        //}
    }
}

// https://ffainelli.github.io/openal-example/
class OpenALNativeSoundNoStream(
    val provider: OpenALNativeSoundProvider,
    coroutineContext: CoroutineContext,
    val data: AudioData?,
    val sourceProvider: SourceProvider = SourceProvider(0.convert()),
    override val name: String = "Unknown"
) : Sound(coroutineContext), SoundProps by JnaSoundPropsProvider(sourceProvider) {
    override suspend fun decode(maxSamples: Int): AudioData = data ?: AudioData.DUMMY

    var source: ALuint
        get() = sourceProvider.source
        set(value) { sourceProvider.source = value }

    override val length: TimeSpan get() = data?.totalTime ?: 0.seconds

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val data = data ?: return DummySoundChannel(this)
        provider.makeCurrent()
        val buffer = AL.alGenBuffer()
        AL.alBufferData(buffer, data, panning, volume)

        source = AL.alGenSource()
        AL.alSourcei(source, AL.AL_BUFFER, buffer.convert())
        AL.checkAlErrors("alSourcei")

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
                AL.checkAlErrors("alGetSourceState")
                return when (result) {
                    AL.AL_INITIAL -> SoundChannelState.INITIAL
                    AL.AL_PLAYING -> SoundChannelState.PLAYING
                    AL.AL_PAUSED -> SoundChannelState.PAUSED
                    AL.AL_STOPPED -> SoundChannelState.STOPPED
                    else -> SoundChannelState.STOPPED
                }
            }

            override fun pause() {
                AL.alSourcePause(source)
            }

            override fun resume() {
                AL.alSourcePlay(source)
            }

            override fun stop() {
                if (!stopped) {
                    stopped = true
                    AL.alDeleteSource(source)
                    AL.alDeleteBuffer(buffer)
                }
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
                    while (channel.playing) delay(1L)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                channel.stop()
            }
        }
        return channel
    }
}

data class SourceProvider(var source: ALuint)

class JnaSoundPropsProvider(val sourceProvider: SourceProvider) : SoundProps {
    val source get() = sourceProvider.source

    private val temp1 = FloatArray(3)
    private val temp2 = FloatArray(3)
    private val temp3 = FloatArray(3)

    override var pitch: Double
        get() = AL.alGetSourcef(source, AL.AL_PITCH).toDouble()
        set(value) = AL.alSourcef(source, AL.AL_PITCH, value.toFloat())
    override var volume: Double
        get() = AL.alGetSourcef(source, AL.AL_GAIN).toDouble()
        set(value) = AL.alSourcef(source, AL.AL_GAIN, value.toFloat())
    override var panning: Double
        get() = memScoped {
            val temp1 = alloc<ALfloatVar>()
            val temp2 = alloc<ALfloatVar>()
            val temp3 = alloc<ALfloatVar>()
            AL.alGetSource3f(source, AL.AL_POSITION, temp1.ptr, temp2.ptr, temp3.ptr)
            temp1.value.toDouble()
        }
        set(value) {
            val pan = value.toFloat()
            AL.alSourcef(source, AL.AL_ROLLOFF_FACTOR, 0.0f);
            AL.alSourcei(source, AL.AL_SOURCE_RELATIVE, 1);
            AL.alSource3f(source, AL.AL_POSITION, pan, 0f, -sqrt(1.0f - pan * pan));
            //println("SET PANNING: source=$source, pan=$pan")
        }
}

