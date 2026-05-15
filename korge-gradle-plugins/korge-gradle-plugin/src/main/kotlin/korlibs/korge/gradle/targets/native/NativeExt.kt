package korlibs.korge.gradle.targets.native

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.util.Locale
import java.util.Locale.getDefault

fun KotlinNativeCompilation.getLinkTask(kind: NativeOutputKind, type: NativeBuildType, project: Project): KotlinNativeLink {
	val targetName = target.name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
    }
    val taskName = "link${
        type.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
    }${
        kind.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
    }$targetName"
    val taskName2 = "link$targetName"

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
	val taskName = "compileKotlin${
        target.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                getDefault()
            ) else it.toString()
        }
    }"
	val tasks = (project.getTasksByName(taskName, true) + project.getTasksByName(taskName, false)).toList()
	return (tasks.firstOrNull()) ?: error("Can't find $taskName from $tasks from ${project.tasks.map { it.name }}")
}

val KotlinNativeTest.executableFolder get() = executable.parentFile ?: error("Can't get executable folder for KotlinNativeTest")

fun KotlinTarget.configureKotlinNativeTarget(project: Project) {
    // Do nothing for now
}
