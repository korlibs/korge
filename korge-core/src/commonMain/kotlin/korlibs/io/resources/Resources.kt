package korlibs.io.resources

import korlibs.io.async.asyncImmediately
import korlibs.io.experimental.KorioExperimentalApi
import korlibs.io.file.VfsFile
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

annotation class ResourcePath()

interface Resourceable<T : Any> {
    fun getOrNull(): T?
    suspend fun get(): T

    data class Fixed<T : Any>(val value: T) : Resourceable<T> {
        override fun getOrNull() = value
        override suspend fun get() = value
    }
}

fun <T : Any> Resourceable(value: T) = Resourceable.Fixed(value)

open class Resource<T : Any>(
    val resources: Resources,
    val name: String,
    val cache: ResourceCache,
    private val gen: suspend Resources.() -> T
) : Resourceable<T> {
    private var valueDeferred: Deferred<T>? = null

    var valueOrNull: T? = null; private set
    @KorioExperimentalApi
    var onGen: (() -> Unit)? = null

    override fun getOrNull(): T? {
        getDeferred()
        return valueOrNull
    }

    fun getDeferred(): Deferred<T> {
        if (valueDeferred == null) {
            valueDeferred = asyncImmediately(resources.coroutineContext) {
                resources.add(this)
                gen(resources).also {
                    valueOrNull = it
                    onGen?.invoke()
                }
            }
        }
        return valueDeferred!!
    }

    override suspend fun get(): T = getDeferred().await()

    suspend fun preload(): T = get()

    fun preloadNoWait() {
        getDeferred()
    }

    fun unload() {
        resources.remove(name)
        valueOrNull = null
    }
}

open class GlobalResources(coroutineContext: CoroutineContext, root: VfsFile = resourcesVfs) : Resources(coroutineContext, root, null)

interface ResourcesContainer {
    val resources: Resources
}

open class Resources(val coroutineContext: CoroutineContext, val root: VfsFile = resourcesVfs, val parent: Resources? = null) : ResourcesContainer {
    override val resources: Resources get() = this
    open val map: MutableMap<String, Resource<*>> = LinkedHashMap()
    open fun remove(name: String) {
        if (map.containsKey(name)) {
            map.remove(name)
        } else {
            parent?.remove(name)
        }
    }
    open fun add(resource: Resource<*>) {
        if (resource.cache == ResourceCache.NONE) return
        if (parent != null && resource.cache == ResourceCache.GLOBAL) {
            parent?.add(resource)
            return
        }
        map[resource.name] = resource
    }
    @Suppress("UNCHECKED_CAST")
    open fun <T : Any> get(name: String, cache: ResourceCache = ResourceCache.GLOBAL): Resource<T>? {
        if (cache == ResourceCache.NONE) return null
        val res = (map as Map<String, Resource<T>>)[name]
        if (res != null) return res
        if (cache == ResourceCache.GLOBAL) return parent?.get(name)
        return null
    }
}

enum class ResourceCache { GLOBAL, LOCAL, NONE }

class ResourceRef<T : Any>(val cache: ResourceCache = ResourceCache.GLOBAL, val gen: suspend Resources.() -> T) {
    operator fun getValue(resourcesContainer: ResourcesContainer, property: KProperty<*>): Resource<T> {
        val resources = resourcesContainer.resources
        val res = resources.get<T>(property.name, cache)
        if (res != null) return res
        val res2 = Resource(resources, property.name, cache, gen)
        resources.add(res2)
        return res2
    }
}

fun <T : Any> resource(cache: ResourceCache = ResourceCache.LOCAL, gen: suspend Resources.() -> T) = ResourceRef(cache, gen)
fun <T : Any> resourceGlobal(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.GLOBAL, gen)
fun <T : Any> resourceLocal(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.LOCAL, gen)
fun <T : Any> resourceUncached(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.NONE, gen)
