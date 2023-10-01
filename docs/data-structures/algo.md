---
permalink: /data-structures/algo/
group: data-structures
layout: default
title: "Data Structure Algorithms"
title_short: Algorithms
description: "genericBinarySearch, genericSort, mapWhile, getCyclic"
fa-icon: fa-hat-wizard
priority: 10
---

## binarySearch: `genericBinarySearch`, `binarySearch`
{: #genericBinarySearch }

Kds provides binarySearch for its collections limiting the indices used. Also provides a `genericBinarySearch` to execute the algorithm in any possible kind of collection. It allows to get exact possitions or nearest positionss when no value is found:

```kotlin
val v = intArrayOf(10, 20, 30, 40, 50)
assertEquals(0, v.binarySearch(10).index)
assertEquals(1, v.binarySearch(20).index)
assertEquals(2, v.binarySearch(30).index)
assertEquals(3, v.binarySearch(40).index)
assertEquals(4, v.binarySearch(50).index)

assertEquals(true, v.binarySearch(10).found)
assertEquals(false, v.binarySearch(11).found)

assertEquals(2, v.binarySearch(21).nearIndex)
```

## genericSort

`genericSort` allows to sort any array-like structure fully or partially without allocating and without having to reimplementing any sort algorithm again.
You just have to implement a `compare` and `swap` methods that receive indices
in the array to compare and optionally a `shiftLeft` method (that fallbacks to use the `swap` one). The SortOps implementation is usually an `object` to prevent allocating.

```kotlin
fun <T> genericSort(subject: T, left: Int, right: Int, ops: SortOps<T>): T
abstract class SortOps<T> {
    abstract fun compare(subject: T, l: Int, r: Int): Int
    abstract fun swap(subject: T, indexL: Int, indexR: Int)
    open fun shiftLeft(subject: T, indexL: Int, indexR: Int)
}
```

So a simple implementation that would sort any `MutableList` could be:

```kotlin
val result = genericSort(arrayListOf(10, 30, 20, 10, 5, 3, 40, 7), 0, 7, ArrayListSortOps)
assertEquals(listOf(3, 5, 7, 10, 10, 20, 30, 40), result)

object ArrayListSortOps : SortOps<ArrayList<Int>>() {
    override fun compare(subject: ArrayList<Int>, l: Int, r: Int): Int {
        return subject[l].compareTo(subject[r])
    }

    override fun swap(subject: ArrayList<Int>, indexL: Int, indexR: Int) {
        val tmp = subject[indexA]
        subject[indexA] = subject[indexB]
        subject[indexB] = tmp
    }
}
```

## mapWhile: `mapWhile`, `mapWhileArray`, `mapWhileInt`, `mapWhileFloat`, `mapWhileDouble`
{: #mapWhile }

This method allows to generate a collection by providing a condition and a generator:

```kotlin
val iterator = listOf(1, 2, 3).iterator()
assertEquals(listOf(1, 2, 3), mapWhile({ iterator.hasNext() }) { iterator.next()})
```

## getCyclic: `List.getCyclic`, `Array.getCyclic`
{: #getCyclic }

For lists and arrays Kds defines a `getCyclic` extension method to get an element wrapping its bounds. So `list.getCylic(-1)` would return the last element of the List, and `list.getCyclic(size)` would return the element at 0:

```kotlin
assertEquals("a", arrayOf("a", "b").getCyclic(2))
assertEquals("b", arrayOf("a", "b").getCyclic(-1))
```

## RLE

KorGE provides a `RLE` (Run-Length-Encoding) implementation, that supports emitting and iterating over RLE chunks.
An RLE chunk is a pair of value + count. So for example 77, 33, 33, 9, 9, 9, 9,
could be represented in RLE as: 1 times 77, 2 times 33, 4 times 9. This is one of the simplest compression algorithms.
In this RLE implementation we store not only the value + count, but also the position in the stream.

### Getting an `RLE` instance from an `IntArray`

```kotlin
val data: IntArray = intArrayOf(77, 33, 33, 9, 9, 9, 9)
val rle = RLE.compute(data, 0, data.size)

rle.fastForEach { n, start, count, value ->
    println("$n, start=$start, count=$count, value=$value")
}

// 0, start=0, count=1, value=77
// 1, start=1, count=2, value=33
// 2, start=3, count=4, value=9
```

### Manually emitting `RLE` segments:

```kotlin
val rle = RLE(capacity = 10)
rle.emit(start = 0, count = 1, value = 77)
rle.emit(start = 1, count = 2, value = 33)
rle.emit(start = 3, count = 4, value = 9)
```

### Determining the number of chunks in a `RLE`

```kotlin
val rle: RLE
println(rle.size) // Number of chunks
```

### Getting an `RLE` instance from an arbitrary source

```kotlin
val str = "aBBBccccc"
val rle = RLE.compute(str.length) { str[it].code }
rle.fastForEach { n, start, count, value ->
    println("$n, start=$start, count=$count, value=${value.toChar()}")
}
// 0, start=0, count=1, value=a
// 1, start=1, count=3, value=B
// 2, start=4, count=5, value=c
```

## Historiogram

Supports generating historiograms for `Int` values in the form of a pair of: value to frequency.

### Generating a Historiogram from a slice of an `IntArray`

If we have an IntArray, we can generate the historiogram with a one-liner, like this:

```kotlin
val frequencies = Historiogram.values(intArrayOf(1, 1, 5, 1, 9, 5))

frequencies == intIntMapOf(
    (1 to 3), 
    (5 to 2), 
    (9 to 1)
)            
```

### Generating and updating a Historiogram

In the case we can to keep track and iteratively update the mutable Historiogram, we can do the following:

```kotlin
val historiogram = Historiogram()

historiogram.add(1)
historiogram.add(2)
historiogram.add(1)
historiogram.addArray(intArrayOf(1, 4, 5))
historiogram.addArray(intArrayOf(1, 4, 5), start = 1, end = 2)
```

Then we can get the historiogram as an `IntIntMap` with:

```kotlin
val frequencies = historiogram.getMapCopy()
```

We can also clone the historiogram at some point with:

```kotlin
val newHistoriogram = historiogram.clone()
```
