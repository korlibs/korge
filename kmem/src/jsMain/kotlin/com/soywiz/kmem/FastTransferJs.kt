package com.soywiz.kmem

@PublishedApi internal val EmptyByteArray = ByteArray(1)
@PublishedApi internal val EmptyShortArray = ShortArray(1)
@PublishedApi internal val EmptyIntArray = IntArray(1)
@PublishedApi internal val EmptyFloatArray = FloatArray(1)

public actual class FastByteTransfer actual constructor() {
    @PublishedApi internal var ptr: ByteArray = EmptyByteArray

    public actual inline operator fun get(index: Int): Byte = ptr[index]
    public actual inline operator fun set(index: Int, value: Byte) { ptr[index] = value }
    public actual inline fun use(array: ByteArray, block: (FastByteTransfer) -> Unit) {
        try {
            ptr = array
            block(this)
        } finally {
            ptr = EmptyByteArray
        }
    }
}

public actual class FastShortTransfer actual constructor() {
    @PublishedApi internal var ptr: ShortArray = EmptyShortArray

    public actual inline operator fun get(index: Int): Short = ptr[index]
    public actual inline operator fun set(index: Int, value: Short) { ptr[index] = value }
    public actual inline fun use(array: ShortArray, block: (FastShortTransfer) -> Unit) {
        try {
            ptr = array
            block(this)
        } finally {
            ptr = EmptyShortArray
        }
    }
}

public actual class FastIntTransfer actual constructor() {
    @PublishedApi internal var ptr: IntArray = EmptyIntArray

    public actual inline operator fun get(index: Int): Int = ptr[index]
    public actual inline operator fun set(index: Int, value: Int) { ptr[index] = value }
    public actual inline fun use(array: IntArray, block: (FastIntTransfer) -> Unit) {
        try {
            ptr = array
            block(this)
        } finally {
            ptr = EmptyIntArray
        }
    }
}

public actual class FastFloatTransfer actual constructor() {
    @PublishedApi internal var ptr: FloatArray = EmptyFloatArray

    public actual inline operator fun get(index: Int): Float = ptr[index]
    public actual inline operator fun set(index: Int, value: Float) { ptr[index] = value }
    public actual inline fun use(array: FloatArray, block: (FastFloatTransfer) -> Unit) {
        try {
            ptr = array
            block(this)
        } finally {
            ptr = EmptyFloatArray
        }
    }
}
