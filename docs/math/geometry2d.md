---
permalink: /math/geometry2d/
group: math
layout: default
title: Geometry 2D
title_short: Geometry 2D
description: "KorMA provides some geometry utilities: Point, Matrix, Ray, PointArrayList, Rectangle, Size, Anchor, Orientation, ScaleMode, BoundsBuilder, BVH, BVH2D..."
fa-icon: fa-shapes
priority: 2
---

## Point and Matrix

`Point` and `Matrix` are classes holding doubles (to get consistency among targets including JavaScript) that represent a 2D Point (with x and y) and a 2D Affine Transform Matrix (with a, b, c, d, tx and ty).
Point is a typealias of `Vector2`.

## Vector2

### Polar coordinates

You can construct a Vector2/Point from polar coordinates like:

```kotlin
Point.polar(Point(100, 100), 45.degrees, 50f, up = Vector2.UP_SCREEN) // (135.35535, 64.64466)
Point.polar(Point(100, 100), 45.degrees, 50f, up = Vector2.UP)        // (135.35535, 135.35535)
Point.polar(Point(100, 100), 45.degrees, 50f)                         // (135.35535, 135.35535)
```

The up vector is to determine where the up is, since by default it is going to be Y+ up,
and that would be interpreted differently for drawn points in the case of KorGE since Y+ is down.

## BoundsBuilder

`BoundsBuilder` is a class that allows to compute the bounds of a set of points without additional allocations.

```kotlin
class BoundsBuilder {
    fun reset()
    fun add(x: Double, y: Double): BoundsBuilder
    fun getBounds(out: Rectangle = Rectangle()): Rectangle
}

inline fun BoundsBuilder.add(x: Number, y: Number)
fun BoundsBuilder.add(p: IPoint)
fun BoundsBuilder.add(ps: Iterable<IPoint>)
fun BoundsBuilder.add(ps: IPointArrayList)
fun BoundsBuilder.add(rect: Rectangle)
```

## PointArrayList

`PointArrayList` and `PointIntArrayList` can be used to store a list of points (pair of numbers) without allocating objects per element. You can later access x and y components with `getX` and `getY` or convert them into a list of `Point` for convenience that actually allocate objects.

```kotlin
class PointArrayList(capacity: Int = 7) {
    constructor(capacity: Int = 7, callback: PointArrayList.() -> Unit)
    constructor(points: List<IPoint>): PointArrayList
    constructor(vararg points: IPoint): PointArrayList

    val size: Int
    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean
    fun add(x: Double, y: Double)
    fun getX(index: Int)
    fun getY(index: Int)

    fun setX(index: Int, x: Double)
    fun setY(index: Int, y: Double)
    fun setXY(index: Int, x: Double, y: Double)
    fun reverse()
    fun sort()
}

fun PointArrayList.getPoint(index: Int): Point
fun PointArrayList.toPoints(): List<Point>
inline fun IPointArrayList.contains(x: Number, y: Number): Boolean

inline fun PointArrayList.add(x: Number, y: Number)
fun PointArrayList.add(p: Point)
fun PointArrayList.add(other: PointArrayList)
inline fun PointArrayList.setX(index: Int, x: Number)
inline fun PointArrayList.setY(index: Int, y: Number)
inline fun PointArrayList.setXY(index: Int, x: Number, y: Number)
```
## Rectangle, Size, Anchor, Orientation and ScaleMode

```kotlin
data class Rectangle(
    var x: Double, var y: Double,
    var width: Double, var height: Double
) : MutableInterpolable<Rectangle>, Interpolable<Rectangle>, IRectangle, Sizeable

inline class Size(val p: Point) : MutableInterpolable<Size>, Interpolable<Size>, ISize, Sizeable

data class Anchor(val sx: Double, val sy: Double) : Interpolable<Anchor>

enum class Orientation(val value: Int) { CW(+1), CCW(-1), COLLINEAR(0) }

class ScaleMode {
    operator fun invoke(item: Size, container: Size, target: Size = Size()): Size

    companion object {
        val COVER: ScaleMode
        val SHOW_ALL: ScaleMode
        val EXACT: ScaleMode
        val NO_SCALE: ScaleMode
    }
}
```

As a sample combining most of these entities:

```kotlin
assertEquals(
    Rectangle(0, -150, 600, 600),
    Size(100, 100).applyScaleMode(
        Rectangle(0, 0, 600, 300),
        ScaleMode.COVER,
        Anchor.MIDDLE_CENTER
    )
)
```

## Ray

The `Ray` class represents an infinite Line starting in a specific `point` and in a `direction`.

### Constructing `Ray`

You can construct a Ray instance with:

```kotlin
val ray: Ray = Ray.fromTwoPoints(Point(1, 1), Point(3, 1)) // A ray starting at 1,1 and going to the right
val ray: Ray = Ray(point = Point(1, 1), direction = Vector2(1, 0)) // A ray starting at 1,1 and going to the right
val ray: Ray = Ray(point = Point(1, 1), angle = 0.degrees) // A ray starting at 1,1 and going to the right
```

The angle represents: `0 degrees` is right, `90 degrees` down, `180 degrees` left, `270 degrees` up.

### Getting the starting point and the direction

You have `point` for the starting point, and `direction` (that is normalized) and `angle` representing the direction of the Ray.

```kotlin
val startPoint: Point = ray.point
val normalizedDirection: Vector2 = ray.direction // direction.length will be ~1
val angle: Angle = ray.angle // the angle: 0.degrees, right, 90.degrees down
```

### Transforming and converting the Ray

You can apply an affine transformation to a `Ray` instance, and also we can convert it into a `Line` instance providing a length:

```kotlin
val newRay: Ray = ray.transformed(matrix) // Creates a new ray transformed
val line: Line = ray.toLine(10f) // Creates a new line going from the start point of the ray to its direction with a length of 10f
```

### Checking for equality

Since Ray uses floating point, it provides a way to check for equality using an epsilon value as tolerance:

```koltin
val isEquals: Boolean = ray1.isAlmostEquals(ray2, epsilon = 0.00001f)
```

## BVH

N-dimensional Bounding Volume Hierarchy implementation 

```kotlin
class BVH<T> {
	data class IntersectResult<T>(val intersect: Double, val obj: Node<T>)
}
```

### BVH2D

A Bounding Volume Hierarchy implementation for 2D. It uses `Rectangle` to describe volumes and `Ray` for raycasting.

```kotlin
open class BVH2D<T>(val allowUpdateObjects: Boolean = true) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)
    fun intersectRay(ray: Ray, rect: Rectangle? = null): BVHIntervals?
    fun envelope(): Rectangle
    fun intersect(ray: Ray, return_array = fastArrayListOf()): FastArrayList<IntersectResult<T>>
    fun search(rect: IRectangle, return_array = fastArrayListOf()): FastArrayList<BVH.Node<T>>
    fun insertOrUpdate(rect: IRectangle, obj: T)
    fun remove(rect: IRectangle, obj: T? = null)
    fun remove(obj: T)
    fun getObjectBounds(obj: T, out: Rectangle = Rectangle()): Rectangle
    fun debug()
}

fun BVHIntervals.toRectangle(out: Rectangle = Rectangle())
fun IRectangle.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals
fun Ray.toBVH(out: BVHIntervals = BVHIntervals(2)): BVHIntervals
```
