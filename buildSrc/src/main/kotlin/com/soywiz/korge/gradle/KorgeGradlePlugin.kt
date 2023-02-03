package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.linux.LDLibraries
import com.soywiz.korge.gradle.util.*
import com.soywiz.korlibs.*
import org.gradle.api.*
import org.gradle.api.Project
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.diagnostics.*
import org.gradle.internal.classloader.*
import org.gradle.plugins.ide.idea.model.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.*
import java.net.*

class KorgeGradleApply(val project: Project) {
	fun apply(includeIndirectAndroid: Boolean = true) = project {
        // @TODO: Doing this disables the ability to use configuration cache
		//System.setProperty("java.awt.headless", "true")

		val currentGradleVersion = SemVer(project.gradle.gradleVersion)
        //val expectedGradleVersion = SemVer("6.8.1")
        val expectedGradleVersion = SemVer("7.5.0")
		val korgeCheckGradleVersion = (project.ext.properties["korgeCheckGradleVersion"] as? Boolean) ?: true

		if (korgeCheckGradleVersion && currentGradleVersion < expectedGradleVersion) {
			error("Korge requires at least Gradle $expectedGradleVersion, but running on Gradle $currentGradleVersion. Please, edit gradle/wrapper/gradle-wrapper.properties")
		}

        if (isLinux) {
            project.logger.info("LD folders: ${LDLibraries.ldFolders}")
            for (lib in listOf("libGL.so.1", "libopenal.so.1")) {
                if (!LDLibraries.hasLibrary(lib)) {
                    System.err.println("Can't find $lib. Please: sudo apt-get -y install freeglut3 libopenal1")
                }
            }
        }

        logger.info("Korge Gradle plugin: ${BuildVersions.ALL}")

        KorgeVersionsTask.registerShowKorgeVersions(project)

        project.korge.init(includeIndirectAndroid)

        project.configureIdea()
		project.addVersionExtension()
		project.configureRepositories()
		project.configureKotlin()

        korge.targetJvm()

        project.afterEvaluate {
            project.configureDependencies()
            if (korge.enableKorgeResourceProcessing) {
                project.addGenResourcesTasks()
            }
        }
	}

	private fun Project.configureDependencies() {
		dependencies {
            add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            add("commonMainApi", "com.soywiz.korlibs.klock:klock:${klockVersion}")
            add("commonMainApi", "com.soywiz.korlibs.kmem:kmem:${kmemVersion}")
            add("commonMainApi", "com.soywiz.korlibs.kds:kds:${kdsVersion}")
            add("commonMainApi", "com.soywiz.korlibs.krypto:krypto:${kryptoVersion}")
            add("commonMainApi", "com.soywiz.korlibs.korge2:korge:${korgeVersion}")
			add("commonMainApi", "com.soywiz.korlibs.korma:korma:${kormaVersion}")
			add("commonMainApi", "com.soywiz.korlibs.korio:korio:${korioVersion}")
			add("commonMainApi", "com.soywiz.korlibs.korim:korim:${korimVersion}")
			add("commonMainApi", "com.soywiz.korlibs.korau:korau:${korauVersion}")
			add("commonMainApi", "com.soywiz.korlibs.korgw:korgw:${korgwVersion}")
		}
	}

	private fun Project.configureIdea() {
		project.plugins.applyOnce("idea")

        project.extensions.getByName<IdeaModel>("idea").apply {
			module {
                val module = it
                module.excludeDirs = module.excludeDirs.also {
                    it.addAll(listOf(
                        ".gradle", ".idea", "gradle/wrapper", "node_modules", "classes", "docs", "dependency-cache",
                        "libs", "reports", "resources", "test-results", "tmp", "bundles", "modules",
                    ).map { file(it) })
                }
			}
		}
	}

	private fun Project.addVersionExtension() {
		ext.set("korioVersion", korioVersion)
		ext.set("kormaVersion", kormaVersion)
		ext.set("korauVersion", korauVersion)
		ext.set("korimVersion", korimVersion)
		ext.set("korgwVersion", korgwVersion)
		ext.set("korgeVersion", korgeVersion)
		ext.set("kotlinVersion", kotlinVersion)
		ext.set("coroutinesVersion", coroutinesVersion)
	}

	private fun Project.configureKotlin() {
		plugins.applyOnce("kotlin-multiplatform")

		project.korge.addDependency("commonMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-common")
		project.korge.addDependency("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test")

		//println("com.soywiz.korlibs.korge2:korge:$korgeVersion")
		//project.dependencies.add("commonMainImplementation", "com.soywiz.korlibs.korge2:korge:$korgeVersion")
		//gkotlin.sourceSets.maybeCreate("commonMain").dependencies {
		//}
		//kotlin.sourceSets.create("")
	}
}

open class KorgeGradlePlugin : Plugin<Project> {
	override fun apply(project: Project) {
        project.configureAutoVersions()

        project.configureBuildScriptClasspathTasks()

		//TODO PABLO changed to have the android tasks enabled again
		KorgeGradleApply(project).apply(includeIndirectAndroid = true)

        //for (res in project.getResourcesFolders()) println("- $res")
	}
}

fun Project.configureAutoVersions() {
    allprojectsThis {
        configurations.all {
            it.resolutionStrategy.eachDependency { details ->
                //println("DETAILS: ${details.requested} : '${details.requested.group}' : '${details.requested.name}' :  '${details.requested.version}'")
                val groupWithName = "${details.requested.group}:${details.requested.name}"
                if (details.requested.version.isNullOrBlank()) {
                    val version = korge.versionSubstitutions[groupWithName]
                    if (version != null) {
                        details.useVersion(version)
                        details.because("korge.versionSubstitutions: '$groupWithName' -> $version")
                    }
                }
            }
        }
    }
}

fun Project.configureBuildScriptClasspathTasks() {
    // https://gist.github.com/xconnecting/4037220
    val printBuildScriptClasspath = project.tasks.createThis<DependencyReportTask>("printBuildScriptClasspath") {
        configurations = project.buildscript.configurations
    }
    val printBuildScriptClasspath2 = project.tasks.createThis<Task>("printBuildScriptClasspath2") {
        doFirst {
            fun getClassLoaderChain(classLoader: ClassLoader, out: ArrayList<ClassLoader> = arrayListOf()): List<ClassLoader> {
                var current: ClassLoader? = classLoader
                while (current != null) {
                    out.add(current)
                    current = current.parent
                }
                return out
            }

            fun printClassLoader(classLoader: ClassLoader) {
                when (classLoader) {
                    is URLClassLoader -> {
                        println(classLoader.urLs.joinToString("\n"))
                    }
                    is ClassLoaderHierarchy -> {
                        classLoader.visit(object : ClassLoaderVisitor() {
                            override fun visit(classLoader: ClassLoader) {
                                super.visit(classLoader)
                            }

                            override fun visitSpec(spec: ClassLoaderSpec) {
                                super.visitSpec(spec)
                            }

                            override fun visitClassPath(classPath: Array<out URL>) {
                                classPath.forEach { println(it) }
                            }

                            override fun visitParent(classLoader: ClassLoader) {
                                super.visitParent(classLoader)
                            }
                        })
                    }
                }
            }

            println("Class loaders:")
            val classLoaders = getClassLoaderChain(Thread.currentThread().contextClassLoader)
            for (classLoader in classLoaders.reversed()) {
                println(" - $classLoader")
            }

            for (classLoader in classLoaders.reversed()) {
                println("")
                println("$classLoader:")
                println("--------------")
                printClassLoader(classLoader)
            }
            //println(ClassLoader.getSystemClassLoader())
            //println((Thread.currentThread().contextClassLoader as URLClassLoader).parent.urLs.joinToString("\n"))
            //println((KorgeGradlePlugin::class.java.classLoader as URLClassLoader).urLs.joinToString("\n"))
        }
    }

}

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
    @OutputDirectory
    open lateinit var targetDir: File
}

var Project.korgeCacheDir by projectExtension { File(System.getProperty("user.home"), ".korge").apply { mkdirs() } }
//val node_modules by lazy { project.file("node_modules") }
