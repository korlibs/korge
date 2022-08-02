package com.soywiz.korvi.mpeg.mux

import com.soywiz.kmem.Uint8Buffer
import com.soywiz.korvi.mpeg.stream.DecoderBase

/**
 * A Demuxer may sit between a Source and a Decoder. It separates the
 * incoming raw data into Video, Audio and other Streams.
 */
interface Demuxer {
    fun connect(streamId: Int, destinationNode: DecoderBase<*>)
    fun write(buffer: Uint8Buffer)
    /** In seconds */
    val currentTime: Double
    /** In seconds */
    val startTime: Double
}
