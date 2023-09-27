package korlibs.korge.gradle.util

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import kotlin.reflect.*

fun <T> ExtensionContainer.getByName(name: String): T = getByName(name) as T

inline fun <reified T : Task> TaskContainer.createThis(name: String, vararg params: Any, block: T.() -> Unit = {}): T {
    return create(name, T::class.java, *params).apply(block)
}


/**
 * Retrieves the [ext][org.gradle.api.plugins.ExtraPropertiesExtension] extension.
 */
val org.gradle.api.Project._ext: org.gradle.api.plugins.ExtraPropertiesExtension get() =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("ext") as org.gradle.api.plugins.ExtraPropertiesExtension

class LazyExt<T>(val root: Boolean = true, val lazyBlock: Project.() -> T) {
    operator fun getValue(project: Project, property: KProperty<*>): T {
        val key = "_ext_${property.name}"
        if (!project._ext.has(key)) {
            project._ext.set(key, lazyBlock(if (root) project.rootProject else project))
        }
        return project._ext.get(key) as T
    }
}
