package korlibs.inject

import kotlin.reflect.*

interface ObjectProvider<T> {
    fun get(injector: Injector): T
    fun deinit()
}

class PrototypeObjectProvider<T>(val generator: Injector.() -> T) : ObjectProvider<T> {
    override fun get(injector: Injector): T = injector.created(generator(injector))
    override fun deinit() = Unit
    override fun toString(): String = "PrototypeObjectProvider()"
}

class FactoryObjectProvider<T>(val generator: Injector.() -> InjectorFactory<T>) :
    ObjectProvider<T> {
    override fun get(injector: Injector): T = injector.created(generator(injector).create())
    override fun deinit() = Unit
    override fun toString(): String = "FactoryObjectProvider()"
}

class SingletonObjectProvider<T>(val generator: Injector.() -> T) : ObjectProvider<T> {
    var value: T? = null
    override fun get(injector: Injector): T {
        if (value == null) value = injector.created(generator(injector))
        return value!!
    }
    override fun deinit() {
        (value as? InjectorDestructor?)?.deinit()
    }

    override fun toString(): String = "SingletonObjectProvider($value)"
}

class InstanceObjectProvider<T>(val instance: T) : ObjectProvider<T> {
    override fun get(injector: Injector): T = instance
    override fun deinit() {
        (instance as? InjectorDestructor?)?.deinit()
    }
    override fun toString(): String = "InstanceObjectProvider($instance)"
}

class Injector(val parent: Injector? = null, val level: Int = 0) {
    companion object;

    inline fun <reified T : Any> getWith(vararg instances: Any): T = getWith(T::class, *instances)
    inline fun <reified T : Any> get(): T = get<T>(T::class)
    @Deprecated("", ReplaceWith("get<T>()"))
    inline fun <reified T : Any> getSync(): T = get<T>(T::class)
    @Deprecated("", ReplaceWith("getOrNull<T>()"))
    inline fun <reified T : Any> getSyncOrNull(): T? = getOrNull<T>(T::class)

    inline fun <reified T : Any> getOrNull(): T? = getOrNull<T>(T::class)
    inline fun <reified T : Any> mapInstance(instance: T): Injector = mapInstance(T::class, instance)
    inline fun <reified T : Any> mapFactory(noinline gen: Injector.() -> InjectorFactory<T>) =
        mapFactory(T::class, gen)

    inline fun <reified T : Any> mapSingleton(noinline gen: Injector.() -> T) = mapSingleton(T::class, gen)
    inline fun <reified T : Any> mapPrototype(noinline gen: Injector.() -> T) = mapPrototype(T::class, gen)

    fun removeMapping(clazz: KClass<*>) {
        providersByClass.remove(clazz)
        parent?.removeMapping(clazz)
    }

    var fallbackProvider: ((clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> ObjectProvider<*>)? = null
    val providersByClass = LinkedHashMap<kotlin.reflect.KClass<*>, ObjectProvider<*>>()

    val root: Injector = parent?.root ?: this
    val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

    fun child() = Injector(this, level + 1)

    fun <T : Any> getWith(clazz: KClass<T>, vararg instances: Any): T {
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
        providersByClass[clazz] = InstanceObjectProvider<T>(instance)
    }

    fun <T : Any> mapFactory(clazz: KClass<T>, gen: Injector.() -> InjectorFactory<T>): Injector =
        this.apply {
            providersByClass[clazz] = FactoryObjectProvider<T>(gen)
        }

    fun <T : Any> mapSingleton(clazz: KClass<T>, gen: Injector.() -> T): Injector = this.apply {
        providersByClass[clazz] = SingletonObjectProvider<T>(gen)
    }

    fun <T : Any> mapPrototype(clazz: KClass<T>, gen: Injector.() -> T): Injector = this.apply {
        providersByClass[clazz] = PrototypeObjectProvider<T>(gen)
    }

    init {
        mapInstance(Injector::class, this)
    }

    data class RequestContext(val initialClazz: KClass<*>)

    fun getClassDefiner(clazz: KClass<*>): Injector? {
        if (clazz in providersByClass) return this
        return parent?.getClassDefiner(clazz)
    }

    fun <T : Any> getProviderOrNull(
        clazz: KClass<T>,
        ctx: RequestContext = RequestContext(clazz)
    ): ObjectProvider<T>? {
        return (providersByClass[clazz]
            ?: parent?.getProviderOrNull<T>(clazz, ctx)
            ?: nearestFallbackProvider?.invoke(clazz, ctx)?.also { providersByClass[clazz] = it }
        ) as? ObjectProvider<T>?
    }

    fun <T : Any> getProvider(
        clazz: KClass<T>,
        ctx: RequestContext = RequestContext(clazz)
    ): ObjectProvider<T> =
        getProviderOrNull<T>(clazz, ctx) ?: throw Injector.NotMappedException(
            clazz, ctx.initialClazz, ctx, "Class '$clazz' doesn't have constructors $ctx"
        )

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
        return getProviderOrNull<T>(clazz, ctx)?.get(this)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T {
        return getProvider<T>(clazz, ctx).get(this)
    }

    fun <T : Any> has(clazz: KClass<T>): Boolean = getProviderOrNull<T>(clazz) != null

    class NotMappedException(
        val clazz: KClass<*>,
        val requestedByClass: KClass<*>,
        val ctx: RequestContext,
        val msg: String = "Not mapped $clazz requested by $requestedByClass in $ctx"
    ) : RuntimeException(msg)

    override fun toString(): String = "Injector(level=$level)"

    internal fun <T> created(instance: T): T {
        if (instance is InjectorDependency) instance.init(this)
        if (instance is InjectorDestructor) deinitList.add(instance)
        return instance
    }

    private val deinitList = arrayListOf<InjectorDestructor>()

    fun addDeinit(value: InjectorDestructor) {
        deinitList.add(value)
    }

    fun deinit() {
        for (pair in providersByClass) pair.value.deinit()
        for (deinit in deinitList) deinit.deinit()
        deinitList.clear()
    }
}

interface InjectorFactory<T> {
    fun create(): T
}

interface InjectedHandler {
    fun injectedInto(instance: Any): Unit
}

annotation class FactoryClass(val clazz: KClass<out InjectorFactory<*>>)
//annotation class FactoryClass(val clazz: KClass<out Factory<*>>)
//annotation class FactoryClass<T : Any>(val clazz: KClass<Factory<T>>)
//annotation class FactoryClass<T>(val clazz: kotlin.reflect.KClass<out Factory<*>>)

interface InjectorDestructor {
    fun deinit(): Unit
}

interface InjectorDependency {
    fun init(injector: Injector): Unit
}
