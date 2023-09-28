package korlibs.audio.sound.impl.awt

import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.datastructure.thread.*
import korlibs.memory.*
import korlibs.time.*
import kotlinx.coroutines.*
import javax.sound.sampled.*
import kotlin.coroutines.*
import kotlin.time.*

// AudioSystem.getMixerInfo()
private val mixer by lazy { AudioSystem.getMixer(null) }

object AwtNativeSoundProvider : NativeSoundProvider() {
    val format = AudioFormat(44100f, 16, 2, true, false)

    val linePool = ConcurrentPool { (mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine).also { it.open() } }

    init {
        // warming and preparing
        mixer.mixerInfo
        val info = DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format, 4096)
        line.start()
        line.write(ByteArray(4), 0, 4)
        line.drain()
        line.stop()
        line.close()
    }

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput =
        JvmPlatformAudioOutput(this, coroutineContext, freq)
}

data class SampleBuffer(val timestamp: Long, val data: AudioSamples)

/*
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
*/


class JvmPlatformAudioOutput(
    val provider: AwtNativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int
) : DequeBasedPlatformAudioOutput(coroutineContext, frequency) {
    val samplesLock = korlibs.datastructure.lock.NonRecursiveLock()
    var nativeThread: NativeThread? = null
    var running = false
    var totalEmittedSamples = 0L

    override suspend fun wait() {
        if (line == null) return
        for (n in 0 until 1000) {
            var currentPositionInSamples: Long = 0L
            var totalEmittedSamples: Long = 0L
            var availableRead = 0
            samplesLock {
                currentPositionInSamples = line?.longFramePosition ?: 0L
                availableRead = this.availableRead
                totalEmittedSamples = this.totalEmittedSamples
            }
            //println("availableRead=$availableRead, waveOutGetPosition=$currentPositionInSamples, totalEmittedSamples=$totalEmittedSamples")
            if (availableRead <= 0 && currentPositionInSamples >= totalEmittedSamples) break
            delay(1.milliseconds)
        }
    }

    val format = provider.format
    var line: SourceDataLine? = null

    val BYTES_PER_SAMPLE = nchannels * Short.SIZE_BYTES

    fun bytesToSamples(bytes: Int): Int = bytes / BYTES_PER_SAMPLE
    fun samplesToBytes(samples: Int): Int = samples * BYTES_PER_SAMPLE

    override fun start() {
        //println("TRYING TO START")
        if (running) return
        //println("STARTED")
        running = true
        nativeThread = nativeThread(isDaemon = true) {
            try {
                var timesWithoutBuffers = 0
                while (running || availableRead > 0) {
                    while (availableRead > 0) {
                        timesWithoutBuffers = 0
                        while (availableRead > 0) {
                            if (line == null) {
                                val prepareLineTime = measureTimedValue {
                                    line = provider.linePool.alloc()
                                    //println("OPEN LINE: $line")
                                    line!!.stop()
                                    line!!.flush()
                                    line!!.start()
                                }
                                //println("prepareLineTime=$prepareLineTime")
                            }
                            val availableBytes = line!!.available()
                            //val availableSamples = minOf(availableRead, bytesToSamples(availableBytes))
                            val availableSamples = minOf(441, minOf(availableRead, bytesToSamples(availableBytes)))

                            val info = AudioSamplesInterleaved(nchannels, availableSamples)
                            val readCount = readShortsInterleaved(info)
                            val bytes = ByteArray(samplesToBytes(readCount))
                            bytes.setArrayLE(0, info.data)
                            //println(bytes.hex)
                            val (written, time) = measureTimedValue { line!!.write(bytes, 0, bytes.size) }
                            if (written != bytes.size) {
                                println("NOT FULLY WRITTEN $written != ${bytes.size}")
                            }
                            //println("written=$written, write time=$time")
                            samplesLock {
                                this.totalEmittedSamples += readCount
                            }
                        }
                        //println(bytes.hex)
                        Thread.sleep(1L)
                    }
                    //println("SHUT($id)!")
                    //Thread.sleep(500L) // 0.5 seconds of grace before shutting down this thread!
                    Thread.sleep(50L) // 0.5 seconds of grace before shutting down this thread!
                    timesWithoutBuffers++
                    if (timesWithoutBuffers >= 10) break
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                //println("CLOSED_LINE: $line running=$running!")
                if (line != null) {
                    //line?.drain()
                    //line?.stop()
                    //line?.close()
                    provider.linePool.free(line!!)
                    line = null
                }
            }
        }
    }

    override fun stop() {
        running = false
        //println("STOPPING")
    }

    /*
                val line by lazy { mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine }
                line.open()
                line.start()
                //println("OPENED_LINE($id)!")
                try {
                    var timesWithoutBuffers = 0
                    while (running) {
                        while (availableBuffers > 0) {
                            timesWithoutBuffers = 0
                            val buf = synchronized(buffers) { buffers.dequeue() }
                            synchronized(buffers) { totalShorts -= buf.data.totalSamples * buf.data.channels }
                            val bdata = convertFromShortToByte(buf.data.interleaved().data)

                            val msChunk = (((bdata.size / 2) * 1000.0) / frequency.toDouble()).toInt()

                            _msElapsed += msChunk
                            val now = System.currentTimeMillis()
                            val latency = now - buf.timestamp
                            //val drop = latency >= 150
                            val start = System.currentTimeMillis()
                            line.write(bdata, 0, bdata.size)
                            //line.drain()
                            val end = System.currentTimeMillis()
                            //println("LINE($id): ${end - start} :: msChunk=$msChunk :: start=$start, end=$end :: available=${line.available()} :: framePosition=${line.framePosition} :: availableBuffers=$availableBuffers")
                        }
                        //println("SHUT($id)!")
                        //Thread.sleep(500L) // 0.5 seconds of grace before shutting down this thread!
                        Thread.sleep(50L) // 0.5 seconds of grace before shutting down this thread!
                        timesWithoutBuffers++
                        if (timesWithoutBuffers >= 10) break
                    }
                } finally {
                    //println("CLOSED_LINE($id)!")
                    line.stop()
                    line.close()
                }
     */
}
