package com.soywiz.korio.file.std

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

class LogVfs(val parent: VfsFile) : Vfs.Proxy() {
	val log = arrayListOf<String>()
	val logstr get() = log.toString()
	val modifiedFiles = LinkedHashSet<String>()
	override suspend fun access(path: String): VfsFile = parent[path]

	override suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler
	): Int {
		log += "exec($path, $cmdAndArgs, $env, $handler)"
		return super.exec(path, cmdAndArgs, env, handler)
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		log += "open($path, $mode)"
		val base = super.open(path, mode)
		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				base.position = position
				return base.read(buffer, offset, len)
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				base.position = position
				base.write(buffer, offset, len)
				modifiedFiles += path
			}

			override suspend fun setLength(value: Long) {
				base.setLength(value)
				modifiedFiles += path
			}

			override suspend fun getLength(): Long {
				return base.getLength()
			}

			override suspend fun close() {
				return base.close()
			}
		}.toAsyncStream()
	}

	override suspend fun readRange(path: String, range: LongRange): ByteArray {
		log += "readRange($path, $range)"
		return super.readRange(path, range)
	}

	override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		modifiedFiles += path
		log += "put($path, $content, $attributes)"
		return super.put(path, content, attributes)
	}

	override suspend fun setSize(path: String, size: Long) {
		modifiedFiles += path
		log += "setSize($path, $size)"
		super.setSize(path, size)
	}

	override suspend fun stat(path: String): VfsStat {
		log += "stat($path)"
		return super.stat(path)
	}

	override suspend fun list(path: String): ReceiveChannel<VfsFile> {
		log += "list($path)"
		return super.list(path)
	}

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        log += "listFlow($path)"
        return super.listFlow(path)
    }

    override suspend fun delete(path: String): Boolean {
		modifiedFiles += path
		log += "delete($path)"
		return super.delete(path)
	}

	override suspend fun setAttributes(path: String, attributes: List<Attribute>) {
		modifiedFiles += path
		log += "setAttributes($path, $attributes)"
		super.setAttributes(path, attributes)
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		modifiedFiles += path
		log += "mkdir($path, $attributes)"
		return super.mkdir(path, attributes)
	}

	override suspend fun touch(path: String, time: DateTime, atime: DateTime) {
		modifiedFiles += path
		log += "touch($path, $time, $atime)"
		super.touch(path, time, atime)
	}

	override suspend fun rename(src: String, dst: String): Boolean {
		modifiedFiles += src
		modifiedFiles += dst
		log += "rename($src, $dst)"
		return super.rename(src, dst)
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		log += "watch($path)"
		return super.watch(path, handler)
	}

	override fun toString(): String = "LogVfs"
}

fun VfsFile.log() = LogVfs(this).root
