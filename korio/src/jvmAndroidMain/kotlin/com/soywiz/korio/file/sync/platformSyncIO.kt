package com.soywiz.korio.file.sync

import java.io.*
import java.nio.file.*
import kotlin.io.path.*

actual val platformSyncIO: SyncIO = object : SyncIO() {
    override fun realpath(path: String): String = File(path).canonicalPath
    override fun readlink(path: String): String? = kotlin.runCatching {
        Files.readSymbolicLink(File(path).toPath())?.absolutePathString()
    }.getOrNull()


    override fun open(path: String, mode: String): SyncIOFD {
        val file = File(path)
        val raf = RandomAccessFile(file, mode)
        return object : SyncIOFD {
            override var length: Long
                get() = raf.length()
                set(value) { raf.setLength(value) }
            override var position: Long
                get() = raf.filePointer
                set(value) { raf.seek(value) }

            override fun write(data: ByteArray, offset: Int, size: Int): Int = raf.write(data, offset, size).run { size }
            override fun read(data: ByteArray, offset: Int, size: Int): Int = raf.read(data, offset, size)
            override fun close() = raf.close()
        }
    }

    override fun stat(path: String): SyncIOStat? = File(path).takeIf { it.exists() }?.let { SyncIOStat(path, it.isDirectory, it.length()) }
    override fun mkdir(path: String): Boolean = File(path).mkdir()
    override fun rmdir(path: String): Boolean = File(path).takeIf { it.isDirectory }?.delete() ?: false
    override fun delete(path: String): Boolean = File(path).takeIf { !it.isDirectory }?.delete() ?: false
    override fun list(path: String): List<String> = File(path).list().toList()
}
