package com.soywiz.korge.gradle.targets.native

import com.soywiz.korge.gradle.*
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
    // mimalloc is broken on raspberry pi
    if (project.korge.useMimalloc && !targetName.contains("Arm32Hfp")) {
        compilations.all {
            it.kotlinOptions.freeCompilerArgs = it.kotlinOptions.freeCompilerArgs + listOf(
                "-Xallocator=mimalloc",
                // https://kotlinlang.slack.com/archives/C3SGXARS6/p1620909233323100?thread_ts=1619349974.244300&cid=C3SGXARS6
                // https://github.com/JetBrains/kotlin/blob/ec6c25ef7ee3e9d89bf9a03c01e4dd91789000f5/kotlin-native/konan/konan.properties#L875
                //"-Xoverride-konan-properties=clangFlags.mingw_x64=-cc1 -emit-obj -disable-llvm-passes -x ir -femulated-tls -target-cpu x86-64"
                "-Xoverride-konan-properties=clangFlags.mingw_x64=-cc1 -emit-obj -disable-llvm-passes -x ir -target-cpu x86-64"
            )
        }
    }
}
