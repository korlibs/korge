package com.soywiz.korma.geom

import com.soywiz.kds.*

class BoundsBuilder {
    val tempRect = MRectangle()

    companion object {
        val POOL: ConcurrentPool<BoundsBuilder> = ConcurrentPool<BoundsBuilder>({ it.reset() }) { BoundsBuilder() }

        private val MIN = Double.NEGATIVE_INFINITY
        private val MAX = Double.POSITIVE_INFINITY
    }

    var npoints = 0; private set

    /**
     * True if some points were added to the [BoundsBuilder],
     * and thus [xmin], [xmax], [ymin], [ymax] have valid values
     **/
    val hasPoints: Boolean get() = npoints > 0

    /** Minimum value found for X. Infinity if ![hasPoints] */
    var xmin = MAX; private set
    /** Maximum value found for X. -Infinity if ![hasPoints] */
    var xmax = MIN; private set
    /** Minimum value found for Y. Infinity if ![hasPoints] */
    var ymin = MAX; private set
    /** Maximum value found for Y. -Infinity if ![hasPoints] */
    var ymax = MIN; private set

    /** Minimum value found for X. null if ![hasPoints] */
    val xminOrNull: Double? get() = if (hasPoints) xmin else null
    /** Maximum value found for X. null if ![hasPoints] */
    val xmaxOrNull: Double? get() = if (hasPoints) xmax else null
    /** Minimum value found for Y. null if ![hasPoints] */
    val yminOrNull: Double? get() = if (hasPoints) ymin else null
    /** Maximum value found for Y. null if ![hasPoints] */
    val ymaxOrNull: Double? get() = if (hasPoints) ymax else null

    /** Minimum value found for X. [default] if ![hasPoints] */
    fun xminOr(default: Double = 0.0): Double = if (hasPoints) xmin else default
    /** Maximum value found for X. [default] if ![hasPoints] */
    fun xmaxOr(default: Double = 0.0): Double = if (hasPoints) xmax else default
    /** Minimum value found for Y. [default] if ![hasPoints] */
    fun yminOr(default: Double = 0.0): Double = if (hasPoints) ymin else default
    /** Maximum value found for Y. [default] if ![hasPoints] */
    fun ymaxOr(default: Double = 0.0): Double = if (hasPoints) ymax else default

    fun isEmpty() = npoints == 0
    fun isNotEmpty() = npoints > 0

    fun reset() {
        xmin = MAX
        xmax = MIN
        ymin = MAX
        ymax = MIN
        npoints = 0
    }

    fun add(x: Double, y: Double): BoundsBuilder {
        if (x < xmin) xmin = x
        if (x > xmax) xmax = x
        if (y < ymin) ymin = y
        if (y > ymax) ymax = y
        npoints++
        //println("add($x, $y) -> ($xmin,$ymin)-($xmax,$ymax)")
        return this
    }

    fun add(x: Int, y: Int): BoundsBuilder = add(x.toDouble(), y.toDouble())
    fun add(x: Float, y: Float): BoundsBuilder = add(x.toDouble(), y.toDouble())
    fun add(x: Double, y: Double, transform: MMatrix?): BoundsBuilder = if (transform != null) add(transform.transformX(x, y), transform.transformY(x, y)) else add(x, y)
    fun add(x: Int, y: Int, transform: MMatrix?): BoundsBuilder = add(x.toDouble(), y.toDouble(), transform)
    fun add(x: Float, y: Float, transform: MMatrix?): BoundsBuilder = add(x.toDouble(), y.toDouble(), transform)
    fun add(p: Point, transform: MMatrix?): BoundsBuilder = add(p.x, p.y, transform)

    fun add(point: Point): BoundsBuilder = add(point.x, point.y)
    fun add(point: MPoint): BoundsBuilder = add(point.x, point.y)
    fun add(point: MPoint, transform: MMatrix): BoundsBuilder = add(point.x, point.y, transform)

    fun addRect(x: Int, y: Int, width: Int, height: Int): BoundsBuilder = addRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    fun addRect(x: Double, y: Double, width: Double, height: Double): BoundsBuilder = add(x, y).add(x + width, y + height)

    fun add(ps: Iterable<MPoint>): BoundsBuilder {
        for (p in ps) add(p)
        return this
    }
    fun add(ps: PointList): BoundsBuilder {
        for (n in 0 until ps.size) add(ps[n])
        return this
    }

    inline fun add(rect: MRectangle?): BoundsBuilder {
        rect?.let { addNonEmpty(rect) }
        return this
    }

    fun addNonEmpty(rect: MRectangle): BoundsBuilder {
        if (rect.isNotEmpty) {
            addEvenEmpty(rect)
        }
        return this
    }
    fun addEvenEmpty(rect: MRectangle?): BoundsBuilder {
        if (rect == null) return this
        add(rect.left, rect.top)
        add(rect.right, rect.bottom)
        return this
    }

    fun add(ps: Iterable<MPoint>, transform: MMatrix): BoundsBuilder {
        for (p in ps) add(p, transform)
        return this
    }
    fun add(ps: PointList, transform: MMatrix): BoundsBuilder {
        for (n in 0 until ps.size) add(ps.getX (n), ps.getY(n), transform)
        return this
    }
    fun add(rect: MRectangle, transform: MMatrix?): BoundsBuilder {
        if (rect.isNotEmpty) {
            add(rect.left, rect.top, transform)
            add(rect.right, rect.top, transform)
            add(rect.right, rect.bottom, transform)
            add(rect.left, rect.bottom, transform)
        }
        return this
    }

    fun getBoundsOrNull(out: MRectangle = MRectangle()): MRectangle? = if (npoints == 0) null else out.setBounds(xmin, ymin, xmax, ymax)

    fun getBounds(out: MRectangle = MRectangle()): MRectangle {
        if (getBoundsOrNull(out) == null) {
            out.setBounds(0, 0, 0, 0)
        }
        return out
    }
}
