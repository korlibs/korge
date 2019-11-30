package com.soywiz.korge.gradle.targets.jvm

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korio.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import proguard.gradle.*

fun Project.configureJvm() {
	val jvmTarget = (gkotlin.presets.getAt("jvm") as KotlinJvmTargetPreset).createTarget("jvm")
	gkotlin.targets.add(jvmTarget)
	//jvmTarget.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)

	project.dependencies.add("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	project.dependencies.add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")
	project.dependencies.add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")

    project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
        it.kotlinOptions {
            this.jvmTarget = "1.8"
        }
    }

    val runJvm = project.addTask<JavaExec>("runJvm", group = GROUP_KORGE) { task ->
		group = GROUP_KORGE_RUN
		dependsOn("jvmMainClasses")
		systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"

        task.doFirst {
            //task.classpath = gkotlin.targets["jvm"]["compilations"]["test"]["runtimeDependencyFiles"] as? FileCollection?
            val jvmCompilation = gkotlin.targets["jvm"]["compilations"] as NamedDomainObjectSet<*>
            val mainJvmCompilation = jvmCompilation["main"] as KotlinJvmCompilation
            //println(jvmCompilation)
            //println(mainJvmCompilation)
            //println(mainJvmCompilation.runtimeDependencyFiles.toList())
            //println(mainJvmCompilation.compileDependencyFiles.toList())

            //task.classpath = gkotlin.targets["jvm"]["compilations"]["main"]["runtimeDependencyFiles"] as? FileCollection?
            task.classpath = mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs

            if (OS.isMac) {
                //task.jvmArgs("-XstartOnFirstThread")
            }

            task.main = korge.jvmMainClassName
        }
	}

	for (jvmJar in project.getTasksByName("jvmJar", true)) {
		(jvmJar as Jar).entryCompression = ZipEntryCompression.STORED
	}

	addProguard()
	configureJvmTest()
}

private fun Project.configureJvmTest() {
	val jvmTest = (tasks.findByName("jvmTest") as Test)
	jvmTest.jvmArgs = (jvmTest.jvmArgs ?: listOf()) + listOf("-Djava.awt.headless=true")
}


private fun Project.addProguard() {
	// packageJvmFatJar
	val packageJvmFatJar = project.addTask<org.gradle.jvm.tasks.Jar>("packageJvmFatJar", group = GROUP_KORGE) { task ->
		task.baseName = "${project.name}-all"
		task.group = GROUP_KORGE_PACKAGE
		project.afterEvaluate {
			task.manifest { manifest ->
				manifest.attributes(
					mapOf(
						"Implementation-Title" to korge.jvmMainClassName,
						"Implementation-Version" to project.version.toString(),
						"Main-Class" to korge.jvmMainClassName
					)
				)
			}
			//it.from()
			//fileTree()
			task.from(GroovyClosure(project) {
				(project.gkotlin.targets["jvm"]["compilations"]["main"]["runtimeDependencyFiles"] as FileCollection).map { if (it.isDirectory) it else project.zipTree(it) as Any }
				//listOf<File>()
			})
			task.with(project.getTasksByName("jvmJar", true).first() as CopySpec)
		}
	}

	val runJvm = tasks.getByName("runJvm") as JavaExec

	project.addTask<ProGuardTask>("packageJvmFatJarProguard", group = GROUP_KORGE, dependsOn = listOf(
		packageJvmFatJar
	)
	) { task ->
		task.group = GROUP_KORGE_PACKAGE
		project.afterEvaluate {
			task.libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
			//println(packageJvmFatJar.outputs.files.toList())
			task.injars(packageJvmFatJar.outputs.files.toList())
			task.outjars(buildDir["/libs/${project.name}-all-proguard.jar"])
			task.dontwarn()
			task.ignorewarnings()
			//task.dontobfuscate()
			task.assumenosideeffects("""
                class kotlin.jvm.internal.Intrinsics {
                    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
                }
            """.trimIndent())

			//task.keep("class jogamp.nativetag.**")
			//task.keep("class jogamp.**")

			task.keep("class com.jogamp.** { *; }")
			task.keep("class jogamp.** { *; }")

			if (runJvm.main?.isNotBlank() == true) {
				task.keep("""public class ${runJvm.main} { public static void main(java.lang.String[]); }""")
			}
		}

	}
}
