package com.soywiz.korau.sound.impl.jna

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.nio.*
import kotlin.coroutines.*
import kotlin.math.*

class JnaOpenALNativeSoundProvider : NativeSoundProvider() {
    val device = alc?.alcOpenDevice(null).also { device ->
        if (device != null) {
            Runtime.getRuntime().addShutdownHook(Thread {
                alc?.alcCloseDevice(device)
            })
        }
    }
    val context = device?.let { alc?.alcCreateContext(device, null) }.also { context ->
        if (context != null) {
            Runtime.getRuntime().addShutdownHook(Thread {
                //alc?.alcDestroyContext(context) // Crashes on mac!
            })
        }
    }

    init {
        doInit()
    }

    fun makeCurrent() {
        context?.let { alc?.alcMakeContextCurrent(it) }
    }

    private fun doInit() {
        //println("ALut.alutInit: ${Thread.currentThread()}")
        makeCurrent()

        al.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
        checkAlErrors("alListener3f")
        al.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
        checkAlErrors("alListener3f")
        //al?.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f), 0)
        al.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f))
        checkAlErrors("alListenerfv")
    }

    //val myNativeAudioFormats = AudioFormats(MyMP3Decoder) + nativeAudioFormats
    //val myNativeAudioFormats = AudioFormats(MyMP3Decoder)
    //val myNativeAudioFormats = nativeAudioFormats
    override val audioFormats = nativeAudioFormats

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound {
        return if (streaming) {
            super.createSound(data, streaming, props, name)
        } else {
            OpenALSoundNoStream(this, coroutineContext, audioFormats.decode(data, props), name = name)
        }
    }

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        OpenALPlatformAudioOutput(this, coroutineContext, freq)
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
                val processed = al.alGetSourcei(source, AL.AL_BUFFERS_PROCESSED)
                val queued = al.alGetSourcei(source, AL.AL_BUFFERS_QUEUED)
                val total = processed + queued
                val state = al.alGetSourceState(source)
                val playing = state == AL.AL_PLAYING

                //println("buffer=$buffer, processed=$processed, queued=$queued, state=$state, playing=$playing, sampleOffset=$sampleOffset")
                //println("Samples.add")

                if (processed <= 0 && total >= 6) {
                    delay(10.milliseconds)
                    continue
                }

                if (total < 6) {
                    al.alGenBuffers(1, tempBuffers)
                    checkAlErrors("alGenBuffers")
                    //println("alGenBuffers: ${tempBuffers[0]}")
                } else {
                    al.alSourceUnqueueBuffers(source, 1, tempBuffers)
                    checkAlErrors("alSourceUnqueueBuffers")
                    //println("alSourceUnqueueBuffers: ${tempBuffers[0]}")
                }
                //println("samples: $samples - $offset, $size")
                //al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning, volume)
                al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning)
                al.alSourceQueueBuffers(source, 1, tempBuffers)
                checkAlErrors("alSourceQueueBuffers")

                //val gain = al.alGetSourcef(source, AL.AL_GAIN)
                //val pitch = al.alGetSourcef(source, AL.AL_PITCH)
                //println("gain=$gain, pitch=$pitch")
                if (!playing) {
                    al.alSourcePlay(source)
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

        source = alGenSource()
        //al.alGenBuffers(buffers.size, buffers)
    }

    override fun start() {
        ensureSource()
        al.alSourcePlay(source)
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

        al.alSourceStop(source)
        if (source != 0) {
            al.alDeleteSource(source)
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
        val buffer = al.alGenBuffer()
        al.alBufferData(buffer, data, panning, volume)

        source = alGenSource()
        al.alSourcei(source, AL.AL_BUFFER, buffer)
        checkAlErrors("alSourcei")

        var stopped = false

        val channel = object : SoundChannel(this), SoundProps by JnaSoundPropsProvider(sourceProvider) {
            val totalSamples get() = data.totalSamples
            var currentSampleOffset: Int
                get() = al.alGetSourcei(source, AL.AL_SAMPLE_OFFSET)
                set(value) = run {
                    al.alSourcei(source, AL.AL_SAMPLE_OFFSET, value)
                }

            override var current: TimeSpan
                get() = data.timeAtSample(currentSampleOffset)
                set(value) = run { al.alSourcef(source, AL.AL_SEC_OFFSET, value.seconds.toFloat())  }
            override val total: TimeSpan get() = data.totalTime

            override val state: SoundChannelState get() {
                val result = al.alGetSourceState(source)
                checkAlErrors("alGetSourceState")
                return when (result) {
                    AL.AL_INITIAL -> SoundChannelState.INITIAL
                    AL.AL_PLAYING -> SoundChannelState.PLAYING
                    AL.AL_PAUSED -> SoundChannelState.PAUSED
                    AL.AL_STOPPED -> SoundChannelState.STOPPED
                    else -> error("Invalid alGetSourceState $result")
                }
            }

            override fun stop() {
                if (!stopped) {
                    stopped = true
                    al.alDeleteSource(source)
                    al.alDeleteBuffer(buffer)
                }
            }

            override fun pause() {
                al.alSourcePause(source)
            }

            override fun resume() {
                al.alSourcePlay(source)
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
                    al.alSourcef(source, AL.AL_SEC_OFFSET, startTime.seconds.toFloat())
                    al.alSourcePlay(source)
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
        get() = al.alGetSourcef(source, AL.AL_PITCH).toDouble()
        set(value) = al.alSourcef(source, AL.AL_PITCH, value.toFloat())
    override var volume: Double
        get() = al.alGetSourcef(source, AL.AL_GAIN).toDouble()
        set(value) = al.alSourcef(source, AL.AL_GAIN, value.toFloat())
    override var panning: Double
        get() = run {
            al.alGetSource3f(source, AL.AL_POSITION, temp1, temp2, temp3)
            temp1[0].toDouble()
        }
        set(value) = run {
            val pan = value.toFloat()
            al.alSourcef(source, AL.AL_ROLLOFF_FACTOR, 0.0f);
            al.alSourcei(source, AL.AL_SOURCE_RELATIVE, 1);
            al.alSource3f(source, AL.AL_POSITION, pan, 0f, -sqrt(1.0f - pan * pan));
            //println("SET PANNING: source=$source, pan=$pan")
        }
}

/*
class OpenALNativeSoundStream(provider: JnaOpenALNativeSoundProvider, coroutineContext: CoroutineContext, val data: AudioStream?) : BaseOpenALNativeSound(provider, coroutineContext) {
    override suspend fun decode(): AudioData = data?.toData() ?: AudioData.DUMMY

    override fun play(): NativeSoundChannel {
        TODO()
    }
}
 */

//private fun alGetSourcef(source: Int, param: Int): Float = tempF.apply { al?.alGetSourcef(source, param, this, 0) }[0]
//private fun alGetSourcei(source: Int, param: Int): Int = tempI.apply { al?.alGetSourcei(source, param, this, 0) }[0]

private fun AL.alBufferData(buffer: Int, data: AudioSamples, freq: Int, panning: Double = 0.0, volume: Double = 1.0) {
    alBufferData(buffer, AudioData(freq, data), panning, volume)
}

private fun applyStereoPanningInline(interleaved: ShortArray, panning: Double = 0.0, volume: Double = 1.0) {
    if (panning == 0.0 || volume != 1.0) return
    val vvolume = volume.clamp01()
    val rratio = ((panning + 1.0) / 2.0).clamp01() * vvolume
    val lratio = (1.0 - rratio) * vvolume
    //println("panning=$panning, lratio=$lratio, rratio=$rratio, vvolume=$vvolume")
    for (n in interleaved.indices step 2) {
        interleaved[n + 0] = (interleaved[n + 0] * lratio).toShort()
        interleaved[n + 1] = (interleaved[n + 1] * rratio).toShort()
    }
}

private fun AL.alBufferData(buffer: Int, data: AudioData, panning: Double = 0.0, volume: Double = 1.0) {
    val samples = data.samplesInterleaved.data

    if (data.stereo && panning != 0.0) applyStereoPanningInline(samples, panning, volume)

    val bufferData = ShortBuffer.wrap(samples)
    //val bufferData = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
    //bufferData.asShortBuffer().put(samples)

    al.alBufferData(
        buffer,
        if (data.stereo) AL.AL_FORMAT_STEREO16 else AL.AL_FORMAT_MONO16,
        if (samples.isNotEmpty()) bufferData else null,
        samples.size * 2,
        data.rate
    )
    checkAlErrors("alBufferData")
}

private fun alGenSource() = al.alGenSource().also { source ->
    al.alSourcef(source, AL.AL_PITCH, 1f)
    al.alSourcef(source, AL.AL_GAIN, 1f)
    al.alSource3f(source, AL.AL_POSITION, 0f, 0f, 0f)
    al.alSource3f(source, AL.AL_VELOCITY, 0f, 0f, 0f)
    al.alSourcei(source, AL.AL_LOOPING, AL.AL_FALSE)
}


//private fun alGenBuffer(): Int = tempI.apply { al?.alGenBuffers(1, this, 0) }[0]
//private fun alGenSource(): Int = tempI.apply { al?.alGenSources(1, this, 0) }[0]
//private fun alDeleteBuffer(buffer: Int): Unit = run { al?.alDeleteBuffers(1, tempI.also { it[0] = buffer }, 0) }
//private fun alDeleteSource(buffer: Int): Unit = run { al?.alDeleteSources(1, tempI.also { it[0] = buffer }, 0) }

/*
val alc by lazy {
    ALFactory.getALC().also { alc ->
        //val error = alc.alcGetError()
        //if (error != AL.AL_NO_ERROR) error("Error initializing OpenAL ${error.shex}")
    } }

private val device by lazy { alc.alcOpenDevice(null).also {
    println("alc.alcOpenDevice: $it")
} }
private val context by lazy { alc.alcCreateContext(device, null).also {
    println("alc.alcCreateContext: $it with device=$device")
} }
*/

fun checkAlErrors(name: String) {
    //val error = al.alGetError()
    //if (error != AL.AL_NO_ERROR) error("OpenAL error ${error.shex} '$name'")
}
