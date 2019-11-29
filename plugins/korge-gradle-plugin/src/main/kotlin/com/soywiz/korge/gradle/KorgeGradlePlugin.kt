package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.android.*
import com.soywiz.korge.gradle.targets.cordova.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.targets.ios.*
import com.soywiz.korge.gradle.targets.isMacos
import com.soywiz.korge.gradle.targets.js.*
import com.soywiz.korge.gradle.targets.jvm.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.Project
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.plugins.ide.idea.model.*
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

open class JsWebCopy() : DefaultTask() {
	open lateinit var targetDir: File
}

val Project.korgeCacheDir by lazy { File(System.getProperty("user.home"), ".korge").apply { mkdirs() } }
//val node_modules by lazy { project.file("node_modules") }

class KorgeGradleApply(val project: Project) {
	fun apply() = project {
		System.setProperty("java.awt.headless", "true")

		val currentGradleVersion = SemVer(project.gradle.gradleVersion)
		val expectedGradleVersion = SemVer("5.1.1")
		val korgeCheckGradleVersion = (project.ext.properties["korgeCheckGradleVersion"] as? Boolean) ?: true

		if (korgeCheckGradleVersion && currentGradleVersion < expectedGradleVersion) {
			error("Korge requires at least Gradle $expectedGradleVersion, but running on Gradle $currentGradleVersion")
		}

		//KorgeBuildServiceProxy.init()
		project.addVersionExtension()
		project.configureRepositories()
		project.configureKotlin()

		project.addGenResourcesTasks()
		project.configureIdea()

		project.configureJvm()

		project.configureNativeDesktop()
		project.configureNativeAndroid()
		if (isMacos) {
			project.configureNativeIos()
		}
		project.configureCordova()
		project.configureJavaScript()

		project.korge.init()

		project.configureDependencies()
	}

	private fun Project.configureDependencies() {
		korge.apply {
			/*
			dependencyMulti("com.soywiz.korlibs.klock:klock:${BuildVersions.KLOCK}")
			dependencyMulti("com.soywiz:kmem:${BuildVersions.KMEM}")
			dependencyMulti("com.soywiz:kds:${BuildVersions.KDS}")
			dependencyMulti("com.soywiz:korma:${BuildVersions.KORMA}")
			dependencyMulti("com.soywiz:korio:${BuildVersions.KORIO}")
			dependencyMulti("com.soywiz:korim:${BuildVersions.KORIM}")
			dependencyMulti("com.soywiz:korau:${BuildVersions.KORAU}")
			dependencyMulti("com.soywiz:kgl:${BuildVersions.KGL}")
			dependencyMulti("com.soywiz:korag:${BuildVersions.KORAG}")
			dependencyMulti("com.soywiz:korag-opengl:${BuildVersions.KORAG_OPENGL}")
			dependencyMulti("com.soywiz:korgw:${BuildVersions.KORGW}")
			dependencyMulti("com.soywiz:korge:${BuildVersions.KORGE}")
			dependencyMulti("com.soywiz:korev:${BuildVersions.KOREV}")
			 */
		}
		dependencies {
			add("commonMainApi", "com.soywiz.korlibs.korge:korge:${BuildVersions.KORGE}")
		}
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
		//ext.set("koruiVersion", koruiVersion)
		//ext.set("korevVersion", korevVersion)
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

		//gkotlin.sourceSets.maybeCreate("commonMain").dependencies {
		//}

		//kotlin.sourceSets.create("")

	}


}

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
		KorgeGradleApply(project).apply()

		//for (res in project.getResourcesFolders()) println("- $res")
	}
}
