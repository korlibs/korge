package korlibs.image.bitmap.trace

import korlibs.datastructure.*
import korlibs.datastructure.algo.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

inline fun Bitmap.trace() = toBMP32().trace()
inline fun Bitmap.trace(func: (RGBA) -> Boolean): VectorPath = toBMP32().trace(func)

inline fun Bitmap32.trace() = trace { it.a >= 0x3F }
inline fun Bitmap32.trace(func: (RGBA) -> Boolean): VectorPath = this.toBitmap1(func).trace()

//inline fun Bitmap1.trace(): VectorPath = VectorTracer(this).trace()

// @TODO: Combine broken shapes
inline fun Bitmap1.trace(): VectorPath = VectorTracer(this).simpleTrace()

// @TODO: Could we use it to merge several VectorPaths ? with a scanline on each available Y?
class VectorTracer(
    val bmp: Bitmap1,
    val doDebug: Boolean = false
) {
    val comparer = RLEComparer(
        rlePool = Pool(reset = {
            it.left = 0
            it.right = 0
            it.leftPoints = null
            it.rightPoints = null
        }) { LinkedRle() },
        doDebug = doDebug
    )

    inner class LinkedRle(
        var leftPoints: LinkedPoints? = null,
        var rightPoints: LinkedPoints? = null,
    ) : RLEComparer.Rle() {
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
                return buildVectorPath(VectorPath(), fun VectorPath.() {
                    for (n in 0 until points.size) {
                        val x = points.getX(n)
                        val y = points.getY(n)
                        if (n == 0) {
                            moveTo(Point(x, y))
                        } else {
                            lineTo(Point(x, y))
                            if (optimize) {
                                optimizeLastCommand()
                            }
                        }
                    }
                    close()
                })
            }
        }

        fun intersectsWith(that: LinkedRle): Boolean = (this.left <= that.right) and (this.right >= that.left)
        fun intersections(that: List<LinkedRle>): List<LinkedRle> = that.filter { intersectsWith(it) }

        fun start(points: LinkedPoints?, left: Boolean) {
            comparer.debug { "start[${if (left) "left" else "right"}] = $points" }
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
            comparer.debug { "finalize[${if (left) "left" else "right"}] = $points" }
            if (points?.linked != null) {
                val vp = points.toVectorPath()
                comparer.debug { " -> ${vp.toSvgString()}" }
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
            comparer.debug { "add[$y] = $l, $r -- $leftPoints, $rightPoints" }
        }

        fun addLeft(x: Int, y: Int) {
            leftPoints?.add(x, y)
            comparer.debug { "add.left = ($x, y)" }
        }

        fun addRight(x: Int, y: Int) {
            rightPoints?.add(x, y)
            comparer.debug { "add.right = ($x, y)" }
        }
    }

    var id = 0
    val out = VectorPath()

    fun commonTrace(
        ops: RLEComparer.Ops<LinkedRle>
    ): VectorPath {
        comparer.compare(ops, bmp.width, bmp.height) { x, y -> bmp[x, y] != 0 }
        return out
    }

    // @TODO: This is not working properly
    fun complexBuggyTrace(): VectorPath = commonTrace(object : RLEComparer.Ops<LinkedRle> {
        override fun zeroToOne(y: Int, nextRle: LinkedRle) {
            nextRle.startLeft(id++)
            nextRle.startRight(id++, nextRle.leftPoints)
            nextRle.addLeftRight(y, nextRle.left, nextRle.right)
        }

        override fun manyToOne(y: Int, prevRles: List<LinkedRle>, nextRle: LinkedRle) {
            for ((index, prevRle) in prevRles.withIndex()) {
                val first = index == 0
                val last = index == prevRles.size - 1
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
            comparer.debug { "  : $nextRle" }
        }

        override fun oneToZero(y: Int, prevRle: LinkedRle) {
            prevRle.finalizeLeftRight(out)
        }

        override fun oneToOne(y: Int, prevRle: LinkedRle, nextRle: LinkedRle) {
            nextRle.leftPoints = prevRle.leftPoints
            nextRle.rightPoints = prevRle.rightPoints
            nextRle.addLeftRight(y, nextRle.left, nextRle.right)
        }

        override fun oneToMany(y: Int, prevRle: LinkedRle, nextRles: List<LinkedRle>) {
            var lastRight: LinkedRle.LinkedPoints? = null
            for ((index, nextRle) in nextRles.withIndex()) {
                val first = index == 0
                val last = index == nextRles.size - 1
                when {
                    nextRles.size == 1 || (!first && !last) -> {
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
    })

    // @TODO: This doesn't combine sub-shapes
    fun simpleTrace(): VectorPath = commonTrace(object : RLEComparer.Ops<LinkedRle> {
        override fun zeroToOne(y: Int, nextRle: LinkedRle) {
            nextRle.init(y)
        }

        override fun manyToOne(y: Int, prevRles: List<LinkedRle>, nextRle: LinkedRle) {
            for (prevRle in prevRles) {
                prevRle.addLeftRight(y, prevRle.left, prevRle.right)
                prevRle.finalizeLeftRight(out)
            }
            nextRle.init(y)
        }

        override fun oneToZero(y: Int, prevRle: LinkedRle) {
            prevRle.finalizeLeftRight(out)
        }

        override fun oneToOne(y: Int, prevRle: LinkedRle, nextRle: LinkedRle) {
            nextRle.leftPoints = prevRle.leftPoints
            nextRle.rightPoints = prevRle.rightPoints
            nextRle.addLeftRight(y, nextRle.left, nextRle.right)
        }

        override fun oneToMany(y: Int, prevRle: LinkedRle, nextRles: List<LinkedRle>) {
            prevRle.addLeftRight(y, nextRles.first().left, nextRles.last().right)
            prevRle.finalizeLeftRight(out)
            for (nextRle in nextRles) {
                nextRle.init(y)
            }
        }

        private fun LinkedRle.init(y: Int) {
            startLeft(id++)
            startRight(id++, leftPoints)
            addLeftRight(y, left, right)
        }
    })
}
