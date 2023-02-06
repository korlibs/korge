package com.soywiz.korau.sound.backends

import com.soywiz.klock.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korau.sound.*
import com.sun.jna.*
import kotlin.random.*
import kotlin.test.*

class CoreAudioImplTest {
    @Test
    fun test() {
        CoreAudioKit.AudioQueueAllocateBuffer(null, 0, null)
        val queueRef = Memory(16).also { it.clear() }
        val format = AudioStreamBasicDescription(Memory(40).also { it.clear() })

        val bufferSizeInBytes = 2048
        val rate = 44100
        val nchannels = 2
        val numBuffers = 3

        format.mSampleRate = rate.toDouble()
        format.mFormatID = CoreAudioKit.kAudioFormatLinearPCM;
        format.mFormatFlags = CoreAudioKit.kLinearPCMFormatFlagIsSignedInteger or CoreAudioKit.kAudioFormatFlagIsPacked;
        format.mBitsPerChannel = (8 * Short.SIZE_BYTES)
        format.mChannelsPerFrame = nchannels
        format.mBytesPerFrame = (Short.SIZE_BYTES * format.mChannelsPerFrame)
        format.mFramesPerPacket = 1
        format.mBytesPerPacket = format.mBytesPerFrame * format.mFramesPerPacket
        format.mReserved = 0
        val cti = CallbackThreadInitializer()

        val callback = object : AudioQueueNewOutputCallback {
            val tone = AudioTone.generate(1.seconds, 41000.0)
            var m = 0

            override fun callback(inUserData: Pointer?, inAQ: Pointer?, inBuffer: Pointer?): Int {

                println("callback: inUserData=$inUserData, inAQ=$inAQ, inBuffer=$inBuffer, thread=${Thread.currentThread()}")

                val queue = AudioQueueBuffer(inBuffer)
                val ptr = queue.mAudioData
                //println(queue.mAudioDataByteSize)
                val random = Random(0L)
                var available = m < tone.totalSamples
                for (n in 0 until (queue.mAudioDataByteSize / Short.SIZE_BYTES)) {
                    for (ch in 0 until nchannels) {
                        ptr.pointer.setShort((n * nchannels + ch).toLong(), if (m < tone.totalSamples) tone[0, m] else 0)
                        //ptr.pointer.setShort((n * nchannels + ch).toLong(), 0)
                    }
                    m++
                    //ptr.pointer.setShort(n.toLong(), 1000)
                }
                println("queue.mAudioData=${queue.mAudioData}")

                if (available) {
                    val res = CoreAudioKit.AudioQueueEnqueueBuffer(inAQ, queue.pointer, 0, null)
                    println(" -> res=$res")
                }

                //initRuntimeIfNeeded()
                //val output = custom_data?.asStableRef<MyCoreAudioOutputCallback>() ?: return println("outputCallback null[0]")
                //val buf = buffer?.pointed ?: return println("outputCallback null[1]")
                //val dat = buf.mAudioDataByteSize.toInt() / Short.SIZE_BYTES
                //val shortBuf = buf.mAudioData?.reinterpret<ShortVar>() ?: return println("outputCallback null[2]")
                //output.get().generateOutput(shortBuf, dat)
                //AudioQueueEnqueueBuffer(queue, buffer, 0.convert(), null).checkError("AudioQueueEnqueueBuffer")
                return 0
            }
        }

        Native.setCallbackThreadInitializer(callback, cti)

        val result = CoreAudioKit.AudioQueueNewOutput(
            format.pointer, callback, Pointer.createConstant(0L),
            CoreFoundation.CFRunLoopGetCurrent(),
            //CoreFoundation.CFRunLoopGetMain(),
            CoreFoundation.kCFRunLoopCommonModes, 0, queueRef
        )
        val queue = queueRef.getPointer(0L)
        println("result=$result, queue=$queue")
        val buffersArray = Memory((8 * numBuffers).toLong()).also { it.clear() }
        for (buf in 0 until numBuffers) {
            val bufferPtr = Pointer(buffersArray.address + 8 * buf)
            val res = CoreAudioKit.AudioQueueAllocateBuffer(queue, bufferSizeInBytes, bufferPtr)
            val ptr = AudioQueueBuffer(bufferPtr.getPointer(0))
            println("AudioQueueAllocateBuffer=$res, ptr.pointer=${ptr.pointer}")
            ptr.mAudioDataByteSize = bufferSizeInBytes
            callback.callback(null, queue, ptr.pointer)
        }
        val res2 = CoreAudioKit.AudioQueueStart(queue, null)
        println("res2=$res2")
        //CoreFoundation.CFRunLoopRun()
        //CoreAudioImpl2.AudioComponentInstanceNew()
        while (true) {
            Thread.sleep(500L)
        }
    }
}

internal class AudioQueueBuffer(p: Pointer? = null) : KStructure(p) {
    var mAudioDataBytesCapacity by int()
    var mAudioData by pointer<Short>()
    var mAudioDataByteSize by int()
    var mUserData by pointer<Byte>()
    var mPacketDescriptionCapacity by pointer<Int>()
/*
typedef struct AudioQueueBuffer {
    const UInt32                    mAudioDataBytesCapacity;
    void * const                    mAudioData;
    UInt32                          mAudioDataByteSize;
    void * __nullable               mUserData;

    const UInt32                    mPacketDescriptionCapacity;
    AudioStreamPacketDescription * const __nullable mPacketDescriptions;
    UInt32                          mPacketDescriptionCount;
#ifdef __cplusplus
    AudioQueueBuffer() : mAudioDataBytesCapacity(0), mAudioData(0), mPacketDescriptionCapacity(0), mPacketDescriptions(0) { }
#endif
} AudioQueueBuffer;
 */
}

internal class AudioTimeStamp(p: Pointer? = null) : KStructure(p) {
    var mSampleTime by double()
    var mHostTime by long()
    var mRateScalar by double()
    var mWordClockTime by long()
    // ...

    /*
    struct AudioTimeStamp
    {
        Float64             mSampleTime;
        UInt64              mHostTime;
        Float64             mRateScalar;
        UInt64              mWordClockTime;
        SMPTETime           mSMPTETime;
        AudioTimeStampFlags mFlags;
        UInt32              mReserved;
    };

     */
}

internal class AudioStreamBasicDescription(p: Pointer? = null) : KStructure(p) {
    var mSampleRate by double()
    var mFormatID by int()
    var mFormatFlags by int()
    var mBytesPerPacket by int()
    var mFramesPerPacket by int()
    var mBytesPerFrame by int()
    var mChannelsPerFrame by int()
    var mBitsPerChannel by int()
    var mReserved by int()
/*
struct AudioStreamBasicDescription
{
    Float64             mSampleRate;
    AudioFormatID       mFormatID;
    AudioFormatFlags    mFormatFlags;
    UInt32              mBytesPerPacket;
    UInt32              mFramesPerPacket;
    UInt32              mBytesPerFrame;
    UInt32              mChannelsPerFrame;
    UInt32              mBitsPerChannel;
    UInt32              mReserved;
};
 */
}

object CoreAudio {
    @JvmStatic external fun AudioComponentInstanceNew(): Int
    init {
        Native.register("CoreAudio")
    }
}

fun interface AudioQueueNewOutputCallback : Callback {
    fun callback(inUserData: Pointer?, inAQ: Pointer?, inBuffer: Pointer?): Int
}

object CoreAudioKit {
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

object CoreFoundation {
    val library = NativeLibrary.getInstance("CoreFoundation")
    @JvmStatic val kCFRunLoopCommonModes: Pointer? = library.getGlobalVariableAddress("kCFRunLoopCommonModes").getPointer(0L)
    @JvmStatic external fun CFRunLoopGetCurrent(): Pointer?
    @JvmStatic external fun CFRunLoopGetMain(): Pointer?
    @JvmStatic external fun CFRunLoopRun(): Void

    init {
        Native.register("CoreFoundation")
    }
}
