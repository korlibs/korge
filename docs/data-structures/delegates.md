---
permalink: /data-structures/delegates/
group: data-structures
layout: default
title: "Data Structure Delegates"
title_short: Delegates
description: "Extra, Computed, WeakProperty"
fa-icon: fa-swatchbook
priority: 50
---

## Extra

Provides a Extra funtionality to define extrinsic properties to an object that has been decorated with Extra interface implemented by Extra.Mixin. It just adds a extra hashmap to the object, so it can be used to externally define properties. The idea is similar to `WeakProperty` but doesn't require weak references at all. But just works with objects that implements Extra interface.

```kotlin
class Demo : Extra by Extra.Mixin() {
    val default = 9
}

// Externally defined for classes implementing Extra
var Demo.demo by Extra.Property { 0 }
var Demo.demo2 by Extra.PropertyThis<Demo, Int> { default }

val demo = Demo()
assertEquals(0, demo.demo)
assertEquals(9, demo.demo2)
demo.demo = 7
assertEquals(7, demo.demo)
assertEquals("{demo=7, demo2=9}", demo.extra.toString())
```

## Computed

Allows to create nullable properties with a parent object that tries to get its value from the parent or from a default when it is not defined locally:

```kotlin
class Format(override var parent: Format? = null) : Computed.WithParent<Format> {
    var size: Int? = null

    val computedSize by Computed(Format::size) { 10 }
}

val f2 = Format()
val f1 = Format(f2)
assertEquals(10, f1.computedSize)
f2.size = 12
assertEquals(12, f1.computedSize)
f1.size = 15
assertEquals(15, f1.computedSize)
```

## WeakProperty

Similar to Extra, to extend objects, but do not require the objects to implement the `Extra` interface. Each externally defined property creates a WeakMap whose keys are the objects that are going to contain the extra properties. But those properties do not retain the object themselves, so they can be collected when not referenced anywhere else.

```kotlin
class C {
    val value = 1
}

var C.prop by WeakProperty { 0 }
var C.prop2 by WeakPropertyThis<C, String> { "${value * 2}" }

val c1 = C()
val c2 = C()
assertEquals(0, c1.prop)
assertEquals(0, c2.prop)
c1.prop = 1
c2.prop = 2
assertEquals(1, c1.prop)
assertEquals(2, c2.prop)

assertEquals("2", c2.prop2)
c2.prop2 = "3"
assertEquals("3", c2.prop2)
```
