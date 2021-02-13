package com.soywiz.korau.sound.impl.jna

import com.sun.jna.*
import java.nio.Buffer

@Suppress("unused")
interface AL : Library {
    fun alDopplerFactor(value: Float)
    fun alDopplerVelocity(value: Float)
    fun alSpeedOfSound(value: Float)
    fun alDistanceModel(distanceModel: Int)
    fun alEnable(capability: Int)
    fun alDisable(capability: Int)
    fun alIsEnabled(capability: Int): Boolean
    fun alGetString(param: Int): String
    fun alGetBooleanv(param: Int, values: BooleanArray)
    fun alGetIntegerv(param: Int, values: IntArray)
    fun alGetFloatv(param: Int, values: FloatArray)
    fun alGetDoublev(param: Int, values: DoubleArray)
    fun alGetBoolean(param: Int): Boolean
    fun alGetInteger(param: Int): Int
    fun alGetFloat(param: Int): Float
    fun alGetDouble(param: Int): Double
    fun alGetError(): Int
    fun alIsExtensionPresent(extname: String): Boolean
    fun alGetProcAddress(fname: String): Pointer
    fun alGetEnumValue(ename: String): Int
    fun alListenerf(param: Int, value: Float)
    fun alListener3f(param: Int, value1: Float, value2: Float, value3: Float)
    fun alListenerfv(param: Int, values: FloatArray)
    fun alListeneri(param: Int, value: Int)
    fun alListener3i(param: Int, value1: Int, value2: Int, value3: Int)
    fun alListeneriv(param: Int, values: IntArray)
    fun alGetListenerf(param: Int, value: FloatArray)
    fun alGetListener3f(param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    fun alGetListenerfv(param: Int, values: FloatArray)
    fun alGetListeneri(param: Int, value: IntArray)
    fun alGetListener3i(param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    fun alGetListeneriv(param: Int, values: IntArray)
    fun alGenSources(n: Int, sources: IntArray)
    fun alDeleteSources(n: Int, sources: IntArray)
    fun alIsSource(source: Int): Boolean
    fun alSourcef(source: Int, param: Int, value: Float)
    fun alSource3f(source: Int, param: Int, value1: Float, value2: Float, value3: Float)
    fun alSourcefv(source: Int, param: Int, values: FloatArray)
    fun alSourcei(source: Int, param: Int, value: Int)
    fun alSource3i(source: Int, param: Int, value1: Int, value2: Int, value3: Int)
    fun alSourceiv(source: Int, param: Int, values: IntArray)
    fun alGetSourcef(source: Int, param: Int, value: FloatArray)
    fun alGetSource3f(source: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    fun alGetSourcefv(source: Int, param: Int, values: FloatArray)
    fun alGetSourcei(source: Int, param: Int, value: IntArray)
    fun alGetSource3i(source: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    fun alGetSourceiv(source: Int, param: Int, values: IntArray)
    fun alSourcePlayv(n: Int, sources: IntArray)
    fun alSourceStopv(n: Int, sources: IntArray)
    fun alSourceRewindv(n: Int, sources: IntArray)
    fun alSourcePausev(n: Int, sources: IntArray)
    fun alSourcePlay(source: Int)
    fun alSourceStop(source: Int)
    fun alSourceRewind(source: Int)
    fun alSourcePause(source: Int)
    fun alSourceQueueBuffers(source: Int, nb: Int, buffers: IntArray)
    fun alSourceUnqueueBuffers(source: Int, nb: Int, buffers: IntArray)
    fun alGenBuffers(n: Int, buffers: IntArray)
    fun alDeleteBuffers(n: Int, buffers: IntArray)
    fun alIsBuffer(buffer: Int): Boolean
    fun alBufferData(buffer: Int, format: Int, data: Buffer?, size: Int, freq: Int)
    fun alBufferf(buffer: Int, param: Int, value: Float)
    fun alBuffer3f(buffer: Int, param: Int, value1: Float, value2: Float, value3: Float)
    fun alBufferfv(buffer: Int, param: Int, values: FloatArray)
    fun alBufferi(buffer: Int, param: Int, value: Int)
    fun alBuffer3i(buffer: Int, param: Int, value1: Int, value2: Int, value3: Int)
    fun alBufferiv(buffer: Int, param: Int, values: IntArray)
    fun alGetBufferf(buffer: Int, param: Int, value: FloatArray)
    fun alGetBuffer3f(buffer: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    fun alGetBufferfv(buffer: Int, param: Int, values: FloatArray)
    fun alGetBufferi(buffer: Int, param: Int, value: IntArray)
    fun alGetBuffer3i(buffer: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    fun alGetBufferiv(buffer: Int, param: Int, values: IntArray)

    companion object {
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
    }
}

object ALDummy : AL {
    override fun alDopplerFactor(value: Float) = Unit
    override fun alDopplerVelocity(value: Float) = Unit
    override fun alSpeedOfSound(value: Float) = Unit
    override fun alDistanceModel(distanceModel: Int) = Unit
    override fun alEnable(capability: Int) = Unit
    override fun alDisable(capability: Int) = Unit
    override fun alIsEnabled(capability: Int): Boolean = false
    override fun alGetString(param: Int): String = "ALDummy"
    override fun alGetBooleanv(param: Int, values: BooleanArray) = Unit
    override fun alGetIntegerv(param: Int, values: IntArray) = Unit
    override fun alGetFloatv(param: Int, values: FloatArray) = Unit
    override fun alGetDoublev(param: Int, values: DoubleArray) = Unit
    override fun alGetBoolean(param: Int): Boolean = false
    override fun alGetInteger(param: Int): Int = 0
    override fun alGetFloat(param: Int): Float = 0f
    override fun alGetDouble(param: Int): Double = 0.0
    override fun alGetError(): Int = 0
    override fun alIsExtensionPresent(extname: String): Boolean = false
    override fun alGetProcAddress(fname: String): Pointer = TODO()
    override fun alGetEnumValue(ename: String): Int = 0
    override fun alListenerf(param: Int, value: Float) = Unit
    override fun alListener3f(param: Int, value1: Float, value2: Float, value3: Float) = Unit
    override fun alListenerfv(param: Int, values: FloatArray) = Unit
    override fun alListeneri(param: Int, value: Int) = Unit
    override fun alListener3i(param: Int, value1: Int, value2: Int, value3: Int) = Unit
    override fun alListeneriv(param: Int, values: IntArray) = Unit
    override fun alGetListenerf(param: Int, value: FloatArray) = Unit
    override fun alGetListener3f(param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) = Unit
    override fun alGetListenerfv(param: Int, values: FloatArray) = Unit
    override fun alGetListeneri(param: Int, value: IntArray) = Unit
    override fun alGetListener3i(param: Int, value1: IntArray, value2: IntArray, value3: IntArray) = Unit
    override fun alGetListeneriv(param: Int, values: IntArray) = Unit
    override fun alGenSources(n: Int, sources: IntArray) = Unit
    override fun alDeleteSources(n: Int, sources: IntArray) = Unit
    override fun alIsSource(source: Int): Boolean = false
    override fun alSourcef(source: Int, param: Int, value: Float) = Unit
    override fun alSource3f(source: Int, param: Int, value1: Float, value2: Float, value3: Float) = Unit
    override fun alSourcefv(source: Int, param: Int, values: FloatArray) = Unit
    override fun alSourcei(source: Int, param: Int, value: Int) = Unit
    override fun alSource3i(source: Int, param: Int, value1: Int, value2: Int, value3: Int) = Unit
    override fun alSourceiv(source: Int, param: Int, values: IntArray) = Unit
    override fun alGetSourcef(source: Int, param: Int, value: FloatArray) = Unit
    override fun alGetSource3f(source: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) = Unit
    override fun alGetSourcefv(source: Int, param: Int, values: FloatArray) = Unit
    override fun alGetSourcei(source: Int, param: Int, value: IntArray) = Unit
    override fun alGetSource3i(source: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray) = Unit
    override fun alGetSourceiv(source: Int, param: Int, values: IntArray) = Unit
    override fun alSourcePlayv(n: Int, sources: IntArray) = Unit
    override fun alSourceStopv(n: Int, sources: IntArray) = Unit
    override fun alSourceRewindv(n: Int, sources: IntArray) = Unit
    override fun alSourcePausev(n: Int, sources: IntArray) = Unit
    override fun alSourcePlay(source: Int) = Unit
    override fun alSourceStop(source: Int) = Unit
    override fun alSourceRewind(source: Int) = Unit
    override fun alSourcePause(source: Int) = Unit
    override fun alSourceQueueBuffers(source: Int, nb: Int, buffers: IntArray) = Unit
    override fun alSourceUnqueueBuffers(source: Int, nb: Int, buffers: IntArray) = Unit
    override fun alGenBuffers(n: Int, buffers: IntArray) = Unit
    override fun alDeleteBuffers(n: Int, buffers: IntArray) = Unit
    override fun alIsBuffer(buffer: Int): Boolean = false
    override fun alBufferData(buffer: Int, format: Int, data: Buffer?, size: Int, freq: Int) = Unit
    override fun alBufferf(buffer: Int, param: Int, value: Float) = Unit
    override fun alBuffer3f(buffer: Int, param: Int, value1: Float, value2: Float, value3: Float) = Unit
    override fun alBufferfv(buffer: Int, param: Int, values: FloatArray) = Unit
    override fun alBufferi(buffer: Int, param: Int, value: Int) = Unit
    override fun alBuffer3i(buffer: Int, param: Int, value1: Int, value2: Int, value3: Int) = Unit
    override fun alBufferiv(buffer: Int, param: Int, values: IntArray) = Unit
    override fun alGetBufferf(buffer: Int, param: Int, value: FloatArray) = Unit
    override fun alGetBuffer3f(buffer: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray) = Unit
    override fun alGetBufferfv(buffer: Int, param: Int, values: FloatArray) = Unit
    override fun alGetBufferi(buffer: Int, param: Int, value: IntArray) = Unit
    override fun alGetBuffer3i(buffer: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray) = Unit
    override fun alGetBufferiv(buffer: Int, param: Int, values: IntArray) = Unit
}

internal val tempF = FloatArray(1)
internal val tempI = IntArray(1)

fun AL.alGenBuffer(): Int = tempI.apply { al.alGenBuffers(1, this) }[0]
fun AL.alGenSource(): Int = tempI.apply { al.alGenSources(1, this) }[0]
fun AL.alDeleteBuffer(buffer: Int): Unit = run { al.alDeleteBuffers(1, tempI.also { it[0] = buffer }) }
fun AL.alDeleteSource(buffer: Int): Unit = run { al.alDeleteSources(1, tempI.also { it[0] = buffer }) }
fun AL.alGetSourcef(source: Int, param: Int): Float = tempF.apply { al.alGetSourcef(source, param, this) }[0]
fun AL.alGetSourcei(source: Int, param: Int): Int = tempI.apply { al.alGetSourcei(source, param, this) }[0]
fun AL.alGetSourceState(source: Int): Int = alGetSourcei(source, AL.AL_SOURCE_STATE)

@Suppress("unused")
object ALC {
    @JvmStatic external fun alcCreateContext(device: Pointer, attrlist: IntArray?): Pointer
    @JvmStatic external fun alcMakeContextCurrent(context: Pointer): Boolean
    @JvmStatic external fun alcProcessContext(context: Pointer)
    @JvmStatic external fun alcSuspendContext(context: Pointer)
    @JvmStatic external fun alcDestroyContext(context: Pointer)
    @JvmStatic external fun alcGetCurrentContext(): Pointer
    @JvmStatic external fun alcGetContextsDevice(context: Pointer): Pointer
    @JvmStatic external fun alcOpenDevice(devicename: String?): Pointer
    @JvmStatic external fun alcCloseDevice(device: Pointer): Boolean
    @JvmStatic external fun alcGetError(device: Pointer): Int
    @JvmStatic external fun alcIsExtensionPresent(device: Pointer, extname: String): Boolean
    @JvmStatic external fun alcGetProcAddress(device: Pointer, funcname: String): Pointer
    @JvmStatic external fun alcGetEnumValue(device: Pointer, enumname: String): Int
    @JvmStatic external fun alcGetString(device: Pointer, param: Int): String
    @JvmStatic external fun alcGetIntegerv(device: Pointer, param: Int, size: Int, values: IntArray)
    @JvmStatic external fun alcCaptureOpenDevice(devicename: String, frequency: Int, format: Int, buffersize: Int): Pointer
    @JvmStatic external fun alcCaptureCloseDevice(device: Pointer): Boolean
    @JvmStatic external fun alcCaptureStart(device: Pointer)
    @JvmStatic external fun alcCaptureStop(device: Pointer)
    @JvmStatic external fun alcCaptureSamples(device: Pointer, buffer: Buffer, samples: Int)
    private var loaded = false

    init {
        try {
            Native.register(nativeOpenALLibraryPath)
            loaded = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    val instance get() = if (loaded) ALC else null

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

val alc: ALC? by lazy {
    ALC.instance
}

internal inline fun <T> runCatchingAl(block: () -> T): T? {
    val result = runCatching { block() }
    if (result.isFailure) {
        result.exceptionOrNull()?.printStackTrace()
    }
    return result.getOrNull()
}
