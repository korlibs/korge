package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import kotlinx.cinterop.*
//import mystdio.*
import platform.AudioToolbox.*
import platform.CoreAudioTypes.*
import platform.CoreFoundation.*
import platform.darwin.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider get() = CORE_AUDIO_NATIVE_SOUND_PROVIDER
val CORE_AUDIO_NATIVE_SOUND_PROVIDER: CoreAudioNativeSoundProvider by lazy { CoreAudioNativeSoundProvider() }

class CoreAudioNativeSoundProvider : NativeSoundProvider() {
    init {
        appleInitAudio()
    }

    override val audioFormats: AudioFormats = AudioFormats(WAV, com.soywiz.korau.format.mp3.MP3Decoder, NativeOggVorbisDecoderFormat)

    //override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps): NativeSound = AVFoundationNativeSoundNoStream(CoroutineScope(coroutineContext), audioFormats.decode(data))

    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = CoreAudioPlatformAudioOutput(coroutineContext, freq)
}

class CoreAudioPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    freq: Int
) : DequeBasedPlatformAudioOutput(coroutineContext, freq) {
    val generator = CoreAudioGenerator(freq, nchannels) { data, dataSize ->
        for (n in 0 until dataSize / nchannels) {
            for (m in 0 until nchannels) data[n * nchannels + m] = readShort(m)
        }
    }
    override fun start() {
        generator.start()
    }
    override fun stop() {
        generator.dispose()
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////

// https://github.com/spurious/SDL-mirror/blob/master/src/audio/coreaudio/SDL_coreaudio.m
// https://gist.github.com/soywiz/b4f91d1e99e4ff61ea674b777c31f289
interface MyCoreAudioOutputCallback {
    fun generateOutput(data: CPointer<ShortVar>, dataSize: Int)
}

private fun coreAudioOutputCallback(custom_data: COpaquePointer?, queue: AudioQueueRef?, buffer: AudioQueueBufferRef?) {
    val output = custom_data?.asStableRef<MyCoreAudioOutputCallback>() ?: return println("outputCallback null[0]")
    val buf = buffer?.pointed ?: return println("outputCallback null[1]")
    val dat = buf.mAudioDataByteSize.toInt() / Short.SIZE_BYTES
    val shortBuf = buf.mAudioData?.reinterpret<ShortVar>() ?: return println("outputCallback null[2]")
    output.get().generateOutput(shortBuf, dat)
    AudioQueueEnqueueBuffer(queue, buffer, 0.convert(), null).checkError("AudioQueueEnqueueBuffer")
}

private fun OSStatus.checkError(name: String): OSStatus {
    if (this != 0) println("ERROR: $name")
    return this
}

class CoreAudioGenerator(
    val sampleRate: Int,
    val nchannels: Int,
    val numBuffers: Int = 3,
    val bufferSize: Int = 4096,
    val generatorCore: CoreAudioGenerator.(data: CPointer<ShortVar>, dataSize: Int) -> Unit
) : MyCoreAudioOutputCallback {
    val arena = Arena()
    var queue: CPointerVarOf<CPointer<OpaqueAudioQueue>>? = null
    var buffers: CPointer<CPointerVarOf<CPointer<AudioQueueBuffer>>>? = null
    var running = false

    var thisStableRef: StableRef<MyCoreAudioOutputCallback>? = null

    fun start() {
        if (running) return
        running = true

        queue = arena.alloc()
        buffers = arena.allocArray(numBuffers)
        thisStableRef = StableRef.create(this)
        memScoped {
            val format = alloc<AudioStreamBasicDescription>()
            format.mSampleRate = sampleRate.toDouble()
            format.mFormatID = kAudioFormatLinearPCM;
            format.mFormatFlags = kLinearPCMFormatFlagIsSignedInteger or kAudioFormatFlagIsPacked;
            format.mBitsPerChannel = (8 * Short.SIZE_BYTES).convert();
            format.mChannelsPerFrame = nchannels.convert()
            format.mBytesPerFrame = (Short.SIZE_BYTES * format.mChannelsPerFrame.toInt()).convert()
            format.mFramesPerPacket = 1.convert()
            format.mBytesPerPacket = format.mBytesPerFrame * format.mFramesPerPacket
            format.mReserved = 0.convert()

            AudioQueueNewOutput(
                format.ptr, staticCFunction(::coreAudioOutputCallback), thisStableRef!!.asCPointer(),
                CFRunLoopGetCurrent(), kCFRunLoopCommonModes, 0.convert(), queue!!.ptr
            ).also {
                if (it != 0) error("Error in AudioQueueNewOutput")
            }
        }

        for (n in 0 until numBuffers) {
            AudioQueueAllocateBuffer(queue!!.value, bufferSize.convert(), buffers + n).checkError("AudioQueueAllocateBuffer")
            buffers!![n]!!.pointed.mAudioDataByteSize = bufferSize.convert()
            //println(buffers[n])
            //AudioQueueEnqueueBuffer(queue.value, buffers[n], 0.convert(), null).also { println("AudioQueueEnqueueBuffer: $it") }

            //callback()
            coreAudioOutputCallback(thisStableRef!!.asCPointer(), queue!!.value, buffers!![n])
        }

        AudioQueueStart(queue!!.value, null)
    }

    fun dispose() {
        if (running) {
            if (queue != null) {
                AudioQueueStop(queue!!.value, false).checkError("AudioQueueStop")
                //for (n in 0 until NUM_BUFFERS) AudioQueueFreeBuffer(queue.value, buffers[n]).also { println("AudioQueueFreeBuffer: $it") }
                AudioQueueDispose(queue!!.value, true).checkError("AudioQueueDispose")
            }
            queue = null
            thisStableRef?.dispose()
            thisStableRef = null
            arena.clear()
            running = false
        }
    }

    override fun generateOutput(data: CPointer<ShortVar>, dataSize: Int) = generatorCore(this, data, dataSize)
}
