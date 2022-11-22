package com.soywiz.korio.file.sync

import com.soywiz.korio.lang.*

expect val platformSyncIO: SyncIO

interface SyncIO {
    companion object : SyncIO by platformSyncIO

    open fun realpath(path: String): String = path
    open fun readlink(path: String): String? = null
    open fun open(path: String, mode: String): SyncIOFD = TODO()
    open fun stat(path: String): SyncIOStat? = TODO()
    open fun mkdir(path: String): Boolean = TODO()
    open fun rmdir(path: String): Boolean = TODO()
    open fun delete(path: String): Boolean = TODO()
    open fun list(path: String): List<String> = TODO()
}

data class SyncIOStat(val path: String, val isDirectory: Boolean, val size: Long)

interface SyncIOFD : Closeable {
    var length: Long
    var position: Long
    fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
    fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
}
