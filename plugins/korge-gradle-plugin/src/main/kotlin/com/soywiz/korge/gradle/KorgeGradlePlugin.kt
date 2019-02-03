package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.android.*
import com.soywiz.korge.gradle.targets.cordova.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.targets.ios.*
import com.soywiz.korge.gradle.targets.isMacos
import com.soywiz.korge.gradle.targets.js.*
import com.soywiz.korge.gradle.targets.jvm.*
import com.soywiz.korge.gradle.util.*
import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.plugins.ide.idea.model.*
import org.gradle.process.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.*

val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.korge(callback: KorgeExtension.() -> Unit) = korge.apply(callback)
val Project.kotlin: KotlinMultiplatformExtension get() = this.extensions.getByType(KotlinMultiplatformExtension::class.java)
val Project.korge: KorgeExtension
	get() {
		val extension = project.extensions.findByName("korge") as? KorgeExtension?
		return if (extension == null) {
			val newExtension = KorgeExtension(this)
			project.extensions.add("korge", newExtension)
			newExtension
		} else {
			extension
		}
	}

open class JsWebCopy() : Copy() {
	open lateinit var targetDir: File
}

val Project.korgeCacheDir by lazy { File(System.getProperty("user.home"), ".korge").apply { mkdirs() } }
//val node_modules by lazy { project.file("node_modules") }
val Project.korgeGroup get() = "korge"

class KorgeGradleApply(val project: Project) {
	fun apply() {
		System.setProperty("java.awt.headless", "true")

		val expectedGradleVersion = "5.1.1"
		val korgeCheckGradleVersion = (project.ext.properties["korgeCheckGradleVersion"] as? Boolean) ?: true

		if (korgeCheckGradleVersion && project.gradle.gradleVersion != expectedGradleVersion) {
			error("Korge only works with Gradle $expectedGradleVersion, but running on Gradle ${project.gradle.gradleVersion}")
		}

		//KorgeBuildServiceProxy.init()
		project.addVersionExtension()
		project.configureRepositories()
		project.configureKotlin()
		project.addKorgeTasks()
		project.configureIdea()

		project.configureJvm()
		project.configureJavaScript()
		project.configureNativeDesktop()
		project.configureNativeAndroid()
		if (isMacos) {
			project.configureNativeIos()
		}
		project.configureCordova()

		project.korge.init()
	}

	private fun Project.configureIdea() {
		project.plugins.apply("idea")
		(project["idea"] as IdeaModel).apply {
			module { module ->
				for (file in listOf(
					".gradle", "node_modules", "classes", "docs", "dependency-cache",
					"libs", "reports", "resources", "test-results", "tmp"
				)) {
					module.excludeDirs.add(file(".gradle"))
				}
			}
		}
	}

	private fun Project.addVersionExtension() {
		ext.set("korioVersion", korioVersion)
		ext.set("kormaVersion", kormaVersion)
		ext.set("korauVersion", korauVersion)
		ext.set("korimVersion", korimVersion)
		ext.set("koruiVersion", koruiVersion)
		ext.set("korevVersion", korevVersion)
		ext.set("korgwVersion", korgwVersion)
		ext.set("korgeVersion", korgeVersion)
		ext.set("kotlinVersion", kotlinVersion)
		ext.set("coroutinesVersion", coroutinesVersion)
		//ext.set("kotlinVersion", KotlinVersion.CURRENT.toString())
	}

	private fun Project.configureKotlin() {
		plugins.apply("kotlin-multiplatform")

		project.dependencies.add("commonMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-common")
		project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-annotations-common")
		project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-common")


		//println("com.soywiz:korge:$korgeVersion")
		//project.dependencies.add("commonMainImplementation", "com.soywiz:korge:$korgeVersion")

		gkotlin.sourceSets.maybeCreate("commonMain").dependencies {
            api("com.soywiz:klock:${BuildVersions.KLOCK}")
            api("com.soywiz:kmem:${BuildVersions.KMEM}")
            api("com.soywiz:kds:${BuildVersions.KDS}")
            api("com.soywiz:korma:${BuildVersions.KORMA}")
            api("com.soywiz:korio:${BuildVersions.KORIO}")
            api("com.soywiz:korim:${BuildVersions.KORIM}")
            api("com.soywiz:korau:${BuildVersions.KORAU}")
			api("com.soywiz:kgl:${BuildVersions.KGL}")
			api("com.soywiz:korag:${BuildVersions.KORAG}")
			api("com.soywiz:korag-opengl:${BuildVersions.KORAG_OPENGL}")
			api("com.soywiz:korgw:${BuildVersions.KORGW}")
			api("com.soywiz:korge:${BuildVersions.KORGE}")
            api("com.soywiz:korev:${BuildVersions.KOREV}")
		}

		//kotlin.sourceSets.create("")

	}

	fun Project.addKorgeTasks() {
		run {
			try {
				project.dependencies.add("compile", "com.soywiz:korge:${BuildVersions.KORGE}")
			} catch (e: UnknownConfigurationException) {
				//logger.error("KORGE: " + e.message)
			}
		}

		run {
			project.addTask<KorgeResourcesTask>(
				"genResources", group = korgeGroup, description = "process resources",
				//overwrite = true, dependsOn = listOf("build")
				overwrite = true, dependsOn = listOf()
			) {
				it.debug = true
			}
			try {
				project.tasks["processResources"].dependsOn("genResources")
			} catch (e: UnknownTaskException) {
			}
		}

		run {
			project.addTask<KorgeTestResourcesTask>(
				"genTestResources", group = korgeGroup, description = "process test resources",
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

		//val korge = KorgeXml(project.file("korge.project.xml"))
		val korge = project.korge

		run {
			val runJvm = project.addTask<JavaExec>("runJvm", group = korgeGroup) { task ->
				afterEvaluate {
					task.classpath =
						project["kotlin"]["targets"]["jvm"]["compilations"]["test"]["runtimeDependencyFiles"] as? FileCollection?
					task.main = korge.jvmMainClassName
				}
			}
		}
	}
}

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		KorgeGradleApply(project).apply()

		//for (res in project.getResourcesFolders()) println("- $res")
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
		val buildService = KorgeBuildService
		for (p in project.allprojects) {
			for (resourceFolder in p.getResourcesFolders(setOf(inputSourceSet))) {
				if (resourceFolder.exists()) {
					val output = resourceFolder.parentFile["genresources"]
					buildService.processResourcesFolder(resourceFolder, output)
				}
			}
		}
	}
}

object KorgeBuildService {
	fun processResourcesFolder(resourceFolder: File, output: File) {
		//KorgeBuildService.processResourcesFolder(resourceFolder, output)
	}
}

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
