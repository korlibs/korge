---
permalink: /math/angle/
group: math
layout: default
title: Angle
title_short: Angle
version_review: 5.0.0
description: "KorGE provides an Angle class to abstract from radians, degrees or ratios"
fa-icon: fa-shapes
priority: 1
---

## Angle

`Angle` is an inline class backed by a `Double` that represents an angle and that can give additional
type safety and semantics to code.
It can be constructed from and converted to `degrees`, `radians` and `ratio` and offer several
utilities and operators related to angles:

## Predefined angles

There are some already-defined angles to be used: 

```kotlin
val Angle.EPSILON = Angle.fromRatio(0.00001)
val Angle.ZERO = 0.degrees
val Angle.QUARTER = 90.degrees
val Angle.HALF = 180.degrees
val Angle.THREE_QUARTERS = 270.degrees
val Angle.FULL = 360.degrees
```

## Constructing from ratio, radians or degrees

You can construct an Angle from a ratio, radians or degrees.

```kotlin
val angle = Angle.fromRatio(0.5)
val angle = Angle.fromRadians(PI)
val angle = Angle.fromDegrees(180)
```

Or with numeric extension properties:

```kotlin
val angle = PI.radians
val angle = 180.degrees
```

## Getting radians, degrees or ratios from an angle

From an Angle instance, we can get the degrees, radians or ratio:

```kotlin
val angle: Angle = Angle.HALF
val radians: Double = angle.radians
val degrees: Double = angle.degrees
val ratio: Double = angle.ratio
```

## Arithmetic

It is possible to add, subtract two angles,
or multiply divide angles between scalars.
There are also min, max, abs and clamp functions available:

```kotlin
val angle: Angle = 15.degrees + PI.radians - Angle.HALF
val angleX2: Angle = 15.degrees * 2
val angleNeg: Angle = -(15.degrees)
val angleMod: Angle = 45.degrees % 15.degrees
val ratio: Double = 45.degrees / 15.degrees
val abs: Angle = (-15).degrees.absoluteValue
val min: Angle = min(angle1, angle2)
val max: Angle = max(angle1, angle2)
val clamped: Angle = angle.clamp(15.degrees, 30.degrees)
```

You can check if an angle is in an angle range:

```kotlin
val range = 15.degrees until 32.degrees
val inside = 20.degrees in range
```

## Sine, Cosine & Tangent 

You can get the cosine (X), sine (Y) or tangent (Y/X) with:

```kotlin
fun cos(angle: Angle, up: Vector2 = Vector2.UP): Float
fun sin(angle: Angle, up: Vector2 = Vector2.UP): Float
fun tan(angle: Angle, up: Vector2 = Vector2.UP): Float
```

```kotlin
val x = angle.cosine
val y = angle.sine
val tan = angle.tangent
```

Since in KorGE, coordinates are X+ right, and Y+ down, while typical Y+ is up, you can provide a parameter to cosine to specify
the vector representing up or what value would have y for 90.degrees:

```kotlin
val x = angle.cosine(Vector2.UP_SCREEN)
val y = angle.sine(Vector2.UP_SCREEN)
val tan = angle.tangent(Vector2.UP_SCREEN)
```

There are two standard provided up vectors `Vector2.UP` (Y+ up) and `Vector2.UP_SCREEN` (Y+ down)

## Arc Sine/Cosine/Tangent

It is possible to get angles from Arc Cosine, Arc Sine and Arc Tangent:

```kotlin
fun Angle.arcCosine(v: Double): Angle
fun Angle.arcSine(v: Double): Angle
fun Angle.arcTangent(x: Double, y: Double): Angle
```


## Normalizing angle

Sometimes we have an angle greater than 360 degrees or lesser than 0 degrees.
This normalization will provide the angle in the [0, 360[ degrees range

```kotlin
val Angle.normalized: Angle
```

## Angular distance between two angles

Either the short angle or the long one.

```kotlin
val shortDistance: Angle = Angle.shortDistanceTo(15.degrees, 350.degrees)
val longDistance: Angle = Angle.longDistanceTo(15.degrees, 350.degrees)
```

## Angle interpolation

It is possible to interpolate two angles:

```kotlin
val ratio: Ratio = 0.5.toRatio()
val angle1 = 15.degrees
val angle2 = 45.degrees

val newAngle = ratio.interpolateAngle(angle1, angle2, minimizeAngle = true)
val newAngle = ratio.interpolateAngleNormalized(angle1, angle2)
val newAngle = ratio.interpolateAngleDenormalized(angle1, angle2)
```

## Angle between two points

It is possible to determine the angle that two points form, by using the `Angle.between` or `Point.angle` methods.
You can also provide the UP vector, either: `Vector2D.UP` (Y+ up) or `Vector2D.UP_SCREEN` (Y+ down).

```kotlin
val angle1 = Angle.between(Point(10, 10), Point(30, 30), Vector2D.UP_SCREEN)
val angle1 = Point.angle(Point(10, 10), Point(30, 30), Vector2D.UP_SCREEN)
```
