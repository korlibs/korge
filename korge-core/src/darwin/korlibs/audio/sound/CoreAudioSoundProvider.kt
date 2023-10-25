package korlibs.audio.sound

//import mystdio.*
import cnames.structs.OpaqueAudioQueue
import korlibs.memory.*
import kotlinx.cinterop.*
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.convert
import platform.AudioToolbox.*
import platform.AudioToolbox.AudioQueueBufferRef
import platform.AudioToolbox.AudioQueueRef
import platform.CoreAudioTypes.*
import platform.darwin.OSStatus
import platform.posix.*
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider get() = CORE_AUDIO_NATIVE_SOUND_PROVIDER
expect fun appleInitAudio()

val CORE_AUDIO_NATIVE_SOUND_PROVIDER: CoreAudioNativeSoundProvider by lazy { CoreAudioNativeSoundProvider() }

class CoreAudioNativeSoundProvider : NativeSoundProviderNew() {
    init {
        appleInitAudio()
    }

    //override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps): NativeSound = AVFoundationNativeSoundNoStream(CoroutineScope(coroutineContext), audioFormats.decode(data))

    override fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int, gen: (AudioSamplesInterleaved) -> Unit): CoreAudioNewPlatformAudioOutput = CoreAudioNewPlatformAudioOutput(coroutineContext, frequency, channels, gen)
}

class CoreAudioNewPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    freq: Int,
    nchannels: Int,
    gen: (AudioSamplesInterleaved) -> Unit,
) : NewPlatformAudioOutput(coroutineContext, nchannels, freq, gen) {
    //private var samples: AudioSamplesInterleaved? = null

    val generator = CoreAudioGenerator(freq, nchannels, coroutineContext = coroutineContext) { data, dataSize ->
        val totalSamples = dataSize / nchannels
        //if (samples == null || samples!!.totalSamples != totalSamples || samples!!.channels != channels) {
        //}
        val samples = AudioSamplesInterleaved(nchannels, totalSamples)
        genSafe(samples)
        samples.data.usePinned {
            memcpy(data, it.startAddressOf, (dataSize * Short.SIZE_BYTES).convert())
        }
    }
    override fun internalStart() {
        generator.start()
    }
    override fun internalStop() {
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
    initRuntimeIfNeeded()
    val output = custom_data?.asStableRef<MyCoreAudioOutputCallback>() ?: return println("outputCallback null[0]")
    val buf = buffer?.pointed ?: return println("outputCallback null[1]")
    val dat = buf.mAudioDataByteSize.toInt() / Short.SIZE_BYTES
    val shortBuf = buf.mAudioData?.reinterpret<ShortVar>() ?: return println("outputCallback null[2]")
    output.get().generateOutput(shortBuf, dat)
    AudioQueueEnqueueBuffer(queue, buffer, 0.convert(), null).checkError("AudioQueueEnqueueBuffer")
}
fun osStatusToString(status: OSStatus): String = when (status) {
    kAudioQueueErr_InvalidBuffer -> "InvalidBuffer"
    kAudioQueueErr_BufferEmpty -> "BufferEmpty"
    kAudioQueueErr_DisposalPending -> "DisposalPending"
    kAudioQueueErr_InvalidProperty -> "InvalidProperty"
    kAudioQueueErr_InvalidPropertySize -> "InvalidPropertySize"
    kAudioQueueErr_InvalidParameter -> "InvalidParameter"
    kAudioQueueErr_CannotStart -> "CannotStart"
    kAudioQueueErr_InvalidDevice -> "InvalidDevice"
    kAudioQueueErr_BufferInQueue -> "BufferInQueue"
    kAudioQueueErr_InvalidRunState -> "InvalidRunState"
    kAudioQueueErr_InvalidQueueType -> "InvalidQueueType"
    kAudioQueueErr_Permissions -> "Permissions"
    kAudioQueueErr_InvalidPropertyValue -> "InvalidPropertyValue"
    kAudioQueueErr_PrimeTimedOut -> "PrimeTimedOut"
    kAudioQueueErr_CodecNotFound -> "CodecNotFound"
    kAudioQueueErr_InvalidCodecAccess -> "InvalidCodecAccess"
    kAudioQueueErr_QueueInvalidated -> "QueueInvalidated"
    kAudioQueueErr_RecordUnderrun -> "RecordUnderrun"
    kAudioQueueErr_EnqueueDuringReset -> "EnqueueDuringReset"
    kAudioQueueErr_InvalidOfflineMode -> "InvalidOfflineMode"
    kAudioFormatUnsupportedDataFormatError -> "UnsupportedDataFormatError"
    else -> "Unknown$status"
}

private fun OSStatus.checkError(name: String): OSStatus {
    if (this != 0) println("ERROR: $name ($this)(${osStatusToString(this)})")
    return this
}

class CoreAudioGenerator(
    val sampleRate: Int,
    val nchannels: Int,
    val numBuffers: Int = 3,
    val bufferSize: Int = 4096,
    val coroutineContext: CoroutineContext,
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
                null, null, 0.convert(), queue!!.ptr
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

        //println("AudioQueueStart")
        AudioQueueStart(queue!!.value, null)
    }

    fun dispose() {
        if (running) {
            if (queue != null) {
                //println("AudioQueueFlush/AudioQueueStop/AudioQueueDispose")
                //AudioQueueFlush(queue!!.value).checkError("AudioQueueFlush")
                //AudioQueueStop(queue!!.value, false).checkError("AudioQueueStop")
                //for (n in 0 until NUM_BUFFERS) AudioQueueFreeBuffer(queue.value, buffers[n]).also { println("AudioQueueFreeBuffer: $it") }
                AudioQueueDispose(queue!!.value, false).checkError("AudioQueueDispose")
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
