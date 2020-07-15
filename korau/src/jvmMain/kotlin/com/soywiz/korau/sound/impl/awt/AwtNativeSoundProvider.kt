package com.soywiz.korau.sound.impl.awt

/*
import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korau.error.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.stream.*
import java.io.*
import java.nio.*
import javax.sound.sampled.*
import javax.sound.sampled.AudioFormat
import kotlin.coroutines.*

// AudioSystem.getMixerInfo()
val mixer by lazy { AudioSystem.getMixer(null) }

object AwtNativeSoundProvider : NativeSoundProvider() {
    override fun init() {
        // warming and preparing
        mixer.mixerInfo
        val af = AudioFormat(44100f, 16, 2, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, af)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(af, 4096)
        line.start()
        line.write(ByteArray(4), 0, 4)
        line.drain()
        line.stop()
        line.close()
    }

    override suspend fun createAudioStream(freq: Int): PlatformAudioOutput = JvmPlatformAudioOutput(freq)

    override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps): NativeSound {
        val audioData = try {
            nativeAudioFormats.decode(data.openAsync(), props) ?: AudioData.DUMMY
        } catch (e: Throwable) {
            e.printStackTrace()
            AudioData.DUMMY
        }
        return AwtNativeSound(audioData, audioData.toWav()).init()
    }

    override suspend fun createSound(data: AudioData, formats: AudioFormats, streaming: Boolean): NativeSound = AwtNativeSound(data, data.toWav())
}

class AwtNativeSound(val audioData: AudioData, val data: ByteArray) : NativeSound() {
    override var length: TimeSpan = 0.milliseconds
    val format by lazy {
        javax.sound.sampled.AudioFormat(
            javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
            audioData.rate.toFloat(),
            16,
            audioData.channels,
            1024,
            1024.toFloat(),
            false
        )
    }
    //val jsound by lazy { AudioSystem.getAudioInputStream(ByteArrayInputStream(data)) }

    suspend fun init(): AwtNativeSound {
        executeInWorkerJVM {
            val sound = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
            length = (sound.frameLength * 1000.0 / sound.format.frameRate.toDouble()).toLong().milliseconds
        }
        return this
    }

    override suspend fun decode(): AudioData = audioData

    class PooledClip {
        companion object {
            private val pool = Pool({ it.stopped = false }) { PooledClip() }
            fun play(channel: NativeSoundChannel, params: PlaybackParameters): PooledClip {
                //val clip = pool.alloc()
                val clip = PooledClip()
                clip.play(channel)
                return clip
            }
        }

        val lineListener: LineListener = object : LineListener {
            override fun update(event: LineEvent) {
                when (event.type) {
                    LineEvent.Type.STOP, LineEvent.Type.CLOSE -> {
                        event.line.close()
                        clip.removeLineListener(this)
                        stop()
                    }
                }
            }
        }

        val clip = AudioSystem.getClip(mixer.mixerInfo).apply {
            addLineListener(lineListener)
        }
        var stopped = false
        val current: TimeSpan get() = clip.microsecondPosition.toDouble().microseconds

        private var channel: NativeSoundChannel? = null

        fun stop() {
            if (!stopped) {
                stopped = true
                channel?.stop()
                channel = null
                clip.stop()
                //clip.close()
                //pool.free(this)
            }
        }

        fun play(channel: NativeSoundChannel) {
            this.channel = channel
            val sound = channel.sound as AwtNativeSound

            if (sound.audioData.totalTime == 0.seconds) {
                stop()
            } else {
                val data = sound.data
                val time = measureTime {
                    clip.open(sound.format, data, 0, data.size)
                }
                //println("Opening clip time: $time")
                clip.start()
            }
        }
    }

    override fun play(params: PlaybackParameters): NativeSoundChannel {
        return object : NativeSoundChannel(this) {

            var clip: PooledClip? = PooledClip.play(this, params)

            //val len = clip.microsecondLength.toDouble().microseconds
            val len = audioData.totalTime

            override var current: TimeSpan
                get() = clip?.current ?: 0.milliseconds
                set(value) = seekingNotSupported()
            override val total: TimeSpan get() = len
            //override val playing: Boolean get() = !stopped && current < total
            override val playing: Boolean get() = clip != null

            //override var pitch: Double = 1.0
            //    set(value) {
            //        field = value
            //        //(clip.getControl(FloatControl.Type.SAMPLE_RATE) as FloatControl).value = (audioData.rate * pitch).toFloat()
            //    }

            override fun stop() {
                clip?.stop()
                clip = null
            }
        }.also {
            it.copySoundPropsFrom(params)
        }
    }
}

data class SampleBuffer(val timestamp: Long, val data: AudioSamples)

class JvmPlatformAudioOutput(freq: Int) : PlatformAudioOutput(freq) {
    companion object {
        var lastId = 0
        val mixer by lazy { AudioSystem.getMixer(null) }
    }

    val id = lastId++
    val format by lazy { AudioFormat(freq.toFloat(), 16, 2, true, false) }
    var _msElapsed = 0.0
    val msElapsed get() = _msElapsed
    var totalShorts = 0
    val buffers = Queue<SampleBuffer>()
    var thread: Thread? = null
    var running = true

    val availableBuffers: Int get() = synchronized(buffers) { buffers.size }
    val line by lazy { mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine }

    override val availableSamples get() = synchronized(buffers) { totalShorts }

    fun ensureThread() {
        if (thread == null) {

            thread = Thread {
                line.open()
                line.start()
                //println("OPENED_LINE($id)!")
                try {
                    var timesWithoutBuffers = 0
                    while (running) {
                        while (availableBuffers > 0) {
                            timesWithoutBuffers = 0
                            val buf = synchronized(buffers) { buffers.dequeue() }
                            synchronized(buffers) { totalShorts -= buf.data.size }
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

                thread = null
            }.apply {
                name = "NativeAudioStream$id"
                isDaemon = true
                start()
            }
        }
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        val buffer = SampleBuffer(System.currentTimeMillis(), samples.copyOfRange(offset, offset + size))
        synchronized(buffers) {
            totalShorts += buffer.data.size
            buffers.enqueue(buffer)
        }

        ensureThread()

        while (availableBuffers >= 5) {
            coroutineContext.delay(4.milliseconds)
        }

        //val ONE_SECOND = 44100 * 2
        ////val BUFFER_TIME_SIZE = ONE_SECOND / 8 // 1/8 second of buffer
        //val BUFFER_TIME_SIZE = ONE_SECOND / 4 // 1/4 second of buffer
        ////val BUFFER_TIME_SIZE = ONE_SECOND / 2 // 1/2 second of buffer
        //while (bufferSize >= 32 && synchronized(buffers) { totalShorts } > BUFFER_TIME_SIZE) {
        //	ensureThread()
        //	getCoroutineContext().eventLoop.sleepNextFrame()
        //}
    }

    fun convertFromShortToByte(sa: ShortArray, offset: Int = 0, size: Int = sa.size - offset): ByteArray {
        val bb = ByteBuffer.allocate(size * 2).order(ByteOrder.nativeOrder())
        val sb = bb.asShortBuffer()
        sb.put(sa, offset, size)
        return bb.array()
    }

    //suspend fun CoroutineContext.sleepImmediate2() = suspendCoroutine<Unit> { c ->
    //	eventLoop.setImmediate { c.resume(Unit) }
    //}
    override fun stop() {
        running = false
    }

    override fun start() {

    }
}
*/
