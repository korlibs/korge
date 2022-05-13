package com.soywiz.kds.algo

import com.soywiz.kds.IntArrayList

class RLE(val capacity: Int = 7) {
    val data = IntArrayList(capacity)

    val count get() = data.size / 3

    fun emit(start: Int, count: Int, value: Int) {
        this.data.add(start, count, value)
    }

    inline fun fastForEach(block: (n: Int, start: Int, count: Int, value: Int) -> Unit) {
        for (n in 0 until count) {
            block(n, data[n * 3 + 0], data[n * 3 + 1], data[n * 3 + 2])
        }
    }

    override fun toString(): String = buildString {
        append("RLE(")
        fastForEach { n, start, end, value ->
            if (n != 0) append(", ")
            append("[")
            append("(")
            append(value)
            append("),")
            append(start)
            append(",")
            append(end)
            append("]")
        }
        append(")")
    }

    fun clear() {
        data.clear()
    }

    companion object {
        inline fun compute(data: IntArray, start: Int = 0, end: Int = data.size, out: RLE = RLE()): RLE =
            compute(end - start, out) { data[start + it] }

        inline fun compute(count: Int, out: RLE = RLE(), filter: (value: Int) -> Boolean = { true }, gen: (index: Int) -> Int): RLE {
            out.clear()
            var lastValue = 0
            var currentStart = 0
            for (n in 0 until count + 1) {
                val value = if (n == count) lastValue + 1 else gen(n)
                if (n == 0 || value != lastValue) {
                    if (currentStart != n && filter(lastValue)) {
                        out.emit(currentStart, n - currentStart, lastValue)
                    }
                    currentStart = n
                }
                lastValue = value
            }
            return out
        }
    }
}
