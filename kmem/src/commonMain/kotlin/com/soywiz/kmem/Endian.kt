package com.soywiz.kmem

enum class Endian {
    LITTLE_ENDIAN, BIG_ENDIAN;

    companion object {
        val NATIVE = MemBufferAlloc(4).run {
            asInt32Buffer()[0] = 1
            if (asInt8Buffer()[0].toInt() == 1) LITTLE_ENDIAN else BIG_ENDIAN
        }
    }
}
