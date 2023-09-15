---
layout: default
title: "Korinject"
fa-icon: fa-puzzle-piece
priority: 970
---

Portable Kotlin Common library to do asynchronous dependency injection.

[https://github.com/korlibs/korge/tree/main/korinject](https://github.com/korlibs/korge/tree/main/korinject)

{% include stars.html project="korge" central="com.soywiz.korlibs.korinject/korinject" %}

{% include toc_include.md %}

## Creating a new injector

An injector allow to hold instances, singletons and class constructions, so it allows to access instances later.

```kotlin
val injector = Injector()
```

### Creating a child injector

A child injector, will be able to access/create instances created by parent and ancestor injectors,
but all the new mappings will be limited to the child injector.

```kotlin
val injector = injector.child()
```

## Mappings

Before getting or constructing instances, it is mandatory to map some instances, singletons or prototypes.

### Instances

If you want to save an already constructed instance for later usage, you can map an instance like this:

```kotlin
injector.mapInstance(myInstance)
// or
injector.mapInstance<MyInstanceClass>(myInstance)
```

Instances are useful for configurations.

### Singletons

If you want to construct a singleton, in a way that all its dependencies are resolved automatically, you can use a singleton.
A singleton will create a single instance per injector once, lazily when first requested.

```kotlin
injector.mapSingleton<MyClass> { MyClass(get(), get(), get(), ...) }
```

Depending on the number of constructor parameters, it is needed to provide the exact number of `get()`.
Singletons are useful for services.

### Prototypes

If you want to construct a new object every time a specific type instance is requested, you can map prototypes.
Similarly to singletons:

```kotlin
injector.mapPrototype<MyClass> { MyClass(get(), get(), get(), ...) }
```

## Getting instances

Once the injector has been configured, you can start to request instances.
If the requested class was mapped as an instance, the provided instance will be returned,
if the requested class was mapped as a singleton, a new instance will be created once, cached, and returned every time.
And if the requested class was mapped as a prototype, a new class will be constructed and returned every time.

```kotlin
val instanceOrThrow: MyClass = injector.get<MyClass>()
val nullable: MyClass? = injector.getOrNull<MyClass>()
```

## API

```kotlin
class Injector(val parent: Injector? = null, val level: Int = 0) {
    suspend inline fun <reified T : Any> getWith(vararg instances: Any): T
    suspend inline fun <reified T : Any> get(): T
    suspend inline fun <reified T : Any> getOrNull(): T?

    inline fun <reified T : Any> mapInstance(instance: T): Injector
    inline fun <reified T : Any> mapFactory(noinline gen: suspend Injector.() -> AsyncFactory<T>)
    inline fun <reified T : Any> mapSingleton(noinline gen: suspend Injector.() -> T)
    inline fun <reified T : Any> mapPrototype(noinline gen: suspend Injector.() -> T)

    fun <T : Any> mapInstance(clazz: KClass<T>, instance: T): Injector
    fun <T : Any> mapFactory(clazz: KClass<T>, gen: suspend Injector.() -> AsyncFactory<T>): Injector 
    fun <T : Any> mapSingleton(clazz: KClass<T>, gen: suspend Injector.() -> T): Injector
    fun <T : Any> mapPrototype(clazz: KClass<T>, gen: suspend Injector.() -> T): Injector

    var fallbackProvider: (suspend (clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> AsyncObjectProvider<*>)?
    val providersByClass: LinkedHashMap<kotlin.reflect.KClass<*>, AsyncObjectProvider<*>>()

    val root: Injector = parent?.root ?: this
    val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

    fun child(): Injector

    suspend fun <T : Any> getWith(clazz: KClass<T>, vararg instances: Any): T

    data class RequestContext(val initialClazz: KClass<*>)

    suspend fun <T : Any> getProviderOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): AsyncObjectProvider<T>?

    suspend fun <T : Any> getProvider(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): AsyncObjectProvider<T>
    suspend fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T?
    inline fun <reified T : Any> getSync(ctx: RequestContext = RequestContext(T::class)): T
    fun <T : Any> getSync(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T
    suspend fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T
    suspend fun <T : Any> has(clazz: KClass<T>): Boolean

    class NotMappedException(
        val clazz: KClass<*>,
        val requestedByClass: KClass<*>,
        val ctx: RequestContext,
        val msg: String = "Not mapped $clazz requested by $requestedByClass in $ctx"
    ) : RuntimeException(msg)

    override fun toString(): String

    suspend internal fun <T> created(instance: T): T
}
```

```kotlin
interface AsyncDependency {
    suspend fun init(): Unit
}

interface InjectorAsyncDependency {
    suspend fun init(injector: Injector): Unit
}
```

```kotlin

interface AsyncFactory<T> {
    suspend fun create(): T
}

interface InjectedHandler {
    suspend fun injectedInto(instance: Any): Unit
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)

interface AsyncObjectProvider<T> {
    suspend fun get(injector: Injector): T
}

class PrototypeAsyncObjectProvider<T>(val generator: suspend Injector.() -> T) : AsyncObjectProvider<T>
class FactoryAsyncObjectProvider<T>(val generator: suspend Injector.() -> AsyncFactory<T>) :
    AsyncObjectProvider<T>
class SingletonAsyncObjectProvider<T>(val generator: suspend Injector.() -> T) : AsyncObjectProvider<T> {
    var value: T? = null
}
class InstanceAsyncObjectProvider<T>(val instance: T) : AsyncObjectProvider<T>
```

{% include using_with_gradle.md name="korinject" %}
