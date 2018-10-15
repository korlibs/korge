package com.soywiz.korge.gradle

import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korau.format.registerMp3Decoder
import com.soywiz.korau.format.registerOggVorbisDecoder
import com.soywiz.korau.format.registerStandard
import com.soywiz.korge.Korge
import com.soywiz.korge.build.ResourceProcessor
import com.soywiz.korge.build.atlas.AtlasResourceProcessor
import com.soywiz.korge.build.defaultResourceProcessors
import com.soywiz.korge.build.lipsync.LipsyncResourceProcessor
import com.soywiz.korge.build.swf.SwfResourceProcessor
import com.soywiz.korim.format.defaultImageFormats
import com.soywiz.korim.format.registerStandard
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.serialization.Mapper
import com.soywiz.korio.util.jvmFallback
import com.soywiz.korio.vfs.toVfs
import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		//JTranscGradlePlugin().apply(project)

		System.setProperty("java.awt.headless", "true")
		defaultResourceProcessors.register(AtlasResourceProcessor, SwfResourceProcessor, LipsyncResourceProcessor)
		defaultImageFormats.registerStandard()
		Mapper.jvmFallback()
		defaultAudioFormats.registerStandard().registerMp3Decoder().registerOggVorbisDecoder()

		project.dependencies.add("compile", "com.soywiz:korge-common:${Korge.VERSION}")

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

abstract class KorgeBaseResourcesTask : DefaultTask() {
	var debug = false

	suspend fun compile(inputFiles: List<File>, output: File) {
		ignoreErrors { output.mkdirs() }
		ResourceProcessor.process(inputFiles.map { it.toVfs() }, output.toVfs())
	}

	class GeneratePair() {
		val input = ArrayList<File>()
		val output = ArrayList<File>()

		val available: Boolean get() = input.isNotEmpty() && output.isNotEmpty()
	}

	abstract var inputSourceSet: String
	abstract var generatedSourceSet: String
	abstract var processResources: String

	@Suppress("unused")
	@TaskAction open fun task() {
		logger.info("KorgeResourcesTask ($this)")
		for (processor in ResourceProcessor.processorsByExtension.values) {
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

inline fun ignoreErrors(action: () -> Unit) {
	try {
		action()
	} catch (e: Throwable) {
		e.printStackTrace()
	}
}

fun <T> Project.getIfExists(name: String): T? = if (this.hasProperty(name)) this.property(name) as T else null

operator fun <T> NamedDomainObjectSet<T>.get(key: String): T = this.getByName(key)
