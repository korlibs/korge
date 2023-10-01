---
permalink: /data-structures/lists/
group: data-structures
layout: default
title: "Data Structure Lists"
title_short: Lists
description: "ArrayList, FastArrayList, Array2, Deque, Pool, PriorityQueue, Queue, Stack, ListReader..."
fa-icon: fa-ellipsis-h
priority: 20
---

## ArrayList: `IntArrayList`, `FloatArrayList` and `DoubleArrayList`
{: #ArrayList }

Kds provides specialized equivalents of `ArrayList` so it doesn't involve object allocation through boxing. It uses typed arrays internally to store the elements of the ArrayList so it just requires one additional object allocation per list (the `Array`). It will just allocate a new object when the capacity of the list is exhausted.

### *arrayListOf

You can construct literals using the `*arrayListOf` constructors:

```kotlin
val ilist = intArrayListOf(10, 20)
val flist = floatArrayListOf(10f, 20f)
val dlist = doubleArrayListOf(10.0, 20.0)
```

### Expected behaviour

`IntArrayList`, `FloatArrayList` and `DoubleArrayList` work like a normal `ArrayList` but without incurring into boxing.

```kotlin
val list = IntArrayList()
list += 10
list += 20
list[0] = 15
println(list[0])
println(list.toList().map { it * 20 })
println(list.getCyclic(-1))
```

### Optimized collection transformations

`mapInt`, `mapFloat` and `mapDouble` generate optimized `*ArrayList`. And `*ArrayList` have an specialized `filter` function too.

```kotlin
val filter = (0 until 10).mapInt { it * 3 }.filter { it % 2 == 0 }
```

## Array2: `Array2`, `IntArray2`, `FloatArray2`, `DoubleArray2`
{: #Array2 }

Array2 is a bidimensional version of Array variants. It includes a `width` and a `height` instead of size (length) measuing its dimensions.

It provides bidimensional indexers and some convenience methods.

```kotlin
val biarray = IntArray2(64, 64) { 0 }
val biarray = IntArray2(64, 64, 0)
biarray[0, 0] = 1
biarray.width == 64
biarray.height == 64
```

Internally it is represented as a single 1D Array and actual indices are computed using simple arithmetic.
## Deque/CircularList: `Deque`, `ByteDeque`, `IntDeque`, `FloatDeque`, `DoubleDeque`
{: #Deque }

`Deque` variants (and its `CircularList` typealias) allows to insert and delete elements to/from the start or the end of the deque in constant time except when growing the collection. It can be used to implement queues or produce/consumers in an efficient way. The typed variants allow to reduce memory and allocation usage.

```kotlin
val l = IntDeque()
for (n in 0 until 1000) l.addFirst(n)
for (n in 0 until 1000) l.removeFirst()
for (n in 0 until 1000) l.addLast(n)
```

## ListReader
{: #ListReader }

A reader for lists. It can `peek`, `read` or `expect` a specific value in order.

```kotlin
val reader = listOf(1, 2, 3).reader()
assertEquals(true, reader.hasMore)
assertEquals(1, reader.peek())
assertEquals(1, reader.peek())
assertEquals(1, reader.read())
assertEquals(2, reader.read())
assertEquals(3, reader.expect(3))
assertEquals(false, reader.hasMore)
```

## Pool
{: #Pool }

A simple pool implementation allowing to preallocate, to reset objects and to temporally allocate (freeing automatically) using an inline function.
It accepts an instance allocator, and an optional function to reset instances.

```kotlin
val pool = Pool { Demo() }
pool.alloc { demo ->
    println("Temporarilly allocated $demo")
}
```

```kotlin
val pool = Pool(reset = {
    totalResetCount++
    it.x = 0
    it.y = 0
},  gen = {
    totalAllocCount++
    Demo()
})
val a = pool.alloc()
val b = pool.alloc()

assertEquals(0, pool.itemsInPool)
pool.free(c)
assertEquals(1, pool.itemsInPool)
pool.free(b)

pool.alloc {
    assertEquals(1, pool.itemsInPool)
}
assertEquals(2, pool.itemsInPool)

assertEquals(5, totalResetCount) // Number of resets
assertEquals(3, totalAllocCount) // Number of allocs
```

## PriorityQueue: `PriorityQueue`, `IntPriorityQueue`, `FloatPriorityQueue`, `DoublePriorityQueue`
{: #PriorityQueue }

Provides a PriorityQueue that allows to insert items in a Queue by priority. It allows reordering specific items after modification.

```kotlin
val pq = IntPriorityQueue()
pq.add(10)
pq.add(15)
pq.add(5)
assertEquals(5, pq.removeHead())
assertEquals(10, pq.removeHead())
assertEquals(15, pq.removeHead())
assertEquals(0, pq.size)
```

Allows to provide a custom `Comparator`:

```kotlin
val pq = IntPriorityQueue { a, b -> (-a).compareTo(-b) }
pq.addAll(listOf(1, 2, 3, 4))
assertEquals(listOf(4, 3, 2, 1), pq.toList())
```

And to repriorize objects after modification:

```kotlin
val item = Item(10)
pq.add(item)
item.value = 20
pq.updateObject(item)
```

It is implemented using a Min Heap so addition, removing and updating happens in *O(log(n))*.

## Queue: `Queue`, `IntQueue`, `FloatQueue`, `DoubleQueue`
{: #Queue }

A FIFO (First In First Out) collection.

```kotlin
val queue = IntQueue()
queue.enqueue(1)
queue.enqueue(2)
assertEquals(1, queue.dequeue())
```

Internally implemented using a `Deque`.

## Stack: `Stack`, `IntStack`, `FloatStack`, `DoubleStack`
{: #Stack }

A LIFO (Last In First Out) collection.

```kotlin
val queue = IntStack()
queue.push(1)
queue.push(2)
assertEquals(2, queue.pop())
```

Internally implemented using an `ArrayList`.

