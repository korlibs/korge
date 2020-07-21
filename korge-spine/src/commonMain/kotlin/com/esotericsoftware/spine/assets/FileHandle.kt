package com.esotericsoftware.spine.assets

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*

class FileHandle(val filePath: String, val fileContent: ByteArray) {
    fun nameWithoutExtension(): String {
        return filePath.substringBeforeLast('.')
    }

    fun read(bufferSize: Int = 512): ByteArray {
        return fileContent
    }

    fun readAsString(): String {
        return fileContent.toString(Charsets.UTF8)
    }

    fun pathWithoutExtension(): String? {
        TODO()
    }

    fun extension(): String {
        return filePath.substringAfterLast('.')
    }
}

suspend fun VfsFile.toFileHandle(): FileHandle {
    return FileHandle(this.fullName, this.readAll())
}
