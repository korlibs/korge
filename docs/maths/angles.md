---
permalink: /maths/angles/
group: maths
layout: default
title: Angles
title_short: Angles
description: "KorMA provides an Angle class to abstract from radians, degrees or ratios"
fa-icon: fa-shapes
priority: 1
---

## Angle

`Angle` is an inline class backed by a `Float` (when normalized being a ratio value between 0..1 instead of 0..PI2)
that represents an angle and that can give additional type safety and semantics to code.
It is stored as 0..1 to take advantage of this floating point range precision by using a Float.
It can be constructed from and converted to `degrees` and `radians` and offer several
utilities and operators related to angles:

### Predefined angles:

```kotlin
val EPSILON = Angle.fromRatio(0.00001f)
val ZERO = 0.degrees
val QUARTER = 90.degrees
val HALF = 180.degrees
val THREE_QUARTERS = 270.degrees
val FULL = 360.degrees
```

### Constructing from ratio, radians or degrees

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

### Sine, Cosine & Tangent 

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

### Normalizing angle

```kotlin
val Angle.normalized: Angle
```
