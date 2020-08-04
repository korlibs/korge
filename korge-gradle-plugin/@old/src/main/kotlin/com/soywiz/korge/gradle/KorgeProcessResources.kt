package com.soywiz.korge.gradle

import com.soywiz.korge.build.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.resources.*
import org.gradle.api.*
import java.net.*

fun Project.addGenResourcesTasks() = this {
	val genMainResourcesDir = buildDir["genMainResources"]
	val genTestResourcesDir = buildDir["genTestResources"]
	val genResourcesDirs = setOf(genMainResourcesDir, genTestResourcesDir)

	afterEvaluate {
		kotlin.sourceSets["commonMain"].resources.srcDir(genMainResourcesDir)
		kotlin.sourceSets["commonTest"].resources.srcDir(genTestResourcesDir)
	}

	for (test in listOf(false, true)) {
		val genResources = tasks.createTyped<Task>(if (test) "genTestResources" else "genResources") {
			group = GROUP_KORGE_RESOURCES

			val outDir = if (test) genTestResourcesDir else genMainResourcesDir

			afterEvaluate {
				tasks.findByName(if (test) "processTestResources" else "processResources")?.dependsOn(this)
				tasks.findByName(if (test) "jvmTestClasses" else "jvmMainClasses")?.dependsOn(this)
				tasks.findByName(if (test) "jsTestClasses" else "jsMainClasses")?.dependsOn(this)
				for (target in ALL_NATIVE_TARGETS) {
					//tasks.findByName(if (test) "compileTestKotlin${target.capitalize()}" else "compileKotlin${target.capitalize()}")?.dependsOn(this)
				}

				val buildService = KorgeBuildService

				val allResourcesDirs = kotlin.sourceSets
					.flatMap { it.resources.srcDirs.toList() }.filter { it !in genResourcesDirs }

				val resourcesDirs = kotlin.sourceSets
					.filter { if (test) it.name.endsWith("Test") else it.name.endsWith("Main") }
					.flatMap { it.resources.srcDirs.toList() }.filter { it.exists() }.filter { it !in genResourcesDirs }

				for (dir in resourcesDirs) {
					inputs.dir(dir)
				}
				outputs.dir(outDir)

				doLast {
					logger.info("KorgeResourcesTask ($this)")
					logger.info("kotlin.sourceSets.names: ${kotlin.sourceSets.names}")
					logger.info("allResourcesDirs: $allResourcesDirs")
					logger.info("resourcesDirs: $resourcesDirs")
					logger.info("korge.defaultPluginsClassLoader: ${(korge.defaultPluginsClassLoader as URLClassLoader).urLs.toList()}")

					buildService.processResourcesFolders(ResourceProcessor.Group(korge.defaultPluginsClassLoader), resourcesDirs, outDir) { logger.info(it) }
				}
			}
		}

	}
}

