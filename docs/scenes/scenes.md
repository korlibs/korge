---
permalink: /scenes/
group: scenes
layout: default
title: Scenes
title_prefix: KorGE
fa-icon: fa-images
priority: 20
---

While you can create a small application in a few lines, when the application grows in size,
you will want to follow some patterns to be able to split your application effectively.

KorGE includes an asynchronous dependency injector and some tools like the modules and scenes to do so.



## Scenes

### Declaring a Scene

Scenes look like this:

```kotlin
class MyScene : Scene() {
    override fun SContainer.sceneInit() {
        // Your initialization code before the scene transition starts
        // Here you can place resource loading, adding views before they are displayed etc.
    }
    
    override fun SContainer.sceneMain() {
        // Your main code, here you can add views, register for events, do tweens, etc.
    }
}
```

For simplicity, it is possible to only provide one of those methods.
You can for example only use sceneMain in simple cases:

```kotlin
class MyScene : Scene() {
    override fun SContainer.sceneMain() {
        // Do loading, creating views, attaching events, tweens, etc. here.
    }
}
```

### SceneContainer

`Scene`s must be added to a `SceneContainer`. `SceneContainer` is a `View`,
so it can be attached to the scene graph.

```kotlin
fun main() = Korge {
    val sceneContainer = sceneContainer()
    // or
    val sceneContainer = SceneContainer().addTo(this)
}
```

### Changing to a Scene

Once you have the SceneContainer view attached to the stage/scene graph,
you can change it to actually show a specific Scene:

```kotlin
sceneContainer.changeTo({ MyScene() })
```

It is possible to do like that, or if you want to use the injector:

```kotlin
injector.mapSingleton { MyScene() }

sceneContainer.changeTo<MyScene>()
```

## Scene Parameters

It is possible to pass some parameter to scenes, either singletons or specific parameters for that scene.
You do so by adding those parameters to the `Scene` constructor.

### Declaration

So for example, let's consider we want to pass a singleton `service` and the `levelName` we want to load to the scene.
We would create the scene like this:

```kotlin
class MyScene(val service: MyService, val levelName: String) : Scene() {
    // ...
}
```

### Changing to a scene with parameters

Now we can change to the scene like this:

```kotlin
sceneContainer.changeTo({ MyScene(service, "mylevelname") })
```

In the case we are using the injector, and we want to be able to pass singletons, etc. without worrying
about having to change all the `changeTo` if we decide to add extra parameters:

```kotlin
injector.mapSingleton { MyService() }
injector.mapPrototype { MyScene(get(), get()) } // Note the get() here. One per parameter, but only required when configuring the injector once.

// Then we can:

sceneContainer.changeTo<MyScene>("mylevelname") // Now all the parameters provided to the changeTo, will be mapped to the scene subinjector and provided to the constructed scene
```

If you need to pass several strings or integers, or several values of a specific type, you can create and pass a data class wrapping them:

```kotlin
data class MySceneParameters(val levelName: String, val myid: String)
data class MyScene(val service: MyService, val params: MySceneParameters)

sceneContainer.changeTo<MyScene>(MySceneParameters("mylevelname", "myid"))
```

### Changing to another scene with same SceneContainer inside a Scene

Scenes have a `sceneContainer` reference where it has been loaded.
So it is possible do:

```kotlin
fun SContainer.sceneMain() {
    uiButton("Change") {
        onClick { sceneContainer.changeTo { AnotherScene() } }
    }
}
```

## Scene lifecycle

In addition to `sceneInit` and `sceneMain`, Scenes provide other methods you can override:

There are methods where suspensions will block further execution,
while others that will be executed in parallel even if we suspend for example waiting for a tween to complete.

```kotlin
class MyScene() : Scene() {
    override suspend fun SContainer.sceneInit() {
        // BLOCK. This is called to setup the scene. **Nothing will be shown until this method completes**.
        // Here you can read and wait for resources. No need to call super.
    }

    override suspend fun SContainer.sceneMain() {
        // DO NOT BLOCK. This is called as a main method of the scene. This is called after [sceneInit].
        // This method doesn't need to complete as long as it suspends.
        // Its underlying job will be automatically closed on the [sceneAfterDestroy]. No need to call super.
    }

	override suspend fun sceneAfterInit() {
	    // DO NOT BLOCK. Called after the old scene has been destroyed and the transition has been completed.
	}

	override suspend fun sceneBeforeLeaving() {
        // BLOCK. Called on the old scene after the new scene has been
        // initialized, and before the transition is performed.
	}

	override suspend fun sceneDestroy() {
	    // BLOCK. Called on the old scene after the transition
	    // has been performed, and the old scene is not visible anymore.
	}

	override suspend fun sceneAfterDestroy() {
        // DO NOT BLOCK. Called on the old scene after the transition has been performed, and the old scene is not visible anymore.
        //
        // At this stage the scene [coroutineContext] [Job] will be cancelled.
        // Stopping [sceneMain] and other [launch] methods using this scene [CoroutineScope].
	}
	
    open fun onSizeChanged(size: Size) {
        super.onSizeChanged(size)
        // Do something here if the scene size is changed
    }
}
```

## Special Scenes

Instead of overriding `Scene` we can override other Scene subclasses simplifying some operations:

### ScaledScene & PixelatedScene

A Scene where the effective container has a fixed size `sceneWidth` and `sceneHeight`,
and scales and positions its SceneContainer based on `sceneScaleMode` and `sceneAnchor`.
Performs a linear or nearest neighborhood interpolation based `sceneSmoothing`.

This allows to have different scenes with different effective sizes.

The difference of PixelatedScene and ScaledScene is that sceneSmoothing is set to false or true.
The container is written to a texture, and then displayed in the available space by using linear or nearest neighborhood
interpolation methods (smooth or pixelated).

```kotlin
abstract class ScaledScene(
    sceneWidth: Int,
    sceneHeight: Int,
    sceneScaleMode: ScaleMode = ScaleMode.SHOW_ALL,
    sceneAnchor: Anchor = Anchor.CENTER,
    sceneSmoothing: Boolean = true,
) : Scene()
```

```kotlin
abstract class PixelatedScene(
    sceneWidth: Int,
    sceneHeight: Int,
    sceneScaleMode: ScaleMode = ScaleMode.SHOW_ALL,
    sceneAnchor: Anchor = Anchor.CENTER,
    sceneSmoothing: Boolean = false,
) : ScaledScene(sceneWidth, sceneHeight, sceneScaleMode, sceneAnchor, sceneSmoothing = sceneSmoothing)
```

## Transitions

It is also possible to provide timed transitions to scenes. Like doing an alpha transition or more sophisticated transitions like Mask Transitions:

```kotlin
rootSceneContainer.changeTo<IngameScene>(
    transition = MaskTransition(transition = TransitionFilter.Transition.CIRCULAR, reversed = false, filtering = true),
    //transition = AlphaTransition,
    time = 0.5.seconds
)
```

### `AlphaTransition`

It acts like a singleton. So no parameters here. You can just put it in the `transition` argument.

### `MaskTransition`

It performs alpha blending per pixel following a pattern:

```kotlin
fun MaskTransition(
    transition: TransitionFilter.Transition = TransitionFilter.Transition.CIRCULAR,
    reversed: Boolean = false,
    spread: Float = 1f,
    filtering: Boolean = true,
): Transition
```

It supports a transition from a `TransitionFilter` (that you can also use as a normal filter for views).

There are some predefined `TransitionFilter.Transition`, and you can also provide a custom greyscale bitmap: 

```kotlin
fun TransitionFilter.Transition(bmp: Bitmap)
val TransitionFilter.Transition.VERTICAL
val TransitionFilter.Transition.HORIZONTAL
val TransitionFilter.Transition.DIAGONAL1
val TransitionFilter.Transition.DIAGONAL2
val TransitionFilter.Transition.CIRCULAR
val TransitionFilter.Transition.SWEEP
```

## Navigation API

In addition to changing the scene, it is also possible to have a
navigation stack of scenes we can go forward and back. For example:

```kotlin
class AnotherScene1(val params: MySceneParameters) : Scene {}
class AnotherScene2(val params: MySceneParameters) : Scene {}
injector.mapPrototype { AnotherScene1(get()) }
injector.mapPrototype { AnotherScene2(get()) }

sceneContainer.pushTo<AnotherScene1>(MySceneParameters()) // AnotherScene1
sceneContainer.pushTo<AnotherScene2>(MySceneParameters()) // AnotherScene2
sceneContainer.back() // AnotherScene1
sceneContainer.forward() // AnotherScene2

println(sceneContainer.navigationEntries) // List<SceneContainer.VisitEntry>
```

This API is only available along the dependency injector.
Since `back` and `forward` need to know how to construct and recreate the scenes from the injected parameters.
