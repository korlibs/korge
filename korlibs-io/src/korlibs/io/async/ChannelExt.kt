package korlibs.io.async

import korlibs.memory.arraycopy
import korlibs.io.stream.AsyncInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.coroutineContext
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.min

suspend fun <T> ReceiveChannel<T>.chunks(count: Int) = produce {
	val chunk = arrayListOf<T>()

	for (e in this@chunks) {
		chunk += e
		if (chunk.size >= count) {
			send(chunk.toList())
			chunk.clear()
		}
	}

	if (chunk.size > 0) {
		send(chunk.toList())
	}
}

suspend fun <T> Iterable<T>.toChannel(): ReceiveChannel<T> = produce { for (v in this@toChannel) send(v) }

suspend fun <T> Flow<T>.toChannel(): ReceiveChannel<T> = produce { this@toChannel.collect { send(it) }  }

@OptIn(ExperimentalTypeInference::class)
suspend fun <E> produce(capacity: Int = 0, @BuilderInference block: suspend ProducerScope<E>.() -> Unit): ReceiveChannel<E> =
	CoroutineScope(coroutineContext).produce(coroutineContext, capacity, block)

fun ReceiveChannel<ByteArray>.toAsyncInputStream(): AsyncInputStream {
	val channel = this
	return object : AsyncInputStream {
		var currentData: ByteArray = byteArrayOf()
		var currentPos = 0
		val currentAvailable get() = currentData.size - currentPos

		override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			if (len <= 0) return 0
			while (currentAvailable <= 0) {
				currentData = channel.receive()
				currentPos = 0
			}

			val toRead = min(currentAvailable, len)
			arraycopy(currentData, currentPos, buffer, offset, toRead)
			currentPos += toRead
			return toRead
		}

		override suspend fun close() {
			channel.cancel()
		}
	}
}
