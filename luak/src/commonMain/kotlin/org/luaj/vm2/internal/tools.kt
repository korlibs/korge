package org.luaj.vm2.internal

import org.luaj.vm2.io.*
import kotlin.reflect.*

// @TODO: Make this internal
internal fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)
internal fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)
internal fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)
@Suppress("UNCHECKED_CAST")
internal fun arraycopy(src: Array<*>, srcPos: Int, dst: Array<*>, dstPos: Int, count: Int) = (src as Array<Any>).copyInto(dst as Array<Any>, dstPos, srcPos, srcPos + count)

internal val KClass<*>.portableName: String get() = JSystem.Class_portableName(this)
internal fun KClass<*>.isInstancePortable(ins: Any): Boolean = JSystem.Class_isInstancePortable(this, ins)
internal fun KClass<*>.getResourceAsStreamPortable(res: String): LuaBinInput? = JSystem.Class_getResourceAsStreamPortable(this, res)

internal fun Throwable.printStackTrace() {
    println(this)
}

internal fun Int.toHexString() = toString(16)
internal fun Char.isDigit() = this in '0'..'9'
internal fun Char.isLowerCase() = this.toLowerCase() == this
internal fun Char.isUpperCase() = this.toUpperCase() == this

internal class LuaDate(val time: Long) {
    constructor() : this(TODO())

    constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, ms: Int) : this() {
        TODO()
    }

    val year: Int get() = TODO()
    val month: Int get() = TODO()
    val month1: Int get() = TODO()
    val day: Int get() = TODO()
    val wday: Int get() = TODO()
    val yday: Int get() = TODO()
    val hour: Int get() = TODO()
    val minute: Int get() = TODO()
    val second: Int get() = TODO()
    val ms: Int get() = TODO()
}
