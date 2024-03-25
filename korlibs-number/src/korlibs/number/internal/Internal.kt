package korlibs.number.internal

import kotlin.math.*

internal inline fun Float.toIntRound(): Int = round(this).toInt()
internal inline fun Double.toIntRound(): Int = round(this).toInt()
internal inline fun Double.toLongRound(): Long = round(this).toLong()
internal inline fun Int.reinterpretAsFloat(): Float = Float.fromBits(this)
internal fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits) // Int.SIZE_BITS
internal fun Int.mask(): Int = (1 shl this) - 1
