package com.soywiz.korvi.internal

import com.soywiz.kds.TGenDeque
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.seconds
import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.readShortArrayLE
import com.soywiz.kmem.writeArrayLE
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.readBytesUpTo
import com.soywiz.korio.util.toByteArray
import com.soywiz.korvi.BaseKorviStream
import com.soywiz.korvi.KorviAudioFrame
import com.soywiz.korvi.KorviFrame
import com.soywiz.korvi.KorviVideoFrame
import com.soywiz.korvi.KorviVideoLL
import kotlinx.coroutines.runBlocking
import org.jcodec.api.specific.AVCMP4Adaptor
import org.jcodec.common.Demuxer
import org.jcodec.common.DemuxerTrack
import org.jcodec.common.JCodecUtil
import org.jcodec.common.SeekableDemuxerTrack
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.jcodec.scale.ColorUtil
import java.nio.ByteBuffer

internal actual val korviInternal: KorviInternal = JvmKorviInternal()

internal class JvmKorviInternal : KorviInternal() {
    override fun createContainer(stream: AsyncStream): KorviVideoLL {
        return JvmKorviVideoLL(MP4Demuxer.createMP4Demuxer(stream.seekableByteChannel()))
    }
}

class JvmKorviVideoLL(
    val demuxer: Demuxer
) : KorviVideoLL() {
    override val video = demuxer.videoTracks.map { JvmKorviVideoStream(this, it) }
    override val audio = demuxer.audioTracks.map { JvmKorviAudioStream(this, it) }
}

/**
 * A FIFO (First In First Out) structure.
 */
internal class KorviQueue<TGen>() : Collection<TGen> {
    private val items = TGenDeque<TGen>()

    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    constructor(vararg items: TGen) : this() {
        for (item in items) enqueue(item)
    }

    fun enqueue(v: TGen) {  items.addLast(v) }
    fun dequeue(): TGen = items.removeFirst()
    fun peek(): TGen = items.first
    fun remove(v: TGen) { items.remove(v) }
    fun toList() = items.toList()

    override fun contains(element: TGen): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<TGen>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<TGen> = items.iterator()
}

abstract class JvmBaseKorviStream<TFrame : KorviFrame>(
    val container: JvmKorviVideoLL,
    val track: DemuxerTrack
) : BaseKorviStream<TFrame> {
    internal val queue = KorviQueue<TFrame>()

    override suspend fun seek(frame: Long) {
        (track as SeekableDemuxerTrack).gotoFrame(frame)
    }

    override suspend fun seek(time: HRTimeSpan) {
        (track as SeekableDemuxerTrack).seek(time.secondsDouble)
    }

    override suspend fun getTotalFrames(): Long? = track.meta.totalFrames.toLong()
    override suspend fun getDuration(): HRTimeSpan? = track.meta.totalDuration.seconds.hr

    override suspend fun readFrame(): TFrame? {
        prepareFrames()
        return if (queue.isNotEmpty()) queue.dequeue() else null
    }

    protected abstract suspend fun prepareFrames()
}

class JvmKorviVideoStream(
    container: JvmKorviVideoLL,
    private val videoTrack: DemuxerTrack
) : JvmBaseKorviStream<KorviVideoFrame>(container, videoTrack) {
    private val adaptor = AVCMP4Adaptor(videoTrack.meta)
    private val pic = adaptor.allocatePicture()


    override suspend fun prepareFrames() {
        if (queue.isNotEmpty()) return
        val frame = videoTrack.nextFrame() ?: return
        //println("FRAME: ${frame.frameNo}, ${frame.pts}, ${frame.timescale}, ${frame.duration}, ${frame.frameType}")
        val decodedFrame = adaptor.decodeFrame(frame, pic)
        queue.enqueue(KorviVideoFrame({ decodedFrame.toBmp() }, frame.frameNo, frame.ptsD.seconds.hr, frame.durationD.seconds.hr))
    }
}

class JvmKorviAudioStream(
    container: JvmKorviVideoLL,
    private val audioTrack: DemuxerTrack
) : JvmBaseKorviStream<KorviAudioFrame>(container, audioTrack) {
    private val audioMeta = audioTrack.meta
    private val audioDecoder = JCodecUtil.createAudioDecoder(audioMeta.codec, audioTrack.nextFrame()!!.data)
    private val meta = audioMeta.audioCodecMeta

    override protected suspend fun prepareFrames() {
        if (queue.isNotEmpty()) return
        val frame = audioTrack.nextFrame() ?: return
        while (frame.data.hasRemaining()) {
            val out = ByteBuffer.allocate(((meta.sampleRate * frame.durationD) * meta.sampleSize * 2).toInt())
            val audioBuffer = audioDecoder.decodeFrame(frame.data, out)
            //println(audioBuffer.format.channels)
            //println(audioBuffer.format.frameRate)
            //out.flip()
            val bytes = out.toByteArray()
            val shorts = bytes.readShortArrayLE(0, bytes.size / 2)
            val sampleCount = shorts.size / meta.channelCount
            val audioSamples = AudioSamples(meta.channelCount, sampleCount)
            //println(audioBuffer.data.limit())
            for (channel in 0 until meta.channelCount) {
                val array = audioSamples.data[channel]
                arraycopy(shorts, channel * sampleCount, array, 0, array.size)
            }

            queue.enqueue(KorviAudioFrame(AudioData(audioBuffer.format.frameRate, audioSamples), frame.frameNo, frame.ptsD.seconds.hr, frame.durationD.seconds.hr))
            //outs.write(shorts.toByteArrayLE())
        }
    }
}

internal fun ShortArray.toByteArrayLE(): ByteArray {
    val out = ByteArray(size * 2)
    out.writeArrayLE(0, this)
    return out
}

internal fun Picture.toBmp(): Bitmap32 {
    val pic = Picture.createCropped(width, height, ColorSpace.BGR, crop)
    val transform = ColorUtil.getTransform(color, ColorSpace.RGB)
    transform.transform(this, pic)
    val bmp = Bitmap32(pic.width, pic.height, premultiplied = false)
    //RgbToBgr().transform(bgr, bgr)
    val p = pic.getPlaneData(0)
    for (n in 0 until width * height) {
        val r = p[n * 3 + 0].toInt() + 128
        val g = p[n * 3 + 1].toInt() + 128
        val b = p[n * 3 + 2].toInt() + 128
        bmp.ints[n] = RGBA(r, g, b, 0xFF).value
    }
    return bmp
}

internal fun SyncStream.seekableByteChannel(): SeekableByteChannel {
    val ss = this
    return object : SeekableByteChannel {
        var open = true
        override fun setPosition(newPosition: Long): SeekableByteChannel = this.apply { ss.position = newPosition }
        override fun isOpen(): Boolean = open
        override fun position(): Long = ss.position
        override fun write(bb: ByteBuffer): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun size(): Long = ss.length
        override fun close() { ss.close().also { open = false } }
        override fun truncate(size: Long): SeekableByteChannel = this.apply { ss.length = size }
        override fun read(bb: ByteBuffer): Int {
            val data = ss.readBytes(bb.remaining())
            bb.put(data)
            return data.size
        }
    }
}

internal fun AsyncStream.seekableByteChannel(): SeekableByteChannel {
    val ss = this
    return object : SeekableByteChannel {
        var open = true
        override fun setPosition(newPosition: Long): SeekableByteChannel = this.apply { ss.position = newPosition }
        override fun isOpen(): Boolean = open
        override fun position(): Long = ss.position
        override fun write(bb: ByteBuffer): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun size(): Long = runBlocking { ss.getLength() }
        override fun close() = runBlocking { ss.close().also { open = false } }
        override fun truncate(size: Long): SeekableByteChannel = this.apply { runBlocking { ss.setLength(size) } }
        override fun read(bb: ByteBuffer): Int {
            val data = runBlocking { ss.readBytesUpTo(bb.remaining()) }
            bb.put(data)
            return data.size
        }
    }
}
