package korlibs.audio.sound.backend

/*
import korlibs.audio.internal.*
import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.ffi.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.logger.*
import korlibs.math.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

class OpenALException(message: String) : RuntimeException(message)

class FFIOpenALNativeSoundProvider : NativeSoundProvider() {
    companion object {
        val MAX_AVAILABLE_SOURCES = 100
    }

    val device = (AL.alcOpenDevice(null) ?: throw OpenALException("Can't open OpenAL device"))
    val context = (AL.alcCreateContext(device, null) ?: throw OpenALException("Can't get OpenAL context"))

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

    fun unmakeCurrent() {
        AL.alcMakeContextCurrent(null)
    }

    init {
        makeCurrent()

        AL.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
        checkAlErrors("alListener3f", 0)
        AL.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
        checkAlErrors("alListener3f", 0)
        AL.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f))
        checkAlErrors("alListenerfv", 0)

        //java.lang.Runtime.getRuntime().addShutdownHook(Thread {
        //    unmakeCurrent()
        //    AL.alcDestroyContext(context)
        //    AL.alcCloseDevice(device)
        //})
    }

    override suspend fun createNonStreamingSound(data: AudioData, name: String): Sound {
        if (!AL.loaded) return super.createNonStreamingSound(data, name)
        return FFIOpenALSoundNoStream(this, coroutineContext, data, name = name)
    }

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        if (!AL.loaded) return super.createPlatformAudioOutput(coroutineContext, freq)
        return FFIOpenALPlatformAudioOutput(this, coroutineContext, freq)
    }
}

class FFIOpenALPlatformAudioOutput(
    val provider: FFIOpenALNativeSoundProvider,
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
        println("OpenALPlatformAudioOutput.add")
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
    //private val now get() = System.nanoTime()
    private val now get() = PerformanceCounter.reference.nanoseconds.toLong()

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
class FFIOpenALSoundNoStream(
    val provider: FFIOpenALNativeSoundProvider,
    coroutineContext: CoroutineContext,
    val data: AudioData?,
    override val name: String = "Unknown"
) : Sound(coroutineContext), SoundProps {
    private val logger = Logger("OpenALSoundNoStream")

    override suspend fun decode(maxSamples: Int): AudioData = data ?: AudioData.DUMMY

    override var volume: Double = 1.0
    override var pitch: Double = 1.0
    override var panning: Double = 0.0

    override val length: TimeSpan get() = data?.totalTime ?: 0.seconds
    override val nchannels: Int get() = data?.channels ?: 1

    override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
        val data = data ?: return DummySoundChannel(this)
        //println("provider.sourcePool.totalItemsInUse=${provider.sourcePool.totalItemsInUse}, provider.sourcePool.totalAllocatedItems=${provider.sourcePool.totalAllocatedItems}, provider.sourcePool.itemsInPool=${provider.sourcePool.itemsInPool}")
        if (provider.sourcePool.totalItemsInUse >= FFIOpenALNativeSoundProvider.MAX_AVAILABLE_SOURCES) {
            error("OpenAL too many sources in use")
        }
        provider.makeCurrent()
        var buffer = provider.bufferPool.alloc()
        var source = provider.sourcePool.alloc()
        if (source == -1) logger.warn { "UNEXPECTED[0] source=-1" }

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

            override val state: SoundChannelState
                get() {
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
                if (source == -1) logger.warn { "UNEXPECTED[1] source=-1" }
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
            it.copySoundPropsFromCombined(this@FFIOpenALSoundNoStream, params)
        }
        launchImmediately(coroutineContext[ContinuationInterceptor] ?: coroutineContext) {
            var times = params.times
            var startTime = params.startTime
            try {
                while (times.hasMore && !stopped) {
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
    //val bufferData = ShortBuffer.wrap(samples)
    //val bufferData = Int16Buffer(samples)
    val bufferData = samples
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

@Suppress("unused")
//object AL : FFILib(nativeOpenALLibraryPath, "/System/Library/Frameworks/OpenAL.framework/OpenAL") {
object AL : FFILib(nativeOpenALLibraryPath, "OpenAL", "AL") {
    private val logger = Logger("AL")

    val alDopplerFactor: (value: Float) -> Unit by func()
    val alDopplerVelocity: (value: Float) -> Unit by func()
    val alSpeedOfSound: (value: Float) -> Unit by func()
    val alDistanceModel: (distanceModel: Int) -> Unit by func()
    val alEnable: (capability: Int) -> Unit by func()
    val alDisable: (capability: Int) -> Unit by func()
    val alIsEnabled: (capability: Int) -> Boolean by func()
    val alGetString: (param: Int) -> String by func()
    val alGetBooleanv: (param: Int, values: BooleanArray) -> Unit by func()
    val alGetIntegerv: (param: Int, values: IntArray) -> Unit by func()
    val alGetFloatv: (param: Int, values: FloatArray) -> Unit by func()
    val alGetDoublev: (param: Int, values: DoubleArray) -> Unit by func()
    val alGetBoolean: (param: Int) -> Boolean by func()
    val alGetInteger: (param: Int) -> Int by func()
    val alGetFloat: (param: Int) -> Float by func()
    val alGetDouble: (param: Int) -> Double by func()
    val alGetError: () -> Int by func()
    val alIsExtensionPresent: (extname: String) -> Boolean by func()
    val alGetProcAddress: (fname: String) -> FFIPointer? by func()
    val alGetEnumValue: (ename: String) -> Int by func()
    val alListenerf: (param: Int, value: Float) -> Unit by func()
    val alListener3f: (param: Int, value1: Float, value2: Float, value3: Float) -> Unit by func()
    val alListenerfv: (param: Int, values: FloatArray) -> Unit by func()
    val alListeneri: (param: Int, value: Int) -> Unit by func()
    val alListener3i: (param: Int, value1: Int, value2: Int, value3: Int) -> Unit by func()
    val alListeneriv: (param: Int, values: IntArray) -> Unit by func()
    val alGetListenerf: (param: Int, value: FloatArray) -> Unit by func()
    val alGetListener3f: (param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) -> Unit by func()
    val alGetListenerfv: (param: Int, values: FloatArray) -> Unit by func()
    val alGetListeneri: (param: Int, value: IntArray) -> Unit by func()
    val alGetListener3i: (param: Int, value1: IntArray, value2: IntArray, value3: IntArray) -> Unit by func()
    val alGetListeneriv: (param: Int, values: IntArray) -> Unit by func()
    val alGenSources: (n: Int, sources: IntArray) -> Unit by func()
    val alDeleteSources: (n: Int, sources: IntArray) -> Unit by func()
    val alIsSource: (source: Int) -> Boolean by func()
    val alSourcef: (source: Int, param: Int, value: Float) -> Unit by func()
    val alSource3f: (source: Int, param: Int, value1: Float, value2: Float, value3: Float) -> Unit by func()
    val alSourcefv: (source: Int, param: Int, values: FloatArray) -> Unit by func()
    val alSourcei: (source: Int, param: Int, value: Int) -> Unit by func()
    val alSource3i: (source: Int, param: Int, value1: Int, value2: Int, value3: Int) -> Unit by func()
    val alSourceiv: (source: Int, param: Int, values: IntArray) -> Unit by func()
    val alGetSourcef: (source: Int, param: Int, value: FloatArray) -> Unit by func()
    val alGetSource3f: (source: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) -> Unit by func()
    val alGetSourcefv: (source: Int, param: Int, values: FloatArray) -> Unit by func()
    val alGetSourcei: (source: Int, param: Int, value: IntArray) -> Unit by func()
    val alGetSource3i: (source: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray) -> Unit by func()
    val alGetSourceiv: (source: Int, param: Int, values: IntArray) -> Unit by func()
    val alSourcePlayv: (n: Int, sources: IntArray) -> Unit by func()
    val alSourceStopv: (n: Int, sources: IntArray) -> Unit by func()
    val alSourceRewindv: (n: Int, sources: IntArray) -> Unit by func()
    val alSourcePausev: (n: Int, sources: IntArray) -> Unit by func()
    val alSourcePlay: (source: Int) -> Unit by func()
    val alSourceStop: (source: Int) -> Unit by func()
    val alSourceRewind: (source: Int) -> Unit by func()
    val alSourcePause: (source: Int) -> Unit by func()
    val alSourceQueueBuffers: (source: Int, nb: Int, buffers: IntArray) -> Unit by func()
    val alSourceUnqueueBuffers: (source: Int, nb: Int, buffers: IntArray) -> Unit by func()
    val alGenBuffers: (n: Int, buffers: IntArray) -> Unit by func()
    val alDeleteBuffers: (n: Int, buffers: IntArray) -> Unit by func()
    val alIsBuffer: (buffer: Int) -> Boolean by func()
    //val alBufferData: (buffer: Int, format: Int, data: Buffer?, size: Int, freq: Int) -> Unit by func()
    val alBufferData: (buffer: Int, format: Int, data: ShortArray?, size: Int, freq: Int) -> Unit by func()
    val alBufferf: (buffer: Int, param: Int, value: Float) -> Unit by func()
    val alBuffer3f: (buffer: Int, param: Int, value1: Float, value2: Float, value3: Float) -> Unit by func()
    val alBufferfv: (buffer: Int, param: Int, values: FloatArray) -> Unit by func()
    val alBufferi: (buffer: Int, param: Int, value: Int) -> Unit by func()
    val alBuffer3i: (buffer: Int, param: Int, value1: Int, value2: Int, value3: Int) -> Unit by func()
    val alBufferiv: (buffer: Int, param: Int, values: IntArray) -> Unit by func()
    val alGetBufferf: (buffer: Int, param: Int, value: FloatArray) -> Unit by func()
    val alGetBuffer3f: (buffer: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) -> Unit by func()
    val alGetBufferfv: (buffer: Int, param: Int, values: FloatArray) -> Unit by func()
    val alGetBufferi: (buffer: Int, param: Int, value: IntArray) -> Unit by func()
    val alGetBuffer3i: (buffer: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray) -> Unit by func()
    val alGetBufferiv: (buffer: Int, param: Int, values: IntArray) -> Unit by func()

    private val tempF = FloatArray(1)
    private val tempI = IntArray(1)

    fun alGenBuffer(): Int = tempI.also { alGenBuffers(1, it) }[0]
    fun alGenSource(): Int = tempI.also { alGenSources(1, it) }[0]
    fun alDeleteBuffer(buffer: Int) { alDeleteBuffers(1, tempI.also { it[0] = buffer }) }
    fun alDeleteSource(buffer: Int) { alDeleteSources(1, tempI.also { it[0] = buffer }) }
    fun alGetSourcef(source: Int, param: Int): Float = tempF.also { alGetSourcef(source, param, it) }[0]
    fun alGetSourcei(source: Int, param: Int): Int = tempI.also { alGetSourcei(source, param, it) }[0]
    fun alGetSourceState(source: Int): Int = alGetSourcei(source, AL.AL_SOURCE_STATE)

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

    // ALC

    val alcCreateContext: (device: FFIPointer?, attrlist: IntArray?) -> FFIPointer? by func()
    val alcMakeContextCurrent: (context: FFIPointer?) -> Boolean by func()
    val alcProcessContext: (context: FFIPointer?) -> Unit by func()
    val alcSuspendContext: (context: FFIPointer?) -> Unit by func()
    val alcDestroyContext: (context: FFIPointer?) -> Unit by func()
    val alcGetCurrentContext: () -> FFIPointer? by func()
    val alcGetContextsDevice: (context: FFIPointer?) -> FFIPointer? by func()
    val alcOpenDevice: (devicename: String?) -> FFIPointer? by func()
    val alcCloseDevice: (device: FFIPointer?) -> Boolean by func()
    val alcGetError: (device: FFIPointer?) -> Int by func()
    val alcIsExtensionPresent: (device: FFIPointer?, extname: String) -> Boolean by func()
    val alcGetProcAddress: (device: FFIPointer?, funcname: String) -> FFIPointer? by func()
    val alcGetEnumValue: (device: FFIPointer?, enumname: String) -> Int by func()
    val alcGetString: (device: FFIPointer?, param: Int) -> String by func()
    val alcGetIntegerv: (device: FFIPointer?, param: Int, size: Int, values: IntArray) -> Unit by func()
    val alcCaptureOpenDevice: (devicename: String, frequency: Int, format: Int, buffersize: Int) -> FFIPointer? by func()
    val alcCaptureCloseDevice: (device: FFIPointer?) -> Boolean by func()
    val alcCaptureStart: (device: FFIPointer?) -> Unit by func()
    val alcCaptureStop: (device: FFIPointer?) -> Unit by func()
    val alcCaptureSamples: (device: FFIPointer?, buffer: Buffer, samples: Int) -> Unit by func()

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
}

val nativeOpenALLibraryPath: String? by lazy {
    Environment["OPENAL_LIB_PATH"]?.let { path ->
        return@lazy path
    }
    if (Environment["KORAU_JVM_DUMMY_SOUND"] == "true") {
        return@lazy null
    }
    when {
        Platform.isMac -> "OpenAL" // Mac already includes the OpenAL library
        Platform.isLinux -> "libopenal.so.1"
        Platform.isWindows -> "soft_oal.dll"
        else -> {
            println("  - Unknown/Unsupported OS")
            null
        }
    }
}

/*
//private val arch by lazy { System.getProperty("os.arch").toLowerCase() }
//private val alClassLoader by lazy { AL::class.java.classLoader }
//private fun getNativeFileURL(path: String): URL? = alClassLoader.getResource(path)
private fun getNativeFile(path: String): ByteArray = getNativeFileURL(path)?.readBytes() ?: error("Can't find '$path'")
private fun getNativeFileLocalPath(path: String): String {
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    //val tempFile = File.createTempFile("libopenal_", ".${File(path).extension}")
    val tempFile = File(tempDir, "korau_openal.${File(path).extension}")

    val expectedSize = getNativeFileURL(path)?.openStream()?.use { it.available().toLong() }

    if (!tempFile.exists() || tempFile.length() != expectedSize) {
        try {
            tempFile.writeBytes(getNativeFile(path))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return tempFile.absolutePath
}

internal inline fun <T> runCatchingAl(block: () -> T): T? {
    val result = runCatching { block() }
    if (result.isFailure) {
        result.exceptionOrNull()?.printStackTrace()
    }
    return result.getOrNull()
}
*/
*/
