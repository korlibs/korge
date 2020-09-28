package com.soywiz.korvi

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.stream.*
import com.soywiz.korvi.internal.*
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext

suspend fun KorviVideo(file: VfsFile): KorviVideo = korviInternal.createHighLevel(file)
fun KorviVideoLL(stream: AsyncStream): KorviVideoLL = korviInternal.createContainer(stream)

open class KorviVideo : BaseKorviSeekable {
    class Frame(
        var data: Bitmap,
        val position: HRTimeSpan,
        val duration: HRTimeSpan
    )

    val onVideoFrame = Signal<Frame>()
    val onComplete = Signal<Unit>()
    open val running: Boolean = false
    open val elapsedTimeHr: HRTimeSpan get() = 0.milliseconds.hr
    val elapsedTime: TimeSpan get() = elapsedTimeHr.timeSpan
    open fun prepare(): Unit = Unit
    open fun render(): Unit = Unit
    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): HRTimeSpan? = null
    open suspend fun play(): Unit = Unit
    override suspend fun seek(frame: Long): Unit = Unit
    override suspend fun seek(time: HRTimeSpan): Unit = Unit
    open suspend fun stop(): Unit = Unit
    override suspend fun close(): Unit = Unit
}

open class KorviVideoFromLL(val ll: KorviVideoLL) : KorviVideo() {
    override suspend fun getTotalFrames(): Long? = ll.getTotalFrames()
    override suspend fun getDuration(): HRTimeSpan? = ll.getDuration()

    override suspend fun seek(frame: Long) = ll.seek(frame)
    override suspend fun seek(time: HRTimeSpan) = ll.seek(time)

    override suspend fun close(): Unit {
        stop()
        ll.close()
    }

    override suspend fun stop() {
        videoJob?.cancel()
        audioJob?.cancel()
        audioStream?.stop()
        audioStream?.dispose()
        running = false
        videoJob = null
        audioJob = null
        audioStream = null
    }

    var videoJob: Job? = null
    var audioJob: Job? = null
    var audioStream: PlatformAudioOutput? = null

    override var running: Boolean = false
    override var elapsedTimeHr: HRTimeSpan = 0.nanoseconds.hr

    override suspend fun play() {
        if (videoJob != null || audioJob != null) return

        running = true
        //audioStream = nativeSoundProvider.createAudioStream()

        videoJob = launchImmediately(coroutineContext) {
            val frames = arrayListOf<KorviVideoFrame>()
            val startTime = PerformanceCounter.reference
            var completed = false
            do {
                //ll.streams.forEach { stream ->
                //}
                //println("V[1] : ${frames.size}")
                //delay(100.milliseconds)
                if (frames.size <= 3) {
                    //var frame: KorviVideoFrame? = null
                    //ll.streams.fastForEach { s ->
                    //    println("S[0]")
                    //    val f = s.readFrame()
                    //    println("S[1]: $f")
                    //    if (f is KorviVideoFrame) {
                    //        frame = f
                    //    }
                    //}
                    val stream = ll.video.firstOrNull()
                    val frame = stream?.readFrame()
                    if (frame != null) {
                        //println("FRAME: ${frame!!.frame} : ${frame!!.position.millisecondsInt}")
                        frames.add(frame!!)
                        frames.sortBy { it.position.nanosecondsDouble }
                    } else {
                        completed = true
                    }
                } else {
                    //delay(1.milliseconds)
                }
                val elapsedTime = PerformanceCounter.reference - startTime
                if (frames.size >= 3 || (completed && frames.isNotEmpty())) {
                    val frame = frames.removeAt(0)
                    val startTime = PerformanceCounter.reference
                    onVideoFrame(Frame(frame.data, frame.position, frame.duration))
                    val elapsedTime = PerformanceCounter.reference - startTime
                    val time = (frame.duration.timeSpan - elapsedTime)
                    delay(if (time < 1.milliseconds) 1.milliseconds else time)
                    /*
                    var lastFrame: KorviVideoFrame? = null
                    while (frames.isNotEmpty()) {
                        val frame = frames[0]
                        //println("FRAME: ${frame.position} : $elapsedTime")
                        if (elapsedTime >= frame.position) {
                            frames.removeAt(0)
                            lastFrame = frame
                        } else {
                            break
                        }
                    }
                    //println(currentThreadId)
                    if (lastFrame != null) {
                        val frame = lastFrame
                        elapsedTimeHr = frame.position
                        //println("${elapsedTimeHr.timeSpan}")
                        onVideoFrame(Frame(frame.data, frame.position, frame.duration))
                        delay(frame.duration.timeSpan)
                        //delay(4.milliseconds)
                    }
                     */
                } else {
                    //delay(4.milliseconds)
                }
                //println("V[2]: ${frame?.position?.millisecondsInt} : ${frame?.duration?.millisecondsInt}")
            } while (frames.isNotEmpty() || !completed)
            //println("COMPLETED")
            videoJob = null
            audioJob = null
            onComplete(Unit)
            running = false
        }
        //audioJob = launchImmediately(coroutineContext) {
        //    try {
        //        do {
        //            println("A[1]")
        //            var frames = 0
        //            val stream = ll.audio.firstOrNull()
        //            //delay(1000.seconds)
        //            val frame = stream?.readFrame()
        //            println("A[2]: ${frame?.position} : ${frame?.duration}")
        //            if (frame != null) {
        //                frames++
        //                audioStream!!.add(frame.data)
        //            }
        //            // 44100 - 1 second, 4410 - 100 milliseconds, 441 - 10 milliseconds
        //            while (audioStream!!.availableSamples > 4410) {
        //                delay(10.milliseconds)
        //            }
        //        } while (frames > 0)
        //    } catch (e: Throwable) {
        //        e.printStackTrace()
        //    }
        //}
    }
}

open class KorviVideoLL() : BaseKorviSeekable {
    open val video: List<KorviVideoStream> = listOf()
    open val audio: List<KorviAudioStream> = listOf()
    val streams: List<BaseKorviStream<out KorviFrame>> by lazy { video + audio }
    final override suspend fun getTotalFrames(): Long? = streams.mapNotNull { it.getTotalFrames() }.max()
    final override suspend fun getDuration(): HRTimeSpan? = streams.mapNotNull { it.getDuration() }.max()
    final override suspend fun seek(frame: Long): Unit = run { for (v in streams) v.seek(frame) }
    final override suspend fun seek(time: HRTimeSpan): Unit = run { for (v in streams) v.seek(time) }
    override suspend fun close() = Unit
}

interface BaseKorviSeekable : AsyncCloseable {
    suspend fun getTotalFrames(): Long? = null
    suspend fun getDuration(): HRTimeSpan? = null
    suspend fun seek(frame: Long): Unit = TODO()
    suspend fun seek(time: HRTimeSpan): Unit = TODO()
    override suspend fun close() = Unit
}
suspend fun BaseKorviSeekable.seek(time: TimeSpan): Unit = seek(time.hr)

interface BaseKorviStream<T : KorviFrame> : BaseKorviSeekable {
    suspend fun readFrame(): T? = TODO()
}

typealias KorviVideoStream = BaseKorviStream<KorviVideoFrame>
typealias KorviAudioStream = BaseKorviStream<KorviAudioFrame>

interface KorviFrame {
    val frame: Long
    val position: HRTimeSpan
    val duration: HRTimeSpan
    val end: HRTimeSpan get() = position + duration
}
data class KorviAudioFrame(val data: AudioData, override val frame: Long, override val position: HRTimeSpan, override val duration: HRTimeSpan) : KorviFrame
data class KorviVideoFrame(val dataGen: () -> Bitmap32, override val frame: Long, override val position: HRTimeSpan, override val duration: HRTimeSpan) : KorviFrame {
    val data by lazy { dataGen() }
}
