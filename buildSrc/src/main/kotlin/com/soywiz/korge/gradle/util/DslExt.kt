package com.soywiz.korge.gradle.util

import kotlinx.kover.api.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import kotlin.reflect.*

fun org.gradle.api.Project.`koverMerged`(configure: Action<KoverMergedConfig>): Unit = (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("koverMerged", configure)
fun org.gradle.api.Project.`kover`(configure: Action<kotlinx.kover.api.KoverProjectConfig>): Unit = (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kover", configure)
//fun TaskContainer.`dokkaHtml`(configure: org.jetbrains.dokka.gradle.DokkaTask.() -> Unit) {
//    configure(named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").get())
//}

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
