package korlibs.korge.gradle.targets.native

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*

fun KotlinNativeCompilation.getLinkTask(kind: NativeOutputKind, type: NativeBuildType, project: Project): KotlinNativeLink {
	val taskName = "link${type.name.toLowerCase().capitalize()}${kind.name.toLowerCase().capitalize()}${target.name.capitalize()}"
	val tasks = (project.getTasksByName(taskName, true) + project.getTasksByName(taskName, false)).toList()
	return (tasks.firstOrNull() as? KotlinNativeLink) ?: error("Can't find $taskName from $tasks from ${project.tasks.map { it.name }}")
}

fun KotlinNativeCompilation.getCompileTask(kind: NativeOutputKind, type: NativeBuildType, project: Project): Task {
	val taskName = "compileKotlin${target.name.capitalize()}"
	val tasks = (project.getTasksByName(taskName, true) + project.getTasksByName(taskName, false)).toList()
	return (tasks.firstOrNull()) ?: error("Can't find $taskName from $tasks from ${project.tasks.map { it.name }}")
}

val KotlinNativeTest.executableFolder get() = executable.parentFile ?: error("Can't get executable folder for KotlinNativeTest")

fun KotlinTarget.configureKotlinNativeTarget(project: Project) {
    val overridenKonanProperties = mapOf(
        // From: https://github.com/JetBrains/kotlin/blob/1.8.20/kotlin-native/konan/konan.properties#L969
        //"clangFlags.mingw_x86" to "-cc1 -emit-obj -disable-llvm-passes -x ir -femulated-tls",
        "clangFlags.mingw_x86" to "-cc1 -emit-obj -disable-llvm-passes -x ir",
        "clangOptFlags.mingw_x86" to "-O3 -ffunction-sections",
    )

    compilations.all {
        it.kotlinOptions.freeCompilerArgs += listOf("-Xoverride-konan-properties=${overridenKonanProperties.map { "${it.key}=${it.value}" }.joinToString(";")}")
    }
}
