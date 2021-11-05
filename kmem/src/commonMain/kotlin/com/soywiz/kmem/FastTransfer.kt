package com.soywiz.kmem

expect public class FastByteTransfer() {
    public inline operator fun get(index: Int): Byte
    public inline operator fun set(index: Int, value: Byte)
    public inline fun use(array: ByteArray, block: (FastByteTransfer) -> Unit)
}

expect public class FastShortTransfer() {
    public inline operator fun get(index: Int): Short
    public inline operator fun set(index: Int, value: Short)
    public inline fun use(array: ShortArray, block: (FastShortTransfer) -> Unit)
}

expect public class FastIntTransfer() {
    public inline operator fun get(index: Int): Int
    public inline operator fun set(index: Int, value: Int)
    public inline fun use(array: IntArray, block: (FastIntTransfer) -> Unit)
}

expect public class FastFloatTransfer() {
    public inline operator fun get(index: Int): Float
    public inline operator fun set(index: Int, value: Float)
    public inline fun use(array: FloatArray, block: (FastFloatTransfer) -> Unit)
}
