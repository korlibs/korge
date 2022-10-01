package com.soywiz.korio.file.std

import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.stream.AsyncStream
import kotlinx.coroutines.flow.Flow

object EmptyVfs : Vfs() {
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        throw FileNotFoundException(path)
    }
    override suspend fun stat(path: String): VfsStat {
        return createNonExistsStat(path)
    }

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        throw FileNotFoundException(path)
        //return emptyList()
    }
}
