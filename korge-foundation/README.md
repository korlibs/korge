<h2 align="center">KBigNum</h2>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/kbignum/actions"><img alt="Build Status" src="https://github.com/korlibs/kbignum/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.kbignum/kbignum"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.kbignum/kbignum"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://korlibs.soywiz.com/kbignum/>


<h2 align="center">Krypto</h2>

<p align="center">Pure Kotlin cryptography library</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.krypto/krypto"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.krypto/krypto"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/krypto/>





<h2 align="center"><img alt="kds" src="docs/kds-nomargin-256.png" /></h2>

<p align="center">
Kds is a Data Structure library for Multiplatform Kotlin.
It includes a set of optimized data structures written in Kotlin Common so they are available in
JVM, JS and future multiplatform targets. Those structures are designed to be allocation-efficient and fast, so Kds
include specialized versions for primitives like <code>Int</code> or <code>Double</code>.
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.kds/kds"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.kds/kds"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: https://docs.korge.org/kds/

### Some samples:

```kotlin
// Case Insensitive Map
val map = mapOf("hELLo" to 1, "World" to 2).toCaseInsensitiveMap()
println(map["hello"])

// BitSet
val array = BitSet(100) // Stores 100 bits
array[99] = true

// TypedArrayList
val v20 = intArrayListOf(10, 20).getCyclic(-1)

// Deque
val deque = IntDeque().apply {
    addFirst(n)
    removeFirst()
    addLast(n)
}

// CacheMap
val cache = CacheMap<String, Int>(maxSize = 2).apply {
    this["a"] = 1
    this["b"] = 2
    this["c"] = 3
    assertEquals("{b=2, c=3}", this.toString())
}

// IntIntMap
val m = IntIntMap().apply {
    this[0] = 98
}

// Pool
val pool = Pool { Demo() }
pool.alloc { demo ->
    println("Temporarilly allocated $demo")
}

// Priority Queue
val pq = IntPriorityQueue()
pq.add(10)
pq.add(5)
pq.add(15)
assertEquals(5, pq.removeHead())

// Extra Properties
class Demo : Extra by Extra.Mixin() { val default = 9 }
var Demo.demo by Extra.Property { 0 }
var Demo.demo2 by Extra.PropertyThis<Demo, Int> { default }
val demo = Demo()
assertEquals(0, demo.demo)
assertEquals(9, demo.demo2)
demo.demo = 7
assertEquals(7, demo.demo)
assertEquals("{demo=7, demo2=9}", demo.extra.toString())

// mapWhile
val iterator = listOf(1, 2, 3).iterator()
assertEquals(listOf(1, 2, 3), mapWhile({ iterator.hasNext() }) { iterator.next()})

// And much more!
```

### Usage with gradle:
```kotlin
def kdsVersion = "..." //the latest version here (you can find it at the top of the README)

repositories {
    maven { url "https://dl.bintray.com/korlibs/korlibs" }
}

dependencies {
    // For multiplatform projects
    implementation "korlibs.kds:kds:$kdsVersion"
    
    // For JVM/Android only
    implementation "korlibs.kds:kds-jvm:$kdsVersion"
    // For JS only
    implementation "korlibs.kds:kds-js:$kdsVersion"
}
```



<p align="center">
    <img alt="Kmem" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/kmem.png" />
</p>

<h2 align="center">Kmem</h2>

<p align="center">
    This library provides extension methods and properties useful for memory handling, and bit manipulation, as well as array and buffer similar to JS typed arrays.
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.kmem/kmem"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.kmem/kmem"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/kmem/>

### Some samples

```kotlin
// Array copying
val array = arrayOf("a", "b", "c", null, null)
arraycopy(array, 0, array, 1, 4)

// Array filling
val array = intArrayOf(1, 1, 1, 1, 1)
array.fill(2)
assertEquals(intArrayOf(2, 2, 2, 2, 2).toList(), array.toList())

// CLZ32
assertEquals(1, (0b11111111111111111111111111111110).toInt().countTrailingZeros())

// Byte Array building
val byteArray = buildByteArray {
    append(1)
    append(2)
    append(byteArrayOf(3, 4, 5))
    s32LE(6)
}

// Byte Array reading
val byteArray = buildByteArray { f32BE(1f).f32LE(2f) }
byteArray.read {
    assertEquals(1f, f32BE())
    assertEquals(2f, f32LE())
}

// Float16
assertEquals(+1.0, Float16.fromBits(0x3c00).toDouble())

// Power of Two
assertEquals(16, 10.nextPowerOfTwo)
assertEquals(16, 17.prevPowerOfTwo)
assertEquals(true, 1024.isPowerOfTwo)

// Aligned (multiple of)
assertEquals(false, 77.isAlignedTo(10))
assertEquals(70, 77.prevAlignedTo(10))
assertEquals(80, 77.nextAlignedTo(10))

// ByteArray indexed typed reading
val v = byteArray.readS32LE(10)

// Buffers
val mem = Buffer(10)
for (n in 0 until 8) mem[n] = n
assertEquals(0x03020100, mem.getAlignedInt32(0))
assertEquals(0x07060504, mem.getAlignedInt32(1))
```

### Usage with gradle

```
def kmemVersion = "..." // Find latest version on https://search.maven.org/artifact/korlibs.kmem/kmem

repositories {
    maven { url "https://dl.bintray.com/korlibs/korlibs" }
}

dependencies {
    // For multiplatform projects
    implementation "korlibs.kmem:kmem:$kmemVersion"
    
    // For JVM/Android only
    implementation "korlibs.kmem:kmem-jvm:$kmemVersion"
    // For JS only
    implementation "korlibs.kmem:kmem-js:$kmemVersion"
}
```





<p align="center">
    <img alt="Klock" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/256/klock.png" />
</p>

<p align="center">
    Klock is a Date & Time library for Multiplatform Kotlin.
</p>

<p align="center">
    It is designed to be as allocation-free as possible using Kotlin inline classes,
    to be consistent and portable across targets since all the code is written in Common Kotlin,
    and to provide an API that is powerful, fun and easy to use.
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.klock/klock"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.klock/klock"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/klock/>

### Some samples:

```kotlin
val now = DateTime.now()
val duration = 1.seconds
val later = now + 1.months + duration
val is2018Leap = Year(2018).isLeap
val daysInCurrentMonth = now.yearMonth.days
val daysInNextMonth = (now.yearMonth + 1.months).days
```

### Usage with gradle:

```groovy
def klockVersion = "..." // Find latest version in https://search.maven.org/artifact/korlibs.klock/klock

repositories {
    mavenCentral()
}

// For multiplatform Kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation "korlibs.klock:klock:$klockVersion" // Common 
            }
        }
    }
}

// For JVM
dependencies {
    implementation "korlibs.klock:klock-jvm:$klockVersion"
}
```

### Testing & Kotest

Kotest is a flexible and comprehensive testing tool for Kotlin with multiplatform support.
It supports Klock adding additional matchers. For a full list of Klock Kotest matchers, check this link:
<https://kotest.io/docs/assertions/matchers.html>

And you can find a sample here: <https://github.com/kotest/kotest/tree/master/kotest-assertions/kotest-assertions-klock>



<h2 align="center">Logger for Multiplatform Kotlin</h2>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.klogger/klogger"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.klogger/klogger"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/klogger/>



<h1 align="center">korinject</h1>

<p align="center">Portable Kotlin Common library to do asynchronous dependency injection</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korinject/korinject"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korinject/korinject"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/korinject/>




<p align="center"><img alt="Korma" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korma.png" /></p>

<h1 align="center">Korma</h1>

<p align="center">Korma is a Mathematical Library mostly focused on geometry for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korma/korma"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korma/korma"></a>
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
        MPoint(10, 10),
        MPoint(120, 52)
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
    implementation "korlibs.korma:korma:$kormaVersion"
    
    // For JVM/Android only
    implementation "korlibs.korma:korma-jvm:$kormaVersion"
    // For JS only
    implementation "korlibs.korma:korma-js:$kormaVersion"
}

// Additional funcionality using Clipper and poly2try code (with separate licenses):
// - https://github.com/korlibs/korma/blob/master/korma-shape/LICENSE
dependencies {
    implementation "korlibs.korma:korma-shape:$kormaVersion"
}

```
