package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.jvm.KorgeJavaExec
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File
import java.net.*

fun Project.getCompilationKorgeProcessedResourcesFolder(compilation: KotlinCompilation<*>): File {
    return File(project.buildDir, "korgeProcessedResources/${compilation.target.name}/${compilation.name}")
}

fun Project.addGenResourcesTasks() = this {
    tasks.apply {
        val jvmMainClasses by lazy { (tasks["jvmMainClasses"]) }
        val runJvm by lazy { (tasks["runJvm"] as KorgeJavaExec) }

        create("listKorgePlugins", Task::class.java) {
            it.dependsOn(jvmMainClasses)
            it.doLast {
                //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

                executeInPlugin(runJvm.korgeClassPath, "com.soywiz.korge.resources.ResourceProcessorRunner", "printPlugins") { listOf(it) }
            }
        }

        for (target in kotlin.targets) {
            for (compilation in target.compilations) {
                val processedResourcesFolder = getCompilationKorgeProcessedResourcesFolder(compilation)
                compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)
                val korgeProcessedResources = create(getKorgeProcessResourcesTaskName(target, compilation)) {
                    //dependsOn(prepareResourceProcessingClasses)
                    it.dependsOn(jvmMainClasses)

                    it.doLast {
                        processedResourcesFolder.mkdirs()
                        //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

                        executeInPlugin(runJvm.korgeClassPath, "com.soywiz.korge.resources.ResourceProcessorRunner", "run") { classLoader ->
                            val folders = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesFolder }.map { it.toString() }
                            listOf(classLoader, folders, processedResourcesFolder.toString(), compilation.name)
                        }
                    }
                }
                if (compilation.compileKotlinTask.name != "compileKotlinJvm") {
                    compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
                } else {
                    compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                    getByName("runJvm").dependsOn(korgeProcessedResources)

                }
            }
        }
    }
}

fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String =
    "korgeProcessedResources${target.name.capitalize()}${compilation.name.capitalize()}"
