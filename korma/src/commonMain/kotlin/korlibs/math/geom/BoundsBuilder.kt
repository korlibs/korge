package korlibs.math.geom

import korlibs.datastructure.iterators.*

inline class NewBoundsBuilder(val bounds: Rectangle) {
    val isEmpty: Boolean get() = bounds.isNIL

    val xmin: Float get() = kotlin.math.min(bounds.left, bounds.right)
    val xmax: Float get() = kotlin.math.max(bounds.left, bounds.right)
    val ymin: Float get() = kotlin.math.min(bounds.top, bounds.bottom)
    val ymax: Float get() = kotlin.math.max(bounds.top, bounds.bottom)

    companion object {
        val EMPTY = NewBoundsBuilder(Rectangle.NIL)

        operator fun invoke(): NewBoundsBuilder = EMPTY
        operator fun invoke(p1: Point): NewBoundsBuilder = NewBoundsBuilder(Rectangle(p1, Size(0, 0)))
        operator fun invoke(p1: Point, p2: Point): NewBoundsBuilder = NewBoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2), Point.maxComponents(p1, p2)))
        operator fun invoke(p1: Point, p2: Point, p3: Point): NewBoundsBuilder = NewBoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2, p3), Point.maxComponents(p1, p2, p3)))
        operator fun invoke(p1: Point, p2: Point, p3: Point, p4: Point): NewBoundsBuilder = NewBoundsBuilder(Rectangle.fromBounds(Point.minComponents(p1, p2, p3, p4), Point.maxComponents(p1, p2, p3, p4)))
        operator fun invoke(size: Int, func: NewBoundsBuilder.(Int) -> NewBoundsBuilder): NewBoundsBuilder {
            var bb = NewBoundsBuilder()
            for (n in 0 until size) bb = func(bb, n)
            return bb
        }
    }
    operator fun plus(p: Point): NewBoundsBuilder {
        if (bounds.isNIL) return NewBoundsBuilder(Rectangle(p, Size(0, 0)))
        return NewBoundsBuilder(Rectangle.fromBounds(Point.minComponents(bounds.topLeft, p), Point.maxComponents(bounds.bottomRight, p)))
    }
    operator fun plus(bb: NewBoundsBuilder): NewBoundsBuilder = this + bb.bounds
    operator fun plus(rect: Rectangle?): NewBoundsBuilder = if (rect == null) this else plus(rect)
    operator fun plus(rect: Rectangle): NewBoundsBuilder {
        if (rect.isNIL) return this
        return this + rect.topLeft + rect.bottomRight
    }
    operator fun plus(rects: List<Rectangle>): NewBoundsBuilder {
        var bb = this
        rects.fastForEach { bb += it }
        return bb
    }
    fun boundsOrNull(): Rectangle? = if (isEmpty) null else bounds
}
