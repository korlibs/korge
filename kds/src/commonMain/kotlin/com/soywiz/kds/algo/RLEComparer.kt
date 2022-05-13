package com.soywiz.kds.algo

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.Pool

class RLEComparer<T : RLEComparer.Rle>(
    val rlePool: Pool<T>,
    val doDebug: Boolean = false,
) {
    open class Rle(
        var left: Int = 0,
        var right: Int = 0,
    ) {
        fun setTo(left: Int, right: Int) {
            this.left = left
            this.right = right
        }
    }

    fun <T : Rle> T.intersectsWith(that: T): Boolean = (this.left <= that.right) and (this.right >= that.left)
    fun <T : Rle> T.intersections(that: List<T>): List<T> = that.filter { intersectsWith(it) }

    interface Ops<T : Rle> {
        fun zeroToOne(y: Int, nextRle: T): Unit
        fun manyToOne(y: Int, prevRles: List<T>, nextRle: T): Unit
        fun oneToZero(y: Int, prevRle: T): Unit
        fun oneToOne(y: Int, prevRle: T, nextRle: T): Unit
        fun oneToMany(y: Int, prevRle: T, nextRles: List<T>): Unit
    }

    inline fun debug(message: () -> String) {
        if (doDebug) {
            println(message())
        }
    }

    fun compare(
        ops: Ops<T>,
        width: Int,
        height: Int,
        get: (x: Int, y: Int) -> Boolean,
    ) {
        var prevRles = FastArrayList<T>()

        fun RLE.toMy() = FastArrayList<T>().apply {
            fastForEach { n, start, count, value -> add(rlePool.alloc().also { it.setTo(start, start + count) }) }
        }

        for (y in 0 until height) {
            val nextRles = RLE.compute(width, filter = { it != 0 }) { x -> if (get(x, y)) 1 else 0 }.toMy()

            debug { "$nextRles" }

            for (nextRle in nextRles) {
                val intersections = nextRle.intersections(prevRles)

                when {
                    // 0 -> 1
                    intersections.isEmpty() -> {
                        debug { "0 -> 1" }
                        ops.zeroToOne(y, nextRle)
                    }
                    // N -> 1
                    intersections.size >= 2 -> {
                        debug { "N -> 1 :: $intersections" }
                        ops.manyToOne(y, intersections, nextRle)
                    }
                }
            }

            for (prevRle in prevRles) {
                val intersections = prevRle.intersections(nextRles)

                when {
                    // 1 -> 0
                    intersections.isEmpty() -> {
                        debug { "1 -> 0" }
                        ops.oneToZero(y, prevRle)
                    }
                    //// 1 -> 1
                    intersections.size == 1 -> {
                        val nextRle = intersections.first()
                        if (nextRle.intersections(prevRles).size == 1) {
                            debug { "1 -> 1" }
                            ops.oneToOne(y, prevRle, nextRle)
                        }
                    }
                    // 1 -> N
                    else -> {
                        debug { "1 -> N" }
                        ops.oneToMany(y, prevRle, intersections)
                    }
                }
            }

            rlePool.free(prevRles)
            prevRles = nextRles
        }
    }
}
