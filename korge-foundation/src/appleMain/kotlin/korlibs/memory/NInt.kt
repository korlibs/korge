@file:OptIn(ExperimentalForeignApi::class)

package korlibs.memory

import kotlinx.cinterop.*

public inline class NInt(public val data: CPointer<IntVar>?) {
    public constructor(value: Int) : this(value.toLong().toCPointer<IntVar>())
    public constructor(value: Long) : this(value.toCPointer<IntVar>())

    public inline val int: Int get() = data.toLong().toInt()
    public inline val long: Long get() = data.toLong()
}
