package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.kmem.clamp01
import com.soywiz.kmem.startAddressOf
import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.redirected
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.OpenAL.*
import kotlin.coroutines.*
import kotlin.math.sqrt

val openalNativeSoundProvider: OpenALNativeSoundProvider by lazy { OpenALNativeSoundProvider() }
actual val nativeSoundProvider: NativeSoundProvider get() = openalNativeSoundProvider

class OpenALNativeSoundProvider : NativeSoundProvider() {
    val device = alcOpenDevice(null)
    //val device: CPointer<ALCdevice>? = null
    val context = device?.let { alcCreateContext(it, null).also {
        alcMakeContextCurrent(it)
        memScoped {
            alListener3f(AL_POSITION, 0f, 0f, 1.0f)
            alListener3f(AL_VELOCITY, 0f, 0f, 0f)
            val listenerOri = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)
            listenerOri.usePinned {
                alListenerfv(AL_ORIENTATION, it.addressOf(0))
            }
        }
    } }

    internal fun makeCurrent() {
        alcMakeContextCurrent(context)
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
        set(value) = run { sourceProvider.source = value }

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
                    val processed = alGetSourcei(source, AL_BUFFERS_PROCESSED)
                    val queued = alGetSourcei(source, AL_BUFFERS_QUEUED)
                    val total = processed + queued
                    val state = alGetSourceState(source)
                    val playing = state == AL_PLAYING

                    //println("buffer=$buffer, processed=$processed, queued=$queued, state=$state, playing=$playing, sampleOffset=$sampleOffset")
                    //println("Samples.add")

                    if (processed <= 0 && total >= 6) {
                        delay(10.milliseconds)
                        continue
                    }

                    if (total < 6) {
                        tempBuffers.value = alGenBuffer()
                        checkAlErrors("alGenBuffers")
                        //println("alGenBuffers: ${tempBuffers[0]}")
                    } else {
                        alSourceUnqueueBuffers(source, 1, tempBuffers.ptr)
                        checkAlErrors("alSourceUnqueueBuffers")
                        //println("alSourceUnqueueBuffers: ${tempBuffers[0]}")
                    }
                    //println("samples: $samples - $offset, $size")
                    //al.alBufferData(tempBuffers[0], samples.copyOfRange(offset, offset + size), frequency, panning, volume)
                    alBufferData(tempBuffers.value, samples.copyOfRange(offset, offset + size), frequency, panning)
                    alSourceQueueBuffers(source, 1, tempBuffers.ptr)
                    checkAlErrors("alSourceQueueBuffers")

                    //val gain = al.alGetSourcef(source, AL.AL_GAIN)
                    //val pitch = al.alGetSourcef(source, AL.AL_PITCH)
                    //println("gain=$gain, pitch=$pitch")
                    if (!playing) {
                        alSourcePlay(source)
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

        source = alGenSource()
        //for (n in buffers.indices) buffers[n] = alGenBuffer() .toInt()
    }

    override fun start() {
        ensureSource()
        alSourcePlay(source)
        checkAlErrors("alSourcePlay")
        //checkAlErrors()
    }

    override fun stop() {
        dispose()
    }

    // @TODO: Leaking buffers?
    override fun dispose() {
        provider.makeCurrent()

        alSourceStop(source)
        if (source.toInt() != 0) {
            alDeleteSource(source)
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
    val coroutineContext: CoroutineContext,
    val data: AudioData?,
    val sourceProvider: SourceProvider = SourceProvider(0.convert()),
    override val name: String = "Unknown"
) : Sound(), SoundProps by JnaSoundPropsProvider(sourceProvider) {
    override suspend fun decode(): AudioData = data ?: AudioData.DUMMY

    var source: ALuint
        get() = sourceProvider.source
        set(value) = run { sourceProvider.source = value }

    override val length: TimeSpan get() = data?.totalTime ?: 0.seconds

    override fun play(params: PlaybackParameters): SoundChannel {
        val data = data ?: return DummySoundChannel(this)
        provider.makeCurrent()
        val buffer = alGenBuffer()
        alBufferData(buffer, data, panning, volume)

        source = alGenSource()
        alSourcei(source, AL_BUFFER, buffer.convert())
        checkAlErrors("alSourcei")

        var stopped = false

        val channel = object : SoundChannel(this), SoundProps by JnaSoundPropsProvider(sourceProvider) {
            val totalSamples get() = data.totalSamples
            var currentSampleOffset: Int
                get() = alGetSourcei(source, AL_SAMPLE_OFFSET)
                set(value) = run {
                    alSourcei(source, AL_SAMPLE_OFFSET, value)
                }

            override var current: TimeSpan
                get() = data.timeAtSample(currentSampleOffset)
                set(value) = run { alSourcef(source, AL_SEC_OFFSET, value.seconds.toFloat())  }
            override val total: TimeSpan get() = data.totalTime
            override val playing: Boolean
                get() {
                    val result = alGetSourceState(source) == AL_PLAYING
                    checkAlErrors("alGetSourceState")
                    return result
                }

            override fun stop() {
                if (!stopped) {
                    stopped = true
                    alDeleteSource(source)
                    alDeleteBuffer(buffer)
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
                    alSourcef(source, AL_SEC_OFFSET, startTime.seconds.toFloat())
                    alSourcePlay(source)
                    //checkAlErrors("alSourcePlay")
                    startTime = 0.seconds
                    while (channel.playing) delay(1L)
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

data class SourceProvider(var source: ALuint)

class JnaSoundPropsProvider(val sourceProvider: SourceProvider) : SoundProps {
    val source get() = sourceProvider.source

    private val temp1 = FloatArray(3)
    private val temp2 = FloatArray(3)
    private val temp3 = FloatArray(3)

    override var pitch: Double
        get() = alGetSourcef(source, AL_PITCH).toDouble()
        set(value) = alSourcef(source, AL_PITCH, value.toFloat())
    override var volume: Double
        get() = alGetSourcef(source, AL_GAIN).toDouble()
        set(value) = alSourcef(source, AL_GAIN, value.toFloat())
    override var panning: Double
        get() = memScoped {
            val temp1 = alloc<ALfloatVar>()
            val temp2 = alloc<ALfloatVar>()
            val temp3 = alloc<ALfloatVar>()
            alGetSource3f(source, AL_POSITION, temp1.ptr, temp2.ptr, temp3.ptr)
            temp1.value.toDouble()
        }
        set(value) = run {
            val pan = value.toFloat()
            alSourcef(source, AL_ROLLOFF_FACTOR, 0.0f);
            alSourcei(source, AL_SOURCE_RELATIVE, 1);
            alSource3f(source, AL_POSITION, pan, 0f, -sqrt(1.0f - pan * pan));
            //println("SET PANNING: source=$source, pan=$pan")
        }
}

private fun alGetSourcef(source: ALuint, param: ALenum): ALfloat =
    memScoped { alloc<ALfloatVar>().also { alGetSourcef(source, param, it.ptr) }.value }

private fun alGetSourcei(source: ALuint, param: ALenum): ALint =
    memScoped { alloc<ALintVar>().also { alGetSourcei(source, param, it.ptr) }.value }

private fun alGetSourceState(source: ALuint): ALint = alGetSourcei(source, AL_SOURCE_STATE)

private fun alBufferData(buffer: ALuint, data: AudioData) {
    val samples = data.samplesInterleaved.data
    samples.usePinned { pin ->
        alBufferData(
            buffer,
            if (data.channels == 1) AL_FORMAT_MONO16 else AL_FORMAT_STEREO16,
            if (samples.isNotEmpty()) pin.addressOf(0) else null,
            samples.size * 2,
            data.rate.convert()
        )
    }
}

private fun alGenBuffer(): ALuint = memScoped { alloc<ALuintVar>().apply { alGenBuffers(1, this.ptr) }.value }
private fun alDeleteBuffer(buffer: ALuint): Unit =
    run { memScoped { alloc<ALuintVar>().apply { this.value = buffer }.apply { alDeleteBuffers(1, this.ptr) } } }

private fun alDeleteSource(buffer: ALuint): Unit =
    run { memScoped { alloc<ALuintVar>().apply { this.value = buffer }.apply { alDeleteSources(1, this.ptr) } } }



private val tempF = FloatArray(1)
private val tempI = IntArray(1)
//private fun alGetSourcef(source: Int, param: Int): Float = tempF.apply { al?.alGetSourcef(source, param, this, 0) }[0]
//private fun alGetSourcei(source: Int, param: Int): Int = tempI.apply { al?.alGetSourcei(source, param, this, 0) }[0]

private fun alBufferData(buffer: ALuint, data: AudioSamples, freq: Int, panning: Double = 0.0, volume: Double = 1.0) {
    alBufferData(buffer, com.soywiz.korau.sound.AudioData(freq, data), panning, volume)
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

private fun alBufferData(buffer: ALuint, data: AudioData, panning: Double = 0.0, volume: Double = 1.0) {
    val samples = data.samplesInterleaved.data

    if (data.stereo && panning != 0.0) applyStereoPanningInline(samples, panning, volume)

    //val bufferData = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
    //bufferData.asShortBuffer().put(samples)

    samples.usePinned { pin ->
        alBufferData(
            buffer,
            if (data.stereo) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16,
            if (samples.isNotEmpty()) pin.startAddressOf else null,
            samples.size * 2,
            data.rate
        )
    }

    checkAlErrors("alBufferData")
}

private fun alGenSourceBase(): ALuint = memScoped { alloc<ALuintVar>().apply { alGenSources(1, this.ptr) }.value }

private fun alGenSource() = alGenSourceBase().also { source ->
    alSourcef(source, AL_PITCH, 1f)
    alSourcef(source, AL_GAIN, 1f)
    alSource3f(source, AL_POSITION, 0f, 0f, 0f)
    alSource3f(source, AL_VELOCITY, 0f, 0f, 0f)
    alSourcei(source, AL_LOOPING, AL_FALSE)
}

fun checkAlErrors(name: String) {
    //val error = al.alGetError()
    //if (error != AL.AL_NO_ERROR) error("OpenAL error ${error.shex} '$name'")
}
