---
layout: default
title: "Hot Reloading"
title_prefix: KorGE
fa-icon: fa-fire
priority: 901
---

KorGE supports Hot Reloading / Auto Reloading on the JVM target. That means that, you can make changes in the code and see those changes
reflected almost immediately in your game window. To do so, you need to use scenes and call the `runJvmAutoreload` gradle task.

## Main & Scene

```kotlin
suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo({ MyScene() })
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
        // Put your code here
    }
}
```

## Gradle Task

```
./gradlew runJvmAutoreload
```
