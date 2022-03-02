package com.soywiz.kmem

import kotlinx.cinterop.*

@PublishedApi internal val EmptyBytePtr: CPointer<ByteVarOf<Byte>> = ByteArray(1).pin().addressOf(0)
@PublishedApi internal val EmptyShortPtr: CPointer<ShortVarOf<Short>> = ShortArray(1).pin().addressOf(0)
@PublishedApi internal val EmptyIntPtr: CPointer<IntVarOf<Int>> = IntArray(1).pin().addressOf(0)
@PublishedApi internal val EmptyFloatPtr: CPointer<FloatVarOf<Float>> = FloatArray(1).pin().addressOf(0)

public actual class FastByteTransfer actual constructor() {
    @PublishedApi internal var ptr: CPointer<ByteVar> = EmptyBytePtr

    public actual inline operator fun get(index: Int): Byte = ptr[index]
    public actual inline operator fun set(index: Int, value: Byte) { ptr[index] = value }
    public actual inline fun use(array: ByteArray, block: (FastByteTransfer) -> Unit) {
        array.usePinned { pin ->
            ptr = pin.startAddressOf
            try {
                block(this)
            } finally {
                ptr = EmptyBytePtr
            }
        }
    }
}

public actual class FastShortTransfer actual constructor() {
    @PublishedApi internal var ptr: CPointer<ShortVar> = EmptyShortPtr

    public actual inline operator fun get(index: Int): Short = ptr[index]
    public actual inline operator fun set(index: Int, value: Short) { ptr[index] = value }
    public actual inline fun use(array: ShortArray, block: (FastShortTransfer) -> Unit) {
        array.usePinned { pin ->
            ptr = pin.startAddressOf
            try {
                block(this)
            } finally {
                ptr = EmptyShortPtr
            }
        }
    }
}

public actual class FastIntTransfer actual constructor() {
    @PublishedApi internal var ptr: CPointer<IntVar> = EmptyIntPtr

    public actual inline operator fun get(index: Int): Int = ptr[index]
    public actual inline operator fun set(index: Int, value: Int) { ptr[index] = value }
    public actual inline fun use(array: IntArray, block: (FastIntTransfer) -> Unit) {
        array.usePinned { pin ->
            ptr = pin.startAddressOf
            try {
                block(this)
            } finally {
                ptr = EmptyIntPtr
            }
        }
    }
}

public actual class FastFloatTransfer actual constructor() {
    @PublishedApi internal var ptr: CPointer<FloatVar> = EmptyFloatPtr

    public actual inline operator fun get(index: Int): Float = ptr[index]
    public actual inline operator fun set(index: Int, value: Float) { ptr[index] = value }
    public actual inline fun use(array: FloatArray, block: (FastFloatTransfer) -> Unit) {
        array.usePinned { pin ->
            ptr = pin.startAddressOf
            try {
                block(this)
            } finally {
                ptr = EmptyFloatPtr
            }
        }
    }
}
