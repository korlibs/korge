package com.soywiz.korge.gradle.targets.native

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

fun KotlinNativeCompilation.getLinkTask(kind: NativeOutputKind, type: NativeBuildType, project: Project) : KotlinNativeLink {
	//linkDebugExecutableMacosX64
	val linkTaskName = "link${type.name.toLowerCase().capitalize()}${kind.name.toLowerCase().capitalize()}${target.name.capitalize()}"
	val tasks = (project.getTasksByName(linkTaskName, true) + project.getTasksByName(linkTaskName, false)).toList()
	//println("this.linkTaskName: ${linkTaskName}")
	//println(" - " + project.tasks.toList().map { it.name })
	//println(" - " + project.getTasksByName(binariesTaskName, true).first().javaClass)
	return (tasks.firstOrNull() as? KotlinNativeLink) ?: error("Can't find $linkTaskName from $tasks from ${project.tasks.map { it.name }}")
	//TODO("compilation.getLinkTask($kind, $type)")
}
