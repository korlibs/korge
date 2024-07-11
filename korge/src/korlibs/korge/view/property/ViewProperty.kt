package korlibs.korge.view.property

import korlibs.io.async.*
import korlibs.io.experimental.*
import kotlin.reflect.*

/**
 * Used by the debugger to make a property to appear in the debug panel.
 */
@Suppress("unused")
@SuppressIfAnnotated
annotation class ViewProperty(
    val min: Double = 0.0,
    val max: Double = 2000.0,
    val clampMin: Boolean = false,
    val clampMax: Boolean = false,
    val decimalPlaces: Int = 2,
    val groupName: String = "",
    val order: Int = 0,
    val name: String = "",
    val editable: Boolean = true,
)

annotation class ViewPropertyProvider(val provider: KClass<out ViewPropertyProvider.Impl<*, *>>) {
    interface Impl<T, R> {
        fun provider(instance: T): Map<String, R>
    }
    abstract class ListImpl<T, R> : Impl<T, R> {
        final override fun provider(instance: T): Map<String, R> = listProvider(instance).associateBy { it.toString() }
        abstract fun listProvider(instance: T): List<R>
    }
    abstract class ItemsImpl<R> : Impl<Any?, R> {
        abstract val ITEMS: List<R>
        final override fun provider(instance: Any?): Map<String, R> = ITEMS.associateBy { it.toString() }
    }
    abstract class ItemsMapImpl<R> : Impl<Any?, R> {
        abstract val ITEMS: Map<String, R>
        final override fun provider(instance: Any?): Map<String, R> = ITEMS
    }
}
annotation class ViewPropertyFileRef(val extensions: Array<String>)
annotation class ViewPropertySubTree()
