package korlibs.inject

@Deprecated("", replaceWith = ReplaceWith("Injector", "korlibs.inject.Injector"))
typealias AsyncInjector = Injector

@Deprecated("", replaceWith = ReplaceWith("InjectorFactory<T>", "korlibs.inject.InjectorFactory"))
typealias AsyncFactory<T> = InjectorFactory<T>

@Deprecated("", replaceWith = ReplaceWith("InjectorDependency", "korlibs.inject.InjectorDependency"))
typealias InjectorAsyncDependency = InjectorDependency
@Deprecated("", replaceWith = ReplaceWith("InjectorDependency", "korlibs.inject.InjectorDependency"))
typealias AsyncDependency = InjectorDependency

@Deprecated("", replaceWith = ReplaceWith("PrototypeObjectProvider", "korlibs.inject.PrototypeObjectProvider"))
typealias PrototypeAsyncObjectProvider<T> = PrototypeObjectProvider<T>
@Deprecated("", replaceWith = ReplaceWith("SingletonObjectProvider", "korlibs.inject.SingletonObjectProvider"))
typealias SingletonAsyncObjectProvider<T> = SingletonObjectProvider<T>
@Deprecated("", replaceWith = ReplaceWith("FactoryObjectProvider", "korlibs.inject.FactoryObjectProvider"))
typealias FactoryAsyncObjectProvider<T> = FactoryObjectProvider<T>


/*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

interface AsyncObjectProvider<T> {
    suspend fun get(injector: Injector): T
    suspend fun deinit()
}

class PrototypeAsyncObjectProvider<T>(val generator: suspend Injector.() -> T) : AsyncObjectProvider<T> {
    override suspend fun get(injector: Injector): T = injector.created(generator(injector))
    override suspend fun deinit() = Unit
    override fun toString(): String = "PrototypeAsyncObjectProvider()"
}

class FactoryAsyncObjectProvider<T>(val generator: suspend Injector.() -> AsyncFactory<T>) :
    AsyncObjectProvider<T> {
    override suspend fun get(injector: Injector): T = injector.created(generator(injector).create())
    override suspend fun deinit() = Unit
    override fun toString(): String = "FactoryAsyncObjectProvider()"
}

class SingletonAsyncObjectProvider<T>(val generator: suspend Injector.() -> T) : AsyncObjectProvider<T> {
    var value: T? = null
    override suspend fun get(injector: Injector): T {
        if (value == null) value = injector.created(generator(injector))
        return value!!
    }
    override suspend fun deinit() {
        (value as? AsyncDestructor?)?.deinit()
    }

    override fun toString(): String = "SingletonAsyncObjectProvider($value)"
}

class InstanceAsyncObjectProvider<T>(val instance: T) : AsyncObjectProvider<T> {
    override suspend fun get(injector: Injector): T = instance
    override suspend fun deinit() {
        (instance as? AsyncDestructor?)?.deinit()
    }
    override fun toString(): String = "InstanceAsyncObjectProvider($instance)"
}

class Injector(val parent: Injector? = null, val level: Int = 0) {
    companion object {}
    suspend inline fun <reified T : Any> getWith(vararg instances: Any): T = getWith(T::class, *instances)
    suspend inline fun <reified T : Any> get(): T = get<T>(T::class)
    suspend inline fun <reified T : Any> getOrNull(): T? = getOrNull<T>(T::class)
    inline fun <reified T : Any> mapInstance(instance: T): Injector = mapInstance(T::class, instance)
    inline fun <reified T : Any> mapFactory(noinline gen: suspend Injector.() -> AsyncFactory<T>) =
        mapFactory(T::class, gen)

    inline fun <reified T : Any> mapSingleton(noinline gen: suspend Injector.() -> T) = mapSingleton(T::class, gen)
    inline fun <reified T : Any> mapPrototype(noinline gen: suspend Injector.() -> T) = mapPrototype(T::class, gen)

    fun removeMapping(clazz: KClass<*>) {
        providersByClass.remove(clazz)
        parent?.removeMapping(clazz)
    }

    var fallbackProvider: (suspend (clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> AsyncObjectProvider<*>)? = null
    val providersByClass = LinkedHashMap<kotlin.reflect.KClass<*>, AsyncObjectProvider<*>>()

    val root: Injector = parent?.root ?: this
    val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

    fun child() = Injector(this, level + 1)

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

    fun <T : Any> mapInstance(clazz: KClass<T>, instance: T): Injector = this.apply {
        providersByClass[clazz] = InstanceAsyncObjectProvider<T>(instance)
    }

    fun <T : Any> mapFactory(clazz: KClass<T>, gen: suspend Injector.() -> AsyncFactory<T>): Injector =
        this.apply {
            providersByClass[clazz] = FactoryAsyncObjectProvider<T>(gen)
        }

    fun <T : Any> mapSingleton(clazz: KClass<T>, gen: suspend Injector.() -> T): Injector = this.apply {
        providersByClass[clazz] = SingletonAsyncObjectProvider<T>(gen)
    }

    fun <T : Any> mapPrototype(clazz: KClass<T>, gen: suspend Injector.() -> T): Injector = this.apply {
        providersByClass[clazz] = PrototypeAsyncObjectProvider<T>(gen)
    }

    init {
        mapInstance(Injector::class, this)
    }

    data class RequestContext(val initialClazz: KClass<*>)

    fun getClassDefiner(clazz: KClass<*>): Injector? {
        if (clazz in providersByClass) return this
        return parent?.getClassDefiner(clazz)
    }

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
        getProviderOrNull<T>(clazz, ctx) ?: throw Injector.NotMappedException(
            clazz, ctx.initialClazz, ctx, "Class '$clazz' doesn't have constructors $ctx"
        )

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
        return getProviderOrNull<T>(clazz, ctx)?.get(this)
    }

    inline fun <reified T : Any> getSync(ctx: RequestContext = RequestContext(T::class)): T = getSync(T::class, ctx)
    inline fun <reified T : Any> getSyncOrNull(ctx: RequestContext = RequestContext(T::class)): T? = getSyncOrNull(T::class, ctx)
    fun <T : Any> getSync(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T {
        return getSyncOrNull(clazz, ctx) ?: throw RuntimeException("Couldn't get instance of type $clazz synchronously")
    }
    fun <T : Any> getSyncOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
        var rresult: T? = null
        var rexception: Throwable? = null
        suspend {
            getOrNull(clazz, ctx)
        }.startCoroutine(object : Continuation<T?> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T?>) {
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    rexception = exception
                } else {
                    rresult = result.getOrNull()
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

    override fun toString(): String = "Injector(level=$level)"

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
    suspend fun init(injector: Injector): Unit
}

suspend fun <T> withInjector(injector: Injector, block: suspend () -> T): T =
    withContext(AsyncInjectorContext(injector)) {
        block()
    }

suspend fun injector(): Injector =
    coroutineContext[AsyncInjectorContext]?.injector
        ?: error("Injector not in the context, please call withInjector function")

class AsyncInjectorContext(val injector: Injector) : CoroutineContext.Element {
    companion object : CoroutineContext.Key<AsyncInjectorContext>

    override val key get() = AsyncInjectorContext
}
*/
