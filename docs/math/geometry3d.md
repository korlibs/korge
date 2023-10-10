---
permalink: /math/geometry3d/
group: math
layout: default
title: Geometry 3D
title_short: Geometry 3D
description: "KorMA provides some geometry utilities: Vector3D, Matrix3D, AABB3D, Ray3D, EulerRotation, Quaternion, BVH3D..."
fa-icon: fa-shapes
priority: 3
---


## Vector3D and Matrix3D

`Vector3D` and `Matrix3D` are vectors and matrices of 4 components / 4 rows and 4 columns. They can also be used as 2, 3 and 4 component vectors, and 2x2, 3x3 and 4x4 matrices.

## CylindricalVector

`CylindricalVector` represents a coordinate in a cylinder.

``` kotlin
val cylindricalVector = CylindricalVector(radius = 10.0, angle = 45.degrees, y = 10.0)
val vector: Vector3F = cylindricalVector.toVector3()
val again: CylindricalVector = vector.toCylindrical()

val radius = cylindricalVector.radius
val angle = cylindricalVector.angle
val y = cylindricalVector.y
```

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
