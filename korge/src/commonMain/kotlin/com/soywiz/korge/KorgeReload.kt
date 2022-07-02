package com.soywiz.korge

import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import com.soywiz.korinject.AsyncInjector
import kotlin.jvm.JvmStatic
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

@ThreadLocal
private var KorgeReload_eventDispatcher: EventDispatcher? = null

class ReloadClassContext(val injector: AsyncInjector, val refreshedClasses: Set<String>)

internal expect fun <T : Any> KorgeReload_getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T>

object KorgeReload {
    @JvmStatic
    @Suppress("unused") // This is called from [com.soywiz.korge.reloadagent.KorgeReloadAgent]
    fun triggerReload(classes: List<String>, success: Boolean) {
        println("KorgeReloadAgent detected a class change. Reload: $classes")
        KorgeReload_eventDispatcher?.dispatch(ReloadEvent::class, ReloadEvent(classes.toSet(), success))
    }

    fun registerEventDispatcher(eventDispatcher: EventDispatcher) {
        KorgeReload_eventDispatcher = eventDispatcher
    }

    //fun <T : Any> getReloadedClass(clazz: KClass<T>): KClass<T> = KorgeReload_getReloadedClass(clazz)
}

data class ReloadEvent(
    val refreshedClasses: Set<String>,
    /** Was able to reload all classes successfully in the existing class loader */
    val reloadSuccess: Boolean
) : Event() {
    fun <T : Any> getReloadedClass(clazz: KClass<T>, injector: AsyncInjector): KClass<T> = KorgeReload_getReloadedClass(clazz, ReloadClassContext(injector, refreshedClasses))
}
