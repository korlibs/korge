package com.soywiz.korlibs

import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmTask
import com.soywiz.korlibs.modules.*
import com.soywiz.korlibs.targets.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.*

class KorlibsPluginNoNativeNoAndroid : BaseKorlibsPlugin(nativeEnabled = false, androidEnabled = false)
class KorlibsPluginNoNative : BaseKorlibsPlugin(nativeEnabled = false, androidEnabled = true)
class KorlibsPlugin : BaseKorlibsPlugin(nativeEnabled = true, androidEnabled = true)

open class BaseKorlibsPlugin(val nativeEnabled: Boolean, val androidEnabled: Boolean) : Plugin<Project> {
    override fun apply(project: Project) = project {
        val korlibs = KorlibsExtension(this, nativeEnabled, androidEnabled)
        extensions.add("korlibs", korlibs)

        plugins.apply("kotlin-multiplatform")
        plugins.apply("com.moowork.node")

        configureKorlibsRepos()

        // Platforms
        configureTargetCommon()
        configureTargetAndroid()
        if (nativeEnabled) {
            configureTargetNative()
        }
        configureTargetJavaScript()
        configureTargetJVM()

        // Publishing
        configurePublishing()
    }
}

class KorlibsExtension(val project: Project, val nativeEnabled: Boolean, val androidEnabled: Boolean) {
    //init { println("KorlibsExtension:${project.name},nativeEnabled=$nativeEnabled,androidEnabled=$androidEnabled") }
    var hasAndroid = androidEnabled && ((System.getProperty("sdk.dir") != null) || (System.getenv("ANDROID_HOME") != null))

    init {
        if (!hasAndroid && androidEnabled) {
            val trySdkDir = File(System.getProperty("user.home") + "/Library/Android/sdk")
            if (trySdkDir.exists()) {
                File(project.rootDir, "local.properties").writeText("sdk.dir=${trySdkDir.absolutePath}")
                hasAndroid = true
            }
        }
    }

    fun dependencyProject(name: String) = project {
        dependencies {
            add("commonMainApi", project(name))
            add("commonTestImplementation", project(name))
        }
    }

    val LINUX_DESKTOP_NATIVE_TARGETS = listOf("linuxX64")
    val MACOS_DESKTOP_NATIVE_TARGETS = listOf("macosX64")
    //val WINDOWS_DESKTOP_NATIVE_TARGETS = listOf("mingwX64", "mingwX86")
    val WINDOWS_DESKTOP_NATIVE_TARGETS = listOf("mingwX64")
    val DESKTOP_NATIVE_TARGETS = LINUX_DESKTOP_NATIVE_TARGETS + MACOS_DESKTOP_NATIVE_TARGETS + WINDOWS_DESKTOP_NATIVE_TARGETS
    val IOS_TARGETS = listOf("iosArm64", "iosArm32", "iosX64")
    val ALL_NATIVE_TARGETS = IOS_TARGETS + DESKTOP_NATIVE_TARGETS
    val ALL_ANDROID_TARGETS = if (hasAndroid) listOf("android") else listOf()
    val JS_TARGETS = listOf("js")
    val JVM_TARGETS = listOf("jvm")
    val COMMON_TARGETS = listOf("metadata")
    val ALL_TARGETS = ALL_ANDROID_TARGETS + JS_TARGETS + JVM_TARGETS + COMMON_TARGETS + ALL_NATIVE_TARGETS

    @JvmOverloads
    fun dependencyMulti(group: String, name: String, version: String, targets: List<String> = ALL_TARGETS, suffixCommonRename: Boolean = false, androidIsJvm: Boolean = false) = project {
        dependencies {
            for (target in targets) {
                val base = when (target) {
                    "metadata" -> "common"
                    else -> target
                }
                val suffix = when {
                    target == "android" && androidIsJvm -> "-jvm"
                    target == "metadata" && suffixCommonRename -> "-common"
                    else -> "-${target.toLowerCase()}"
                }

                val packed = "$group:$name$suffix:$version"
                add("${base}MainApi", packed)
                add("${base}TestImplementation", packed)
            }
        }
    }

    @JvmOverloads
    fun dependencyMulti(dependency: String, targets: List<String> = ALL_TARGETS) {
        val (group, name, version) = dependency.split(":", limit = 3)
        return dependencyMulti(group, name, version, targets)
    }

    @JvmOverloads
    fun dependencyNodeModule(name: String, version: String) = project {
        val node = extensions.getByType(NodeExtension::class.java)

        val installNodeModule = tasks.create<NpmTask>("installJs${name.capitalize()}") {
            onlyIf { !File(node.nodeModulesDir, name).exists() }
            setArgs(arrayListOf("install", "$name@$version"))
        }

        tasks.getByName("jsTestNode").dependsOn(installNodeModule)
    }

    data class CInteropTargets(val name: String, val targets: List<String>)

    val cinterops = arrayListOf<CInteropTargets>()


    @JvmOverloads
    fun dependencyCInterops(name: String, targets: List<String>) = project {
        cinterops += CInteropTargets(name, targets)
        for (target in targets) {
            (kotlin.targets[target].compilations["main"] as KotlinNativeCompilation).apply {
                cinterops.apply {
                    maybeCreate(name).apply {
                    }
                }
            }
        }
    }

    @JvmOverloads
    fun dependencyCInteropsExternal(dependency: String, cinterop: String, targets: List<String> = ALL_NATIVE_TARGETS) {
        dependencyMulti("$dependency:cinterop-$cinterop@klib", targets)
    }

    @JvmOverloads
    fun exposeVersion(name: String = project.name) {
        project.projectDir["src/commonMain/kotlin/com/soywiz/$name/internal/${name.capitalize()}Version.kt"].text = """
            package com.soywiz.$name.internal

            internal const val ${name.toUpperCase()}_VERSION = "${project.version}"
        """.trimIndent()
    }
}

val Project.korlibs get() = extensions.getByType(KorlibsExtension::class.java)
fun Project.korlibs(callback: KorlibsExtension.() -> Unit) = korlibs.apply(callback)
val Project.hasAndroid get() = korlibs.hasAndroid
