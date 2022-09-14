package com.soywiz.korim.util

import com.soywiz.kds.DoubleArrayList
import com.soywiz.korma.geom.range.DoubleRangeExclusive

class NinePatchSlices private constructor(val ranges: List<DoubleRangeExclusive>, dummy: Unit) {
    constructor(ranges: List<DoubleRangeExclusive>) : this(ranges.sortedBy { it.start }, Unit)
    constructor(vararg ranges: DoubleRangeExclusive) : this(ranges.sortedBy { it.start }, Unit)
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
        transform1DInplace(oldLen, newLen, input.size, get = { output[it] }, set = { index, value -> output[index] = value })
        return output
    }

    fun transform1D(input: List<DoubleArrayList>, oldLen: Double, newLen: Double): List<DoubleArrayList> =
        input.map { transform1D(it, oldLen, newLen) }

}
