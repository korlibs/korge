package com.soywiz.korau.sound.backends

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.*
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

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps, name: String): Sound {
        return if (streaming) {
            super.createSound(data, streaming, props, name)
        } else {
            OpenALNativeSoundNoStream(this, coroutineContext, audioFormats.decode(data, props), name = name)
        }
    }

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
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

    override var pitch: Double by sourceProv::pitch
    override var volume: Double by sourceProv::volume
    override var panning: Double by sourceProv::panning

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
                val tempBuffers = alloc<ALuintVar>()
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

//typealias ALuintVar = UIntVar
typealias ALuint = Int
typealias ALuintVar = IntVar
typealias ALfloat = Float
typealias ALfloatVar = FloatVar
typealias ALDevicePointer = COpaquePointer?
typealias ALContextPointer = COpaquePointer?
typealias ALBufferPointer = COpaquePointer?
typealias ALCString = CValuesRef<CPointerVar<ByteVar>>?
typealias CBooleanArray = CArrayPointer<ByteVar>?
typealias CIntArray = CArrayPointer<IntVar>?
typealias CFloatArray = CArrayPointer<FloatVar>?
typealias CDoubleArray = CArrayPointer<DoubleVar>?

@Suppress("unused")
internal object AL : DynamicLibrary(
    Environment["OPENAL_LIB_PATH"],
    when {
        Platform.isWindows -> "OpenAL32.dll"
        Platform.isMac -> "OpenAL"
        Platform.isLinux -> "libopenal.so.1"
        else -> "libopenal"
    },
    "libopenal",
    "libopenal.so.1",
    "OpenAL"
) {
    val alDopplerFactor by func<(value: Float) -> Unit>()
    val alDopplerVelocity by func<(value: Float) -> Unit>()
    val alSpeedOfSound by func<(value: Float) -> Unit>()
    val alDistanceModel by func<(distanceModel: Int) -> Unit>()
    val alEnable by func<(capability: Int) -> Unit>()
    val alDisable by func<(capability: Int) -> Unit>()
    val alIsEnabled by func<(capability: Int) -> Boolean>()
    val alGetString by func<(param: Int) -> ALCString>()
    val alGetBooleanv by func<(param: Int, values: BooleanArray) -> Unit>()
    val alGetIntegerv by func<(param: Int, values: CIntArray) -> Unit>()
    val alGetFloatv by func<(param: Int, values: CFloatArray) -> Unit>()
    val alGetDoublev by func<(param: Int, values: CDoubleArray) -> Unit>()
    val alGetBoolean by func<(param: Int) -> Boolean>()
    val alGetInteger by func<(param: Int) -> Int>()
    val alGetFloat by func<(param: Int) -> Float>()
    val alGetDouble by func<(param: Int) -> Double>()
    val alGetError by func<() -> Int>()
    val alIsExtensionPresent by func<(extname: ALCString) -> Boolean>()
    val alGetProcAddress by func<(fname: ALCString) -> COpaquePointer?>()
    val alGetEnumValue by func<(ename: ALCString) -> Int>()
    val alListenerf by func<(param: Int, value: Float) -> Unit>()
    val alListener3f by func<(param: Int, value1: Float, value2: Float, value3: Float) -> Unit>()
    val alListenerfv by func<(param: Int, values: CFloatArray) -> Unit>()
    val alListeneri by func<(param: Int, value: Int) -> Unit>()
    val alListener3i by func<(param: Int, value1: Int, value2: Int, value3: Int) -> Unit>()
    val alListeneriv by func<(param: Int, values: CIntArray) -> Unit>()
    val alGetListenerf by func<(param: Int, value: CFloatArray) -> Unit>()
    val alGetListener3f by func<(param: Int, value1: CFloatArray, value2: CFloatArray, value3: CFloatArray) -> Unit>()
    val alGetListenerfv by func<(param: Int, values: CFloatArray) -> Unit>()
    val alGetListeneri by func<(param: Int, value: CIntArray) -> Unit>()
    val alGetListener3i by func<(param: Int, value1: CIntArray, value2: CIntArray, value3: CIntArray) -> Unit>()
    val alGetListeneriv by func<(param: Int, values: CIntArray) -> Unit>()
    val alGenSources by func<(n: Int, sources: CIntArray) -> Unit>()
    val alDeleteSources by func<(n: Int, sources: CIntArray) -> Unit>()
    val alIsSource by func<(source: Int) -> Boolean>()
    val alSourcef by func<(source: Int, param: Int, value: Float) -> Unit>()
    val alSource3f by func<(source: Int, param: Int, value1: Float, value2: Float, value3: Float) -> Unit>()
    val alSourcefv by func<(source: Int, param: Int, values: CFloatArray) -> Unit>()
    val alSourcei by func<(source: Int, param: Int, value: Int) -> Unit>()
    val alSource3i by func<(source: Int, param: Int, value1: Int, value2: Int, value3: Int) -> Unit>()
    val alSourceiv by func<(source: Int, param: Int, values: CIntArray) -> Unit>()
    val alGetSourcef by func<(source: Int, param: Int, value: CFloatArray) -> Unit>()
    val alGetSource3f by func<(source: Int, param: Int, value1: CFloatArray, value2: CFloatArray, value3: CFloatArray) -> Unit>()
    val alGetSourcefv by func<(source: Int, param: Int, values: CFloatArray) -> Unit>()
    val alGetSourcei by func<(source: Int, param: Int, value: CIntArray) -> Unit>()
    val alGetSource3i by func<(source: Int, param: Int, value1: CIntArray, value2: CIntArray, value3: CIntArray) -> Unit>()
    val alGetSourceiv by func<(source: Int, param: Int, values: CIntArray) -> Unit>()
    val alSourcePlayv by func<(n: Int, sources: CIntArray) -> Unit>()
    val alSourceStopv by func<(n: Int, sources: CIntArray) -> Unit>()
    val alSourceRewindv by func<(n: Int, sources: CIntArray) -> Unit>()
    val alSourcePausev by func<(n: Int, sources: CIntArray) -> Unit>()
    val alSourcePlay by func<(source: Int) -> Unit>()
    val alSourceStop by func<(source: Int) -> Unit>()
    val alSourceRewind by func<(source: Int) -> Unit>()
    val alSourcePause by func<(source: Int) -> Unit>()
    val alSourceQueueBuffers by func<(source: Int, nb: Int, buffers: CIntArray) -> Unit>()
    val alSourceUnqueueBuffers by func<(source: Int, nb: Int, buffers: CIntArray) -> Unit>()
    val alGenBuffers by func<(n: Int, buffers: CIntArray) -> Unit>()
    val alDeleteBuffers by func<(n: Int, buffers: CIntArray) -> Unit>()
    val alIsBuffer by func<(buffer: Int) -> Boolean>()
    val alBufferData by func<(buffer: Int, format: Int, data: ALBufferPointer, size: Int, freq: Int) -> Unit>()
    val alBufferf by func<(buffer: Int, param: Int, value: Float) -> Unit>()
    val alBuffer3f by func<(buffer: Int, param: Int, value1: Float, value2: Float, value3: Float) -> Unit>()
    val alBufferfv by func<(buffer: Int, param: Int, values: CFloatArray) -> Unit>()
    val alBufferi by func<(buffer: Int, param: Int, value: Int) -> Unit>()
    val alBuffer3i by func<(buffer: Int, param: Int, value1: Int, value2: Int, value3: Int) -> Unit>()
    val alBufferiv by func<(buffer: Int, param: Int, values: CIntArray) -> Unit>()
    val alGetBufferf by func<(buffer: Int, param: Int, value: CFloatArray) -> Unit>()
    val alGetBuffer3f by func<(buffer: Int, param: Int, value1: CFloatArray, value2: CFloatArray, value3: CFloatArray) -> Unit>()
    val alGetBufferfv by func<(buffer: Int, param: Int, values: CFloatArray) -> Unit>()
    val alGetBufferi by func<(buffer: Int, param: Int, value: CIntArray) -> Unit>()
    val alGetBuffer3i by func<(buffer: Int, param: Int, value1: CIntArray, value2: CIntArray, value3: CIntArray) -> Unit>()
    val alGetBufferiv by func<(buffer: Int, param: Int, values: CIntArray) -> Unit>()

// ALC

    val alcCreateContext by func<(device: ALContextPointer, attrlist: CIntArray) -> ALContextPointer>()
    val alcMakeContextCurrent by func<(context: ALContextPointer) -> Boolean>()
    val alcProcessContext by func<(context: ALContextPointer) -> Unit>()
    val alcSuspendContext by func<(context: ALContextPointer) -> Unit>()
    val alcDestroyContext by func<(context: ALContextPointer) -> Unit>()
    val alcGetCurrentContext by func<() -> ALContextPointer>()
    val alcGetContextsDevice by func<(context: ALContextPointer) -> COpaquePointer>()
    val alcOpenDevice by func<(devicename: ALCString) -> ALDevicePointer>()
    val alcCloseDevice by func<(device: ALDevicePointer) -> Boolean>()
    val alcGetError by func<(device: ALDevicePointer) -> Int>()
    val alcIsExtensionPresent by func<(device: ALDevicePointer, extname: ALCString) -> Boolean>()
    val alcGetProcAddress by func<(device: ALDevicePointer, funcname: ALCString) -> COpaquePointer>()
    val alcGetEnumValue by func<(device: ALDevicePointer, enumname: ALCString) -> Int>()
    val alcGetString by func<(device: ALDevicePointer, param: Int) -> ALCString>()
    val alcGetIntegerv by func<(device: ALDevicePointer, param: Int, size: Int, values: CIntArray) -> Unit>()
    val alcCaptureOpenDevice by func<(devicename: ALCString, frequency: Int, format: Int, buffersize: Int) -> ALDevicePointer>()
    val alcCaptureCloseDevice by func<(device: ALDevicePointer) -> Boolean>()
    val alcCaptureStart by func<(device: ALDevicePointer) -> Unit>()
    val alcCaptureStop by func<(device: ALDevicePointer) -> Unit>()
    val alcCaptureSamples by func<(device: ALDevicePointer, buffer: ALBufferPointer, samples: Int) -> Unit>()


    fun alGenBuffer(): Int = memScoped { allocArray<IntVar>(1).also { alGenBuffers(1, it) }[0] }
    //fun alGenSource(): Int = memScoped { allocArray<IntVar>(1).also { alGenSources(1, it) }[0] }
    fun alDeleteBuffer(buffer: Int): Unit = memScoped {
        val value = allocArray<IntVar>(1)
        alDeleteBuffers(1, value.also { it[0] = buffer })
    }

    fun alDeleteSource(buffer: Int): Unit = memScoped {
        val value = allocArray<IntVar>(1)
        alDeleteSources(1, value.also { it[0] = buffer })
    }

    fun alGetSourcef(source: Int, param: Int): Float = memScoped { allocArray<FloatVar>(1).also { alGetSourcef(source, param, it) }[0] }
    fun alGetSourcei(source: Int, param: Int): Int = memScoped { allocArray<IntVar>(1).also { alGetSourcei(source, param, it) }[0] }
    fun alGetSourceState(source: Int): Int = alGetSourcei(source, AL_SOURCE_STATE)

    const val AL_NONE = 0
    const val AL_FALSE = 0
    const val AL_TRUE = 1
    const val AL_SOURCE_RELATIVE = 0x202
    const val AL_CONE_INNER_ANGLE = 0x1001
    const val AL_CONE_OUTER_ANGLE = 0x1002
    const val AL_PITCH = 0x1003
    const val AL_POSITION = 0x1004
    const val AL_DIRECTION = 0x1005
    const val AL_VELOCITY = 0x1006
    const val AL_LOOPING = 0x1007
    const val AL_BUFFER = 0x1009
    const val AL_GAIN = 0x100A
    const val AL_MIN_GAIN = 0x100D
    const val AL_MAX_GAIN = 0x100E
    const val AL_ORIENTATION = 0x100F
    const val AL_SOURCE_STATE = 0x1010
    const val AL_INITIAL = 0x1011
    const val AL_PLAYING = 0x1012
    const val AL_PAUSED = 0x1013
    const val AL_STOPPED = 0x1014
    const val AL_BUFFERS_QUEUED = 0x1015
    const val AL_BUFFERS_PROCESSED = 0x1016
    const val AL_REFERENCE_DISTANCE = 0x1020
    const val AL_ROLLOFF_FACTOR = 0x1021
    const val AL_CONE_OUTER_GAIN = 0x1022
    const val AL_MAX_DISTANCE = 0x1023
    const val AL_SEC_OFFSET = 0x1024
    const val AL_SAMPLE_OFFSET = 0x1025
    const val AL_BYTE_OFFSET = 0x1026
    const val AL_SOURCE_TYPE = 0x1027
    const val AL_STATIC = 0x1028
    const val AL_STREAMING = 0x1029
    const val AL_UNDETERMINED = 0x1030
    const val AL_FORMAT_MONO8 = 0x1100
    const val AL_FORMAT_MONO16 = 0x1101
    const val AL_FORMAT_STEREO8 = 0x1102
    const val AL_FORMAT_STEREO16 = 0x1103
    const val AL_FREQUENCY = 0x2001
    const val AL_BITS = 0x2002
    const val AL_CHANNELS = 0x2003
    const val AL_SIZE = 0x2004
    const val AL_UNUSED = 0x2010
    const val AL_PENDING = 0x2011
    const val AL_PROCESSED = 0x2012
    const val AL_NO_ERROR = 0
    const val AL_INVALID_NAME = 0xA001
    const val AL_INVALID_ENUM = 0xA002
    const val AL_INVALID_VALUE = 0xA003
    const val AL_INVALID_OPERATION = 0xA004
    const val AL_OUT_OF_MEMORY = 0xA005
    const val AL_VENDOR = 0xB001
    const val AL_VERSION = 0xB002
    const val AL_RENDERER = 0xB003
    const val AL_EXTENSIONS = 0xB004
    const val AL_DOPPLER_FACTOR = 0xC000
    const val AL_DOPPLER_VELOCITY = 0xC001
    const val AL_SPEED_OF_SOUND = 0xC003
    const val AL_DISTANCE_MODEL = 0xD000
    const val AL_INVERSE_DISTANCE = 0xD001
    const val AL_INVERSE_DISTANCE_CLAMPED = 0xD002
    const val AL_LINEAR_DISTANCE = 0xD003
    const val AL_LINEAR_DISTANCE_CLAMPED = 0xD004
    const val AL_EXPONENT_DISTANCE = 0xD005
    const val AL_EXPONENT_DISTANCE_CLAMPED = 0xD006

    const val ALC_FALSE = 0
    const val ALC_TRUE = 1
    const val ALC_FREQUENCY = 0x1007
    const val ALC_REFRESH = 0x1008
    const val ALC_SYNC = 0x1009
    const val ALC_MONO_SOURCES = 0x1010
    const val ALC_STEREO_SOURCES = 0x1011
    const val ALC_NO_ERROR = 0
    const val ALC_INVALID_DEVICE = 0xA001
    const val ALC_INVALID_CONTEXT = 0xA002
    const val ALC_INVALID_ENUM = 0xA003
    const val ALC_INVALID_VALUE = 0xA004
    const val ALC_OUT_OF_MEMORY = 0xA005
    const val ALC_MAJOR_VERSION = 0x1000
    const val ALC_MINOR_VERSION = 0x1001
    const val ALC_ATTRIBUTES_SIZE = 0x1002
    const val ALC_ALL_ATTRIBUTES = 0x1003
    const val ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004
    const val ALC_DEVICE_SPECIFIER = 0x1005
    const val ALC_EXTENSIONS = 0x1006
    const val ALC_EXT_CAPTURE = 1
    const val ALC_CAPTURE_DEVICE_SPECIFIER = 0x310
    const val ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311
    const val ALC_CAPTURE_SAMPLES = 0x312
    const val ALC_ENUMERATE_ALL_EXT = 1
    const val ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012
    const val ALC_ALL_DEVICES_SPECIFIER = 0x1013

    ///////////////////


    fun alBufferData(buffer: ALuint, data: AudioData) {
        val samples = data.samplesInterleaved.data
        samples.usePinned { pin ->
            AL.alBufferData(
                buffer,
                if (data.channels == 1) AL.AL_FORMAT_MONO16 else AL.AL_FORMAT_STEREO16,
                if (samples.isNotEmpty()) pin.addressOf(0) else null,
                samples.size * 2,
                data.rate.convert()
            )
        }
    }

    fun alBufferData(buffer: ALuint, data: AudioSamples, freq: Int, panning: Double = 0.0, volume: Double = 1.0) {
        alBufferData(buffer, com.soywiz.korau.sound.AudioData(freq, data), panning, volume)
    }

    fun applyStereoPanningInline(interleaved: ShortArray, panning: Double = 0.0, volume: Double = 1.0) {
        if (panning == 0.0 || volume != 1.0) return
        val vvolume = volume.clamp01()
        val rratio = ((panning + 1.0) / 2.0).clamp01() * vvolume
        val lratio = (1.0 - rratio) * vvolume
        //println("panning=$panning, lratio=$lratio, rratio=$rratio, vvolume=$vvolume")
        for (n in interleaved.indices step 2) {
            interleaved[n + 0] = (interleaved[n + 0] * lratio).toInt().toShort()
            interleaved[n + 1] = (interleaved[n + 1] * rratio).toInt().toShort()
        }
    }

    fun alBufferData(buffer: ALuint, data: AudioData, panning: Double = 0.0, volume: Double = 1.0) {
        val samples = data.samplesInterleaved.data

        if (data.stereo && panning != 0.0) applyStereoPanningInline(samples, panning, volume)

        //val bufferData = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        //bufferData.asShortBuffer().put(samples)

        samples.usePinned { pin ->
            AL.alBufferData(
                buffer,
                if (data.stereo) AL.AL_FORMAT_STEREO16 else AL.AL_FORMAT_MONO16,
                if (samples.isNotEmpty()) pin.startAddressOf else null,
                samples.size * 2,
                data.rate
            )
        }

        checkAlErrors("alBufferData")
    }

    fun alGenSourceBase(): ALuint = memScoped { alloc<ALuintVar>().apply { AL.alGenSources(1, this.ptr) }.value }

    fun alGenSource(): ALuint /* = kotlin.Int */ = alGenSourceBase().also { source ->
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
}
