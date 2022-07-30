package com.soywiz.korvi.mpeg

import com.soywiz.klock.PerformanceCounter
import com.soywiz.kmem.BaseIntBuffer
import com.soywiz.kmem.Int32Buffer
import com.soywiz.kmem.Int8Buffer
import com.soywiz.kmem.MemBufferWrap
import com.soywiz.kmem.NewInt8Buffer
import com.soywiz.kmem.Uint32Buffer
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.get
import com.soywiz.kmem.size
import com.soywiz.krypto.encoding.fromBase64

// This sets up the JSMpeg "Namespace". The object is empty apart from the Now()
// utility function and the automatic CreateVideoElements() after DOMReady.
object JSMpeg {
    // The Player sets up the connections between source, demuxer, decoders,
    // renderer and audio output. It ties everything together, is responsible
    // of scheduling decoding and provides some convenience methods for
    // external users.
    //var Player: Any? = null

    // A Video Element wraps the Player, shows HTML controls to start/pause
    // the video and handles Audio unlocking on iOS. VideoElements can be
    // created directly in HTML using the <div class="jsmpeg"/> tag.
    //var VideoElement: Any? = null

    // The BitBuffer wraps a Uint8Array and allows reading an arbitrary number
    // of bits at a time. On writing, the BitBuffer either expands its
    // internal buffer (for static files) or deletes old data (for streaming).
    //var BitBuffer: BitBuffer? = null

    // A Source provides raw data from HTTP, a WebSocket connection or any
    // other mean. Sources must support the following API:
    //   .connect(destinationNode)
    //   .write(buffer)
    //   .start() - start reading
    //   .resume(headroom) - continue reading; headroom to play pos in seconds
    //   .established - boolean, true after connection is established
    //   .completed - boolean, true if the source is completely loaded
    //   .progress - float 0-1
    //var Source: Any? = {},

    // A Decoder accepts an incoming Stream of raw Audio or Video data, buffers
    // it and upon `.decode()` decodes a single frame of data. Video decoders
    // call `destinationNode.render(Y, Cr, CB)` with the decoded pixel data;
    // Audio decoders call `destinationNode.play(left, right)` with the decoded
    // PCM data. API:
    //   .connect(destinationNode)
    //   .write(pts, buffer)
    //   .decode()
    //   .seek(time)
    //   .currentTime - float, in seconds
    //   .startTime - float, in seconds
    //var Decoder: {},

    // A Renderer accepts raw YCrCb data in 3 separate buffers via the render()
    // method. Renderers typically convert the data into the RGBA color space
    // and draw it on a Canvas, but other output - such as writing PNGs - would
    // be conceivable. API:
    //   .render(y, cr, cb) - pixel data as Uint8Arrays
    //   .enabled - wether the renderer does anything upon receiving data
    //var Renderer: {},

    // Audio Outputs accept raw Stero PCM data in 2 separate buffers via the
    // play() method. Outputs typically play the audio on the user's device.
    // API:
    //   .play(sampleRate, left, right) - rate in herz; PCM data as Uint8Arrays
    //   .stop()
    //   .enqueuedTime - float, in seconds
    //   .enabled - wether the output does anything upon receiving data
    //var AudioOutput: {},

    fun Now(): Double = PerformanceCounter.reference.seconds

    fun CreateVideoElements() {
        TODO()
        //var elements = document.querySelectorAll('.jsmpeg');
        //for (var i = 0; i < elements.length; i++) {
        //    new JSMpeg.VideoElement(elements[i]);
        //}
    }

    fun Base64ToArrayBuffer(base64: String): Uint8Buffer = Uint8Buffer(com.soywiz.kmem.buffer.Int8Buffer(MemBufferWrap(base64.fromBase64())))
}

@Deprecated("", ReplaceWith("size")) internal val <T> List<T>.length: Int get() = size
@Deprecated("", ReplaceWith("size")) internal val Array<*>.length: Int get() = size
@Deprecated("", ReplaceWith("size")) internal val FloatArray.length: Int get() = size
@Deprecated("", ReplaceWith("size")) internal val IntArray.length: Int get() = size

internal val Uint8Buffer.byteLength: Int get() = size
internal fun Uint8Buffer.set(other: Uint8Buffer, targetOffset: Int = 0) {
    //for (n in 0 until other.size) this[targetOffset + n] = other[n]
    arraycopy(other.b, 0, this.b, targetOffset, other.size)
}

internal fun hashArray(array: IntArray): Int {
    var hash = 0;
    for (n in array.indices) hash = (hash + (array[n] * (n + 1)))
    return hash
}

internal fun hashArray(array: Int32Buffer): Int {
    var hash = 0;
    for (n in 0 until array.size) hash = (hash + (array[n] * (n + 1)))
    return hash
}

internal fun hashArray(array: Uint32Buffer): Int {
    var hash = 0;
    for (n in 0 until array.size) hash = (hash + ((array[n].toInt() and 0x7fffffff) * (n + 1)))
    return hash
}

internal fun hashArray(array: BaseIntBuffer): Int {
    var hash = 0;
    for (n in 0 until array.size) hash = (hash + (array[n] * (n + 1)))
    return hash
}
