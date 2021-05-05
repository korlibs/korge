package com.soywiz.korau.sound.impl.jna

import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.time.*
import com.soywiz.korio.util.*
import com.sun.jna.*
import java.io.*
import java.net.*
import java.nio.Buffer
import kotlin.printStackTrace

@Suppress("unused")
object AL {
    @JvmStatic external fun alDopplerFactor(value: Float)
    @JvmStatic external fun alDopplerVelocity(value: Float)
    @JvmStatic external fun alSpeedOfSound(value: Float)
    @JvmStatic external fun alDistanceModel(distanceModel: Int)
    @JvmStatic external fun alEnable(capability: Int)
    @JvmStatic external fun alDisable(capability: Int)
    @JvmStatic external fun alIsEnabled(capability: Int): Boolean
    @JvmStatic external fun alGetString(param: Int): String
    @JvmStatic external fun alGetBooleanv(param: Int, values: BooleanArray)
    @JvmStatic external fun alGetIntegerv(param: Int, values: IntArray)
    @JvmStatic external fun alGetFloatv(param: Int, values: FloatArray)
    @JvmStatic external fun alGetDoublev(param: Int, values: DoubleArray)
    @JvmStatic external fun alGetBoolean(param: Int): Boolean
    @JvmStatic external fun alGetInteger(param: Int): Int
    @JvmStatic external fun alGetFloat(param: Int): Float
    @JvmStatic external fun alGetDouble(param: Int): Double
    @JvmStatic external fun alGetError(): Int
    @JvmStatic external fun alIsExtensionPresent(extname: String): Boolean
    @JvmStatic external fun alGetProcAddress(fname: String): Pointer
    @JvmStatic external fun alGetEnumValue(ename: String): Int
    @JvmStatic external fun alListenerf(param: Int, value: Float)
    @JvmStatic external fun alListener3f(param: Int, value1: Float, value2: Float, value3: Float)
    @JvmStatic external fun alListenerfv(param: Int, values: FloatArray)
    @JvmStatic external fun alListeneri(param: Int, value: Int)
    @JvmStatic external fun alListener3i(param: Int, value1: Int, value2: Int, value3: Int)
    @JvmStatic external fun alListeneriv(param: Int, values: IntArray)
    @JvmStatic external fun alGetListenerf(param: Int, value: FloatArray)
    @JvmStatic external fun alGetListener3f(param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    @JvmStatic external fun alGetListenerfv(param: Int, values: FloatArray)
    @JvmStatic external fun alGetListeneri(param: Int, value: IntArray)
    @JvmStatic external fun alGetListener3i(param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    @JvmStatic external fun alGetListeneriv(param: Int, values: IntArray)
    @JvmStatic external fun alGenSources(n: Int, sources: IntArray)
    @JvmStatic external fun alDeleteSources(n: Int, sources: IntArray)
    @JvmStatic external fun alIsSource(source: Int): Boolean
    @JvmStatic external fun alSourcef(source: Int, param: Int, value: Float)
    @JvmStatic external fun alSource3f(source: Int, param: Int, value1: Float, value2: Float, value3: Float)
    @JvmStatic external fun alSourcefv(source: Int, param: Int, values: FloatArray)
    @JvmStatic external fun alSourcei(source: Int, param: Int, value: Int)
    @JvmStatic external fun alSource3i(source: Int, param: Int, value1: Int, value2: Int, value3: Int)
    @JvmStatic external fun alSourceiv(source: Int, param: Int, values: IntArray)
    @JvmStatic external fun alGetSourcef(source: Int, param: Int, value: FloatArray)
    @JvmStatic external fun alGetSource3f(source: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    @JvmStatic external fun alGetSourcefv(source: Int, param: Int, values: FloatArray)
    @JvmStatic external fun alGetSourcei(source: Int, param: Int, value: IntArray)
    @JvmStatic external fun alGetSource3i(source: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    @JvmStatic external fun alGetSourceiv(source: Int, param: Int, values: IntArray)
    @JvmStatic external fun alSourcePlayv(n: Int, sources: IntArray)
    @JvmStatic external fun alSourceStopv(n: Int, sources: IntArray)
    @JvmStatic external fun alSourceRewindv(n: Int, sources: IntArray)
    @JvmStatic external fun alSourcePausev(n: Int, sources: IntArray)
    @JvmStatic external fun alSourcePlay(source: Int)
    @JvmStatic external fun alSourceStop(source: Int)
    @JvmStatic external fun alSourceRewind(source: Int)
    @JvmStatic external fun alSourcePause(source: Int)
    @JvmStatic external fun alSourceQueueBuffers(source: Int, nb: Int, buffers: IntArray)
    @JvmStatic external fun alSourceUnqueueBuffers(source: Int, nb: Int, buffers: IntArray)
    @JvmStatic external fun alGenBuffers(n: Int, buffers: IntArray)
    @JvmStatic external fun alDeleteBuffers(n: Int, buffers: IntArray)
    @JvmStatic external fun alIsBuffer(buffer: Int): Boolean
    @JvmStatic external fun alBufferData(buffer: Int, format: Int, data: Buffer?, size: Int, freq: Int)
    @JvmStatic external fun alBufferf(buffer: Int, param: Int, value: Float)
    @JvmStatic external fun alBuffer3f(buffer: Int, param: Int, value1: Float, value2: Float, value3: Float)
    @JvmStatic external fun alBufferfv(buffer: Int, param: Int, values: FloatArray)
    @JvmStatic external fun alBufferi(buffer: Int, param: Int, value: Int)
    @JvmStatic external fun alBuffer3i(buffer: Int, param: Int, value1: Int, value2: Int, value3: Int)
    @JvmStatic external fun alBufferiv(buffer: Int, param: Int, values: IntArray)
    @JvmStatic external fun alGetBufferf(buffer: Int, param: Int, value: FloatArray)
    @JvmStatic external fun alGetBuffer3f(buffer: Int, param: Int, value1: FloatArray, value2: FloatArray, value3: FloatArray)
    @JvmStatic external fun alGetBufferfv(buffer: Int, param: Int, values: FloatArray)
    @JvmStatic external fun alGetBufferi(buffer: Int, param: Int, value: IntArray)
    @JvmStatic external fun alGetBuffer3i(buffer: Int, param: Int, value1: IntArray, value2: IntArray, value3: IntArray)
    @JvmStatic external fun alGetBufferiv(buffer: Int, param: Int, values: IntArray)

    private val tempF = FloatArray(1)
    private val tempI = IntArray(1)

    fun alGenBuffer(): Int = tempI.also { alGenBuffers(1, it) }[0]
    fun alGenSource(): Int = tempI.also { alGenSources(1, it) }[0]
    fun alDeleteBuffer(buffer: Int): Unit { alDeleteBuffers(1, tempI.also { it[0] = buffer }) }
    fun alDeleteSource(buffer: Int): Unit { alDeleteSources(1, tempI.also { it[0] = buffer }) }
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

    @JvmStatic external fun alcCreateContext(device: Pointer, attrlist: IntArray?): Pointer?
    @JvmStatic external fun alcMakeContextCurrent(context: Pointer): Boolean
    @JvmStatic external fun alcProcessContext(context: Pointer)
    @JvmStatic external fun alcSuspendContext(context: Pointer)
    @JvmStatic external fun alcDestroyContext(context: Pointer)
    @JvmStatic external fun alcGetCurrentContext(): Pointer
    @JvmStatic external fun alcGetContextsDevice(context: Pointer): Pointer
    @JvmStatic external fun alcOpenDevice(devicename: String?): Pointer?
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

    internal var loaded = false

    init {
        try {
            if (nativeOpenALLibraryPath == null) error("Can't get OpenAL library")
            traceTime("OpenAL Native.register") {
                Native.register(nativeOpenALLibraryPath)
            }
            loaded = true
        } catch (e: Throwable) {
            com.soywiz.klogger.Console.error("Failed to initialize OpenAL: arch=$arch, OS.rawName=${OS.rawName}, nativeOpenALLibraryPath=$nativeOpenALLibraryPath, message=${e.message}")
            //e.printStackTrace()
        }
    }
}

val nativeOpenALLibraryPath: String? by lazy {
    if (Environment["KORAU_JVM_DUMMY_SOUND"] == "true") {
        return@lazy null
    }
    when {
        OS.isMac -> {
            //getNativeFileLocalPath("natives/macosx64/libopenal.dylib")
            "OpenAL" // Mac already includes the OpenAL library
        }
        OS.isLinux -> {
            when {
                arch.contains("arm") -> getNativeFileLocalPath("natives/linuxarm/libopenal.so")
                arch.contains("64") -> getNativeFileLocalPath("natives/linuxx64/libopenal.so")
                else -> getNativeFileLocalPath("natives/linuxx86/libopenal.so")
            }
        }
        OS.isWindows -> {
            when {
                arch.contains("64") -> getNativeFileLocalPath("natives/winx64/soft_oal.dll")
                else -> getNativeFileLocalPath("natives/winx86/soft_oal.dll")
            }
        }
        else -> {
            println("  - Unknown/Unsupported OS")
            null
        }
    }
}

private val arch by lazy { System.getProperty("os.arch").toLowerCase() }
private val alClassLoader by lazy { AL::class.java.classLoader }
private fun getNativeFileURL(path: String): URL? = alClassLoader.getResource(path)
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
