package com.soywiz.korim.util

import com.soywiz.kds.DoubleArrayList
import com.soywiz.korma.geom.range.DoubleRangeExclusive
import com.soywiz.korma.geom.range.until

class NinePatchSlices private constructor(val ranges: List<DoubleRangeExclusive>, dummy: Unit) {
    constructor(ranges: List<DoubleRangeExclusive>) : this(ranges.sortedBy { it.start }, Unit)
    constructor(vararg ranges: DoubleRangeExclusive) : this(ranges.sortedBy { it.start }, Unit)
    companion object {
        operator fun invoke(vararg doubles: Double): NinePatchSlices {
            if (doubles.size % 2 != 0) error("Number of slices must be pair")
            return NinePatchSlices((0 until (doubles.size / 2)).map { doubles[it * 2] until doubles[it * 2 + 1] })
        }
    }

    val lengths get() = ranges.sumOf { it.length }

    // @TODO: newLen < oldLen should make corners (non-stretch areas) smaller
    inline fun transform1DInplace(oldLen: Double, newLen: Double, count: Int, get: (index: Int) -> Double, set: (index: Int, value: Double) -> Unit, iscale: Double = 1.0) {
        val slices: NinePatchSlices = this
        val rscale = if (slices.ranges.isEmpty()) {
            newLen / oldLen
        } else {
            val rscale = if (newLen / iscale < oldLen) newLen / oldLen else iscale
            val scale = (newLen / rscale - oldLen) / slices.lengths
            val position = get(count - 1)
            for (slice in slices.ranges) {
                if (position > slice.start) {
                    var offset = slice.length * scale
                    if (position <= slice.endExclusive) offset *= (position - slice.start) / slice.length
                    for (i in 0 until count) set(i, get(i) + offset)
                }
            }
            rscale
        }
        for (i in 0 until count) set(i, get(i) * rscale)
    }

    fun transform1DInplace(positions: DoubleArrayList, oldLen: Double, newLen: Double) {
        transform1DInplace(oldLen, newLen, positions.size, get = { positions[it] }, set = { index, value -> positions[index] = value })
    }

    fun transform1D(input: DoubleArrayList, oldLen: Double, newLen: Double, output: DoubleArrayList = DoubleArrayList()): DoubleArrayList {
        output.size = input.size
        for (n in 0 until input.size) output[n] = input[n]
        transform1DInplace(output, oldLen, newLen)
        return output
    }

    fun transform1D(input: List<DoubleArrayList>, oldLen: Double, newLen: Double): List<DoubleArrayList> =
        input.map { transform1D(it, oldLen, newLen) }

    override fun hashCode(): Int = ranges.hashCode()
    override fun equals(other: Any?): Boolean = other is NinePatchSlices && this.ranges == other.ranges
    override fun toString(): String = "NinePatchSlices(${ranges.joinToString(", ")})"
}
