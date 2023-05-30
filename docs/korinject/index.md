---
layout: default
title: "Korinject"
fa-icon: fa-puzzle-piece
priority: 970
---

Portable Kotlin Common library to do asynchronous dependency injection.

[https://github.com/korlibs/korge/tree/main/korinject](https://github.com/korlibs/korge/tree/main/korinject)

{% include stars.html project="korinject" %}

{% include toc_include.md %}

## API

```kotlin
class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) {
    suspend inline fun <reified T : Any> getWith(vararg instances: Any): T
    suspend inline fun <reified T : Any> get(): T
    suspend inline fun <reified T : Any> getOrNull(): T?

    inline fun <reified T : Any> mapInstance(instance: T): AsyncInjector
    inline fun <reified T : Any> mapFactory(noinline gen: suspend AsyncInjector.() -> AsyncFactory<T>)
    inline fun <reified T : Any> mapSingleton(noinline gen: suspend AsyncInjector.() -> T)
    inline fun <reified T : Any> mapPrototype(noinline gen: suspend AsyncInjector.() -> T)

    fun <T : Any> mapInstance(clazz: KClass<T>, instance: T): AsyncInjector
    fun <T : Any> mapFactory(clazz: KClass<T>, gen: suspend AsyncInjector.() -> AsyncFactory<T>): AsyncInjector 
    fun <T : Any> mapSingleton(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector
    fun <T : Any> mapPrototype(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector

    var fallbackProvider: (suspend (clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> AsyncObjectProvider<*>)?
    val providersByClass: LinkedHashMap<kotlin.reflect.KClass<*>, AsyncObjectProvider<*>>()

    val root: AsyncInjector = parent?.root ?: this
    val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

    fun child(): AsyncInjector

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
    suspend fun init(injector: AsyncInjector): Unit
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
    suspend fun get(injector: AsyncInjector): T
}

class PrototypeAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T>
class FactoryAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> AsyncFactory<T>) :
    AsyncObjectProvider<T>
class SingletonAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T> {
    var value: T? = null
}
class InstanceAsyncObjectProvider<T>(val instance: T) : AsyncObjectProvider<T>
```

{% include using_with_gradle.md name="korinject" %}
