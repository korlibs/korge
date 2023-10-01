---
permalink: /data-structures/maps/
group: data-structures
layout: default
title: "Data Structure Maps"
title_short: Maps
description: "FastMap, IntMap, IntIntMap, IntFloatMap, CacheMap, CaseInsensitiveStringMap, WeakMap, MapList extensions..."
fa-icon: fa-sitemap
priority: 40
---

## FastMap: `FastIntMap`, `FastStringMap`
{: #FastMap }

Simpler Map-like structures that uses native specific implementations to improve performance and reduce allocations.

```kotlin
val map = FastIntMap<String>()
assertEquals(0, map.size)
map[1] = "a"
map[2] = "b"
assertEquals(listOf(1, 2), map.keys.sorted())
assertEquals(2, map.size)
assertEquals("a", map[1])
assertEquals("b", map[2])
assertEquals(null, map[3])
```


## IntMap: `IntMap`, `IntIntMap`, `IntFloatMap`
{: #IntMap }

Variants of a hashmap implementation using int as keys without boxing (and Object, int or float for values). The implementation requires just a couple of arrays for working (no nodes at all). It uses a multihash approach for filling as much as possible with a logarithmic stash. Just allocates when growing.

```kotlin
val m = IntIntMap()
m[0] = 98
assertEquals(1, m.size)
assertEquals(98, m[0])
assertEquals(0, m[1])
```

## CacheMap
{: #CacheMap }

Works like a `LinkedHashMap` with a limited amount of elements. When inserting new elements after reaching the maximum amount of elements, the oldest element inserted is deleted.

```kotlin
val cache = CacheMap<String, Int>(maxSize = 2)
cache["a"] = 1
cache["b"] = 2
cache["c"] = 3
assertEquals("{b=2, c=3}", cache.toString())
```

## CaseInsensitiveStringMap
{: #CaseInsensitiveStringMap }

Map with `String` keys considered case insensitive. Case of the original keys is preserved, but keys can be accessed with any case.

```kotlin
val map = CaseInsensitiveStringMap("hELLo" to 1, "World" to 2)
assertEquals(2, map.size)
assertEquals(1, map["hello"])
assertEquals(2, map["world"])
```

It is possible to convert a normal `Map<String, *>` to a CaseInsensitive one with the `toCaseInsensitiveMap` extension:

```kotlin
val map = mapOf("hELLo" to 1, "World" to 2).toCaseInsensitiveMap()
```

## WeakMap
{: #WeakMap }

Provides a WeakMap data structure that internally uses JS's `WeakMap`, JVM's `WeakHashMap` and Native's `WeakReference`. WeakProperty allow to define external/extrinsic properties to objects that are collected once the object is not referenced anymore.

```kotlin
val map = WeakMap<Demo, String>()
val demo1 = Demo()
map[demo1] = "hello"

assertEquals("hello", map[demo1])
```

Note that using this primitive on JavaScript requires ES6 support (and it doesn't work on IE10 or lower). Check the [JS's WeakMap compatibility table](https://kangax.github.io/compat-table/es6/#test-WeakMap) for more information.
{: .note }

## MapList extensions
{: #MapList }

Instead of providing a `MutableMap<K, MutableList<V>>` implementation. Kds provides a set of methods and extension methods to easily work with those kind of maps.

```kotlin
val map = linkedHashMapListOf("a" to 10, "a" to 20, "b" to 30)

assertEquals(10, map.getFirst("a"))
assertEquals(20, map.getLast("a"))

assertEquals(30, map.getFirst("b"))
assertEquals(30, map.getLast("b"))

assertEquals(null, map.getLast("c"))

assertEquals(listOf("a" to 10, "a" to 20, "b" to 30), map.flatten())
```
