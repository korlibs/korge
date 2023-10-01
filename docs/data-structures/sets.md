---
permalink: /data-structures/sets/
group: data-structures
layout: default
title: "Data Structure Sets"
title_short: Sets
description: "IntSet, BitSet"
fa-icon: fa-layer-group
priority: 30
---

## IntSet
{: #IntSet }

A set working with integers without boxing.

```kotlin
val set = intSetOf(1, 2, 4)
assertEquals(3, set.size)

assertEquals(true, 1 in set)
assertEquals(true, 2 in set)
assertEquals(false, 3 in set)
assertEquals(true, 4 in set)

set.remove(2)
assertEquals(2, set.size)
assertEquals(true, 1 in set)
assertEquals(false, 2 in set)
assertEquals(true, 4 in set)
```

## BitSet
{: #BitSet }

`BitSet` structure that works like a `BoolArray` but it is more efficient in terms of memory usage.

```kotlin
val array = BitSet(100) // Stores 100 bits
array[99] = true
val bool: Boolean = array[99]
```

It packs bits in an `IntArray` internally so it requires up to eight times less space than a BoolArray that potentially uses internally a ByteArray.
