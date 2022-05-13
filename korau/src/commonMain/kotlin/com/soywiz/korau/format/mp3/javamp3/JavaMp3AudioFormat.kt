package com.soywiz.korau.format.mp3.javamp3

import com.soywiz.kmem.readU16LE
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.MP3
import com.soywiz.korau.format.MP3Base
import com.soywiz.korau.internal.copyChunkTo
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioSamplesDeque
import com.soywiz.korau.sound.AudioStream
import com.soywiz.korio.lang.unsupported
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.DequeSyncStream
import com.soywiz.korio.stream.markable
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll

internal open class JavaMp3AudioFormat() : AudioFormat("mp3") {
    companion object : JavaMp3AudioFormat()

    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = MP3.tryReadInfo(data, props)
    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? = createJavaMp3DecoderStream(data, props)
    override fun toString(): String = "NativeMp3DecoderFormat"
}

private suspend fun createJavaMp3DecoderStream(s: AsyncStream, props: AudioDecodingProps): AudioStream {
    //println("s.hasLength()=${s.hasLength()}")
    return when (s.hasLength()) {
        true -> createJavaMp3DecoderStream(s.readAll(), props)
        else -> createJavaMp3DecoderStreamNoSeek(s, props)
    }
}

// @TODO: Use AsyncStream and read frame chunks
private suspend fun createJavaMp3DecoderStream(idata: ByteArray, props: AudioDecodingProps, table: MP3Base.SeekingTable? = null): AudioStream {
    val sdata = idata.openAsync()
    val data = JavaMp3Decoder.init(idata) ?: error("Not an mp3 file [2]")
    val samplesBuffer = data._samplesBuffer!!
    val samples = ShortArray(samplesBuffer.size / 2)
    val deque = AudioSamplesDeque(data.nchannels)
    var samplesPos = 0L
    var seekPos = -1L
    val mp3SeekingTable: MP3Base.SeekingTable? = if (props.exactTimings == true) table ?: MP3Base.Parser(sdata, sdata.getLength()).getSeekingTable(44100) else null

    fun decodeSamples() {
        for (n in samples.indices) samples[n] = samplesBuffer.readU16LE(n * 2).toShort()
    }

    return object : AudioStream(data.frequency, data.nchannels) {
        override var finished: Boolean = false

        override val totalLengthInSamples: Long? = mp3SeekingTable?.lengthSamples

        override var currentPositionInSamples: Long
            get() = samplesPos
            set(value) {
                seekPos = value
                samplesPos = value
                finished = false
            }

        private fun seek(pos: Long) {
            //if (pos == 0L) data = JavaMp3Decoder.init(idata)!! else data.seek(pos)
            data.seek(pos)
            while (JavaMp3Decoder.decodeFrame(data) == JavaMp3Decoder.DecodeStatus.ERROR) Unit
            JavaMp3Decoder.decodeFrame(data)
            //JavaMp3Decoder.decodeFrame(data)
            //JavaMp3Decoder.decodeFrame(data)
            //JavaMp3Decoder.decodeFrame(data)
            //val s = idata.openSync()
            //s.position = pos
            //data = JavaMp3Decoder.init(s)!!
        }

        override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
            if (seekPos >= 0L) {
                deque.clear()
                if (seekPos == 0L) {
                    seek(0L)
                } else {
                    if (mp3SeekingTable != null) {
                        seek(mp3SeekingTable.locateSample(seekPos))
                    }
                }
                seekPos = -1L
            }

            if (deque.availableRead < length) {
                if (!finished && JavaMp3Decoder.decodeFrame(data) != JavaMp3Decoder.DecodeStatus.COMPLETED) {
                    decodeSamples()
                    deque.writeInterleaved(samples, 0)
                } else {
                    finished = true
                }
            }
            return deque.read(out, offset, length).also {
                samplesPos += length
            }
        }

        override suspend fun clone(): AudioStream = createJavaMp3DecoderStream(idata, props, mp3SeekingTable)

        override fun close() {
            finished = true
        }
    }
}

private suspend fun createJavaMp3DecoderStreamNoSeek(s: AsyncStream, props: AudioDecodingProps): AudioStream {
    //println(s.readBytesExact(4096).toList())

    val tempBuffer = ByteArray(2 * 4096)
    val sync = DequeSyncStream()

    suspend fun fillSync() {
        while (!s.eof() && sync.availableRead < tempBuffer.size) {
            s.copyChunkTo(sync, tempBuffer)
        }
    }

    fillSync()
    //println(sync.readAll().toList())
    //println("sync.availableRead=${sync.availableRead}")
    val data = JavaMp3Decoder.init(sync.markable()) ?: error("Not an mp3 file [2]")
    val samplesBuffer = data._samplesBuffer!!
    val samples = ShortArray(samplesBuffer.size / 2)
    val deque = AudioSamplesDeque(data.nchannels)
    var samplesPos = 0L

    fun decodeSamples() {
        for (n in samples.indices) samples[n] = samplesBuffer.readU16LE(n * 2).toShort()
    }

    return object : AudioStream(data.frequency, data.nchannels) {
        override var finished: Boolean = false

        override val totalLengthInSamples: Long? = null

        override var currentPositionInSamples: Long
            get() = samplesPos
            set(value) {
                samplesPos = value
                finished = false
            }

        override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
            fillSync()

            if (deque.availableRead < length) {
                if (!finished && JavaMp3Decoder.decodeFrame(data) != JavaMp3Decoder.DecodeStatus.COMPLETED) {
                    decodeSamples()
                    deque.writeInterleaved(samples, 0)
                } else {
                    finished = true
                }
            }
            return deque.read(out, offset, length).also {
                samplesPos += length
            }
        }

        override suspend fun clone(): AudioStream = unsupported()

        override fun close() {
            finished = true
        }
    }
}
