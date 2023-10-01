---
permalink: /math/vector/
group: math
layout: default
title: Vector
title_prefix: KorMA
description: "VectorPath, VectorBuilder, Shape2d, Shape2d union, difference, Triangulation, Vector Collision, Bezier..."
fa-icon: fa-bezier-curve
priority: 10
---

KorMA provide several vectorial capabilities to generate all kind of vectorial shapes formed from lines, polygons and curves.

## VectorPath and VectorBuilder 

```kotlin
open class VectorPath(
    val commands: IntArrayList = IntArrayList(),
    val data: DoubleArrayList = DoubleArrayList(),
    val winding: Winding = Winding.EVEN_ODD
) : VectorBuilder

interface VectorBuilder {
    val totalPoints: Int
    val lastX: Double
    val lastY: Double
    fun moveTo(x: Double, y: Double)
    fun lineTo(x: Double, y: Double)
    fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
    fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
    fun close()
}
```

### Constructing vectors with lines and curves

The basic classes for vector building are `VectorPath` and `VectorBuilder`.
They do not include color information, but just the vector shape.
You can make other classes to implement the `VectorBuilder` interface by delegation to for example provide a Context2D-like interface with filling and stroking including all the extension methods provided.

Extension methods using the basic interface:

```kotlin
fun VectorBuilder.isEmpty(): Boolean
fun VectorBuilder.isNotEmpty(): Boolean

fun VectorBuilder.arcTo(ax: Double, ay: Double, cx: Double, cy: Double, r: Double)
fun VectorBuilder.rect(x: Double, y: Double, width: Double, height: Double)
fun VectorBuilder.rectHole(x: Double, y: Double, width: Double, height: Double)
fun VectorBuilder.roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx)
fun VectorBuilder.arc(x: Double, y: Double, r: Double, start: Double, end: Double)
fun VectorBuilder.circle(x: Double, y: Double, radius: Double)
fun VectorBuilder.ellipse(x: Double, y: Double, rw: Double, rh: Double)
fun VectorBuilder.moveTo(p: Point)
fun VectorBuilder.lineTo(p: Point)
fun VectorBuilder.quadTo(c: Point, a: Point)
fun VectorBuilder.cubicTo(c1: Point, c2: Point, a: Point)

inline fun VectorBuilder.moveTo(x: Number, y: Number)
inline fun VectorBuilder.lineTo(x: Number, y: Number)
inline fun VectorBuilder.quadTo(controlX: Number, controlY: Number, anchorX: Number, anchorY: Number)
inline fun VectorBuilder.cubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number)

inline fun VectorBuilder.moveToH(x: Number)
inline fun VectorBuilder.rMoveToH(x: Number)

inline fun VectorBuilder.moveToV(y: Number)
inline fun VectorBuilder.rMoveToV(y: Number)

inline fun VectorBuilder.lineToH(x: Number)
inline fun VectorBuilder.rLineToH(x: Number)

inline fun VectorBuilder.lineToV(y: Number)
inline fun VectorBuilder.rLineToV(y: Number)

inline fun VectorBuilder.rMoveTo(x: Number, y: Number)
inline fun VectorBuilder.rLineTo(x: Number, y: Number)

inline fun VectorBuilder.rQuadTo(cx: Number, cy: Number, ax: Number, ay: Number)
inline fun VectorBuilder.rCubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number)
inline fun VectorBuilder.arcTo(ax: Number, ay: Number, cx: Number, cy: Number, r: Number)
inline fun VectorBuilder.rect(x: Number, y: Number, width: Number, height: Number)
inline fun VectorBuilder.rectHole(x: Number, y: Number, width: Number, height: Number)
inline fun VectorBuilder.roundRect(x: Number, y: Number, w: Number, h: Number, rx: Number, ry: Number = rx)
inline fun VectorBuilder.arc(x: Number, y: Number, r: Number, start: Number, end: Number)
inline fun VectorBuilder.circle(x: Number, y: Number, radius: Number)
inline fun VectorBuilder.ellipse(x: Number, y: Number, rw: Number, rh: Number)
```

You can also determine if a point is contained inside a `VectorPath`:

```kotlin
fun VectorPath.containsPoint(x: Double, y: Double): Boolean
```

## Shape2d

Several algorithms require to work with simple straight segments.
Korma provides a Shape2d set of classes to describe shapes.

You can convert a `VectorPath` to a `Shape2d` using the `toShape2d` extension method:

```kotlin
val shape = VectorPath {
    moveTo(0, 0)
    lineTo(100, 0)
    lineTo(100, 100)
    close()
}.toShape2d()
```

## Shape2d Operations

### Intersection, Union, Xor, Difference, Collision Test, Growing/Shrinking

Korma provides a separate artifact called `korma-shape-ops` that includes a Kotlin port of the `Clipper` library integrated with the `Shape2D` API.
It provides boolean methods to operate with two paths.

```kotlin
infix fun Shape2d.collidesWith(other: Shape2d): Boolean

infix fun Shape2d.intersection(other: Shape2d): Shape2d
infix fun Shape2d.union(other: Shape2d): Shape2d
infix fun Shape2d.xor(other: Shape2d): Shape2d
infix fun Shape2d.difference(other: Shape2d): Shape2d

operator fun Shape2d.plus(other: Shape2d): Shape2d
operator fun Shape2d.minus(other: Shape2d): Shape2d

fun Shape2d.extend(size: Double, cap: VectorPath.LineCap = VectorPath.LineCap.ROUND): Shape2d
fun Shape2d.extendLine(size: Double, join: VectorPath.LineJoin = VectorPath.LineJoin.SQUARE, cap: VectorPath.LineCap = VectorPath.LineCap.SQUARE): Shape2d 
```

## Triangulation

### Shape2d: Triangulation and Triangulation-based Node and Point Path Finding

Korma provides a separate artifact called `korma-triangulate-pathfind` to do triangulation and triangulation-based path finding.

Triangulating a set of polygons (or curves too after converting them into polygons with `toShape2d`) has several use cases like drawing a vectorial shape into the GPU, doing physics or doing path finding.

To triangulate a set of points, a `Shape2d` or a `VectorPath`:

```kotlin
fun List<IPoint>.triangulate(): List<Triangle>
fun Shape2d.triangulate(): List<List<Triangle>>
fun Shape2d.triangulateFlat(): List<Triangle>
fun VectorPath.triangulate(): List<List<Triangle>>
fun VectorPath.triangulateFlat(): List<Triangle>

```

For pathfinding:

```kotlin
fun List<Triangle>.toSpatialMesh(): SpatialMesh
fun List<Triangle>.pathFind(): SpatialMeshFind
fun SpatialMeshFind.funnel(p0: IPoint, p1: IPoint): List<IPoint>
fun List<Triangle>.funnel(p0: IPoint, p1: IPoint): List<IPoint>
fun List<Triangle>.pathFind(p0: IPoint, p1: IPoint): List<IPoint>
fun Shape2d.toSpatialMesh(): SpatialMesh
fun Shape2d.pathFind(): SpatialMeshFind
fun Shape2d.pathFind(p0: IPoint, p1: IPoint): List<IPoint>
```

Additionally this library allows to compute the area of Shape2d by doing triangulation:

```kotlin
val Shape2d.area: Double
```

## Extra: Bezier tools

Korma provides a `Bezier` object with several methods to compute in a bezier curve (quadratic and cubic) their points, their length or their bounds.
