---
permalink: /io/lang/
group: io
layout: default
title: Language Tools
title_prefix: KorIO
description: "Reflection utilities, Delegates, KDynamic (dynamic access), Array Tools..."
fa-icon: fa-dna
priority: 8
---

KorIO has some general language tools to make life easier.



## Misc

```kotlin
// To get the simple name of a class in all the targets without reflection.
val <T : Any> KClass<T>.portableSimpleName: String

// To build a list. Analogous to buildString
inline fun <T> buildList(callback: ArrayList<T>.() -> Unit): List<T>

inline fun Int.compareToChain(callback: () -> Int): Int = if (this != 0) this else callback()

fun String.htmlspecialchars() = Xml.Entities.encode(this)

fun Regex.Companion.quote(str: String): String

// Ranges extensions
val IntRange.length: Int get() = (this.endInclusive - this.start) + 1
val LongRange.length: Long get() = (this.endInclusive - this.start) + 1

val IntRange.endExclusive: Int get() = this.endInclusive + 1
val LongRange.endExclusive: Long get() = this.endInclusive + 1
val LongRange.endExclusiveClamped: Long get() = if (this.endInclusive == Long.MAX_VALUE) Long.MAX_VALUE else this.endInclusive + 1
val LONG_ZERO_TO_MAX_RANGE = 0L..Long.MAX_VALUE
fun IntRange.toLongRange() = this.start.toLong()..this.endInclusive.toLong()
```

{:#kdynamic}
## KDynamic (Dynamic Access)

This functionality makes it easier to navigate from an untyped object hierarchy.

```kotlin
// To inject the KDynamic extensions in your scope
inline fun <T> KDynamic(callback: KDynamic.() -> T): T = callback(KDynamic)
inline fun <T, R> KDynamic(value: T, callback: KDynamic.(T) -> R): R = callback(KDynamic, value)

object KDynamic {
	val global: Any?

	interface Invokable {
		fun invoke(name: String, args: Array<out Any?>): Any?
	}

	fun Any?.dynamicInvoke(name: String, vararg args: Any?): Any?
	operator fun Any?.set(key: Any?, value: Any?)
	operator fun Any?.get(key: Any?): Any?

	val Any?.map: Map<Any?, Any?>
	val Any?.list: List<Any?>
	val Any?.keys: List<Any?>

	fun Any?.toNumber(): Number
	fun Any?.toBool(): Boolean
	fun Any?.toByte(): Byte
	fun Any?.toChar(): Char
	fun Any?.toShort(): Short
	fun Any?.toInt(): Int
	fun Any?.toLong(): Long
	fun Any?.toFloat(): Float
	fun Any?.toDouble(): Double

	fun Any?.toBoolOrNull(): Boolean?
	fun Any?.toIntOrNull(): Int?
	fun Any?.toLongOrNull(): Long?
	fun Any?.toDoubleOrNull(): Double?

	fun Any?.toIntDefault(default: Int = 0): Int
	fun Any?.toLongDefault(default: Long = 0L): Long
	fun Any?.toFloatDefault(default: Float = 0f): Float
	fun Any?.toDoubleDefault(default: Double = 0.0): Double

	val Any?.str: String
	val Any?.int: Int
	val Any?.bool: Boolean
	val Any?.float: Float
	val Any?.double: Double
	val Any?.long: Long

	val Any?.intArray: IntArray
	val Any?.floatArray: FloatArray
	val Any?.doubleArray: DoubleArray
	val Any?.longArray: LongArray
}
```

## Delegates

### Lazy

## lazyVar

```kotlin
class lazyVar<T : Any>(val callback: () -> T) {
    var current: T? = null
    operator fun getValue(obj: Any, property: KProperty<*>): T
    operator fun setValue(obj: Any, property: KProperty<*>, value: T)
}
```

### Redirected / Transformed

```kotlin
inline fun <V> (() -> KProperty0<V>).redirected(): RedirectFieldGen
inline fun <V> (() -> KMutableProperty0<V>).redirected(): = RedirectMutableFieldGen
inline fun <V> KMutableProperty0<V>.redirected(): RedirectMutableField
inline fun <V> KProperty0<V>.redirected(): RedirectField

fun <V, R> KMutableProperty0<V>.transformed(transform: (V) -> R, reverseTransform: (R) -> V): TransformedMutableField
fun <V, R> KProperty0<V>.transformed(transform: (V) -> R): TransformedField

class RedirectField<V>(val redirect: KProperty0<V>)
class RedirectMutableField<V>(val redirect: KMutableProperty0<V>)
class RedirectMutableFieldGen<V>(val redirect: () -> KMutableProperty0<V>)
class RedirectFieldGen<V>(val redirect: () -> KProperty0<V>)
class TransformedField<V, R>(val prop: KProperty0<V>, val transform: (V) -> R)
class TransformedMutableField<V, R>(val prop: KMutableProperty0<V>, val transform: (V) -> R, val reverseTransform: (R) -> V)
```

Example:

```kotlin
class A {
    var z: Int = 10
    val i: Int = 10
}

class B(val a: A) {
    var z: Int by a::z.redirected()
    val y: Int by this::z.redirected()
    val i: Int by a::i.redirected()
    val l: Int by { a::i }.redirected()
    val r: Int by { a::z }.redirected()
}
```

## Array tools

```kotlin
inline operator fun ByteArray.set(o: Int, v: Int)
inline operator fun ByteArray.set(o: Int, v: Long)

fun List<BooleanArray>.join(): BooleanArray
fun List<ByteArray>.join(): ByteArray
fun List<ShortArray>.join(): ShortArray
//fun List<CharArray>.join(): CharArray
fun List<IntArray>.join(): IntArray
fun List<LongArray>.join(): LongArray
fun List<FloatArray>.join(): FloatArray
fun List<DoubleArray>.join(): DoubleArray

fun BooleanArray.indexOf(v: Boolean, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun ByteArray.indexOf(v: Byte, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun ShortArray.indexOf(v: Short, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
//fun CharArray.indexOf(v: Char, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun IntArray.indexOf(v: Int, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun LongArray.indexOf(v: Long, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun FloatArray.indexOf(v: Float, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
fun DoubleArray.indexOf(v: Double, startOffset: Int = 0, endOffset: Int = this.size, default: Int = -1): Int
```

