package com.soywiz.korio.util

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*

inline operator fun ByteArray.set(o: Int, v: Int) = run { this[o] = v.toByte() }
inline operator fun ByteArray.set(o: Int, v: Long) = run { this[o] = v.toByte() }

private inline fun <TArray> _join(items: List<TArray>, build: (Int) -> TArray, size: (TArray) -> Int, arraycopy: (TArray, Int, TArray, Int, Int) -> Unit): TArray {
	val out = build(items.sumBy { size(it) })
	var pos = 0
	items.fastForEach { c ->
		arraycopy(c, 0, out, pos, size(c))
		pos += size(c)
	}
	return out
}

fun List<BooleanArray>.join(): BooleanArray = _join(this, { BooleanArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<ByteArray>.join(): ByteArray = _join(this, { ByteArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<ShortArray>.join(): ShortArray = _join(this, { ShortArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
//fun List<CharArray>.join(): CharArray = _join(this, { CharArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<IntArray>.join(): IntArray = _join(this, { IntArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<LongArray>.join(): LongArray = _join(this, { LongArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<FloatArray>.join(): FloatArray = _join(this, { FloatArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })
fun List<DoubleArray>.join(): DoubleArray = _join(this, { DoubleArray(it) }, { it.size }, { a, b, c, d, e -> arraycopy(a, b, c, d, e) })

private inline fun <TArray, T> _indexOf(array: TArray, access: (TArray, Int) -> T, v: T, startOffset: Int, endOffset: Int, default: Int): Int {
	for (n in startOffset until endOffset) if (access(array, n) == v) return n
	return default
}

fun BooleanArray.indexOf(v: Boolean, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun ByteArray.indexOf(v: Byte, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun ShortArray.indexOf(v: Short, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
//fun CharArray.indexOf(v: Char, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun IntArray.indexOf(v: Int, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun LongArray.indexOf(v: Long, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun FloatArray.indexOf(v: Float, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
fun DoubleArray.indexOf(v: Double, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int = _indexOf(this, { a, b -> a[b] }, v, startOffset, endOffset, default)
