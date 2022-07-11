<p align="center"><img alt="Korma" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korma.png" /></p>

<h1 align="center">Korma</h1>

<p align="center">Korma is a Mathematical Library mostly focused on geometry for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.korma/korma"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.korma/korma"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: https://docs.korge.org/korma/

## Info:

It includes structures for Points and Matrices (2D and 3D), Typed Angles, Rectangles, BoundsBuilder, Anchors, Vector graphics with Bezier curves support and Context2D-like API for building vectors, Interpolation facilities, Easing, Triangulation, BinPacking and Path Finding in Bidimensional arrays and Triangulated Spatial Meshes.

### Some samples:

```kotlin
val vector = VectorPath {
    // Here we can use moveTo, lineTo, quadTo, cubicTo, circle, ellipse, arc...
    rect(0, 0, 100, 100)
    rect(300, 0, 100, 100)
}.triangulate().toString()
// "[[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0))], [Triangle((300, 100), (400, 0), (400, 100)), Triangle((300, 100), (300, 0), (400, 0))]]"

// Angles
val angle = 90.degrees
val angleInRadians = angle.radians

// Matrices
val a = Matrix(2, 1, 1, 2, 10, 10)
val b = a.inverted()
assertEquals(identity, a * b)

// Rectangle + ScaleMode + Anchor
assertEquals(Rectangle(0, -150, 600, 600), Size(100, 100).applyScaleMode(Rectangle(0, 0, 600, 300), ScaleMode.COVER, Anchor.MIDDLE_CENTER))

// PathFinding (Matrix)
val points = AStar.find(
    board = Array2("""
        .#....
        .#.##.
        .#.#..
        ...#..
    """) { c, x, y -> c == '#' },
    x0 = 0,
    y0 = 0,
    x1 = 4,
    y1 = 2,
    findClosest = false
)
println(points)
// [(0, 0), (0, 1), (0, 2), (0, 3), (1, 3), (2, 3), (2, 2), (2, 1), (2, 0), (3, 0), (4, 0), (5, 0), (5, 1), (5, 2), (4, 2)]

// PathFinding (Shape)
assertEquals(
    "[(10, 10), (100, 50), (120, 52)]",
    (Rectangle(0, 0, 100, 100).toShape() + Rectangle(100, 50, 50, 50).toShape()).pathFind(
        IPoint(10, 10),
        IPoint(120, 52)
    ).toString()
)
```

### Usage with gradle:

```kotlin
def kormaVersion = "..." // Check latest version on the top of this README

repositories {
    mavenCentral()
}

dependencies {
    // For multiplatform projects
    implementation "com.soywiz.korlibs.korma:korma:$kormaVersion"
    
    // For JVM/Android only
    implementation "com.soywiz.korlibs.korma:korma-jvm:$kormaVersion"
    // For JS only
    implementation "com.soywiz.korlibs.korma:korma-js:$kormaVersion"
}

// Additional funcionality using Clipper and poly2try code (with separate licenses):
// - https://github.com/korlibs/korma/blob/master/korma-shape/LICENSE
dependencies {
    implementation "com.soywiz.korlibs.korma:korma-shape:$kormaVersion"
}

```
