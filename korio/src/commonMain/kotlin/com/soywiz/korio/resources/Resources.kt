package com.soywiz.korio.resources

import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlin.reflect.*

annotation class ResourcePath()

interface Resourceable<T : Any> {
    fun getOrNull(): T?
    suspend fun get(): T
}

class Resource<T : Any>(
    val resources: Resources,
    val name: String,
    val cache: ResourceCache,
    private val gen: suspend Resources.() -> T
) : Resourceable<T> {
    private var valueDeferred: Deferred<T>? = null
    private var valueOrNull: T? = null

    override fun getOrNull(): T? {
        getDeferred()
        return valueOrNull
    }

    fun getDeferred(): Deferred<T> {
        if (valueDeferred == null) {
            valueDeferred = asyncImmediately(resources.coroutineContext) {
                resources.add(this)
                gen(resources).also { valueOrNull = it }
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
    internal val map = LinkedHashMap<String, Resource<*>>()
    internal fun remove(name: String) {
        if (map.containsKey(name)) {
            map.remove(name)
        } else {
            parent?.remove(name)
        }
    }
    internal fun add(resource: Resource<*>) {
        if (resource.cache == ResourceCache.NONE) return
        if (parent != null && resource.cache == ResourceCache.GLOBAL) return parent?.add(resource)
        map[resource.name] = resource
    }
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> get(name: String, cache: ResourceCache = ResourceCache.GLOBAL): Resource<T>? {
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
