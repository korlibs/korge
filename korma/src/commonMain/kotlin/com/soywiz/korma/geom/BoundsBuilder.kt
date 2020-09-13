package com.soywiz.korma.geom

class BoundsBuilder {
    val tempRect = Rectangle()

    companion object {
        private val MIN = Double.NEGATIVE_INFINITY
        private val MAX = Double.POSITIVE_INFINITY
    }

    var npoints = 0; private set
    var xmin = MAX; private set
    var xmax = MIN; private set
    var ymin = MAX; private set
    var ymax = MIN; private set

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

    fun add(x: Double, y: Double, transform: Matrix): BoundsBuilder = add(transform.transformX(x, y), transform.transformY(x, y))
    fun add(x: Int, y: Int, transform: Matrix): BoundsBuilder = add(x.toDouble(), y.toDouble(), transform)
    fun add(x: Float, y: Float, transform: Matrix): BoundsBuilder = add(x.toDouble(), y.toDouble(), transform)

    fun add(point: IPoint): BoundsBuilder = add(point.x, point.y)
    fun add(point: IPoint, transform: Matrix): BoundsBuilder = add(point.x, point.y, transform)

    fun add(ps: Iterable<IPoint>): BoundsBuilder {
        for (p in ps) add(p)
        return this
    }
    fun add(ps: IPointArrayList): BoundsBuilder {
        for (n in 0 until ps.size) add(ps.getX(n), ps.getY(n))
        return this
    }
    fun add(rect: Rectangle): BoundsBuilder {
        if (rect.isNotEmpty) {
            add(rect.left, rect.top)
            add(rect.right, rect.bottom)
        }
        return this
    }

    fun add(ps: Iterable<IPoint>, transform: Matrix): BoundsBuilder {
        for (p in ps) add(p, transform)
        return this
    }
    fun add(ps: IPointArrayList, transform: Matrix): BoundsBuilder {
        for (n in 0 until ps.size) add(ps.getX(n), ps.getY(n), transform)
        return this
    }
    fun add(rect: Rectangle, transform: Matrix): BoundsBuilder {
        if (rect.isNotEmpty) {
            add(rect.left, rect.top, transform)
            add(rect.right, rect.bottom, transform)
        }
        return this
    }

    fun getBoundsOrNull(out: Rectangle = Rectangle()): Rectangle? = if (npoints == 0) null else out.setBounds(xmin, ymin, xmax, ymax)

    fun getBounds(out: Rectangle = Rectangle()): Rectangle {
        if (getBoundsOrNull(out) == null) {
            out.setBounds(0, 0, 0, 0)
        }
        return out
    }
}
