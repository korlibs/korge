package korlibs.io.stream

import java.io.InputStream
import java.io.OutputStream

fun InputStream.toSyncInputStream(): SyncInputStream {
    return object : SyncInputStream {
        override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
            //println("READ[s]: $len")
            return this@toSyncInputStream.read(buffer, offset, len).also {
                //println("READ[e]: $len")
            }
        }
    }
}

fun OutputStream.toSyncOutputStream(): SyncOutputStream {
    return object : SyncOutputStream {
        override fun write(buffer: ByteArray, offset: Int, len: Int) {
            //println("WRITE[s]: $len")
            return this@toSyncOutputStream.write(buffer, offset, len).also {
                this@toSyncOutputStream.flush()
                //println("WRITE[e]: $len")
            }
        }
    }
}
