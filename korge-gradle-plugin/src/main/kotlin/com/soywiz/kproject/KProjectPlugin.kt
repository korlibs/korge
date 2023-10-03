package com.soywiz.kproject

import com.android.build.gradle.*
import com.soywiz.kproject.internal.*
import com.soywiz.kproject.model.*
import com.soywiz.kproject.util.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.testing.logging.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

@Suppress("unused")
class KProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.defineStandardRepositories()

        project.plugins.applyOnce("kotlin-multiplatform")
        //project.repositories()
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val depsKprojectYml = File(project.rootProject.rootDir, "deps.kproject.yml")
        val info = when {
            depsKprojectYml.exists() -> Yaml.decode(depsKprojectYml.readText())
            else -> null
        }.dyn
        val info2 = NewKProjectModel.loadFile(depsKprojectYml.fileRef)

        fun getPropertyValue(name: String, default: String): String {
            return info[name].toStringOrNull()
                ?: project.findProperty(name)?.toString()
                ?: default
        }

        val jvmVersion = getPropertyValue("kproject.jvm.version", korlibs.korge.gradle.targets.android.GRADLE_JAVA_VERSION_STR)
        val androidJvmVersion = getPropertyValue("kproject.android.jvm.version", korlibs.korge.gradle.targets.android.ANDROID_JAVA_VERSION_STR)

        //val kprojectYml = File(project.projectDir, "kproject.yml")
        //val kproject = if (kprojectYml.exists()) KProject.load(kprojectYml, KSet(File("modules")), true) else null

        // @TODO: Configure
        fun hasTarget(name: KProjectTarget): Boolean {
            if (name.isKotlinNative && isWindowsOrLinuxArm) return false
            // Do not include K/N (iOS) on Windows or Linux (only on Macos)
            if (name.isKotlinNative && !korlibs.korge.gradle.targets.isMacos) return false
            return info2.hasTarget(name)
        }

        kotlin.apply {
            metadata()
            if (hasTarget(KProjectTarget.JVM)) {
                jvm().apply {
                    compilations.all {
                        it.kotlinOptions.jvmTarget = jvmVersion
                    }
                    //withJava()
                    testRuns.maybeCreate("test").executionTask.configure {
                        it.useJUnit()
                    }
                }
            }
            if (hasTarget(KProjectTarget.ANDROID)) {
                project.plugins.applyOnce("com.android.library")
                androidTarget().apply {
                    compilations.all {
                        it.kotlinOptions.jvmTarget = androidJvmVersion
                    }
                }
                project.extensions.getByType(LibraryExtension::class.java).apply {
                    compileSdk = ANDROID_DEFAULT_COMPILE_SDK
                    namespace = ("${project.group}.${project.name}").replace("-", ".")
                    sourceSets.apply {
                        maybeCreate("main").apply {
                            manifest.srcFile(AndroidConfig.getAndroidManifestFile(project))
                        }
                    }
                }
                //println(project.extensions.getByName("android"))
                //println(project.extensions.getByName("android")::class)
            }
            if (hasTarget(KProjectTarget.JS)) {
                js(KotlinJsCompilerType.IR) {
                    browser {
                        //commonWebpackConfig { cssSupport { it.enabled.set(true) } }
                    }
                }
            }
            if (hasTarget(KProjectTarget.WASM_JS)) {
                wasmJs().apply {
                    browser()
                }
            }
            //println(isWindows)
            //println(isLinux)
            //println(isArm)
            //println(isWindowsOrLinuxArm)
            //for (target in KProjectTarget.values()) {
            //    println("target=$target, has=${hasTarget(target)}")
            //}
            //if (hasTarget(KProjectTarget.DESKTOP)) {
            //    macosArm64()
            //    macosX64()
            //    mingwX64()
            //    linuxX64()
            //    linuxArm64()
            //}
            if (hasTarget(KProjectTarget.MOBILE)) {
                iosX64()
                iosArm64()
                iosSimulatorArm64()
                tvosX64()
                tvosArm64()
                tvosSimulatorArm64()
            }
            sourceSets.apply {
                val common = createPair("common")
                common.test.dependencies { implementation(kotlin("test")) }
                val concurrent = createPair("concurrent")
                val jvmAndroid = createPair("jvmAndroid").dependsOn(concurrent)
                if (hasTarget(KProjectTarget.JVM)) {
                    val jvm = createPair("jvm").dependsOn(jvmAndroid)
                }
                if (hasTarget(KProjectTarget.ANDROID)) {
                    val android = createPair("android").dependsOn(jvmAndroid)
                }
                if (hasTarget(KProjectTarget.JS)) {
                    val js = createPair("js")
                }
                //if (hasTarget(KProjectTarget.DESKTOP) || hasTarget(KProjectTarget.MOBILE)) {
                if (hasTarget(KProjectTarget.MOBILE)) {
                    val native = createPair("native").dependsOn(concurrent)
                    val posix = createPair("posix").dependsOn(native)
                    val apple = createPair("apple").dependsOn(posix)
                    val macos = createPair("macos").dependsOn(apple)
                    //if (hasTarget(KProjectTarget.DESKTOP)) {
                    //    createPair("macosX64").dependsOn(macos)
                    //    createPair("macosArm64").dependsOn(macos)
                    //    val linux = createPair("linux").dependsOn(posix)
                    //    createPair("linuxX64").dependsOn(linux)
                    //    createPair("linuxArm64").dependsOn(linux)
                    //    createPair("mingwX64").dependsOn(native)
                    //}
                    if (hasTarget(KProjectTarget.MOBILE)) {
                        val ios = createPair("ios").dependsOn(apple)
                        createPair("iosArm64").dependsOn(ios)
                        createPair("iosSimulatorArm64").dependsOn(ios)
                    }
                }
            }
            //println("KProjectPlugin: $project")
        }

        project.tasks.withType(org.gradle.api.tasks.testing.AbstractTestTask::class.java).all {
            it.testLogging {
                it.events = mutableSetOf(
                    //TestLogEvent.STARTED, TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.FAILED,
                    TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR
                )
                it.exceptionFormat = TestExceptionFormat.FULL
                it.showStackTraces = true
                it.showStandardStreams = true
            }
        }

    }

    // SourceSet pairs: main + test
    data class KotlinSourceSetPair(val main: KotlinSourceSet, val test: KotlinSourceSet)
    fun NamedDomainObjectContainer<KotlinSourceSet>.createPair(name: String): KotlinSourceSetPair = KotlinSourceSetPair(maybeCreate("${name}Main"), maybeCreate("${name}Test"))
    fun KotlinSourceSetPair.dependsOn(other: KotlinSourceSetPair): KotlinSourceSetPair {
        this.main.dependsOn(other.main)
        this.test.dependsOn(other.test)
        return this
    }
}

//fun <T : Plugin<*>> PluginContainer.applyOnce(clazz: Class<T>): T = findPlugin(clazz) ?: apply(clazz)
fun <T : Plugin<*>> PluginContainer.applyOnce(clazz: Class<T>): T = apply(clazz)
inline fun <reified T : Plugin<*>> PluginContainer.applyOnce(): T = applyOnce(T::class.java)
fun PluginContainer.applyOnce(id: String) {
    if (!hasPlugin(id)) {
        apply(id)
    }
}

private const val ANDROID_DEFAULT_MIN_SDK = 18
private const val ANDROID_DEFAULT_COMPILE_SDK = 30
private const val ANDROID_DEFAULT_TARGET_SDK = 30
