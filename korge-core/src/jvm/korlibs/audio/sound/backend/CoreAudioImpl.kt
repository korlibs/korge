package korlibs.audio.sound.backend

import com.sun.jna.*
import korlibs.audio.sound.*
import korlibs.ffi.*
import korlibs.io.annotations.*
import korlibs.memory.dyn.*
import korlibs.memory.dyn.osx.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

val jvmCoreAudioNativeSoundProvider: JvmCoreAudioNativeSoundProvider? by lazy {
    try {
        JvmCoreAudioNativeSoundProvider()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

class JvmCoreAudioNativeSoundProvider : NativeSoundProvider() {
    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = JvmCoreAudioPlatformAudioOutput(coroutineContext, freq)
}

private val audioOutputsById = ConcurrentHashMap<Int, JvmCoreAudioPlatformAudioOutput>()

private val cti by lazy { CallbackThreadInitializer() }
private val jnaCoreAudioCallback by lazy {
    AudioQueueNewOutputCallback { inUserData, inAQ, inBuffer ->
        val output = audioOutputsById[(inUserData?.address ?: 0L).toInt()] ?: return@AudioQueueNewOutputCallback 0

        //val tone = AudioTone.generate(1.seconds, 41000.0)
        val queue = AudioQueueBuffer(inBuffer)
        val ptr = queue.mAudioData
        val samplesCount = (queue.mAudioDataByteSize / Short.SIZE_BYTES) / 2

        if (output.left.size != samplesCount) output.left = ShortArray(samplesCount)
        if (output.right.size != samplesCount) output.right = ShortArray(samplesCount)

        val left: ShortArray = output.left
        val right: ShortArray = output.right

        //val availableRead = this@JvmCoreAudioPlatformAudioOutput.availableRead
        output._readShorts(0, left)
        output._readShorts(1, right)

        //println("callback: availableRead=$availableRead, completed=$completed, inUserData=$inUserData, inAQ=$inAQ, inBuffer=$inBuffer, thread=${Thread.currentThread()}")

        //println(queue.mAudioDataByteSize)
        if (ptr != null) {
            for (n in 0 until samplesCount) {
                ptr[n * DequeBasedPlatformAudioOutput.nchannels + 0] = left[n]
                ptr[n * DequeBasedPlatformAudioOutput.nchannels + 1] = right[n]
            }
        }
        //println("queue.mAudioData=${queue.mAudioData}")

        if (!output.completed) {
            CoreAudioKit.AudioQueueEnqueueBuffer(inAQ, queue.ptr, 0, null).also {
                if (it != 0) println("CoreAudioKit.AudioQueueEnqueueBuffer -> $it")
            }
        } else {
            Unit
            //println("COMPLETED!")
        }

        //initRuntimeIfNeeded()
        //val output = custom_data?.asStableRef<MyCoreAudioOutputCallback>() ?: return println("outputCallback null[0]")
        //val buf = buffer?.pointed ?: return println("outputCallback null[1]")
        //val dat = buf.mAudioDataByteSize.toInt() / Short.SIZE_BYTES
        //val shortBuf = buf.mAudioData?.reinterpret<ShortVar>() ?: return println("outputCallback null[2]")
        //output.get().generateOutput(shortBuf, dat)
        //AudioQueueEnqueueBuffer(queue, buffer, 0.convert(), null).checkError("AudioQueueEnqueueBuffer")
        0
    }.also {
        Native.setCallbackThreadInitializer(it, cti)
    }
}

private class JvmCoreAudioPlatformAudioOutput(
    coroutineContext: CoroutineContext,
    frequency: Int
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    val id = lastId.incrementAndGet()
    companion object {
        private var lastId = AtomicInteger(0)
        const val bufferSizeInBytes = 2048
        const val numBuffers = 3
    }

    init {
        audioOutputsById[id] = this
    }

    internal var completed = false

    var queue: Pointer? = null

    var left: ShortArray = ShortArray(0)
    var right: ShortArray = ShortArray(0)

    internal fun _readShorts(channel: Int, out: ShortArray, offset: Int = 0, count: Int = out.size - offset) {
        readShorts(channel, out, offset, count)
    }

    override fun start() {
        completed = false
        val queueRef = Memory(16).also { it.clear() }
        val format = AudioStreamBasicDescription(Memory(40).also { it.clear() })

        format.mSampleRate = frequency.toDouble()
        format.mFormatID = CoreAudioKit.kAudioFormatLinearPCM
        format.mFormatFlags = CoreAudioKit.kLinearPCMFormatFlagIsSignedInteger or CoreAudioKit.kAudioFormatFlagIsPacked
        format.mBitsPerChannel = (8 * Short.SIZE_BYTES)
        format.mChannelsPerFrame = nchannels
        format.mBytesPerFrame = (Short.SIZE_BYTES * format.mChannelsPerFrame)
        format.mFramesPerPacket = 1
        format.mBytesPerPacket = format.mBytesPerFrame * format.mFramesPerPacket
        format.mReserved = 0

        val userDefinedPtr = Pointer(id.toLong())

        CoreAudioKit.AudioQueueNewOutput(
            format.ptr, jnaCoreAudioCallback, userDefinedPtr,
            //CoreFoundation.CFRunLoopGetCurrent(),
            CoreFoundation.CFRunLoopGetMain(),
            CoreFoundation.kCFRunLoopCommonModes, 0, queueRef
        ).also {
            if (it != 0) println("CoreAudioKit.AudioQueueNewOutput -> $it")
        }
        queue = queueRef.getPointer(0L)
        //println("result=$result, queue=$queue")
        val buffersArray = Memory((8 * numBuffers).toLong()).also { it.clear() }
        for (buf in 0 until numBuffers) {
            val bufferPtr = Pointer(buffersArray.address + 8 * buf)
            CoreAudioKit.AudioQueueAllocateBuffer(queue, bufferSizeInBytes, bufferPtr).also {
                if (it != 0) println("CoreAudioKit.AudioQueueAllocateBuffer -> $it")
            }
            val ptr = AudioQueueBuffer(bufferPtr.getPointer(0))
            //println("AudioQueueAllocateBuffer=$res, ptr.pointer=${ptr.pointer}")
            ptr.mAudioDataByteSize = bufferSizeInBytes
            jnaCoreAudioCallback.callback(userDefinedPtr, queue, ptr.ptr)
        }
        CoreAudioKit.AudioQueueStart(queue, null).also {
            if (it != 0) println("CoreAudioKit.AudioQueueStart -> $it")
        }

    }

    override fun stop() {
        completed = true
        CoreAudioKit.AudioQueueDispose(queue, false)
        audioOutputsById.remove(id)
    }
}


private class AudioQueueBuffer(p: FFIPointer? = null) : FFIStructure(p) {
    var mAudioDataBytesCapacity by int()
    var mAudioData by pointer<Short>()
    var mAudioDataByteSize by int()
    var mUserData by pointer<Byte>()
    var mPacketDescriptionCapacity by pointer<Int>()
    //const UInt32                    mPacketDescriptionCapacity;
    //AudioStreamPacketDescription * const __nullable mPacketDescriptions;
    //UInt32                          mPacketDescriptionCount;
}
//private class AudioTimeStamp(p: Pointer? = null) : KStructure(p) {
//    var mSampleTime by double()
//    var mHostTime by long()
//    var mRateScalar by double()
//    var mWordClockTime by long()
//    //SMPTETime           mSMPTETime;
//    //AudioTimeStampFlags mFlags;
//    //UInt32              mReserved;
//}

private class AudioStreamBasicDescription(p: FFIPointer? = null) : FFIStructure(p) {
    var mSampleRate by double()
    var mFormatID by int()
    var mFormatFlags by int()
    var mBytesPerPacket by int()
    var mFramesPerPacket by int()
    var mBytesPerFrame by int()
    var mChannelsPerFrame by int()
    var mBitsPerChannel by int()
    var mReserved by int()
}

//private object CoreAudio {
//    @JvmStatic external fun AudioComponentInstanceNew(): Int
//    init {
//        Native.register("CoreAudio")
//    }
//}

private fun interface AudioQueueNewOutputCallback : Callback {
    fun callback(inUserData: Pointer?, inAQ: Pointer?, inBuffer: Pointer?): Int
}

@Keep
private object CoreAudioKit {
    @JvmStatic external fun AudioQueueNewOutput(
        inFormat: Pointer?,
        inCallbackProc: Callback?,
        inUserData: Pointer?,
        inCallbackRunLoop: Pointer?,
        inCallbackRunLoopMode: Pointer?,
        inFlags: Int,
        outAQ: Pointer?
    ): Int
    @JvmStatic external fun AudioQueueAllocateBuffer(inAQ: Pointer?, inBufferByteSize: Int, buffer: Pointer?): Int
    @JvmStatic external fun AudioQueueStart(inAQ: Pointer?, inStartTime: Pointer?): Int
    @JvmStatic external fun AudioQueueDispose(inAQ: Pointer?, immediate: Boolean): Int
    @JvmStatic external fun AudioQueueEnqueueBuffer(inAQ: Pointer?, inBuffer: Pointer?, inNumPacketDescs: Int, inPacketDescs: Pointer?): Int

    const val kAudioFormatLinearPCM: Int = 0x6C70636D
    const val kLinearPCMFormatFlagIsSignedInteger: Int = 4
    const val kAudioFormatFlagIsPacked: Int = 8

    init {
        Native.register("CoreAudioKit")
    }
}
