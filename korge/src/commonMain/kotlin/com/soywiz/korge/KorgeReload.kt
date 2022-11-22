package com.soywiz.korge

import com.soywiz.kmem.Platform
import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.lang.Environment
import kotlin.jvm.JvmStatic
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

class ReloadClassContext(val injector: AsyncInjector, val refreshedClasses: Set<String>)

internal open class KorgeReloadInternalImpl {
    open fun <T : Any> getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> = clazz
    open fun transferKeepProperties(old: Any, new: Any) = Unit
}

internal expect val KorgeReloadInternal: KorgeReloadInternalImpl

object KorgeReload {
    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    private var KorgeReload_eventDispatcher: EventDispatcher? = null

    @JvmStatic
    @Suppress("unused") // This is called from [com.soywiz.korge.reloadagent.KorgeReloadAgent]
    fun triggerReload(classes: List<String>, success: Boolean) {
        println("KorgeReloadAgent detected a class change. Reload: $classes")
        KorgeReload_eventDispatcher?.dispatch(ReloadEvent::class, ReloadEvent(classes.toSet(), success))
    }

    fun registerEventDispatcher(eventDispatcher: EventDispatcher) {
        // Only in JVM and if KORGE_AUTORELOAD env is set
        if (!Platform.runtime.isJvm || Environment["KORGE_AUTORELOAD"] != "true") return
        KorgeReload_eventDispatcher = eventDispatcher
    }

    fun unregisterEventDispatcher() {
        if (!Platform.runtime.isJvm) return // Prevent mutation in K/N
        if (KorgeReload_eventDispatcher != null) KorgeReload_eventDispatcher = null
    }

    //fun <T : Any> getReloadedClass(clazz: KClass<T>): KClass<T> = KorgeReload_getReloadedClass(clazz)
}

data class ReloadEvent(
    val refreshedClasses: Set<String>,
    /** Was able to reload all classes successfully in the existing class loader */
    val reloadSuccess: Boolean
) : Event() {
    val doFullReload: Boolean get() = !reloadSuccess
    fun <T : Any> getReloadedClass(clazz: KClass<T>, injector: AsyncInjector): KClass<T> = KorgeReloadInternal.getReloadedClass(clazz, ReloadClassContext(injector, refreshedClasses))
    fun transferKeepProperties(old: Any, new: Any) = KorgeReloadInternal.transferKeepProperties(old, new)
}

/**
 * Annotate properties in your Scene class that will be persisted upon reload
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepOnReload
