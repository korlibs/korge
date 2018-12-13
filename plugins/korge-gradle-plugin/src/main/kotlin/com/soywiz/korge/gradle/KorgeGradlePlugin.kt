package com.soywiz.korge.gradle

import com.soywiz.korau.format.*
import com.soywiz.korge.*
import com.soywiz.korge.build.*
import com.soywiz.korge.build.atlas.*
import com.soywiz.korge.build.lipsync.*
import com.soywiz.korge.build.swf.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.util.*
import groovy.lang.*
import kotlinx.coroutines.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.internal.*
import org.gradle.api.tasks.*
import org.gradle.language.jvm.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*
import java.net.*

open class KorgeGradlePlugin : Plugin<Project> {
	val Project.kotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
	val korgeVersion get() = Korge.VERSION

	private fun Project.configureRepositories() {
		repositories.apply {
			mavenLocal()
			maven { it.url = URI("https://dl.bintray.com/soywiz/soywiz") }
			jcenter()
			mavenCentral()
		}
	}

	private fun Project.configureKotlin() {
		plugins.apply("kotlin-multiplatform")

		kotlin.targets.add((kotlin.presets.getAt("jvm") as KotlinJvmTargetPreset).createTarget("jvm"))
		kotlin.targets.add((kotlin.presets.getAt("js") as KotlinJsTargetPreset).createTarget("js").apply {
			compilations.getAt("main").apply {
				//this.apiConfigurationName
				//compileKotlinTaskName
			}
		})

		project.dependencies.add("commonMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-common")
		project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-annotations-common")
		project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-common")

		//println("com.soywiz:korge:$korgeVersion")
		//project.dependencies.add("commonMainImplementation", "com.soywiz:korge:$korgeVersion")

		kotlin.sourceSets.maybeCreate("commonMain").dependencies {
			api("com.soywiz:korge:$korgeVersion")
		}

		//kotlin.sourceSets.create("")
	}

	override fun apply(project: Project) {
		System.setProperty("java.awt.headless", "true")

		project.configureRepositories()
		project.configureKotlin()

		defaultResourceProcessors.register(AtlasResourceProcessor, SwfResourceProcessor)
		try {
			defaultResourceProcessors.register(LipsyncResourceProcessor)
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		Mapper.jvmFallback()
		defaultAudioFormats.registerStandard().registerMp3Decoder().registerOggVorbisDecoder()

		//return

		try {
			project.dependencies.add("compile", "com.soywiz:korge:$korgeVersion")
		} catch (e: UnknownConfigurationException) {
			//logger.error("KORGE: " + e.message)
		}

		project.addTask<KorgeResourcesTask>(
			"genResources", group = "korge", description = "process resources",
			//overwrite = true, dependsOn = listOf("build")
			overwrite = true, dependsOn = listOf()
		) {
			it.debug = true
		}
		try {
			project.tasks["processResources"].dependsOn("genResources")
		} catch (e: UnknownTaskException) {
		}

		project.addTask<KorgeTestResourcesTask>(
			"genTestResources", group = "korge", description = "process test resources",
			//overwrite = true, dependsOn = listOf("build")
			overwrite = true, dependsOn = listOf()
		) {
			it.debug = true
		}
		try {
			project.tasks["processTestResources"].dependsOn("genTestResources")
		} catch (e: UnknownTaskException) {
		}
	}
}

abstract class KorgeBaseResourcesTask : DefaultTask() {
	var debug = false

	suspend fun compile(inputFiles: List<File>, output: File) {
		ignoreErrors { output.mkdirs() }
		ResourceProcessor.process(inputFiles.map { it.toVfs() }, output.toVfs())
	}

	class GeneratePair {
		val input = ArrayList<File>()
		val output = ArrayList<File>()

		val available: Boolean get() = input.isNotEmpty() && output.isNotEmpty()
	}

	abstract var inputSourceSet: String
	abstract var generatedSourceSet: String
	abstract var processResources: String

	@Suppress("unused")
	@TaskAction
	open fun task() {
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
				runBlocking {
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

inline fun <reified T : AbstractTask> Project.addTask(
	name: String,
	group: String = "",
	description: String = "",
	overwrite: Boolean = true,
	dependsOn: List<String> = listOf(),
	noinline configure: (T) -> Unit = {}
): Task {
	return project.task(
		mapOf(
			"type" to T::class.java,
			"group" to group,
			"description" to description,
			"overwrite" to overwrite
		), name, LambdaClosure({ it: T ->
		configure(it)
	})
	).dependsOn(dependsOn)
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
