package korlibs.io.stream

import java.io.*

fun InputStream.toSyncInputStream(): SyncInputStream = object : SyncInputStream {
    override fun read(): Int = this@toSyncInputStream.read()
    override fun read(buffer: ByteArray, offset: Int, len: Int): Int = this@toSyncInputStream.read(buffer, offset, len)
    override fun close() = this@toSyncInputStream.close()
    override fun skip(count: Int) { this@toSyncInputStream.skip(count.toLong()) }
}

fun OutputStream.toSyncOutputStream(): SyncOutputStream = object : SyncOutputStream {
    override fun write(buffer: ByteArray, offset: Int, len: Int) {
        return this@toSyncOutputStream.write(buffer, offset, len).also {
            this@toSyncOutputStream.flush()
        }
    }

    override fun write(byte: Int) = this@toSyncOutputStream.write(byte)
    override fun flush() = this@toSyncOutputStream.flush()
    override fun close() = this@toSyncOutputStream.close()
}
