---
layout: default
title: Path Finding
title_prefix: KorMA
description: "A*, TriA* path finding algorithms"
fa-icon: fa-magic
priority: 10
---

KorMA provides some path finding algorithms.




## Path Finding

Usually in games you might want to find the shortest path between two points.

### AStar (A*)

Korma includes an AStar implementation to find paths in bidimensional grids.

```kotlin
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
```

### Path finding in vectors and polygons

Check the **Shape2d: Triangulation-based Node and Point Path Finding** section.
