---
permalink: /memory/
group: reference
layout: default
title: Memory
fa-icon: fa-microchip
priority: 59
artifact: 'com.soywiz.korge:korge-foundation'
package: korlibs.memory
---

<img src="/i/logos/kmem.svg" width="196" height="196" style="float: left;margin: 0 16px 16px 0;" alt="KMEM: Memory Utilities" />

Kmem is bit, array and fast memory utilities library for multiplatform Kotlin

## arraycopy and arrayfill

Kotlin 1.3 introduces an `Array.copyInto` extension, but the signature is a bit confusing. KMem introduces a wrapper around it that makes the signature more familiar (like Java's `System.arraycopy`). This is the kind of function that works better like a global or static function since there is no obvious receiver even with the "To" suffix
since the receiver should be a couple of parameters or even three considering the length and making it a extension method breaks the symmetry unless the count would be the receiver.

This signature is easy to remember: `SRC -> DST, HOW MUCH`. Both, src and dst are formed from a couple of parameters (an array, and a position). So:

```kotlin
arraycopy(src: Array, srcPos: Int, dst: Array, dstPos: Int, count: Int)
```

As for Kotlin 1.3, no `Array.fill` is provided. For fill, there is an obvious receiver, so it is exposed as an extension method instead. To make it symmetric with arraycopy, it is exposed in two flavors:

```kotlin
arrayfill(array: Array<T>, value: T, start: Int = 0, end: Int = this.size)
Array<T>.fill(value: T, start: Int = 0, end: Int = this.size)
```

## UByteArrayInt and FloatArrayFromIntArray

Sometimes we just want to use some arrays like `ByeArray` as unsigned or `IntArray` as if the elements were float.
You can do it manually by converting the values for each array access.

Since Kotlin 1.3, there is an `UByteArray`, but the problem is that it returns `UByte` values that are inconvenient to use in some cases since there is no autoconversion for fitting values.

`UByteArrayInt` works like `UByte` but receives and returns `Int` values instead, and just considers the lower 8 bits.

`FloatArrayFromIntArray` works by reinterpreting backed Int as Float in each access.

```kotlin
val uba = ByteArray(16).asUByteArrayInt()
val ba = uba.asByteArray()

uba[0] = 255
assertEquals(-1, ba[0])
assertEquals(255, uba[0])

val fa = IntArray(16).asFloatArray()
val ia = fa.asIntArray()

fa[0] = 1f
assertEquals(0x3f800000, ia[0])
```

## MemBuffer, DataBuffer, Int8Buffer, Int16Buffer, Int32Buffer, Float32Buffer, Float64Buffer

Analogous to JavaScript typed arrays. `MemBuffer` is like an ArrayBuffer. `DataBuffer` (DataBuffer) and `*TypeBuffer`, works like views of a single ArrayBuffer. And they do a single thing: provide a view of the data in a fast way in an immutable way. It doesn't work like the JVM Buffers that do too much in a mutable way (providing the data and mutating a pointer).

## FBuffer

`FBuffer` combines all the `*Buffer` classes in a single class to provide a `DataBuffer` like class but with faster aligned access in all the targets.

## ByteArrayBuilder, ByteArrayBuilderLE, ByteArrayBuilderBE

Analogous to `StringBuilder`, this class allows to generate a `ByteArray` by appending data. It also provides a `buildByteArray` method and two variants for simpler Little Endian and BigEndian writting (`buildByteArrayLE` and `buildByteArrayBE`).

```kotlin
val byteArray = buildByteArray {
    append(1)
    append(2)
    append(byteArrayOf(3, 4, 5))
    s32LE(6)
}
assertEquals(9, byteArray.size)
assertEquals(listOf(1, 2, 3, 4, 5, 6, 0, 0, 0), byteArray.map { it.toInt() })
```

It provides `append` methods for appending bytes and `s*` and `f*` methods to append several primitive packed types.

## ByteArrayReader, ByteArrayReaderLE, ByteArrayReaderBE

`ByteArrayReader` allows to sequentially read a `ByteArray`. For convenience it provides `ByteArray.reader` and `ByteArray.read` extension methods.

```kotlin
val byteArray = buildByteArray { f32BE(1f).f32LE(2f) }

byteArray.read {
    assertEquals(1f, f32BE())
    assertEquals(2f, f32LE())
    assertEquals(0, remaining)
}
```

It provides `s*` and `f*` methods to read several primitive packed types.

## ByteArray.read*, ByteArray.write*

Similar to `ByteArrayBuilder` and `ByteArrayReader`, Kmem exposes several methods for reading values directly from a `ByteArray` by just providing the index of the array to read from without any kind of allocation or intermediary object.

It provides variants for reading and writing `Byte`, `Short`, `Char`, `Int24`, `Int`, `Long`, `Float16`, `Float`, `Double`, in signed and unsigned forms, in little and big endian.

## Float16

`Float16` is an inline class backed by an Int that represents a 16-bit floating point. This representation
is not used for computations traditionally except in GPUs, but can be used to store some values in a floating
point format in half the size with less precission.

```kotlin
val fp16 = Float16.fromBits(0x1F00)
val fp16AsInt = fp16.toBits()
val fp32 = fp16.toFloat()
```

## Bit tools

### Int,Float,Long,Double reinterpret*

Kotlin has `Float.Companion.fromBits` and `Float.toBits`. But sometimes is more obvious to use `reinterpret*` extension methods instead:

```kotlin
inline fun Float.reinterpretAsInt() = this.toBits()
inline fun Int.reinterpretAsFloat() = Float.fromBits(this)
inline fun Double.reinterpretAsLong() = this.toBits()
inline fun Long.reinterpretAsDouble() = Double.fromBits(this)
```

### Int,UInt,Long.rotateLeft|rotateRight

Kotlin defines `shl`, `shr` and `ushr` infix functions for primitive integral types. But no way to rotate bits without losing its bit count. These extension functions do exactly that:

```kotlin
fun Int.rotateLeft(bits: Int): Int = (this shl bits) or (this ushr (32 - bits))
fun Int.rotateRight(bits: Int): Int = (this shl (32 - bits)) or (this ushr bits)
```

### reverseBytes, reverseBits

When working with several endians (either little or big), you might want to convert between endians. And that's exactly what `reverseBytes` extension function does.

```kotlin
assertEquals(0x04030201, 0x01020304.reverseBytes())
```

`reverseBits` does a reversing too but instead of doing it in octets it does the reversing bit to bit.

### countLeadingZeros, countTrailingZeros, countLeadingOnes, countTrailingOnes

Some algorithms might require to count the number of leading zeros of a number in a binary representation. For example to compute `ilog2`. `countLeadingZeros` is usually a processor intrinsic and it is defined in JS (`Math.clz32`) and JVM (`java.lang.Integer.numberOfLeadingZeros`). Kmem uses actual to provide the fastest implementation for each platform.

### signExtend*

Sometimes we have an unsigned value, and we want to make it signed by extending the last bit in a complement-of-two representation:

```kotlin
fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits) // Int.SIZE_BITS
```

```kotlin
val signExtended = 0xFFFF.signExtend(16) // -1
```

### mask, extract*, extractSigned, insert*, hasFlags

To work at the bit level we might want to extract part of the bits of a number, or construct a new number replacing some bits, or verifying if a number has all certain bits set.

`Int.mask` allow to construct a bit mask of a certain number:

```kotlin
assertEquals(0b0, 0.mask())
assertEquals(0b1, 1.mask())
assertEquals(0b11, 2.mask())
assertEquals(0b111, 3.mask())
```

`Int.extract` has several signatures to extract either a single bit, or several bits at once from an integer:

```kotlin
assertEquals(true,   0b00111010.extract(1))
assertEquals(0b1101, 0b00111010.extract(1, 4))
```

`Int.insert` allows to construct a new integer from one changing a set of bits:

```kotlin
assertEquals(0b11010010,  0b00000010.insert(0b1101, 4))
```

`Int.hasFlags` allows to check if several bits are set at once:

```kotlin
assertEquals(true,  0b00111010.hasFlags(0b00101010))
assertEquals(false, 0b00111010.hasFlags(0b10000010))
```

## Numeric tools

### toIntCeil/toIntFloor/toIntRound

Methods to convert a Double or Float value into an Int in one step using different methods for approximating for the integral value.

### clamp, convertRange, convertRangeClamped

`convertRange`, `convertRangeClamped` extension methods allows to convert one value from one range of values to another interpolating the value. The clamped version ensures that the converted value is not outside the target bounds.

`clamp` extension methods allows to generate a value between a range of values. When the value is outside bounds, it returns the nearest upper or lower bound that is inside the range.

### isAlmostZero, isNanOrInfinite

Extensions for Float and Double to determine if the number is very near to zero or is not a finite number.

### isOdd, isEven

Integral extension properties to determine if an integer is odd or even.

### umod

`umod` works like the remaining `%` operator, but works for negative values too, wrapping around the maximum type generating an unsigned value.

### nextAlignedTo, prevAlignedTo, isAlignedTo, nextPowerOfTwo, prevPowerOfTwo, isPowerOfTwo

When working with memory, it is pretty usual to need to compute aligned addresses or power of two values
from near values to perform some kind of optimizations (like reading sectors), create power of two textures,
or work with instructions that require to be 16-byte aligned (like some aligned SIMD instructions or plain
instructions in some processors).

```kotlin
assertEquals(20, 13.nextAlignedTo(10))
assertEquals(10, 13.prevAlignedTo(10))
assertEquals(false, 13.isAlignedTo(10))

assertEquals(32, 20.nextPowerOfTwo)
assertEquals(16, 20.prevPowerOfTwo)
assertEquals(false, 20.isPowerOfTwo)
```
