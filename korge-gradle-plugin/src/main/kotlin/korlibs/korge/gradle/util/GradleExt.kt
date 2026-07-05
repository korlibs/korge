package korlibs.korge.gradle.util

import org.gradle.api.*
import org.gradle.api.plugins.PluginContainer
import kotlin.reflect.*

fun PluginContainer.applyOnce(id: String) {
    if (!hasPlugin(id)) {
        apply(id)
    }
}

fun <T : Plugin<*>> PluginContainer.applyOnce(clazz: Class<T>) {
    if (!hasPlugin(clazz)) {
        apply(clazz)
    }
}

inline fun <reified T : Plugin<*>> PluginContainer.applyOnce() {
    applyOnce(T::class.java)
}

fun Project.ordered(vararg dependencyPaths: String): List<Task> {
    val dependencies = dependencyPaths.map { tasks.getByPath(it) }
    for (n in 0 until dependencies.size - 1) {
        dependencies[n + 1].mustRunAfter(dependencies[n])
    }
    return dependencies
}

