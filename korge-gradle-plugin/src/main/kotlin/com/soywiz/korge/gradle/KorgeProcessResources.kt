package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.jvm.KorgeJavaExec
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File
import java.net.*
import javax.inject.Inject

fun Project.getCompilationKorgeProcessedResourcesFolder(compilation: KotlinCompilation<*>): File {
    return getCompilationKorgeProcessedResourcesFolder(compilation.target.name, compilation.name)
}

fun Project.getCompilationKorgeProcessedResourcesFolder(targetName: String, compilationName: String): File {
    return File(project.buildDir, "korgeProcessedResources/${targetName}/${compilationName}")
}

fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String =
    getKorgeProcessResourcesTaskName(target.name, compilation.name)

fun getKorgeProcessResourcesTaskName(targetName: String, compilationName: String): String =
    "korgeProcessedResources${targetName.capitalize()}${compilationName.capitalize()}"

fun Project.addGenResourcesTasks(): Project {
    val jvmMainClasses by lazy { (tasks["jvmMainClasses"]) }
    val runJvm by lazy { (tasks["runJvm"] as KorgeJavaExec) }

    tasks.create("listKorgeTargets", Task::class.java) {
        it.group = GROUP_KORGE_LIST
        it.doLast {
            println("gkotlin.targets: ${gkotlin.targets.names}")
        }
    }

    tasks.create("listKorgePlugins", Task::class.java) {
        it.group = GROUP_KORGE_LIST
        it.dependsOn("jvmMainClasses")
        it.doLast {
            //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

            executeInPlugin(runJvm.korgeClassPath, "com.soywiz.korge.resources.ResourceProcessorRunner", "printPlugins") { listOf(it) }
        }
    }

    for (target in kotlin.targets) {
        for (compilation in target.compilations) {
            val isJvm = compilation.compileKotlinTask.name == "compileKotlinJvm"
            val processedResourcesFolder = getCompilationKorgeProcessedResourcesFolder(compilation)
            compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)

            //val compilation = project.kotlin.targets.getByName(config.targetName).compilations.getByName(config.compilationName)
            val folders: List<String> = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesFolder }.map { it.toString() }

            val korgeProcessedResources = tasks.create(
                getKorgeProcessResourcesTaskName(target, compilation),
                KorgeProcessedResourcesTask::class.java,
                KorgeProcessedResourcesTaskConfig(
                    isJvm, target.name, compilation.name, runJvm.korgeClassPath,
                    project.korge.getIconBytes(),
                )
            ).also { task ->
                task.group = GROUP_KORGE_RESOURCES
                task.dependsOn("jvmMainClasses")
                task.outputs.dirs(processedResourcesFolder)
                task.folders = folders.map { File(it) }
                task.processedResourcesFolder = processedResourcesFolder
            }
            if (!isJvm) {
                compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
            } else {
                compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                tasks.getByName("runJvm").dependsOn(korgeProcessedResources)
            }
        }
    }
    return this
}

data class KorgeProcessedResourcesTaskConfig(
    val isJvm: Boolean,
    val targetName: String,
    val compilationName: String,
    val korgeClassPath: FileCollection,
    val iconBytes: ByteArray,
)

open class KorgeProcessedResourcesTask @Inject constructor(
    private val config: KorgeProcessedResourcesTaskConfig,
    //private val fs: FileSystemOperations,
) : DefaultTask() {
    @get:OutputDirectory lateinit var processedResourcesFolder: File
    // https://docs.gradle.org/7.4/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
    @get:InputFiles @get:Classpath lateinit var folders: List<File>

    @TaskAction
    fun run() {
        processedResourcesFolder.mkdirs()
        //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

        if (config.isJvm) {
            processedResourcesFolder["@appicon.png"].writeBytes(config.iconBytes)
            //processedResourcesFolder["@appicon-16.png"].writeBytes(korge.getIconBytes(16))
            //processedResourcesFolder["@appicon-32.png"].writeBytes(korge.getIconBytes(32))
            //processedResourcesFolder["@appicon-64.png"].writeBytes(korge.getIconBytes(64))
        }

        executeInPlugin(config.korgeClassPath, "com.soywiz.korge.resources.ResourceProcessorRunner", "run") { classLoader ->
            listOf(classLoader, folders.map { it.toString() }, processedResourcesFolder.toString(), config.compilationName)
        }
    }
}
