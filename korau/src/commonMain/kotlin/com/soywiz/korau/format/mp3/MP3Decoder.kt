package com.soywiz.korau.format.mp3

import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.stream.*

open class MP3Decoder() : AudioFormat("mp3") {
    companion object : MP3Decoder()

    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = MP3.tryReadInfo(data, props)
    override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? = createJavaMp3DecoderStream(data)
    override fun toString(): String = "NativeMp3DecoderFormat"
}

private suspend fun createJavaMp3DecoderStream(s: AsyncStream): AudioStream = createJavaMp3DecoderStream(s.readAll())

// @TODO: Use AsyncStream and read frame chunks
private suspend fun createJavaMp3DecoderStream(idata: ByteArray): AudioStream {
    val sdata = idata.openAsync()
    var data = JavaMp3Decoder.init(idata) ?: error("Not an mp3 file [2]")
    val samples = ShortArray(data.samplesBuffer.size / 2)
    val deque = AudioSamplesDeque(data.nchannels)
    var samplesPos = 0L
    var seekPos = -1L
    val mp3SeekingTable = MP3Base.Parser(sdata).getSeekingTable(44100)

    fun decodeSamples() {
        for (n in samples.indices) samples[n] = data.samplesBuffer.readU16LE(n * 2).toShort()
    }

    return object : AudioStream(data.frequency, data.nchannels) {
        override var finished: Boolean = false

        override val totalLengthInSamples: Long? = mp3SeekingTable.lengthSamples

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
                    seek(mp3SeekingTable.locateSample(seekPos))
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

        override suspend fun clone(): AudioStream = createJavaMp3DecoderStream(idata)

        override fun close() {
            finished = true
        }
    }
}
