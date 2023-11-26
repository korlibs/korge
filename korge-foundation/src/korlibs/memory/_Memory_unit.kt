@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.unit

import kotlin.math.pow
import kotlin.math.roundToInt

fun test() {

}

inline class ByteUnits private constructor(val bytes: Double) {
    val bytesLong: Long get() = bytes.toLong()
    val kiloBytes: Double get() = bytes / 1024.0
    val megaBytes: Double get() = bytes / (1024.0 * 1024.0)
    val gigaBytes: Double get() = bytes / (1024.0 * 1024.0 * 1024.0)

    operator fun unaryMinus(): ByteUnits = fromBytes(-this.bytesLong)
    operator fun plus(other: ByteUnits): ByteUnits = fromBytes(this.bytes + other.bytes)
    operator fun minus(other: ByteUnits): ByteUnits = fromBytes(this.bytes - other.bytes)
    operator fun rem(other: ByteUnits): ByteUnits = fromBytes(this.bytes % other.bytes)
    operator fun times(other: Number): ByteUnits = fromBytes((this.bytes * other.toDouble()))
    operator fun div(other: Number): ByteUnits = fromBytes((this.bytes / other.toDouble()))
    operator fun div(other: ByteUnits): Double = this.bytes / other.bytes

    companion object {
        val ZERO = ByteUnits(0.0)

        fun fromBytes(bytes: Double): ByteUnits = ByteUnits(bytes)
        fun fromBytes(bytes: Int): ByteUnits = ByteUnits(bytes.toDouble())
        fun fromBytes(bytes: Long): ByteUnits = ByteUnits(bytes.toDouble())
        fun fromBytes(bytes: Number): ByteUnits = fromBytes(bytes.toDouble())
        fun fromKiloBytes(kiloBytes: Number): ByteUnits = fromBytes((kiloBytes.toDouble() * 1024))
        fun fromMegaBytes(megaBytes: Number): ByteUnits = fromBytes((megaBytes.toDouble() * 1024 * 1024))
        fun fromGigaBytes(gigaBytes: Number): ByteUnits = fromBytes((gigaBytes.toDouble() * 1024 * 1024 * 1024))
    }
    private fun Double.roundToDigits(digits: Int): Double {
        val num = 10.0.pow(digits)
        return (this * num).roundToInt().toDouble() / num
    }
    override fun toString(): String = when {
        gigaBytes >= 1.0 -> "${gigaBytes.roundToDigits(1)} GB"
        megaBytes >= 1.0 -> "${megaBytes.roundToDigits(1)} MB"
        kiloBytes >= 1.0 -> "${kiloBytes.roundToDigits(1)} KB"
        else -> "$bytesLong B"
    }
}
