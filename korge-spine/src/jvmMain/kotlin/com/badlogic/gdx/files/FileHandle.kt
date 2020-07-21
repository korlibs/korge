package com.badlogic.gdx.files

import java.io.*

class FileHandle {
    fun nameWithoutExtension(): String {
        throw NotImplementedError()
    }

    fun read(i: Int): ByteArray {
        return ByteArray(0)
    }

    fun reader(charset: String?): Reader {
        throw NotImplementedError()
    }

    fun pathWithoutExtension(): String? {
        return null
    }

    fun extension(): String? {
        return null
    }
}
