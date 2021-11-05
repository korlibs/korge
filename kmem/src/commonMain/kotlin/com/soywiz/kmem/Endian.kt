package com.soywiz.kmem

public enum class Endian {
    LITTLE_ENDIAN, BIG_ENDIAN;

    public companion object {
        public val NATIVE: Endian = MemBufferAlloc(4).run {
            asInt32Buffer()[0] = 1
            if (asInt8Buffer()[0].toInt() == 1) LITTLE_ENDIAN else BIG_ENDIAN
        }
    }
}
