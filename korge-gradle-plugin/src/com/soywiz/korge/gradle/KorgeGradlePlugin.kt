package com.soywiz.korge.gradle

import com.jtransc.error.ignoreErrors
import com.jtransc.gradle.get
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
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.util.*

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.dependencies.add("compile", "com.soywiz:korge-core:${Korge.VERSION}")


		project.addTask<KorgeResourcesTask>(
			"genResources", group = "korge", description = "process resources",
			//overwrite = true, dependsOn = listOf("build")
			overwrite = true, dependsOn = listOf()
		) {
			it.debug = true
		}
		project.tasks["processResources"].dependsOn("genResources")

		project.addTask<KorgeTestResourcesTask>(
			"genTestResources", group = "korge", description = "process test resources",
			//overwrite = true, dependsOn = listOf("build")
			overwrite = true, dependsOn = listOf()
		) {
			it.debug = true
		}
		project.tasks["processTestResources"].dependsOn("genTestResources")
	}
}

abstract class KorgeBaseResourcesTask() : DefaultTask() {
	var debug = false

	suspend fun compile(inputFiles: List<File>, output: File) {
		logger.info("Processing: $inputFiles to $output")
		ignoreErrors { output.mkdirs() }
		val outputVfs = output.toVfs().jail()
		for (inputFile in inputFiles) {
			val inputVfs = inputFile.toVfs().jail()
			for (file in inputVfs.listRecursive()) {
				val processor = processorsByExtension[file.extensionLC] ?: continue
				val fileInput = file
				val folderOutput = outputVfs[file.path].parent
				ignoreErrors { folderOutput.ensureParents() }
				ignoreErrors { folderOutput.mkdir() }
				logger.info("Processing: $fileInput with $processor with output to $folderOutput")
				try {
					processor.process(fileInput, folderOutput)
					//logger.info(file.toString())
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}
		}
	}

	class GeneratePair() {
		val input = arrayListOf<File>()
		val output = arrayListOf<File>()

		val available: Boolean get() = input.isNotEmpty() && output.isNotEmpty()
	}

	abstract var inputSourceSet: String
	abstract var generatedSourceSet: String
	abstract var processResources: String


	val processorsByExtension: Map<String, ResourceProcessor> by lazy {
		try {
			ServiceLoader.load(ResourceProcessor::class.java).toList().flatMap { processor -> processor.extensionLCs.map { it to processor } }.toMap()
		} catch (e: Throwable) {
			e.printStackTrace()
			mapOf<String, ResourceProcessor>()
		}
	}

	@Suppress("unused")
	@TaskAction open fun task() {
		logger.info("KorgeResourcesTask ($this)")
		for (processor in processorsByExtension.values) {
			logger.info("${processor.extensionLCs} -> $processor")
		}
		for (p in project.allprojects) {
			val sourceSets = project.property("sourceSets") as SourceSetContainer
			val folder = File(p.buildFile.parentFile, "build/resources/main")
			logger.info("KorgeResourcesTask! project: $p : $folder")
			val availableSourceSets = sourceSets.map { it.name }.toSet()
			logger.info("sourceSets:" + sourceSets.map { it.name })

			val pair = GeneratePair()

			for (sourceSet in availableSourceSets) {
				val resources = sourceSets[sourceSet].resources
				logger.info("$sourceSet.resources: ${resources.srcDirs}")
			}

			if (inputSourceSet in availableSourceSets) {
				pair.input += sourceSets[inputSourceSet].resources.srcDirs
			}
			if (generatedSourceSet in availableSourceSets) {
				pair.output += sourceSets[generatedSourceSet].resources.srcDirs
				val processResources = project.property(processResources) as ProcessResources
				for (item in pair.output) {
					processResources.from(item)
				}
			}
			if (!pair.available) {
				logger.info("No $inputSourceSet.resources.srcDirs + $generatedSourceSet.resources.srcDirs")
			} else {
				syncTest {
					compile(pair.input, pair.output.first())
				}
			}
		}
	}
}

open class KorgeTestResourcesTask() : KorgeBaseResourcesTask() {
	override var inputSourceSet = "test"
	override var generatedSourceSet = "testGenerated"
	override var processResources = "processTestResources"
}

open class KorgeResourcesTask() : KorgeBaseResourcesTask() {
	override var inputSourceSet = "main"
	override var generatedSourceSet = "generated"
	override var processResources = "processResources"
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
