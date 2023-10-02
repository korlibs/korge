---
permalink: /math/binpacker/
group: math
layout: default
title: Bin Packing
title_short: Bin Packing
description: "KorMA provides some utilities for bin packing: Bin Packing, MaxRects..."
fa-icon: fa-box
priority: 10
---

## BinPacker

`BinPacker` allows to place rectangles tightly packed without overlapping in a reduced space.
A popular use case is generating atlases; packing several smaller images in a texture
either at compilation time, or dynamically at runtime.

```kotlin
val packer = BinPacker(100, 100)
val result = packer.addBatch(listOf(Size(20, 10), Size(10, 30), Size(100, 20), Size(20, 80)))
assertEquals(
    "[Rectangle(x=20, y=50, width=20, height=10), Rectangle(x=20, y=20, width=10, height=30), Rectangle(x=0, y=0, width=100, height=20), Rectangle(x=0, y=20, width=20, height=80)]",
    result.toString()
)
```
