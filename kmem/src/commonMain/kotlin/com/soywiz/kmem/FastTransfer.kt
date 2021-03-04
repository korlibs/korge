package com.soywiz.kmem

expect class FastByteTransfer() {
    inline operator fun get(index: Int): Byte
    inline operator fun set(index: Int, value: Byte)
    inline fun use(array: ByteArray, block: (FastByteTransfer) -> Unit)
}

expect class FastShortTransfer() {
    inline operator fun get(index: Int): Short
    inline operator fun set(index: Int, value: Short)
    inline fun use(array: ShortArray, block: (FastShortTransfer) -> Unit)
}

expect class FastIntTransfer() {
    inline operator fun get(index: Int): Int
    inline operator fun set(index: Int, value: Int)
    inline fun use(array: IntArray, block: (FastIntTransfer) -> Unit)
}

expect class FastFloatTransfer() {
    inline operator fun get(index: Int): Float
    inline operator fun set(index: Int, value: Float)
    inline fun use(array: FloatArray, block: (FastFloatTransfer) -> Unit)
}
