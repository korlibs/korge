package korlibs.memory.dyn

import com.sun.jna.*

fun Memory(data: IntArray): Memory {
    val out = Memory(data.size.toLong() * 4)
    for (n in data.indices) out.setInt((n * 4).toLong(), data[n])
    return out
}

fun Memory(data: LongArray): Memory {
    val out = Memory(data.size.toLong() * 8)
    for (n in data.indices) out.setLong((n * 8).toLong(), data[n])
    return out
}

val Pointer.address: Long get() = Pointer.nativeValue(this)
