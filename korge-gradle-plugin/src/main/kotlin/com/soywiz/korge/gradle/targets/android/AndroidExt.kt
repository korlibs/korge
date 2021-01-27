package com.soywiz.korge.gradle.targets.android

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.*

//Linux: ~/Android/Sdk
//Mac: ~/Library/Android/sdk
//Windows: %LOCALAPPDATA%\Android\sdk
val Project.androidSdkPath: String get() {
    val localPropertiesFile = projectDir["local.properties"]
    if (localPropertiesFile.exists()) {
        val props = Properties().apply { load(localPropertiesFile.readText().reader()) }
        if (props.getProperty("sdk.dir") != null) {
            return props.getProperty("sdk.dir")!!
        }
    }
    val userHome = System.getProperty("user.home")
    return listOfNotNull(
        System.getenv("ANDROID_HOME"),
        "$userHome/AppData/Local/Android/sdk",
        "$userHome/Library/Android/sdk",
        "$userHome/Android/Sdk"
    ).firstOrNull { File(it).exists() } ?: error("Can't find android sdk (ANDROID_HOME environment not set and Android SDK not found in standard locations)")
}

fun Project.ensureAndroidLocalPropertiesWithSdkDir(outputFolder: File = project.rootDir) {
    val path = project.tryToDetectAndroidSdkPath()
    if (path != null) {
        val localProperties = File(outputFolder, "local.properties")
        if (!localProperties.exists()) {
            localProperties
                .ensureParents()
                .writeText("sdk.dir=${path.absolutePath.replace("\\", "/")}")
        }
    }
}

fun Project.installAndroidRun(dependsOnList: List<String>, direct: Boolean) {
    // adb shell am start -n com.package.name/com.package.name.ActivityName
    for (debug in listOf(false, true)) {
        val suffixDebug = if (debug) "Debug" else "Release"
        val installAndroidTask = when {
            direct -> tasks.create("installAndroid$suffixDebug", Task::class.java) { task ->
                task.group = GROUP_KORGE_INSTALL
                for (dependsOnTaskNAme in dependsOnList) {
                    task.dependsOn(dependsOnTaskNAme)
                }
                task.dependsOn("korgeProcessedResourcesJvmMain")
                task.dependsOn("install$suffixDebug")
            }
            else -> tasks.create("installAndroid$suffixDebug", GradleBuild::class.java) { task ->
                task.group = GROUP_KORGE_INSTALL
                for (dependsOnTaskNAme in dependsOnList) {
                    task.dependsOn(dependsOnTaskNAme)
                }
                task.dependsOn("korgeProcessedResourcesJvmMain")
                task.buildFile = File(buildDir, "platforms/android/build.gradle")
                //task.versi = "4.10.1"
                task.tasks = listOf("install$suffixDebug")
            }
        }

        for (emulator in listOf(null, false, true)) {
            val suffixDevice = when (emulator) {
                null -> ""
                false -> "Device"
                true -> "Emulator"
            }

            val extra = when (emulator) {
                null -> arrayOf()
                false -> arrayOf("-d")
                true -> arrayOf("-e")
            }

            tasks.createTyped<DefaultTask>("runAndroid$suffixDevice$suffixDebug") {
                group = GROUP_KORGE_RUN
                dependsOn(installAndroidTask)
                doFirst {
                    execLogger {
                        it.commandLine(
                            "$androidSdkPath/platform-tools/adb", *extra, "shell", "am", "start", "-n",
                            "${korge.id}/${korge.id}.MainActivity"
                        )
                    }
                }
            }
        }
    }

    tasks.createTyped<DefaultTask>("adbDeviceList") {
        group = GROUP_KORGE_ADB
        doFirst {
            execLogger {
                it.commandLine("$androidSdkPath/platform-tools/adb", "devices", "-l")
            }
        }
    }

    tasks.createTyped<DefaultTask>("adbLogcat") {
        group = GROUP_KORGE_ADB
        doFirst {
            execLogger {
                it.commandLine("$androidSdkPath/platform-tools/adb", "logcat")
            }
        }
    }
}


fun Project.androidGetResourcesFolders(): Pair<List<File>, List<File>> {
    val targets = listOf(kotlin.metadata())
    val mainSourceSets = targets.flatMap { it.compilations["main"].allKotlinSourceSets }

    val resourcesSrcDirsBase = mainSourceSets.flatMap { it.resources.srcDirs } + listOf(file("src/androidMain/resources"), file("src/main/resources"))
    val resourcesSrcDirsBundle = project.korge.bundles.getPaths("android", resources = true, test = false)
    val resourcesSrcDirs = resourcesSrcDirsBase + resourcesSrcDirsBundle

    val kotlinSrcDirsBase = mainSourceSets.flatMap { it.kotlin.srcDirs } + listOf(file("src/androidMain/kotlin"), file("src/main/java"))
    val kotlinSrcDirsBundle = project.korge.bundles.getPaths("android", resources = false, test = false)
    val kotlinSrcDirs = kotlinSrcDirsBase + kotlinSrcDirsBundle

    return Pair(resourcesSrcDirs, kotlinSrcDirs)
}

fun isKorlibsDependency(cleanFullName: String): Boolean {
    if (cleanFullName.startsWith("org.jetbrains")) return false
    if (cleanFullName.startsWith("junit:junit")) return false
    if (cleanFullName.startsWith("org.hamcrest:hamcrest-core")) return false
    if (cleanFullName.startsWith("org.jogamp")) return false
    return true
}

fun androidExcludePatterns(): List<String> = listOf(
    "META-INF/DEPENDENCIES",
    "META-INF/LICENSE",
    "META-INF/LICENSE.txt",
    "META-INF/license.txt",
    "META-INF/NOTICE",
    "META-INF/NOTICE.txt",
    "META-INF/notice.txt",
    "META-INF/LGPL*",
    "META-INF/AL2.0",
    "META-INF/*.kotlin_module",
    "**/*.kotlin_metadata",
    "**/*.kotlin_builtins",
)
