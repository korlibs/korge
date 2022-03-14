package com.soywiz.korinject

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

interface AsyncObjectProvider<T> {
    suspend fun get(injector: AsyncInjector): T
    suspend fun deinit()
}

class PrototypeAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T> {
    override suspend fun get(injector: AsyncInjector): T = injector.created(generator(injector))
    override suspend fun deinit() = Unit
    override fun toString(): String = "PrototypeAsyncObjectProvider()"
}

class FactoryAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> AsyncFactory<T>) :
    AsyncObjectProvider<T> {
    override suspend fun get(injector: AsyncInjector): T = injector.created(generator(injector).create())
    override suspend fun deinit() = Unit
    override fun toString(): String = "FactoryAsyncObjectProvider()"
}

class SingletonAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T> {
    var value: T? = null
    override suspend fun get(injector: AsyncInjector): T {
        if (value == null) value = injector.created(generator(injector))
        return value!!
    }
    override suspend fun deinit() {
        (value as? AsyncDestructor?)?.deinit()
    }

    override fun toString(): String = "SingletonAsyncObjectProvider($value)"
}

class InstanceAsyncObjectProvider<T>(val instance: T) : AsyncObjectProvider<T> {
    override suspend fun get(injector: AsyncInjector): T = instance
    override suspend fun deinit() {
        (instance as? AsyncDestructor?)?.deinit()
    }
    override fun toString(): String = "InstanceAsyncObjectProvider($instance)"
}

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) {
    suspend inline fun <reified T : Any> getWith(vararg instances: Any): T = getWith(T::class, *instances)
    suspend inline fun <reified T : Any> get(): T = get<T>(T::class)
    suspend inline fun <reified T : Any> getOrNull(): T? = getOrNull<T>(T::class)
    inline fun <reified T : Any> mapInstance(instance: T): AsyncInjector = mapInstance(T::class, instance)
    inline fun <reified T : Any> mapFactory(noinline gen: suspend AsyncInjector.() -> AsyncFactory<T>) =
        mapFactory(T::class, gen)

    inline fun <reified T : Any> mapSingleton(noinline gen: suspend AsyncInjector.() -> T) = mapSingleton(T::class, gen)
    inline fun <reified T : Any> mapPrototype(noinline gen: suspend AsyncInjector.() -> T) = mapPrototype(T::class, gen)

    var fallbackProvider: (suspend (clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> AsyncObjectProvider<*>)? = null
    val providersByClass = LinkedHashMap<kotlin.reflect.KClass<*>, AsyncObjectProvider<*>>()

    val root: AsyncInjector = parent?.root ?: this
    val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

    fun child() = AsyncInjector(this, level + 1)

    suspend fun <T : Any> getWith(clazz: KClass<T>, vararg instances: Any): T {
        val c = child()
        for (i in instances) {
            @Suppress("UNCHECKED_CAST")
            c.mapInstance(i::class as KClass<Any>, i)
        }
        return c.get(clazz)
    }

    fun dump() {
        println("$this")
        for ((k, v) in providersByClass) {
            println("- $k: $v")
        }
        parent?.dump()
    }

    fun <T : Any> mapInstance(clazz: KClass<T>, instance: T): AsyncInjector = this.apply {
        providersByClass[clazz] = InstanceAsyncObjectProvider<T>(instance)
    }

    fun <T : Any> mapFactory(clazz: KClass<T>, gen: suspend AsyncInjector.() -> AsyncFactory<T>): AsyncInjector =
        this.apply {
            providersByClass[clazz] = FactoryAsyncObjectProvider<T>(gen)
        }

    fun <T : Any> mapSingleton(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector = this.apply {
        providersByClass[clazz] = SingletonAsyncObjectProvider<T>(gen)
    }

    fun <T : Any> mapPrototype(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector = this.apply {
        providersByClass[clazz] = PrototypeAsyncObjectProvider<T>(gen)
    }

    init {
        mapInstance(AsyncInjector::class, this)
    }

    data class RequestContext(val initialClazz: KClass<*>)

    suspend fun <T : Any> getProviderOrNull(
        clazz: KClass<T>,
        ctx: RequestContext = RequestContext(clazz)
    ): AsyncObjectProvider<T>? {
        return (providersByClass[clazz]
            ?: parent?.getProviderOrNull<T>(clazz, ctx)
            ?: nearestFallbackProvider?.invoke(clazz, ctx)?.also { providersByClass[clazz] = it }
        ) as? AsyncObjectProvider<T>?
    }

    suspend fun <T : Any> getProvider(
        clazz: KClass<T>,
        ctx: RequestContext = RequestContext(clazz)
    ): AsyncObjectProvider<T> =
        getProviderOrNull<T>(clazz, ctx) ?: throw AsyncInjector.NotMappedException(
            clazz, ctx.initialClazz, ctx, "Class '$clazz' doesn't have constructors $ctx"
        )

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
        return getProviderOrNull<T>(clazz, ctx)?.get(this)
    }

    inline fun <reified T : Any> getSync(ctx: RequestContext = RequestContext(T::class)): T = getSync(T::class, ctx)
    fun <T : Any> getSync(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T {
        lateinit var rresult: T
        var rexception: Throwable? = null
        suspend {
            get(clazz, ctx)
        }.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    rexception = exception
                } else {
                    rresult = result.getOrThrow()
                }
            }
        })
        if (rexception != null) throw rexception!!
        try {
            return rresult
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            throw RuntimeException("Couldn't get instance of type $clazz synchronously", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T {
        return getProvider<T>(clazz, ctx).get(this)
    }

    suspend fun <T : Any> has(clazz: KClass<T>): Boolean = getProviderOrNull<T>(clazz) != null

    class NotMappedException(
        val clazz: KClass<*>,
        val requestedByClass: KClass<*>,
        val ctx: RequestContext,
        val msg: String = "Not mapped $clazz requested by $requestedByClass in $ctx"
    ) : RuntimeException(msg)

    override fun toString(): String = "AsyncInjector(level=$level)"

    suspend internal fun <T> created(instance: T): T {
        if (instance is AsyncDependency) instance.init()
        if (instance is InjectorAsyncDependency) instance.init(this)
        if (instance is AsyncDestructor) deinitList.add(instance)
        return instance
    }

    private val deinitList = arrayListOf<AsyncDestructor>()

    fun addDeinit(value: AsyncDestructor) {
        deinitList.add(value)
    }

    suspend fun deinit() {
        for (pair in providersByClass) pair.value.deinit()
        for (deinit in deinitList) deinit.deinit()
        deinitList.clear()
    }
}

interface AsyncFactory<T> {
    suspend fun create(): T
}

interface InjectedHandler {
    suspend fun injectedInto(instance: Any): Unit
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)
//annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)
//annotation class AsyncFactoryClass<T : Any>(val clazz: KClass<AsyncFactory<T>>)
//annotation class AsyncFactoryClass<T>(val clazz: kotlin.reflect.KClass<out AsyncFactory<*>>)

interface AsyncDependency {
    suspend fun init(): Unit
}

interface AsyncDestructor {
    suspend fun deinit(): Unit
}

interface InjectorAsyncDependency {
    suspend fun init(injector: AsyncInjector): Unit
}
