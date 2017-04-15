package com.soywiz.korge.gradle

import com.soywiz.korge.Korge
import com.soywiz.korge.build.ResourceProcessor
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.toVfs
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.dependencies.add("compile", "com.soywiz:korge-core:${Korge.VERSION}")


		project.addTask<KorgeResourcesTask>("genResources", group = "korge", description = "process resources", overwrite = true, dependsOn = listOf("build")) {
			it.debug = true
		}
	}
}

open class KorgeResourcesTask() : DefaultTask() {
	var debug = false

	@Suppress("unused")
	@TaskAction open fun task() {
		logger.info("KorgeResourcesTask")
		val processorsByExtension = ServiceLoader.load(com.soywiz.korge.build.ResourceProcessor::class.java).toList().flatMap { processor -> processor.extensionLCs.map { it to processor } }.toMap()
		for (processor in processorsByExtension.values) {
			logger.info("${processor.extensionLCs} -> $processor")
		}
		for (p in project.allprojects) {
			val folder = File(p.buildFile.parentFile, "build/resources/main")
			logger.info("KorgeResourcesTask! project: $p : $folder")
			syncTest {
				logger.info("<Sync>")
				val folderVfs = folder.toVfs()
				for (file in folderVfs.listRecursive()) {
					val processor = processorsByExtension[file.extensionLC] ?: continue
					try {
						processor.process(file, file.parent)
						//logger.info(file.toString())
					} catch (e: Throwable) {
						e.printStackTrace()
					}
				}
				logger.info("</Sync>")
			}
		}
	}
}

open class LambdaClosure<T, TR>(val lambda: (value: T) -> TR) : Closure<T>(Unit) {
	@Suppress("unused")
	fun doCall(vararg arguments: T) = lambda(arguments[0])

	override fun getProperty(property: String): Any = "lambda"
}

inline fun <reified T : AbstractTask> Project.addTask(name: String, group: String = "", description: String = "", overwrite: Boolean = true, dependsOn: List<String> = listOf(), noinline configure: (T) -> Unit = {}): Task {
	return project.task(mapOf(
		"type" to T::class.java,
		"group" to group,
		"description" to description,
		"overwrite" to overwrite
	), name, LambdaClosure({ it: T ->
		configure(it)
	})).dependsOn(dependsOn)
}
