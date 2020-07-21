package com.badlogic.gdx.utils

import java.io.IOException
import java.io.EOFException



open class JDataInput(private val data: ByteArray) {
    private var n = 0

    fun read(): Int {
        if (n >= data.size) {
            return -1
        }
        return data[n++].toInt() and 0xFF
    }

    fun close(): Unit {
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readBoolean(): Boolean {
        return readUnsignedByte() != 0
    }

    fun readInt(optimizePositive: Boolean): Int {
        var b = read()
        var result = b and 0x7F
        if (b and 0x80 != 0) {
            b = read()
            result = result or (b and 0x7F shl 7)
            if (b and 0x80 != 0) {
                b = read()
                result = result or (b and 0x7F shl 14)
                if (b and 0x80 != 0) {
                    b = read()
                    result = result or (b and 0x7F shl 21)
                    if (b and 0x80 != 0) {
                        b = read()
                        result = result or (b and 0x7F shl 28)
                    }
                }
            }
        }
        return if (optimizePositive) result else result.ushr(1) xor -(result and 1)
    }
    fun readInt(): Int {
        val a = readUnsignedByte()
        val b = readUnsignedByte()
        val c = readUnsignedByte()
        val d = readUnsignedByte()
        return a shl 24 or (b shl 16) or (c shl 8) or d
    }

    fun readUnsignedByte(): Int {
        val i = read()
        if (i == -1) {
            throw EOFException()
        }
        return i
    }

    fun readByte(): Byte {
        return readUnsignedByte().toByte()
    }

    fun readShort(): Short {
        val a = readUnsignedByte()
        val b = readUnsignedByte()
        return (a shl 8 or b).toShort()
    }}
