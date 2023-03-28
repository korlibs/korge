package korlibs.image.util

import korlibs.datastructure.*
import korlibs.math.geom.range.*

class NinePatchSlices private constructor(val ranges: List<FloatRangeExclusive>, dummy: Unit) {
    constructor(ranges: List<FloatRangeExclusive>) : this(ranges.sortedBy { it.start }, Unit)
    constructor(vararg ranges: FloatRangeExclusive) : this(ranges.sortedBy { it.start }, Unit)
    companion object {
        operator fun invoke(vararg values: Float): NinePatchSlices {
            if (values.size % 2 != 0) error("Number of slices must be pair")
            return NinePatchSlices((0 until (values.size / 2)).map { values[it * 2] until values[it * 2 + 1] })
        }
    }

    val lengths: Float get() = ranges.sumOf { it.length.toDouble() }.toFloat()

    // @TODO: newLen < oldLen should make corners (non-stretch areas) smaller
    inline fun transform1DInplace(oldLen: Float, newLen: Float, count: Int, get: (index: Int) -> Float, set: (index: Int, value: Float) -> Unit, iscale: Float = 1f) {
        val slices: NinePatchSlices = this
        val rscale = if (slices.ranges.isEmpty()) {
            newLen / oldLen
        } else {
            val rscale: Float = if (newLen / iscale < oldLen) newLen / oldLen else iscale
            val scale: Float = (newLen / rscale - oldLen) / slices.lengths
            val position: Float = get(count - 1)
            for (slice in slices.ranges) {
                if (position > slice.start) {
                    var offset: Float = slice.length.toFloat() * scale
                    if (position <= slice.endExclusive) offset *= (position - slice.start.toFloat()) / slice.length.toFloat()
                    for (i in 0 until count) set(i, get(i) + offset)
                }
            }
            rscale
        }
        for (i in 0 until count) set(i, get(i) * rscale)
    }

    fun transform1DInplace(positions: FloatArrayList, oldLen: Float, newLen: Float) {
        transform1DInplace(oldLen, newLen, positions.size, get = { positions[it] }, set = { index, value -> positions[index] = value })
    }

    fun transform1D(input: FloatArrayList, oldLen: Float, newLen: Float, output: FloatArrayList = FloatArrayList()): FloatArrayList {
        output.size = input.size
        for (n in 0 until input.size) output[n] = input[n]
        transform1DInplace(output, oldLen, newLen)
        return output
    }

    fun transform1D(input: List<FloatArrayList>, oldLen: Float, newLen: Float): List<FloatArrayList> =
        input.map { transform1D(it, oldLen, newLen) }

    override fun hashCode(): Int = ranges.hashCode()
    override fun equals(other: Any?): Boolean = other is NinePatchSlices && this.ranges == other.ranges
    override fun toString(): String = "NinePatchSlices(${ranges.joinToString(", ")})"
}
