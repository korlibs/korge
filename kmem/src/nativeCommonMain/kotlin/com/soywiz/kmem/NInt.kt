package com.soywiz.kmem

import kotlinx.cinterop.*

inline class NInt(val data: CPointer<IntVar>?) {
    constructor(value: Int) : this(value.toLong().toCPointer<IntVar>())
    constructor(value: Long) : this(value.toCPointer<IntVar>())

    inline val int: Int get() = data.toLong().toInt()
    inline val long: Long get() = data.toLong()
}
