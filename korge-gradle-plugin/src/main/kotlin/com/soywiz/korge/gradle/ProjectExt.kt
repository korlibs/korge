package com.soywiz.korge.gradle

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*

fun Project.hasKotlinTarget(name: String) = kotlin.targets.findByName(name) != null
fun Project.korge(callback: KorgeExtension.() -> Unit) = korge.apply(callback)
val Project.kotlin: KotlinMultiplatformExtension get() = this.extensions.getByType(KotlinMultiplatformExtension::class.java)
val Project.korge: KorgeExtension
    get() {
        val extension = project.extensions.findByName("korge") as? KorgeExtension?
        return if (extension == null) {
            val newExtension = KorgeExtension(this)
            project.extensions.add("korge", newExtension)
            newExtension
        } else {
            extension
        }
    }

