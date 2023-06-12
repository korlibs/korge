---
layout: default
title: Geometry
title_prefix: KorMA
description: "Angle, Point, Matrix, Ray, Vector3D, Matrix3D, AABB3D, Ray3D, EulerRotation, Quaternion, PointArrayList, Rectangle, Size, Anchor, Orientation, ScaleMode, BoundsBuilder, BVH, BVH2D, BVH3D..."
fa-icon: fa-shapes
priority: 1
---

KorMA provides some geometry utilities.

{% include toc_include.md max_level=2 %}

## Angle

`Angle` is an inline class backed by a `Double` that represents an angle and that can give additional type safety and semantics to code. It can be constructed from and converted to `degrees` and `radians` and offer several utilities and operators related to angles:

```kotlin
inline class Angle(val radians: Double)

fun cos(angle: Angle): Double
fun sin(angle: Angle): Double
fun tan(angle: Angle): Double

inline val Number.degrees: Angle
inline val Number.radians: Angle

val Angle.degrees: Double
val Angle.radians: Double

val Angle.normalized: Angle
```

## Point and Matrix

`Point` and `Matrix` are classes holding doubles (to get consistency among targets including JavaScript) that represent a 2D Point (with x and y) and a 2D Affine Transform Matrix (with a, b, c, d, tx and ty).

## Vector3D and Matrix3D

`Vector3D` and `Matrix3D` are vectors and matrices of 4 components / 4 rows and 4 columns. They can also be used as 2, 3 and 4 component vectors, and 2x2, 3x3 and 4x4 matrices.

## AABB3D & Sphere3D

```kotlin
data class Sphere3D(val origin: Vector3, val radius: Float) {
}

data class AABB3D(val min: Vector3 = Vector3(), val max: Vector3) {
    var minX: Float
    var minY: Float
    var minZ: Float
    var maxX: Float
    var maxY: Float
    var maxZ: Float
    val sizeX: Float
    val sizeY: Float
    val sizeZ: Float

    companion object {
        operator fun invoke(min: Float = Float.POSITIVE_INFINITY, max: Float = Float.NEGATIVE_INFINITY): AABB3D
        fun fromSphere(pos: IVector3, radius: Float): AABB3D
    }

    fun setX(min: Float, max: Float)
    fun setY(min: Float, max: Float)
    fun setZ(min: Float, max: Float)
    fun copyFrom(other: AABB3D)
    fun expandBy(that: AABB3D)
    fun expandToFit(that: AABB3D)
    fun expandedBy(that: AABB3D, out: AABB3D = AABB3D()): AABB3D
    fun intersectsSphere(sphere: Sphere3D): Boolean
    fun intersectsSphere(origin: Vector3, radius: Float): Boolean
    fun intersectsAABB(box: AABB3D): Boolean
    fun clone(): AABB3D
}
```

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

## Ray3D

```kotlin
class Ray3D {
	val pos: Vector3D
	val dir: Vector3D
	
    fun transformed(mat: Matrix3D): Ray3D
    
	companion object {
        fun fromPoints(p1: Vector3D, p2: Vector3D): Ray3D
    }
}

fun Ray3D.intersectRayAABox1(box: AABB3D) : Boolean 
```

## EulerRotation

```kotlin

class EulerRotation {
    var x: Angle = 0.degrees,
    var y: Angle = 0.degrees,
    var z: Angle = 0.degrees

    companion object {
        fun toQuaternion(roll: Angle, pitch: Angle, yaw: Angle, out: Quaternion = Quaternion()): Quaternion
        fun toQuaternion(euler: EulerRotation, out: Quaternion = Quaternion()): Quaternion
    }

    fun toQuaternion(out: Quaternion = Quaternion()): Quaternion
    fun setQuaternion(x: Double, y: Double, z: Double, w: Double): EulerRotation
    fun setQuaternion(x: Int, y: Int, z: Int, w: Int): EulerRotation
    fun setQuaternion(x: Float, y: Float, z: Float, w: Float): EulerRotation

    fun setQuaternion(quaternion: Quaternion): EulerRotation
    fun setTo(x: Angle, y: Angle, z: Angle): EulerRotation
    fun setTo(other: EulerRotation): EulerRotation
    fun toMatrix(out: Matrix3D = Matrix3D()): Matrix3D
}
```

## Quaternion

A Quaternion is a kind of vector that represent a rotation in a 3D space.
It can be converted from/to EulerRotation, and can also be converted to/from a rotation Matrix4.

### Constructing a Quaternion:

The Quaternion identity (no rotation) can be obtained with:

```kotlin
Quaternion.IDENTITY
```

You can also construct it from two vectors: an original vector plus a resultant vector after the rotation. For example:

```kotlin
Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT) // A Quaternion that would rotate to the right
```

### Code:

```kotlin
// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
class Quaternion {
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var w: Double = 1.0

    val xyz: Vector3D

    constructor(xyz: IVector3, w: Double)

    val lengthSquared: Double
    val length: Double

    companion object {
        fun dotProduct(l: Quaternion, r: Quaternion): Double
        operator fun invoke(x: Float, y: Float, z: Float, w: Float): Quaternion
        operator fun invoke(x: Int, y: Int, z: Int, w: Int): Quaternion
        fun toEuler(q: Quaternion, out: EulerRotation = EulerRotation()): EulerRotation
        fun toEuler(x: Double, y: Double, z: Double, w: Double, euler: EulerRotation
        fun toEuler(x: Int, y: Int, z: Int, w: Int, euler: EulerRotation = EulerRotation()): EulerRotation
        fun toEuler(x: Float, y: Float, z: Float, w: Float, out: EulerRotation = EulerRotation()): EulerRotation
    }

    operator fun get(index: Int): Double
    inline fun setToFunc(callback: (Int) -> Double)
    fun setTo(x: Double, y: Double, z: Double, w: Double): Quaternion
    fun setTo(x: Int, y: Int, z: Int, w: Int): Quaternion
    fun setTo(x: Float, y: Float, z: Float, w: Float): Quaternion
    fun setTo(euler: EulerRotation): Quaternion
    fun setTo(other: Quaternion): Quaternion

    fun setEuler(x: Angle, y: Angle, z: Angle): Quaternion
    fun setEuler(euler: EulerRotation): Quaternion

    fun copyFrom(other: Quaternion): Quaternion

    operator fun unaryMinus(): Quaternion
    operator fun plus(other: Quaternion): Quaternion
    operator fun minus(other: Quaternion): Quaternion
    operator fun times(scale: Double): Quaternion

    fun negate()

    inline fun setToFunc(l: Quaternion, r: Quaternion, func: (l: Double, r: Double) -> Double)
    fun setToSlerp(left: Quaternion, right: Quaternion, t: Double, tleft: Quaternion = Quaternion(), tright: Quaternion = Quaternion()): Quaternion
    fun setToNlerp(left: Quaternion, right: Quaternion, t: Double): Quaternion
    fun setToInterpolated(left: Quaternion, right: Quaternion, t: Double): Quaternion
    fun setFromRotationMatrix(m: Matrix3D): Quaternion
    fun normalize(v: Quaternion = this): Quaternion
    fun toMatrix(out: Matrix3D = Matrix3D()): Matrix3D
    fun inverted(out: Quaternion = Quaternion()): Quaternion

    operator fun times(other: Quaternion): Quaternion
    fun transform(vec: Vector3D, out: Vector3D = Vector3D()): Vector3D
}
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

### BVH3D

A Bounding Volume Hierarchy implementation for 3D. It uses `AABB3D` to describe volumes and `Ray3D` for raycasting.

```kotlin
open class BVH3D<T>(val allowUpdateObjects: Boolean = true) {
    val bvh = BVH<T>(allowUpdateObjects = allowUpdateObjects)
    fun intersectRay(ray: Ray3D, rect: AABB3D? = null): BVHIntervals?
    fun envelope(): AABB3D
    fun intersect(ray: Ray3D, return_array = fastArrayListOf()): FastArrayList<BVH.IntersectResult<T>>
    fun search(rect: AABB3D, return_array = fastArrayListOf()): FastArrayList<BVH.Node<T>>
    fun insertOrUpdate(rect: AABB3D, obj: T)
    fun remove(rect: AABB3D, obj: T? = null)
    fun remove(obj: T)
    fun getObjectBounds(obj: T, out: AABB3D = AABB3D()): AABB3D?
    fun debug()
}

fun BVHIntervals.toAABB3D(out: AABB3D = AABB3D()): AABB3D
fun AABB3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals
fun Ray3D.toBVH(out: BVHIntervals = BVHIntervals(3)): BVHIntervals
```
