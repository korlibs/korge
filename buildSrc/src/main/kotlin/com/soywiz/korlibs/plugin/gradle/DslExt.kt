package com.soywiz.korlibs.plugin.gradle

val org.gradle.api.Project.`kotlin`: org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension get() =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("kotlin") as org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
