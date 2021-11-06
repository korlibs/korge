package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.channels.*

interface IAsyncByteArrayDeque : AsyncOutputStream, AsyncInputStream

class AsyncByteArrayDeque(private val bufferSize: Int = 1024) : IAsyncByteArrayDeque {
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

class AsyncByteArrayDequeChunked(val maxSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE) : IAsyncByteArrayDeque {
    companion object {
        const val DEFAULT_MAX_SIZE = 8 * 1024 * 1024
    }

    var name: String? = null
    private val chunks = ChunkedByteDeque()
    private var completed = false

    private suspend fun waitToWriteMore(len: Int) {
        if (chunks.availableRead > maxSize) {
            while (chunks.availableRead > 1 + (maxSize / 2)) {
                //println("WRITE WAITING: availableRead=${chunks.availableRead} > ${1 + (maxSize / 2)}")
                //delay(100.milliseconds) // @TODO: Proper synchronization
                delay(1.milliseconds) // @TODO: Proper synchronization
            }
        }
    }

    override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
        if (len <= 0) return
        if (completed) error("Trying to write to a completed $this")

        waitToWriteMore(len)
        chunks.write(buffer, offset, len)
    }

    override suspend fun write(byte: Int) {
        waitToWriteMore(1)
        chunks.write(byte)
    }

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        if (len <= 0) return 0
        if (offset < 0 || offset + len > buffer.size) throw OutOfBoundsException()
        while (true) {
            val out = chunks.read(buffer, offset, len)

            if (out <= 0 && !completed) {
                //println("READ WAITING: out=$out, completed=$completed")
                //delay(100.milliseconds) // @TODO: Proper synchronization
                delay(1.milliseconds) // @TODO: Proper synchronization
                continue
            }

            return out
        }
    }

    override suspend fun close() {
        completed = true
    }

    override fun toString(): String = "AsyncByteArrayDequeV2($name)"
}
