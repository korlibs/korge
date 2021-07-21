package com.soywiz.kmem

class FixedSizeByteArrayBuilder(val data: ByteArray) {
    val capacity: Int get() = data.size
    var size: Int = 0
        private set
    constructor(size: Int) : this(ByteArray(size))

    fun clear() {
        size = 0
    }

    fun append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) {
        arraycopy(array, offset, this.data, size, len)
        this.size += len
    }

    fun appendFast(v: Byte) {
        data[size++] = v
    }

    inline fun append(v: Byte): FixedSizeByteArrayBuilder {
        appendFast(v)
        return this
    }

    fun append(vararg v: Byte) = append(v)
    fun append(vararg v: Int) {
        for (n in 0 until v.size) this.data[this.size + n] = v[n].toByte()
        this.size += v.size
    }

    fun toByteArray() = data.copyOf(size)
}
