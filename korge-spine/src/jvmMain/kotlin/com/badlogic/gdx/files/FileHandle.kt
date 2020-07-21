package com.badlogic.gdx.files

import java.io.*
import java.net.*

class FileHandle(val url: URL) {
    constructor(resourcePath: String, classLoader: ClassLoader = ClassLoader.getSystemClassLoader()) : this(
        classLoader.getResource(resourcePath)
    )

    fun nameWithoutExtension(): String {
        return url.file.substringBeforeLast('.')
    }

    fun read(@Suppress("UNUSED_PARAMETER") bufferSize: Int): ByteArray {
        return url.readBytes()
    }

    fun reader(charset: String?): Reader {
        TODO()
    }

    fun pathWithoutExtension(): String? {
        TODO()
    }

    fun extension(): String? {
        TODO()
    }
}
