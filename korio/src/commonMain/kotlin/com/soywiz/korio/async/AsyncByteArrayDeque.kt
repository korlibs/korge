package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.channels.*

class AsyncByteArrayDeque(private val bufferSize: Int = 1024) : AsyncOutputStream, AsyncInputStream {
	private val notifyRead = Channel<Unit>(Channel.CONFLATED)
	private val notifyWrite = Channel<Unit>(Channel.CONFLATED)
	private val temp = ByteArrayDeque(ilog2(bufferSize) + 1)
	private var completed = false

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
		if (len <= 0) return
		while (temp.availableRead > bufferSize) {
			notifyRead.receive()
		}
		temp.write(buffer, offset, len)
		notifyWrite.send(Unit)
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		if (len <= 0) return len
		notifyRead.send(Unit)
		while (!completed && temp.availableRead == 0) notifyWrite.receive()
		if (completed && temp.availableRead == 0) return -1
		return temp.read(buffer, offset, len)
	}

	override suspend fun close() {
		completed = true
		notifyWrite.send(Unit)
	}
}
