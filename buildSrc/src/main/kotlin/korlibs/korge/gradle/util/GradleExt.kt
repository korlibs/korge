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