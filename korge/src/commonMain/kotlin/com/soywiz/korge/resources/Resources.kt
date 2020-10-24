package com.soywiz.korge.resources

import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.coroutines.*
import kotlin.reflect.*

annotation class ResourcePath()

interface Resourceable<T : Any> {
    suspend fun get(): T
}

class Resource<T : Any>(
    val resources: Resources,
    val name: String,
    val cache: ResourceCache,
    val gen: suspend Resources.() -> T
) : Resourceable<T> {
    private val thread = AsyncThread()
    var valueOrNull: T? = null

    override suspend fun get(): T = thread {
        if (valueOrNull == null) {
            resources.add(this)
            valueOrNull = gen(resources)
        }
        valueOrNull!!
    }

    fun preload() {
        launchImmediately(resources.coroutineContext) {
            get()
        }
    }

    fun unload() {
        resources.remove(name)
        valueOrNull = null
    }
}

open class GlobalResources(coroutineContext: CoroutineContext, root: VfsFile = resourcesVfs) : Resources(coroutineContext, root, null)

open class Resources(val coroutineContext: CoroutineContext, val root: VfsFile = resourcesVfs, val parent: Resources? = null) : AsyncDestructor {
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

    override suspend fun deinit() {
        map.clear()
    }
}

enum class ResourceCache { GLOBAL, LOCAL, NONE }

suspend fun resources(): Resources = injector().get()

class ResourceRef<T : Any>(val cache: ResourceCache = ResourceCache.GLOBAL, val gen: suspend Resources.() -> T) {
    operator fun getValue(resources: Resources, property: KProperty<*>): Resource<T> {
        val res = resources.get<T>(property.name, cache)
        if (res != null) return res
        val res2 = Resource(resources, property.name, cache, gen)
        resources.add(res2)
        return res2
    }
}

fun <T : Any> resourceGlobal(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.GLOBAL, gen)
fun <T : Any> resourceLocal(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.LOCAL, gen)
fun <T : Any> resourceUncached(gen: suspend Resources.() -> T) = ResourceRef(ResourceCache.NONE, gen)
