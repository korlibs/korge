---
layout: default
title: Scenes
title_prefix: KorGE
fa-icon: fa-images
priority: 20
---

While you can create a small application in a few lines, when the application grows in size, you will want
to follow some patterns and to be able to split your application effectively.

KorGE includes an asynchronous dependency injector and some tools like the modules and scenes to do so.

{% include toc_include.md %}

## Module

Usually, you have one single module in your application. In the module you can describe some stuff from your module,
as well as define a Scene entry point, and configure the asynchronous injector.

```kotlin
object MyModule : Module() {
    override val mainScene = MyScene::class

    override suspend fun AsyncInjector.configure() {
        mapInstance(MyDependency("HELLO WORLD"))
        mapPrototype { MyScene(get()) }
    }
}
```

And you use the module with:

```kotlin
suspend fun main() = Korge(Korge.Config(module = MyModule))
```

You can create several mains in different classes and packages using different entry point modules.

## The `Scene` class

The `Scene` class looks like this:

```kotlin
abstract class Scene : InjectorAsyncDependency, ViewsContainer, CoroutineScope {
    // Context stuff
    val coroutineContext: CoroutineContext
    var injector: AsyncInjector
    var views: Views
    val ag: AG
    
    // Resources and lifecycle
    var resourcesRoot: ResourcesRoot
    val cancellables: CancellableGroup
    
    // Related to the scene
    var sceneContainer: SceneContainer
    val sceneView: Container
    
    // Main Entrypoint of the scene
    abstract suspend fun Container.sceneInit(): Unit
    
    // Lifecycle
    open suspend fun sceneAfterInit()
    open suspend fun sceneBeforeLeaving()
    open suspend fun sceneDestroy()
    open suspend fun sceneAfterDestroy()
}
```

And you usually declare scenes like this:

```kotlin
class MyScene : Scene() {
	override suspend fun Container.sceneInit(): Unit {
	    solidRect(100, 100, Colors.RED)
	}
}
```

Scenes are like controllers and allow you to split the application by Screens or Scenes.
In a Scene you usually create Views and configure/decorate them giving them behaviour.

For each scene you will have to tell the injector how to construct it. Usually you do this mapping in the `Module` descriptor:

```kotlin
injector.mapPrototype { MyScene(get()) }
```

Inside the lambda of `mapPrototype`, you have injected `this: AsyncInjector`, that's why you can use the method `get()`.
Usually the `get()` method won't require type parameters since it is generic and the type is inferred from the arguments. 
{:.note}

## The `SceneContainer` class
{:#SceneContainer}

The `SceneContainer` is a `View` that will contain the view of a `Scene`.

```kotlin
class SceneContainer(val views: Views) : Container() {
	val transitionView = TransitionView()
	var currentScene: Scene? = null

	suspend inline fun <reified TScene : Scene> changeTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): TScene
	suspend inline fun <reified TScene : Scene> pushTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): TScene
	suspend fun back(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene
	suspend fun forward(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene
	
	suspend fun <TScene : Scene> pushTo(clazz: KClass<TScene>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): TScene
	suspend fun <TScene : Scene> changeTo(clazz: KClass<TScene>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): TScene
}
```

Like other views, the `SceneContainer` has a builder method to construct the instance and add it to a container:

```kotlin
inline fun Container.sceneContainer(views: Views, callback: SceneContainer.() -> Unit = {}): SceneContainer = SceneContainer(views).addTo(this).apply(callback)
```

Scenes have access to its container, so you can change the current scene to another one with:

```kotlin
class MyScene : Scene() {
	override suspend fun Container.sceneInit(): Unit {
	    solidRect(100, 100, Colors.RED).onClick { launchImmediately { sceneContainer.changeTo<OtherScene>() } }
	}
}
```

