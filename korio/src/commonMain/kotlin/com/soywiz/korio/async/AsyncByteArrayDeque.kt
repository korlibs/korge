package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.channels.*

class AsyncByteArrayDeque(private val bufferSize: Int = 1024) : AsyncOutputStream, AsyncInputStream {
    var name: String? = null
	private val notifyRead = Channel<Unit>(Channel.CONFLATED)
	private val notifyWrite = Channel<Unit>(Channel.CONFLATED)
	private val temp = ByteArrayDeque(ilog2(bufferSize) + 1)
	private var completed = false

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
		if (len <= 0) return
        if (completed) error("Trying to write to a completed $this")
        //println("$this.write[0]: len=$len")

        notifyRead.receive()

        //println("$this.write[1]")
		temp.write(buffer, offset, len)
        //println("$this.write[2]")
		notifyWrite.send(Unit)
        //println("$this.write[3]")
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		if (len <= 0) return len
        if (temp.availableRead > 0) {
            return temp.read(buffer, offset, len)
        }

		notifyRead.send(Unit)
        //println("$this:read.completed=$completed")
		while (!completed && temp.availableRead == 0) notifyWrite.receive()
		if (completed && temp.availableRead == 0) return -1
		return temp.read(buffer, offset, len)
	}

	override suspend fun close() {
        //println("AsyncByteArrayDeque.close[$this]")
		completed = true
		notifyWrite.send(Unit)
	}

    override fun toString(): String = "AsyncByteArrayDeque($name)"
}
