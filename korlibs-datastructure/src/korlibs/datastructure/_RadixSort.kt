package korlibs.datastructure

import korlibs.datastructure.internal.*
import korlibs.datastructure.internal.memory.Memory.arraycopy
import korlibs.datastructure.internal.memory.Memory.countLeadingZeros
import kotlin.math.*

fun <T : CharSequence> Array<T>.sortedRadix(start: Int = 0, end: Int = this.size, default: Char = '\u0000', transform: (Char) -> Char = { it }): Array<T> = this.copyOf().also { it.sortRadix(start, end, default, transform) }
fun <T : CharSequence> Array<T>.sortRadix(start: Int = 0, end: Int = this.size, default: Char = '\u0000', transform: (Char) -> Char = { it }) {
    val maxLength = (start ..< end).maxOfOrNull { this[it].length } ?: return
    val maxChar = maxOf((start ..< end).maxOfOrNull { this[it].max().code } ?: return, default.code)
    val temp = arrayOfNulls<CharSequence>(end - start)
    //println("maxChar=$maxChar, usedBits=${maxChar.usedBits()}, offsets=${offsets.size}")
    //println(maxChar)
    radixSortGeneric(
        start, end,
        0, maxLength,
        get = { this[it] },
        setTemp = { n, it -> temp[n] = it },
        flip = { arraycopy(temp, 0, this, start, temp.size) },
        getRadix = { n, it -> transform(it.getOrElse(maxLength - 1 - n) { default }).code and 0xFFFF },
        noffsets = 1 shl (maxChar.usedBits())
    )
}

fun IntArray.sortedArrayRadix(start: Int = 0, end: Int = this.size, bits: Int = 16): IntArray = this.copyOf().also { it.sortRadix(start, end, bits) }

fun IntArray.sortRadix(start: Int = 0, end: Int = this.size, bits: Int = 16) {
    val maxBits = this.maxBits(start, end)
    val bits = bits.coerceIn(1, min(16, maxBits))
    val temp = IntArray(end - start)
    val mask = (1 shl bits) - 1

    radixSortGeneric(
        start, end,
        0, ceil(maxBits.toFloat() / bits.toFloat()).toInt(),
        get = { this[it] },
        setTemp = { n, it -> temp[n - start] = it },
        flip = { arraycopy(temp, 0, this, start, temp.size) },
        getRadix = { n, it -> (it ushr (n * bits)) and mask },
        noffsets = 1 shl bits
    )
}

inline fun <T> radixSortGeneric(start: Int, end: Int, stepStart: Int, stepEnd: Int, get: (index: Int) -> T, setTemp: (index: Int, v: T) -> Unit, flip: () -> Unit, getRadix: (index: Int, v: T) -> Int, noffsets: Int) {
    val offsets = IntArray(noffsets)
    for (n in stepStart..<stepEnd) {
        _radixSortStep(start, end, get = get, setTemp = setTemp, getRadix = { getRadix(n, it) }, offsets)
        offsets.fill(0)
        flip()
    }
}

@PublishedApi
internal inline fun <T> _radixSortStep(start: Int, end: Int, get: (Int) -> T, setTemp: (Int, T) -> Unit, getRadix: (T) -> Int, offsets: IntArray) {
    for (n in start..<end) offsets[getRadix(get(n))]++
    for (i in 1..<offsets.size) offsets[i] += offsets[i - 1]
    for (i in start..<end) {
        val v = get(end - 1 - i)
        val index = getRadix(v)
        setTemp(offsets[index] - 1, v)
        offsets[index]--
    }
}

private fun Int.usedBits(): Int = 32 - this.countLeadingZeros()

private fun IntArray.maxBits(start: Int = 0, end: Int = size): Int {
    var maxNum = 0u
    for (n in start..< end) maxNum = max(maxNum, this[n].toUInt())
    val maxBits = maxNum.toInt().usedBits()
    return maxBits
}
