---
permalink: /entrypoint/
group: reference
layout: default
title: Entry Point
title_prefix: Reference
fa-icon: fa-sort-alpha-down
priority: 4
---

KorGE has a `Stage` like AS3, Pixi and HTML DOM and a [display tree with `View` nodes](/views/).

## DSL

The very basic Hello World in Korge, looks like this:

```kotlin
suspend fun main() = Korge { // this: Stage ->
    solidRect(100, 100, Colors.RED)
}
```

## Module+Scene-based

But larger applications define a module, scenes and use the [Korinject's dependency injector](/injector/):

```kotlin
suspend fun main() = Korge(Korge.Config(module = MyModule))

object MyModule : Module() {
    override val mainScene = MyScene1::class

    override suspend fun Injector.configure(): Unit {
        mapInstance(MyDependency("HELLO WORLD"))
        mapPrototype { MyScene1(get()) }
    }
}

class MyDependency(val value: String)

class MyScene1(val myDependency: MyDependency) : Scene() {
    override suspend fun Container.sceneInit() {
        text("MyScene1: ${myDependency.value}")
        solidRect(100, 100, Colors.RED).position(100, 100).onClick { launchImmediately { sceneContainer.changeTo<MyScene1>(MyDependency("other")) } }
    }
}
```

KorGE defines `ResourcesRoot`, that is a mountable virtual file system that works with Korio's VfsFile.
It allows to load all kind of resources asynchronously from different sources and mount points.

* Using KorIO it can read `ByteArray`s, `String`s, `XML`s, `JSON`s, `YAML`s...
* Using KorIM it can load images
* And using KorAU can load sound and music files

It [defines `Scene` that act as a controller](/scenes/) and a way to split the application in smaller parts.
It uses the `Injector` from Korio as an [IoC](https://en.wikipedia.org/wiki/Inversion_of_control) mechanism to declare dependencies to Scenes.

Optionally (not imposed) it can use the Korio's `Bus` for communicating different parts of the application.

You can do MVCS as Robot-legs. Or use your own way of programming. KorGE gives you some facilities for
some different programming strategies but without imposing anything.

## Debugging

KorGE games can be easily debugged using IntelliJ, by running the `runJvm` task in debug mode.

Additionally, KorGE allows you to debug the position and size of the components. Simply press `F7` while debugging your applications to toggle the display of borders around the views.
