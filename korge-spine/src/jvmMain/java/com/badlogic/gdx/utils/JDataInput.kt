package com.badlogic.gdx.utils

import java.io.IOException

open class JDataInput(private val data: ByteArray) {
    fun readFloat(): Float = TODO()
    fun readBoolean(): Boolean = TODO()
    fun readInt(b: Boolean): Int = TODO()
    fun readInt(): Int = TODO()
    fun readByte(): Byte = TODO()
    fun read(): Int = TODO()
    @Throws(IOException::class)
    fun close(): Unit = TODO()
    fun readShort(): Short = TODO()
}
