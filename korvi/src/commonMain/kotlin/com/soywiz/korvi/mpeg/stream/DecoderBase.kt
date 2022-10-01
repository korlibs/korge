package com.soywiz.korvi.mpeg.stream

import com.soywiz.kds.FastArrayList
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.korvi.mpeg.util.BitBuffer
import com.soywiz.korvi.mpeg.length

abstract class DecoderBase<TDestination : Destination>(
    val streaming: Boolean
) {
    abstract val bits: BitBuffer

    var destination: TDestination? = null
    var canPlay = false

    val collectTimestamps = !streaming
    var bytesWritten = 0
    val timestamps = FastArrayList<Timestamp>()
    var timestampIndex = 0

    var startTime = 0.0
    var decodedTime = 0.0

    open val currentTime: Double get() = decodedTime

    fun destroy() {}

    fun connect(destination: TDestination) {
        this.destination = destination
    }

    fun bufferGetIndex(): Int {
        return this.bits.index
    }

    fun bufferSetIndex(index: Int) {
        this.bits.index = index
    }

    fun bufferWrite(buffers: FastArrayList<Uint8Buffer>): Int {
        return this.bits.write(buffers)
    }

    class Timestamp(val index: Int, val time: Double)

    open fun write(pts: Double, buffers: FastArrayList<Uint8Buffer>) {
        if (this.collectTimestamps) {
            if (this.timestamps.size == 0) {
                this.startTime = pts
                this.decodedTime = pts
            }
            this.timestamps.add(Timestamp(index = this.bytesWritten shl 3, time = pts))
        }

        this.bytesWritten += this.bufferWrite(buffers)
        this.canPlay = true
    }

    fun seek(time: Double) {
        if (!this.collectTimestamps) {
            return
        }

        this.timestampIndex = 0
        for (i in 0 until this.timestamps.length) {
            if (this.timestamps[i].time > time) {
                break
            }
            this.timestampIndex = i
        }

        val ts = this.timestamps.getOrNull(this.timestampIndex)
        if (ts != null) {
            this.bufferSetIndex(ts.index)
            this.decodedTime = ts.time
        }
        else {
            this.bufferSetIndex(0)
            this.decodedTime = this.startTime
        }
    }

    open fun decode(): Boolean {
        this.advanceDecodedTime(0.0)
        return true
    }

    fun advanceDecodedTime(seconds: Double) {
        if (this.collectTimestamps) {
            var newTimestampIndex = -1
            val currentIndex = this.bufferGetIndex()
            for (i in this.timestampIndex until this.timestamps.length) {
                if (this.timestamps[i].index > currentIndex) {
                    break
                }
                newTimestampIndex = i
            }

            // Did we find a new PTS, different from the last? If so, we don't have
            // to advance the decoded time manually and can instead sync it exactly
            // to the PTS.
            if (
                newTimestampIndex != -1 &&
                newTimestampIndex != this.timestampIndex
            ) {
                this.timestampIndex = newTimestampIndex
                this.decodedTime = this.timestamps[this.timestampIndex].time
                return
            }
        }

        this.decodedTime += seconds
    }
}
