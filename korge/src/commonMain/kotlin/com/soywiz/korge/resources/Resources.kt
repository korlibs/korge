package com.soywiz.korge.resources

import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.coroutines.*
import kotlin.reflect.*

annotation class ResourcePath()

class Resource<T : Any>(
    val resources: Resources,
    val name: String,
    val scope: ResourceScope,
    val gen: suspend (Resources) -> T
) {
    private val thread = AsyncThread()
    var valueOrNull: T? = null

    suspend fun get(): T = thread {
        if (valueOrNull == null) {
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

open class Resources(val coroutineContext: CoroutineContext, val root: VfsFile = resourcesVfs, val parent: Resources? = null) {
    internal val map = LinkedHashMap<String, Resource<*>>()
    internal fun remove(name: String) {
        if (map.containsKey(name)) {
            map.remove(name)
        } else {
            parent?.remove(name)
        }
    }
    internal fun add(resource: Resource<*>) {
        if (resource.scope == ResourceScope.NONE) return
        if (parent != null && resource.scope == ResourceScope.GLOBAL) return parent?.add(resource)
        map[resource.name] = resource
    }
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> get(name: String, scope: ResourceScope = ResourceScope.GLOBAL): Resource<T>? {
        if (scope == ResourceScope.NONE) return null
        val res = (map as Map<String, Resource<T>>)[name]
        if (res != null) return res
        if (scope == ResourceScope.GLOBAL) return parent?.get(name)
        return null
    }
}

enum class ResourceScope { GLOBAL, LOCAL, NONE }

class ResourceRef<T : Any>(val scope: ResourceScope = ResourceScope.GLOBAL, val gen: suspend (Resources) -> T) {
    operator fun getValue(resources: Resources, property: KProperty<*>): Resource<T> {
        val res = resources.get<T>(property.name, scope)
        if (res != null) return res
        val res2 = Resource(resources, property.name, scope, gen)
        resources.add(res2)
        return res2
    }
}
