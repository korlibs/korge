package com.soywiz.korge.gradle.util

import org.gradle.api.*
import org.gradle.api.plugins.ExtraPropertiesExtension
import kotlin.reflect.*

private val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)

class projectExtension<T : Any>(val overrideName: String? = null, val gen: Project.() -> T) {
    val KProperty<*>.extensionName get() = "extension.${overrideName ?: name}"

    operator fun setValue(project: Project, property: KProperty<*>, value: T) {
        project.ext.set(property.extensionName, value)
    }

    operator fun getValue(project: Project, property: KProperty<*>): T {
        if (!project.ext.has(property.extensionName)) {
            project.ext.set(property.extensionName, gen(project))
        }
        return project.ext.get(property.extensionName) as T
    }
}
