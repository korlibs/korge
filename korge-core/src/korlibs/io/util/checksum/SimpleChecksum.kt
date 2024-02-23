package korlibs.io.util.checksum

import korlibs.io.async.use
import korlibs.io.stream.*

interface SimpleChecksum {
	val initialValue: Int
	fun update(old: Int, data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int
}

fun SimpleChecksum.compute(data: ByteArray, offset: Int = 0, len: Int = data.size - offset) = update(initialValue, data, offset, len)

fun ByteArray.checksum(checksum: SimpleChecksum): Int = checksum.compute(this)

fun SyncInputStream.checksum(checksum: SimpleChecksum): Int {
	var value = checksum.initialValue
	val temp = ByteArray(1024)

	while (true) {
		val read = this.read(temp)
		if (read <= 0) break
		value = checksum.update(value, temp, 0, read)
	}

	return value
}

suspend fun AsyncInputStream.checksum(checksum: SimpleChecksum): Int {
	var value = checksum.initialValue
	val temp = ByteArray(1024)

	while (true) {
		val read = this.read(temp)
		if (read <= 0) break
		value = checksum.update(value, temp, 0, read)
	}

	return value
}

suspend fun AsyncInputOpenable.checksum(checksum: SimpleChecksum) = this.openRead().use { this.checksum(checksum) }

fun SimpleChecksum.updater(): SimpleChecksumUpdater = SimpleChecksumUpdater(this)

class SimpleChecksumUpdater(val checksum: SimpleChecksum) {
    var current: Int = checksum.initialValue
        private set

    fun update(data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int {
        current = checksum.update(current, data, offset, len)
        return current
    }
}

fun AsyncOutputStream.withChecksumUpdater(checksum: SimpleChecksumUpdater): AsyncOutputStream = object : AsyncOutputStream by this@withChecksumUpdater {
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
        checksum.update(buffer, offset, len)
        this@withChecksumUpdater.write(buffer, offset, len)
    }
}
