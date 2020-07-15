package com.soywiz.korau.sound.impl.jogamp

/*
import com.jogamp.openal.*
import com.jogamp.openal.util.*
import com.soywiz.klock.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.*
import java.nio.*
import kotlin.coroutines.*

internal inline fun <T> runCatchingAl(block: () -> T): T? {
    val result = runCatching { block() }
    if (result.isFailure) {
        result.exceptionOrNull()?.printStackTrace()
    }
    return result.getOrNull()
}

val al: AL? by lazy {
    runCatchingAl {
        ALFactory.getAL().also { al ->
            //val error = al.alGetError()
            //if (error != AL.AL_NO_ERROR) error("Error initializing OpenAL ${error.shex}")
        }
    }
}

class JogampNativeSoundProvider : NativeSoundProvider() {
    init {
        //println("ALut.alutInit: ${Thread.currentThread()}")
        runCatchingAl {
            ALut.alutInit()
        }
        //alc.alcMakeContextCurrent(context)
        al?.alListener3f(AL.AL_POSITION, 0f, 0f, 1.0f)
        checkAlErrors()
        al?.alListener3f(AL.AL_VELOCITY, 0f, 0f, 0f)
        checkAlErrors()
        al?.alListenerfv(AL.AL_ORIENTATION, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f), 0)
        checkAlErrors()
    }

    override suspend fun createSound(data: ByteArray, streaming: Boolean): NativeSound {
        return OpenALNativeSoundNoStream(coroutineContext, nativeAudioFormats.decode(data))
    }

    override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean): NativeSound {
        return super.createSound(vfs, path, streaming)
    }

    override suspend fun createSound(data: AudioData, formats: AudioFormats, streaming: Boolean): NativeSound {
        return super.createSound(data, formats, streaming)
    }
}

// https://ffainelli.github.io/openal-example/
class OpenALNativeSoundNoStream(val coroutineContext: CoroutineContext, val data: AudioData?) : NativeSound() {
    override suspend fun decode(): AudioData = data ?: AudioData.DUMMY

    override fun play(): NativeSoundChannel {
        //if (openalNativeSoundProvider.device == null || openalNativeSoundProvider.context == null) return DummyNativeSoundChannel(this, data)
        //println("OpenALNativeSoundNoStream.play : $data")
        //alc.alcMakeContextCurrent(context)
        val data = data ?: return DummyNativeSoundChannel(this)

        val buffer = alGenBuffer()
        alBufferData(buffer, data)

        val source = alGenSource()
        al?.alSourcef(source, AL.AL_PITCH, 1f)
        al?.alSourcef(source, AL.AL_GAIN, 1f)
        al?.alSource3f(source, AL.AL_POSITION, 0f, 0f, 0f)
        al?.alSource3f(source, AL.AL_VELOCITY, 0f, 0f, 0f)
        al?.alSourcei(source, AL.AL_LOOPING, AL.AL_FALSE)
        al?.alSourcei(source, AL.AL_BUFFER, buffer)
        checkAlErrors()

        al?.alSourcePlay(source)
        checkAlErrors()

        var stopped = false

        val channel = object : NativeSoundChannel(this) {
            val totalSamples get() = data.totalSamples
            val currentSampleOffset get() = alGetSourcei(source, AL.AL_SAMPLE_OFFSET)

            override var volume: Double
                get() = run { alGetSourcef(source, AL.AL_GAIN).toDouble() }
                set(value) = run { al?.alSourcef(source, AL.AL_GAIN, value.toFloat()) }
            override var pitch: Double
                get() = run { alGetSourcef(source, AL.AL_PITCH).toDouble() }
                set(value) = run { al?.alSourcef(source, AL.AL_PITCH, value.toFloat()) }
            override var panning: Double = 0.0
                set(value) = run {
                    field = value
                    al?.alSource3f(source, AL.AL_POSITION, panning.toFloat(), 0f, 0f)
                }

            override val current: TimeSpan get() = data.timeAtSample(currentSampleOffset)
            override val total: TimeSpan get() = data.totalTime
            override val playing: Boolean get() {
                val result = alGetSourceState(source) == AL.AL_PLAYING
                checkAlErrors()
                return result
            }

            override fun stop() {
                if (!stopped) {
                    stopped = true
                    alDeleteSource(source)
                    alDeleteBuffer(buffer)
                }
            }
        }
        launchImmediately(coroutineContext[ContinuationInterceptor] ?: coroutineContext) {
            try {
                do {
                    delay(1L)
                } while (channel.playing)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                channel.stop()
            }
        }
        return channel

    }
}

private val tempF = FloatArray(1)
private val tempI = IntArray(1)
private fun alGetSourcef(source: Int, param: Int): Float = tempF.apply { al?.alGetSourcef(source, param, this, 0) }[0]
private fun alGetSourcei(source: Int, param: Int): Int = tempI.apply { al?.alGetSourcei(source, param, this, 0) }[0]
private fun alGetSourceState(source: Int): Int = alGetSourcei(source, AL.AL_SOURCE_STATE)

private fun alBufferData(buffer: Int, data: AudioData) {
    val samples = data.samplesInterleaved.data

    val bufferData = ShortBuffer.wrap(samples)
    //val bufferData = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
    //bufferData.asShortBuffer().put(samples)

    al?.alBufferData(
        buffer,
        if (data.channels == 1) AL.AL_FORMAT_MONO16 else AL.AL_FORMAT_STEREO16,
        if (samples.isNotEmpty()) bufferData else null,
        samples.size * 2,
        data.rate
    )
    checkAlErrors()
}

private fun alGenBuffer(): Int = tempI.apply { al?.alGenBuffers(1, this, 0) }[0]
private fun alGenSource(): Int = tempI.apply { al?.alGenSources(1, this, 0) }[0]
private fun alDeleteBuffer(buffer: Int): Unit = run { al?.alDeleteBuffers(1, tempI.also { it[0] = buffer }, 0) }
private fun alDeleteSource(buffer: Int): Unit = run { al?.alDeleteSources(1, tempI.also { it[0] = buffer }, 0) }

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

fun checkAlErrors() {
//    val error = al.alGetError()
//    if (error != AL.AL_NO_ERROR) error("OpenAL error ${error.shex}")
}
*/
