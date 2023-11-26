package korlibs.math.geom

import korlibs.datastructure.iterators.*

inline class BoundsBuilder(val bounds: Rectangle) {
    val isEmpty: Boolean get() = bounds.isNIL
    val isNotEmpty: Boolean get() = bounds.isNotNIL

    val xmin: Double get() = kotlin.math.min(bounds.left, bounds.right)
    val xmax: Double get() = kotlin.math.max(bounds.left, bounds.right)
    val ymin: Double get() = kotlin.math.min(bounds.top, bounds.bottom)
    val ymax: Double get() = kotlin.math.max(bounds.top, bounds.bottom)

    /** Minimum value found for X. [default] if ![hasPoints] */
    fun xminOr(default: Double = 0.0): Double = if (hasPoints) xmin else default
    /** Maximum value found for X. [default] if ![hasPoints] */
    fun xmaxOr(default: Double = 0.0): Double = if (hasPoints) xmax else default
    /** Minimum value found for Y. [default] if ![hasPoints] */
    fun yminOr(default: Double = 0.0): Double = if (hasPoints) ymin else default
    /** Maximum value found for Y. [default] if ![hasPoints] */
    fun ymaxOr(default: Double = 0.0): Double = if (hasPoints) ymax else default

    val hasPoints: Boolean get() = isNotEmpty

    companion object {
        val EMPTY = BoundsBuilder(Rectangle.NIL)

        operator fun invoke(): BoundsBuilder = EMPTY
        operator fun invoke(p1: Point): BoundsBuilder = BoundsBuilder(Rectangle(p1, Size(0, 0)))
        operator fun invoke(p1: Point, p2: Point): BoundsBuilder = BoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2), Point.maxComponents(p1, p2)))
        operator fun invoke(p1: Point, p2: Point, p3: Point): BoundsBuilder = BoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2, p3), Point.maxComponents(p1, p2, p3)))
        operator fun invoke(p1: Point, p2: Point, p3: Point, p4: Point): BoundsBuilder = BoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2, p3, p4), Point.maxComponents(p1, p2, p3, p4)))
        operator fun invoke(size: Int, func: BoundsBuilder.(Int) -> BoundsBuilder): BoundsBuilder {
            var bb = BoundsBuilder()
            for (n in 0 until size) bb = func(bb, n)
            return bb
        }
    }
    operator fun plus(p: Point): BoundsBuilder {
        if (bounds.isNIL) return BoundsBuilder(Rectangle(p, Size(0, 0)))
        return BoundsBuilder(Rectangle.fromBounds(Point.minComponents(bounds.topLeft, p), Point.maxComponents(bounds.bottomRight, p)))
    }
    operator fun plus(p: PointList): BoundsBuilder {
        var bb = this
        p.fastForEach { bb += it }
        return bb
    }
    operator fun plus(bb: BoundsBuilder): BoundsBuilder = this + bb.bounds
    operator fun plus(rect: Rectangle?): BoundsBuilder {
        if (rect == null) return this
        if (rect.isNIL) return this
        return this + rect.topLeft + rect.bottomRight
    }
    //operator fun plus(rect: Rectangle): BoundsBuilder = TODO()
    operator fun plus(rects: List<Rectangle>): BoundsBuilder {
        var bb = this
        rects.fastForEach { bb += it }
        return bb
    }
    fun boundsOrNull(): Rectangle? = if (isEmpty) null else bounds
}
