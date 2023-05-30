---
layout: default
title: "Standard 3D Views"
title_prefix: KorGE
fa-icon: fa-cube
priority: 901
status: experimental
---
## Cubes
Use it:
```kotlin
scene3D {
  ...
  val cube = cube()
}
```
```kotlin
inline fun Container3D.cube(callback: Cube3D.() -> Unit = {})
```
