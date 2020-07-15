package com.soywiz.korio.async

import com.soywiz.kmem.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*
import kotlin.experimental.*
import kotlin.math.*

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

@UseExperimental(ExperimentalTypeInference::class)
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
