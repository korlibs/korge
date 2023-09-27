package korlibs.io.stream

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.io.lang.*

class ByteArrayDequeSyncStream(val deque: ByteArrayDeque) : SyncStreamBase() {
    override val separateReadWrite: Boolean get() = true
    override val seekable: Boolean get() = false
    var closed = false
    private val lock = Lock()

    override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        loop@while (true) {
            for (n in 0 until 4) {
                if (closed) return -1
                if (deque.availableRead > 0) break@loop
                Thread_sleep(n.toLong())
            }
        }
        return lock { deque.read(buffer, offset, len) }
    }

    override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
        if (closed) error("Writing to a closed stream")
        lock { deque.write(buffer, offset, len) }
    }

    override var length: Long
        get() = TODO()
        set(value) = TODO()

    override fun close() {
        closed = true
    }
}

fun ByteArrayDeque.toSyncStream(): SyncStream = ByteArrayDequeSyncStream(this).toSyncStream()
