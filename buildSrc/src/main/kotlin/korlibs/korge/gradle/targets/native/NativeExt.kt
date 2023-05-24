package korlibs.korge.gradle.targets.native

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*

fun KotlinNativeCompilation.getLinkTask(kind: NativeOutputKind, type: NativeBuildType, project: Project): KotlinNativeLink {
	val taskName = "link${type.name.toLowerCase().capitalize()}${kind.name.toLowerCase().capitalize()}${target.name.capitalize()}"
    val taskName2 = "link${target.name.capitalize()}"

	val tasks = listOf(
        project.getTasksByName(taskName, true),
        project.getTasksByName(taskName, false),
        project.getTasksByName(taskName2, true),
        project.getTasksByName(taskName2, false),
    ).flatMap { it }
	return (tasks.firstOrNull() as? KotlinNativeLink)
        ?: error("Can't find [$taskName or $taskName2] from $tasks from ${project.tasks.map { it.name }}")
}

fun KotlinNativeCompilation.getCompileTask(kind: NativeOutputKind, type: NativeBuildType, project: Project): Task {
	val taskName = "compileKotlin${target.name.capitalize()}"
	val tasks = (project.getTasksByName(taskName, true) + project.getTasksByName(taskName, false)).toList()
	return (tasks.firstOrNull()) ?: error("Can't find $taskName from $tasks from ${project.tasks.map { it.name }}")
}

val KotlinNativeTest.executableFolder get() = executable.parentFile ?: error("Can't get executable folder for KotlinNativeTest")

fun KotlinTarget.configureKotlinNativeTarget(project: Project) {
    // Do nothing for now
}
