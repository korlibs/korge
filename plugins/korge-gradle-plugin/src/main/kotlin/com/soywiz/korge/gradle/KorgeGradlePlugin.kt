package com.soywiz.korge.gradle

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.internal.*
import org.gradle.api.internal.artifacts.configurations.*
import org.gradle.api.internal.file.collections.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*
import java.net.*

val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)
val korgeVersion get() = KorgeBuildServiceProxy.version()

open class KorgeGradlePlugin : Plugin<Project> {

	override fun apply(project: Project) {
		System.setProperty("java.awt.headless", "true")

		KorgeBuildServiceProxy.init()
		project.addVersionExtension()
		project.configureRepositories()
		project.configureKotlin()
		project.addKorgeTasks()

		//for (res in project.getResourcesFolders()) println("- $res")
	}

	private fun Project.addVersionExtension() {
		ext.set("korgeVersion", korgeVersion)
		ext.set("kotlinVersion", "1.3.11")
		//ext.set("kotlinVersion", KotlinVersion.CURRENT.toString())
	}

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

		gkotlin.targets.add((gkotlin.presets.getAt("jvm") as KotlinJvmTargetPreset).createTarget("jvm"))
		gkotlin.targets.add((gkotlin.presets.getAt("js") as KotlinJsTargetPreset).createTarget("js").apply {
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

		gkotlin.sourceSets.maybeCreate("commonMain").dependencies {
			api("com.soywiz:korge:$korgeVersion")
		}

		//kotlin.sourceSets.create("")

	}

	fun Project.addKorgeTasks() {
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

		project.ext.set("mainClassName", "")
		project.addTask<org.gradle.jvm.tasks.Jar>(
				"packageJvmFatJar", group = "korge"
		) {
			it.manifest { manifest ->
				manifest.attributes(mapOf(
						"Implementation-Title" to project.ext.get("mainClassName"),
						"Implementation-Version" to project.version.toString(),
						"Main-Class" to project.ext.get("mainClassName")
				))
			}
			it.baseName = "${project.name}-all"
			//it.from()
			//fileTree()
			val config = (GroovyDynamic { project["kotlin"]["targets"]["jvm"]["compilations"]["main"]["runtimeDependencyFiles"] } as DefaultConfiguration)
			it.from(GroovyClosure(project) {
				config.map { if (it.isDirectory) it else project.zipTree(it) as Any }
				//listOf<File>()
			})
			it.with(project.getTasksByName("jvmJar", true).first() as CopySpec)
		}
	}
}

abstract class KorgeBaseResourcesTask : DefaultTask() {
	var debug = false

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
		for (p in project.allprojects) {
			for (resourceFolder in p.getResourcesFolders(setOf(inputSourceSet))) {
				if (resourceFolder.exists()) {
					val output = resourceFolder.parentFile["genresources"]
					KorgeBuildServiceProxy.processResourcesFolder(resourceFolder, output)
				}
			}
		}
	}
}

operator fun File.get(name: String) = File(this, name)

open class KorgeTestResourcesTask : KorgeBaseResourcesTask() {
	override var inputSourceSet = "test"
	override var generatedSourceSet = "testGenerated"
	override var processResources = "processTestResources"
}

open class KorgeResourcesTask : KorgeBaseResourcesTask() {
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

fun Project.getResourcesFolders(sourceSets: Set<String>? = null): List<File> {
	val out = arrayListOf<File>()
	try {
		for (target in gkotlin.targets.toList()) {
			//println("TARGET: $target")
			for (compilation in target.compilations) {
				//println("  - COMPILATION: $compilation :: name=${compilation.name}")
				if (sourceSets != null && compilation.name !in sourceSets) continue
				for (sourceSet in compilation.allKotlinSourceSets) {
					//println("    - SOURCE_SET: $sourceSet")
					for (resource in sourceSet.resources.srcDirs) {
						out += resource
						//println("        - RESOURCE: $resource")
					}
				}
			}
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	}
	return out.distinct()
}
