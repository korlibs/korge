package com.soywiz.korim.bitmap.trace

import com.soywiz.kds.*
import com.soywiz.kds.algo.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*

inline fun Bitmap.trace() = toBMP32().trace()
inline fun Bitmap.trace(func: (RGBA) -> Boolean): VectorPath = toBMP32().trace(func)

inline fun Bitmap32.trace() = trace { it.a >= 0x7F }
inline fun Bitmap32.trace(func: (RGBA) -> Boolean): VectorPath = this.toBitmap1(func).trace()

inline fun Bitmap1.trace(): VectorPath = VectorTracer().trace(this)

class VectorTracer(val doDebug: Boolean = false) {

    inner class MyRle(
        val left: Int,
        val right: Int,
        var leftPoints: LinkedPoints? = null,
        var rightPoints: LinkedPoints? = null,
    ) {
        inner class LinkedPoints(
            val id: Int,
            var linked: LinkedPoints? = null,
            var linkedReverse: Boolean = false,
        ) : PointIntArrayList() {
            override fun toString(): String = "LinkedPoints[$id](${super.toString()}, ${linked != null})"

            fun toVectorPath(optimize: Boolean = true): VectorPath {
                //fun toVectorPath(optimize: Boolean = false): VectorPath {
                if (linked != null) {
                    addReverse(linked!!)
                    if (linkedReverse) {
                        reverse()
                        linkedReverse = false
                    }
                    linked = null
                }
                val points = this
                return buildPath {
                    for (n in 0 until points.size) {
                        val x = points.getX(n)
                        val y = points.getY(n)
                        if (n == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                            if (optimize) {
                                optimizeLastCommand()
                            }
                        }
                    }
                    close()
                }
            }
        }

        fun intersectsWith(that: MyRle): Boolean = (this.left <= that.right) and (this.right >= that.left)
        fun intersections(that: List<MyRle>): List<MyRle> = that.filter { intersectsWith(it) }

        fun start(points: LinkedPoints?, left: Boolean) {
            debug { "start[${if (left) "left" else "right"}] = $points" }
        }

        fun startLeft(id: Int) {
            leftPoints = LinkedPoints(id)
            start(leftPoints, left = true)
        }
        fun startRight(id: Int, linked: LinkedPoints? = null) {
            rightPoints = LinkedPoints(id, linked)
            start(rightPoints, left = false)
        }

        fun finalize(out: VectorPath, points: LinkedPoints?, left: Boolean) {
            debug { "finalize[${if (left) "left" else "right"}] = $points" }
            if (points?.linked != null) {
                val vp = points.toVectorPath()
                debug { " -> ${vp.toSvgString()}" }
                out.write(vp)
            }
        }

        fun finalizeLeft(out: VectorPath) {
            finalize(out, leftPoints, left = true)
        }
        fun finalizeRight(out: VectorPath) {
            finalize(out, rightPoints, left = false)
        }
        fun finalizeLeftRight(out: VectorPath) {
            finalizeLeft(out)
            finalizeRight(out)
        }

        fun addLeftRight(y: Int, l: Int, r: Int) {
            leftPoints?.add(l, y)
            rightPoints?.add(r, y)
            debug { "add[$y] = $l, $r -- $leftPoints, $rightPoints" }
        }

        fun addLeft(x: Int, y: Int) {
            leftPoints?.add(x, y)
            debug { "add.left = ($x, y)" }
        }

        fun addRight(x: Int, y: Int) {
            rightPoints?.add(x, y)
            debug { "add.right = ($x, y)" }
        }
    }

    fun RLE.toMy() = FastArrayList<MyRle>().apply {
        fastForEach { n, start, count, value -> add(MyRle(start, start + count)) }
    }

    fun trace(bmp: Bitmap1): VectorPath {
        val out = VectorPath()
        var prevRles = FastArrayList<MyRle>()

        var id = 0

        for (y in 0 until bmp.height) {
            val nextRles = RLE.compute(bmp.width, filter = { it != 0 }) { x -> bmp[x, y] }.toMy()

            debug { "$nextRles" }

            for (nextRle in nextRles) {
                val intersections = nextRle.intersections(prevRles)

                when {
                    // 0 -> 1
                    intersections.isEmpty() -> {
                        debug { "0 -> 1" }
                        nextRle.startLeft(id++)
                        nextRle.startRight(id++, nextRle.leftPoints)
                        nextRle.addLeftRight(y, nextRle.left, nextRle.right)
                    }
                    // N -> 1
                    intersections.size >= 2 -> {
                        debug { "N -> 1 :: $intersections" }
                        for ((index, prevRle) in intersections.withIndex()) {
                            val first = index == 0
                            val last = index == intersections.size - 1
                            when {
                                first -> {
                                    nextRle.leftPoints = prevRle.leftPoints
                                    prevRle.finalizeRight(out)
                                }
                                last -> {
                                    prevRle.finalizeLeft(out)
                                    nextRle.rightPoints = prevRle.rightPoints
                                }
                                else -> {
                                    prevRle.finalizeLeftRight(out)
                                }
                            }
                        }
                        nextRle.addLeftRight(y, nextRle.left, nextRle.right)
                        debug { "  : $nextRle" }
                    }
                }
            }

            for (prevRle in prevRles) {
                val intersections = prevRle.intersections(nextRles)

                when {
                    // 1 -> 0
                    intersections.isEmpty() -> {
                        debug { "1 -> 0" }
                        prevRle.finalizeLeftRight(out)
                    }
                    //// 1 -> 1
                    intersections.size == 1 -> {
                        val nextRle = intersections.first()
                        if (nextRle.intersections(prevRles).size == 1) {
                            debug { "1 -> 1" }
                            nextRle.leftPoints = prevRle.leftPoints
                            nextRle.rightPoints = prevRle.rightPoints
                            nextRle.addLeftRight(y, nextRle.left, nextRle.right)
                        }
                    }
                    // 1 -> N
                    else -> {
                        debug { "1 -> N" }
                        var lastRight: MyRle.LinkedPoints? = null
                        for ((index, nextRle) in intersections.withIndex()) {
                            val first = index == 0
                            val last = index == intersections.size - 1
                            when {
                                intersections.size == 1 || (!first && !last) -> {
                                    nextRle.startLeft(id++)
                                    nextRle.startRight(id++, nextRle.leftPoints)
                                }
                                first -> {
                                    nextRle.leftPoints = prevRle.leftPoints
                                    nextRle.startRight(id++)
                                }
                                last -> {
                                    nextRle.startLeft(id++)
                                    nextRle.rightPoints = prevRle.rightPoints
                                }
                            }
                            nextRle.leftPoints?.linked = lastRight
                            nextRle.leftPoints?.linkedReverse = true
                            lastRight = nextRle.rightPoints
                            nextRle.addLeftRight(y, nextRle.left, nextRle.right)
                        }
                    }
                }
            }

            prevRles = nextRles
        }
        return out
    }

    inline fun debug(message: () -> String) {
        if (doDebug) {
            println(message())
        }
    }
}
