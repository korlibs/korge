package korlibs.audio.internal

import korlibs.datastructure.ByteArrayDeque
import korlibs.memory.arraycopy
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.SyncStream

internal fun List<ShortArray>.combine(): ShortArray {
    val combined = ShortArray(this.sumBy { it.size })
    var pos = 0
    for (buffer in this) {
        arraycopy(buffer, 0, combined, pos, buffer.size)
        pos += size
    }
    return combined
}

internal suspend  fun AsyncStream.copyChunkTo(deque: ByteArrayDeque, temp: ByteArray, maxSize: Int = temp.size): Int {
    val size = this.read(temp, 0, maxSize)
    deque.write(temp, 0, size)
    return size
}

internal suspend fun AsyncStream.copyChunkTo(out: SyncStream, temp: ByteArray, maxSize: Int = temp.size): Int {
    val size = this.read(temp, 0, maxSize)
    out.write(temp, 0, size)
    return size
}

internal fun ShortArray.toByteArrayLE(): ByteArray {
    val out = ByteArray(this.size * 2)
    for (n in 0 until this.size) {
        out[n * 2 + 0] = (this[n].toInt() shr 0).toByte()
        out[n * 2 + 1] = (this[n].toInt() shr 8).toByte()
    }
    return out
}

internal fun ByteArray.toShortArrayLE(): ShortArray {
    val out = ShortArray(this.size / 2)
    for (n in 0 until out.size) {
        val l = this[n * 2 + 0].toInt() and 0xFF
        val h = this[n * 2 + 1].toInt() and 0xFF
        out[n] = ((h shl 8) or l).toShort()
    }
    return out
}
