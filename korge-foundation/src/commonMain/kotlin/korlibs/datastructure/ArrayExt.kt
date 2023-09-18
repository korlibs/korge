package korlibs.datastructure

import korlibs.datastructure.internal.*
import korlibs.math.*

public fun <T> MutableList<T>.reverse(fromIndex: Int, toIndex: Int): Unit {
    if (fromIndex < 0 || toIndex > size) {
        throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
    }
    if (fromIndex > toIndex) {
        throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
    }
    val midPoint = (fromIndex + toIndex) / 2
    if (fromIndex == midPoint) return
    var reverseIndex = toIndex - 1
    for (index in fromIndex until midPoint) {
        val tmp = this[index]
        this[index] = this[reverseIndex]
        this[reverseIndex] = tmp
        reverseIndex--
    }
}


fun BooleanArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun ByteArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun CharArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun ShortArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun IntArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun LongArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun FloatArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun DoubleArray.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun <T> Array<T>.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }
fun <T> MutableList<T>.swap(lIndex: Int, rIndex: Int) { val temp = this[lIndex]; this[lIndex] = this[rIndex]; this[rIndex] = temp }

fun BooleanArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun ByteArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun CharArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun ShortArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun IntArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun LongArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun FloatArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun DoubleArray.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun <T> Array<T>.rotateLeft(offset: Int = +1) = rotateRight(-offset)
fun <T> MutableList<T>.rotateLeft(offset: Int = +1) = rotateRight(-offset)

fun BooleanArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun ByteArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun CharArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun ShortArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun IntArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun LongArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun FloatArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun DoubleArray.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun <T> Array<T>.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }
fun <T> MutableList<T>.rotateRight(offset: Int = +1) = _rotateRight(size, offset) { start, end -> reverse(start, end) }

fun BooleanArray.rotatedLeft(offset: Int = +1): BooleanArray = copyOf().also { it.rotateLeft(offset) }
fun ByteArray.rotatedLeft(offset: Int = +1): ByteArray = copyOf().also { it.rotateLeft(offset) }
fun CharArray.rotatedLeft(offset: Int = +1): CharArray = copyOf().also { it.rotateLeft(offset) }
fun ShortArray.rotatedLeft(offset: Int = +1): ShortArray = copyOf().also { it.rotateLeft(offset) }
fun IntArray.rotatedLeft(offset: Int = +1): IntArray = copyOf().also { it.rotateLeft(offset) }
fun LongArray.rotatedLeft(offset: Int = +1): LongArray = copyOf().also { it.rotateLeft(offset) }
fun FloatArray.rotatedLeft(offset: Int = +1): FloatArray = copyOf().also { it.rotateLeft(offset) }
fun DoubleArray.rotatedLeft(offset: Int = +1): DoubleArray = copyOf().also { it.rotateLeft(offset) }
fun <T> Array<T>.rotatedLeft(offset: Int = +1): Array<T> = copyOf().also { it.rotateLeft(offset) }
fun <T> List<T>.rotatedLeft(offset: Int = +1): List<T> = toMutableList().also { it.rotateLeft(offset) }

fun BooleanArray.rotatedRight(offset: Int = +1): BooleanArray = copyOf().also { it.rotateRight(offset) }
fun ByteArray.rotatedRight(offset: Int = +1): ByteArray = copyOf().also { it.rotateRight(offset) }
fun CharArray.rotatedRight(offset: Int = +1): CharArray = copyOf().also { it.rotateRight(offset) }
fun ShortArray.rotatedRight(offset: Int = +1): ShortArray = copyOf().also { it.rotateRight(offset) }
fun IntArray.rotatedRight(offset: Int = +1): IntArray = copyOf().also { it.rotateRight(offset) }
fun LongArray.rotatedRight(offset: Int = +1): LongArray = copyOf().also { it.rotateRight(offset) }
fun FloatArray.rotatedRight(offset: Int = +1): FloatArray = copyOf().also { it.rotateRight(offset) }
fun DoubleArray.rotatedRight(offset: Int = +1): DoubleArray = copyOf().also { it.rotateRight(offset) }
fun <T> Array<T>.rotatedRight(offset: Int = +1): Array<T> = copyOf().also { it.rotateRight(offset) }
fun <T> List<T>.rotatedRight(offset: Int = +1): List<T> = toMutableList().also { it.rotateRight(offset) }

private inline fun _rotateRight(size: Int, offset: Int, reverse: (start: Int, end: Int) -> Unit) {
    val offset = offset umod size
    if (offset == 0) return
    check(offset in 1 until size)
    reverse(0, size)
    reverse(0, offset)
    reverse(offset, size)
}
