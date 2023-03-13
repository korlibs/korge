package com.soywiz.korge.gradle.targets.android

import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.jvm.KorgeJavaExec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

val ANDROID_SDK_PATH_KEY = "android.sdk.path"

//Linux: ~/Android/Sdk
//Mac: ~/Library/Android/sdk
//Windows: %LOCALAPPDATA%\Android\sdk
// @TODO: Use [AndroidSdk] class
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
                direct -> tasks.createThis<Task>(installAndroidTaskName) {
                    //task.dependsOn("install$suffixDevice$suffixDebug")
                    dependsOn("install$suffixDebug")
                }
                else -> tasks.createThis<GradleBuild>(installAndroidTaskName) {
                    buildFile = File(buildDir, "platforms/android/build.gradle")
                    //task.version = "4.10.1"
                    //task.tasks = listOf("install$suffixDevice$suffixDebug")
                    tasks = listOf("install$suffixDebug")
                }
            }
            if (emulator == true) {
                installAndroidTask.dependsOn("androidEmulatorStart")
            }
            for (dependsOnTaskName in dependsOnList) {
                installAndroidTask.dependsOn(dependsOnTaskName)
            }
            installAndroidTask.group = GROUP_KORGE_INSTALL

            //installAndroidTask.dependsOn(getKorgeProcessResourcesTaskName("jvm", "main"))
            //installAndroidTask.dependsOn(getKorgeProcessResourcesTaskName("metadata", "main"))

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


fun writeAndroidManifest(outputFolder: File, korge: KorgeExtension, info: AndroidInfo) {
    val ifNotExists = korge.overwriteAndroidFiles

    val generated = AndroidGenerated(
        icons = korge.iconProvider,
        ifNotExists = ifNotExists,
        androidPackageName = korge.id,
        androidInit = korge.plugins.pluginExts.getAndroidInit() + info.androidInit,
        androidMsaa = korge.androidMsaa,
        orientation = korge.orientation,
        realEntryPoint = korge.realEntryPoint,
        androidAppName = korge.name,
        androidManifestChunks = korge.androidManifestChunks,
        androidManifestApplicationChunks = korge.androidManifestApplicationChunks,
        androidManifest = korge.plugins.pluginExts.getAndroidManifestApplication() + info.androidManifest,
        androidLibrary = korge.androidLibrary,
    )

    generated.writeKeystore(outputFolder)
    generated.writeAndroidManifest(File(outputFolder, "src/main"))
    generated.writeResources(File(outputFolder, "src/main/res"))
    generated.writeMainActivity(File(outputFolder, "src/main/java"))
}

class AndroidGenerated(
    val icons: KorgeIconProvider,
    val ifNotExists: Boolean,
    val androidPackageName: String,
    val realEntryPoint: String = "main",
    val androidMsaa: Int? = null,
    val androidInit: List<String> = emptyList(),
    val orientation: Orientation = Orientation.DEFAULT,
    val androidAppName: String = "androidAppName",
    val androidManifestChunks: Set<String> = emptySet(),
    val androidManifestApplicationChunks: Set<String> = emptySet(),
    val androidManifest: List<String> = emptyList(),
    val androidLibrary: Boolean = true,
) {
    fun writeResources(folder: File) {
        File(folder, "mipmap-mdpi/icon.png").conditionally(ifNotExists) {
            ensureParents().writeBytesIfChanged(icons.getIconBytes())
        }
        File(folder, "drawable/app_icon.png").conditionally(ifNotExists) {
            ensureParents().writeBytesIfChanged(icons.getIconBytes())
        }
        File(folder, "drawable/app_banner.png").conditionally(ifNotExists) {
            ensureParents().writeBytesIfChanged(icons.getBannerBytes(432, 243))
        }
    }

    fun writeMainActivity(outputFolder: File) {
        File(outputFolder, "MainActivity.kt").conditionally(ifNotExists) {
            ensureParents().writeTextIfChanged(Indenter {
                line("package $androidPackageName")

                line("import com.soywiz.korio.android.withAndroidContext")
                line("import com.soywiz.korgw.*")
                line("import $realEntryPoint")

                line("class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = ${androidMsaa ?: 1}))") {
                    line("override suspend fun activityMain()") {
                        //line("withAndroidContext(this)") { // @TODO: Probably we should move this to KorgwActivity itself
                        for (text in androidInit) {
                            line(text)
                        }
                        line("${realEntryPoint}()")
                        //}
                    }
                }
            }.toString())
        }
    }

    fun writeAndroidManifest(outputFolder: File) {
        File(outputFolder, "AndroidManifest.xml").also { it.parentFile.mkdirs() }.conditionally(ifNotExists) {
            ensureParents().writeTextIfChanged(Indenter {
                line("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                line("<manifest")
                indent {
                    //line("xmlns:tools=\"http://schemas.android.com/tools\"")
                    line("xmlns:android=\"http://schemas.android.com/apk/res/android\"")
                    line("package=\"$androidPackageName\"")
                }
                line(">")
                indent {
                    line("<uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />")
                    line("<uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />")

                    line("<application")
                    indent {
                        line("")
                        //line("tools:replace=\"android:appComponentFactory\"")
                        line("android:allowBackup=\"true\"")

                        if (!androidLibrary) {
                            line("android:label=\"$androidAppName\"")
                            line("android:icon=\"@mipmap/icon\"")
                            // // line("android:icon=\"@android:drawable/sym_def_app_icon\"")
                            line("android:roundIcon=\"@android:drawable/sym_def_app_icon\"")
                            line("android:theme=\"@android:style/Theme.Holo.NoActionBar\"")
                        }


                        line("android:supportsRtl=\"true\"")
                    }
                    line(">")
                    indent {
                        for (text in androidManifest) {
                            line(text)
                        }
                        for (text in androidManifestApplicationChunks) {
                            line(text)
                        }

                        line("<activity android:name=\".MainActivity\"")
                        indent {
                            val orientationString = when (orientation) {
                                Orientation.LANDSCAPE -> "landscape"
                                Orientation.PORTRAIT -> "portrait"
                                Orientation.DEFAULT -> "sensor"
                            }
                            line("android:banner=\"@drawable/app_banner\"")
                            line("android:icon=\"@drawable/app_icon\"")
                            line("android:label=\"$androidAppName\"")
                            line("android:logo=\"@drawable/app_icon\"")
                            line("android:configChanges=\"orientation|screenSize|screenLayout|keyboardHidden\"")
                            line("android:screenOrientation=\"$orientationString\"")
                            line("android:exported=\"true\"")
                        }
                        line(">")

                        if (!androidLibrary) {
                            indent {
                                line("<intent-filter>")
                                indent {
                                    line("<action android:name=\"android.intent.action.MAIN\"/>")
                                    line("<category android:name=\"android.intent.category.LAUNCHER\"/>")
                                }
                                line("</intent-filter>")
                            }
                        }
                        line("</activity>")
                    }
                    line("</application>")
                    for (text in androidManifestChunks) {
                        line(text)
                    }
                }
                line("</manifest>")
            }.toString())
        }
    }

    fun writeKeystore(outputFolder: File) {
        File(outputFolder, "korge.keystore").conditionally(ifNotExists) {
            ensureParents().writeBytesIfChanged(getResourceBytes("korge.keystore"))
        }
    }
}

class AndroidInfo(val map: Map<String, Any?>?) {
    //init { println("AndroidInfo: $map") }
    val androidInit: List<String> = (map?.get("androidInit") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidManifest: List<String> = (map?.get("androidManifest") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidDependencies: List<String> = (map?.get("androidDependencies") as? List<String>?)?.filterNotNull() ?: listOf()
}

private var _tryAndroidSdkDirs: List<File>? = null
// @TODO: Use [AndroidSdk] class
val tryAndroidSdkDirs: List<File> get() {
    if (_tryAndroidSdkDirs == null) {
        _tryAndroidSdkDirs = listOf(
            File(System.getProperty("user.home"), "/Library/Android/sdk"), // MacOS
            File(System.getProperty("user.home"), "/Android/Sdk"), // Linux
            File(System.getProperty("user.home"), "/AppData/Local/Android/Sdk") // Windows
        )
    }
    return _tryAndroidSdkDirs!!
}

fun Project.tryToDetectAndroidSdkPath(): File? {
	for (tryAndroidSdkDirs in tryAndroidSdkDirs) {
		if (tryAndroidSdkDirs.exists()) {
			return tryAndroidSdkDirs.absoluteFile
		}
	}
	return null
}
