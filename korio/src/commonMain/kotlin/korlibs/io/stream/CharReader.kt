package korlibs.io.stream

import korlibs.datastructure.*
import korlibs.io.lang.*

interface CharReader {
    fun read(out: StringBuilder, count: Int): Int
    fun clone(): CharReader
}
fun CharReader.read(count: Int): String = buildString { read(this, count) }

fun ByteArray.toCharReader(charset: Charset, chunkSize: Int = 1024): CharReader = openSync().toCharReader(charset, chunkSize)
fun SyncStream.toCharReader(charset: Charset, chunkSize: Int = 1024): CharReader =
    CharReaderFromSyncStream(this, charset, chunkSize)

class CharReaderFromSyncStream(val stream: SyncStream, val charset: Charset, val chunkSize: Int = 1024) : CharReader {
    private val temp = ByteArray(chunkSize)
    private val buffer = ByteArrayDeque()
    private var tempStringBuilder = StringBuilder()

    override fun clone(): CharReader = CharReaderFromSyncStream(stream.clone(), charset, chunkSize)

    override fun read(out: StringBuilder, count: Int): Int {
        while (buffer.availableRead < temp.size) {
            val readCount = stream.read(temp)
            if (readCount <= 0) break
            buffer.write(temp, 0, readCount)
        }

        while (tempStringBuilder.length < count) {
            val readCount = buffer.peek(temp)
            val consumed = charset.decode(tempStringBuilder, temp, 0, readCount)
            if (consumed <= 0) break
            buffer.skip(consumed)
        }

        //println("tempStringBuilder=$tempStringBuilder")

        val slice = tempStringBuilder.substring(0, kotlin.math.min(count, tempStringBuilder.length))
        tempStringBuilder = StringBuilder(slice.length).append(tempStringBuilder.substring(slice.length))

        out.append(slice)
        return slice.length
    }
}