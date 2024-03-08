package korlibs.io.stream

import korlibs.datastructure.closeable.*

interface MarkableSyncInputStream : SyncInputStream {
    fun mark(readlimit: Int)
    fun reset()
}

interface SyncInputStream : OptionalCloseable {
    fun read(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int
    fun read(): Int {
        val it = ByteArray(1)
        return if (read(it, 0, 1) > 0) it[0].toUByte().toInt() else -1
    }
    fun skip(count: Int) {
        read(ByteArray(count))
    }
}

interface SyncOutputStream : OptionalCloseable {
    fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset): Unit
    fun write(byte: Int) = write(byteArrayOf(byte.toByte()), 0, 1)
    fun flush() = Unit
}

interface SyncPositionStream {
    var position: Long
}

interface SyncLengthStream {
    var length: Long
}

interface SyncRAInputStream {
    fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface SyncRAOutputStream {
    fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit
    fun flush(): Unit = Unit
}
