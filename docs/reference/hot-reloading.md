---
permalink: /hot-reloading/
group: reference
layout: default
title: "Hot Reloading"
title_prefix: KorGE
fa-icon: fa-fire
priority: 901
version_review: 5.1.0
---

KorGE supports Hot Reloading / Auto Reloading on the JVM target.
That means that, you can make changes in the code, save and see those changes
reflected almost immediately in your game window.

To do so, you need to use scenes and call the `runJvmAutoreload` gradle task.

## Main & Scene

```kotlin
import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

suspend fun main() = Korge {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { MyScene() }
}

class MyScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        // Put your code here (try change the color for example)
        solidRect(100, 100, Colors.RED)
    }
}
```

## Gradle Task

```
./gradlew runJvmAutoreload
```
