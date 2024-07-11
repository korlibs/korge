package korlibs.korge

import korlibs.event.*
import korlibs.inject.*
import korlibs.io.lang.*
import korlibs.korge.internal.*
import korlibs.logger.*
import korlibs.platform.*
import kotlin.jvm.*
import kotlin.reflect.*

class ReloadClassContext(val injector: Injector, val refreshedClasses: Set<String>, val rootFolders: List<String>)

@KorgeInternal
open class KorgeReloadInternalImpl {
    open fun <T : Any> getReloadedClass(clazz: KClass<T>, context: ReloadClassContext): KClass<T> = clazz
    open fun transferKeepProperties(old: Any, new: Any) = Unit
}

internal expect val KorgeReloadInternal: KorgeReloadInternalImpl

object KorgeReload {
    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    private var KorgeReload_eventDispatcher: EventListener? = null

    val logger = Logger("KorgeReload")

    @JvmStatic
    @Suppress("unused") // This is called from [korlibs.korge.reloadagent.KorgeReloadAgent]
    fun triggerReload(classes: List<String>, success: Boolean, rootFolders: List<String>) {
        logger.debug { "KorgeReloadAgent detected a class change. Reload: $classes" }
        KorgeReload_eventDispatcher?.dispatch(ReloadEvent(classes.toSet(), success, rootFolders))
    }

    fun registerEventDispatcher(eventDispatcher: EventListener) {
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
    val reloadSuccess: Boolean,
    val rootFolders: List<String>
) : Event(), TEvent<ReloadEvent> {
    override val type: EventType<ReloadEvent> = ReloadEvent
    companion object : EventType<ReloadEvent>

    val doFullReload: Boolean get() = !reloadSuccess
    fun <T : Any> getReloadedClass(clazz: KClass<T>, injector: Injector): KClass<T> = KorgeReloadInternal.getReloadedClass(clazz, ReloadClassContext(injector, refreshedClasses, rootFolders))
    fun transferKeepProperties(old: Any, new: Any) = KorgeReloadInternal.transferKeepProperties(old, new)
}

/**
 * Annotate properties in your Scene class that will be persisted upon reload
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepOnReload
