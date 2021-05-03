package com.soywiz.korge.gradle.targets.android

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.*

val ANDROID_SDK_PATH_KEY = "android.sdk.path"

//Linux: ~/Android/Sdk
//Mac: ~/Library/Android/sdk
//Windows: %LOCALAPPDATA%\Android\sdk
val Project.androidSdkPath: String get() {
    val extensionAndroidSdkPath = this.findProperty(ANDROID_SDK_PATH_KEY)?.toString() ?: this.extensions.findByName(ANDROID_SDK_PATH_KEY)?.toString()
    if (extensionAndroidSdkPath != null) return extensionAndroidSdkPath

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

val Project.androidAdbPath get() = "$androidSdkPath/platform-tools/adb"
val Project.androidEmulatorPath get() = "$androidSdkPath/emulator/emulator"

fun Project.execAndroidAdb(vararg args: String) {
    execLogger {
        it.commandLine(androidAdbPath, *args)
    }
}

fun Project.androidAdbDeviceList(): List<String> {
    return execOutput(androidAdbPath, "devices", "-l").trim().split("\n").map { it.trim() }.drop(1)

}

fun Project.androidEmulatorListAvds(): List<String> {
    val output = execOutput(androidEmulatorPath, "-list-avds").trim()
    return when {
        output.isBlank() -> listOf()
        else -> output.split("\n").map { it.trim() }
    }
}

fun Project.androidEmulatorIsStarted(): Boolean {
    return androidAdbDeviceList().any { it.contains("emulator") }
}

fun Project.androidEmulatorFirstAvd(): String? {
    val avds = androidEmulatorListAvds()
    return avds.firstOrNull { !it.contains("_TV") } ?: avds.firstOrNull()
}

fun Project.execAndroidEmulator(vararg args: String) {
    execLogger {
        it.commandLine(androidEmulatorPath, *args)
    }
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

            val installAndroidTaskName = "installAndroid$suffixDevice$suffixDebug"
            val installAndroidTask = when {
                direct -> tasks.create(installAndroidTaskName, Task::class.java) { task ->
                    //task.dependsOn("install$suffixDevice$suffixDebug")
                    task.dependsOn("install$suffixDebug")
                }
                else -> tasks.create(installAndroidTaskName, GradleBuild::class.java) { task ->
                    task.buildFile = File(buildDir, "platforms/android/build.gradle")
                    //task.version = "4.10.1"
                    //task.tasks = listOf("install$suffixDevice$suffixDebug")
                    task.tasks = listOf("install$suffixDebug")
                }
            }
            if (emulator == true) {
                installAndroidTask.dependsOn("androidEmulatorStart")
            }
            for (dependsOnTaskName in dependsOnList) {
                installAndroidTask.dependsOn(dependsOnTaskName)
            }
            installAndroidTask.group = GROUP_KORGE_INSTALL
            installAndroidTask.dependsOn("korgeProcessedResourcesJvmMain")
            installAndroidTask.dependsOn("korgeProcessedResourcesMetadataMain")


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
                    val pid = run {
                        for (n in 0 until 10) {
                            try {
                                return@run execOutput("$androidSdkPath/platform-tools/adb", *extra, "shell", "pidof", korge.id).trim()
                            } catch (e: Throwable) {
                                Thread.sleep(500L)
                                if (n == 9) throw e
                            }
                        }
                    }
                    execLogger {
                        it.commandLine(
                            "$androidSdkPath/platform-tools/adb", *extra, "logcat", "--pid=$pid"
                        )
                    }
                }
            }
        }
    }

    tasks.createTyped<DefaultTask>("androidEmulatorDeviceList") {
        group = GROUP_KORGE_ADB
        doFirst {
            println(androidEmulatorListAvds().joinToString("\n"))
            //execAndroidAdb("devices", "-l")
        }
    }

    tasks.createTyped<DefaultTask>("androidEmulatorStart") {
        group = GROUP_KORGE_ADB
        onlyIf { !androidEmulatorIsStarted() }
        doFirst {
            androidEmulatorStart()
        }
    }

    tasks.createTyped<DefaultTask>("adbDeviceList") {
        group = GROUP_KORGE_ADB
        doFirst {
            println(androidAdbDeviceList().joinToString("\n"))
            //execAndroidAdb("devices", "-l")
        }
    }

    tasks.createTyped<DefaultTask>(adbLogcatTaskName) {
        group = GROUP_KORGE_ADB
        doFirst {
            execAndroidAdb("logcat")
        }
    }
}

val adbLogcatTaskName = "adbLogcat"

fun Project.androidEmulatorStart() {
    val avdName = androidEmulatorFirstAvd() ?: error("No android emulators available to start. Please create one using Android Studio")
    val spawner = spawnExt
    spawner.spawn(projectDir, listOf(androidEmulatorPath, "-avd", avdName, "-netdelay", "none", "-netspeed", "full"))
    while (!androidEmulatorIsStarted()) {
        Thread.sleep(1000L)
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
