package com.soywiz.korge.gradle.targets.jvm

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import proguard.gradle.*

fun Project.configureJvm() {
	val jvmTarget = (gkotlin.presets.getAt("jvm") as KotlinJvmTargetPreset).createTarget("jvm")
	gkotlin.targets.add(jvmTarget)
	//jvmTarget.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)

	project.korge.addDependency("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	project.korge.addDependency("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")
	project.korge.addDependency("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")

	project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
        it.kotlinOptions {
            this.jvmTarget = korge.jvmTarget
        }
    }

	project.addTask<KorgeJavaExec>("runJvm", group = GROUP_KORGE) { task ->
		group = GROUP_KORGE_RUN
		dependsOn("jvmMainClasses")
		project.afterEvaluate {
			task.main = korge.realJvmMainClassName
		}
	}

	project.afterEvaluate {
		for (entry in korge.extraEntryPoints) {
			project.addTask<KorgeJavaExec>("runJvm${entry.name.capitalize()}", group = GROUP_KORGE) { task ->
				group = GROUP_KORGE_RUN
				dependsOn("jvmMainClasses")
				task.main = entry.jvmMainClassName
			}
		}
	}

	for (jvmJar in project.getTasksByName("jvmJar", true)) {
		val jvmJar = (jvmJar as Jar)
		jvmJar.apply {
			entryCompression = ZipEntryCompression.STORED
		}
	}

	addProguard()
	configureJvmTest()
}

private val Project.jvmCompilation get() = kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*>
private val Project.mainJvmCompilation get() = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

open class KorgeJavaExec : JavaExec() {
    val mainJvmCompilation by lazy { project.mainJvmCompilation }

    val korgeClassPathGet: FileCollection get() = listOf(
        mainJvmCompilation.runtimeDependencyFiles,
        mainJvmCompilation.compileDependencyFiles,
        mainJvmCompilation.output.allOutputs,
        mainJvmCompilation.output.classesDirs,
        project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation))
    ).reduceRight { l, r -> l + r }

    @get:InputFiles
    val korgeClassPath by lazy {
        korgeClassPathGet
    }

    val useZgc get() = ((System.getenv("JVM_USE_ZGC") == "true") || (javaVersion.majorVersion.toIntOrNull() ?: 8) >= 14) && (System.getenv("JVM_USE_ZGC") != "false")

    override fun exec() {
        if (useZgc) {
            println("Using ZGC")
        }
        if (useZgc) {
            jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC")
        }
        classpath = korgeClassPath
        for (classPath in korgeClassPath.toList()) {
            project.logger.info("- $classPath")
        }
        super.exec()
    }

    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        // https://github.com/korlibs/korge-plugins/issues/25
    }
}

private fun Project.configureJvmTest() {
	val jvmTest = (tasks.findByName("jvmTest") as Test)
    jvmTest.classpath += project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation))
	jvmTest.jvmArgs = (jvmTest.jvmArgs ?: listOf()) + listOf("-Djava.awt.headless=true")
}


private fun Project.addProguard() {
	// packageJvmFatJar
	val packageJvmFatJar = project.addTask<org.gradle.jvm.tasks.Jar>("packageJvmFatJar", group = GROUP_KORGE) { task ->
		task.baseName = "${project.name}-all"
		task.group = GROUP_KORGE_PACKAGE
		task.exclude(
			"com/sun/jna/aix-ppc/**",
			"com/sun/jna/aix-ppc64/**",
			"com/sun/jna/freebsd-x86/**",
			"com/sun/jna/freebsd-x86-64/**",
			"com/sun/jna/linux-ppc/**",
			"com/sun/jna/linux-ppc64le/**",
			"com/sun/jna/linux-s390x/**",
			"com/sun/jna/linux-mips64el/**",
			"com/sun/jna/openbsd-x86/**",
			"com/sun/jna/openbsd-x86-64/**",
			"com/sun/jna/sunos-sparc/**",
			"com/sun/jna/sunos-sparcv9/**",
			"com/sun/jna/sunos-x86/**",
			"com/sun/jna/sunos-x86-64/**",
			"natives/macosx64/**"
		)
        task.doFirst {
            task.from(project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation)))
        }
		project.afterEvaluate {
			task.manifest { manifest ->
				manifest.attributes(
					mapOf(
						"Implementation-Title" to korge.realJvmMainClassName,
						"Implementation-Version" to project.version.toString(),
						"Main-Class" to korge.realJvmMainClassName
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
			// Support newer java versions that doesn't have rt.jar
			task.libraryjars(project.fileTree("${System.getProperty("java.home")}/jmods/") {
				it.include("**/java.*.jmod")
			})
			//println(packageJvmFatJar.outputs.files.toList())
			task.injars(packageJvmFatJar.outputs.files.toList())
			task.outjars(buildDir["/libs/${project.name}-all-proguard.jar"])
			task.dontwarn()
			task.ignorewarnings()
			if (!project.korge.proguardObfuscate) {
				task.dontobfuscate()
			}
			task.assumenosideeffects("""
                class kotlin.jvm.internal.Intrinsics {
                    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
                }
            """.trimIndent())

			task.keepnames("class com.sun.jna.** { *; }")
			task.keepnames("class * extends com.sun.jna.** { *; }")
			//task.keepnames("class org.jcodec.** { *; }")
			task.keepattributes()
			task.keep("class * implements com.sun.jna.** { *; }")
			task.keep("class com.sun.jna.** { *; }")
			task.keep("class ${project.korge.realJvmMainClassName} { *; }")
			task.keep("class org.jcodec.** { *; }")

			if (runJvm.main?.isNotBlank() == true) {
				task.keep("""public class ${runJvm.main} { public static void main(java.lang.String[]); }""")
			}
		}

	}
}
