package korlibs.korge.gradle.targets.android

import java.io.File
import korlibs.korge.gradle.util.SpawnExtension
import korlibs.korge.gradle.util.ensureParents
import korlibs.korge.gradle.util.spawnExt
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

interface AndroidSdkProvider {
    val projectDir: File
    val androidSdkPath: String
    val spawnExt: SpawnExtension

    fun execLogger(vararg params: String, filter: Process.(line: String) -> String? = { it }) {
        spawnExt.execLogger(projectDir, *params, filter = filter)
    }

    fun execOutput(vararg params: String): String {
        return spawnExt.execOutput(projectDir, *params)
    }
}

val Project.androidSdkProvider: AndroidSdkProvider get() = object : AndroidSdkProvider {
    override val projectDir: File get() = this@androidSdkProvider.projectDir
    override val androidSdkPath: String get() = this@androidSdkProvider.androidSdkPath
    override val spawnExt: SpawnExtension get() = this@androidSdkProvider.spawnExt
}

val Project.androidSdkPath: String get() = AndroidSdk.getAndroidSdkPath(this)

val AndroidSdkProvider.androidAdbPath get() = "$androidSdkPath/platform-tools/adb"
val AndroidSdkProvider.androidEmulatorPath get() = "$androidSdkPath/emulator/emulator"

fun AndroidSdkProvider.execAndroidAdb(vararg args: String) {
    execLogger(androidAdbPath, *args)
}

fun AndroidSdkProvider.androidAdbDeviceList(): List<String> {
    return execOutput(androidAdbPath, "devices", "-l").trim().split("\n").map { it.trim() }.drop(1)
}

fun AndroidSdkProvider.androidEmulatorListAvds(): List<String> {
    val output = execOutput(androidEmulatorPath, "-list-avds").trim()
    return when {
        output.isBlank() -> listOf()
        else -> output.split("\n").map { it.trim() }
    }
}

fun AndroidSdkProvider.androidEmulatorIsStarted(): Boolean {
    return androidAdbDeviceList().any { it.contains("emulator") }
}

fun AndroidSdkProvider.androidEmulatorFirstAvd(): String? {
    val avds = androidEmulatorListAvds()
    return avds.firstOrNull { !it.contains("_TV") } ?: avds.firstOrNull()
}

fun AndroidSdkProvider.execAndroidEmulator(vararg args: String) {
    execLogger(androidEmulatorPath, *args)
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


fun AndroidSdkProvider.androidEmulatorStart() {
    val avdName = androidEmulatorFirstAvd() ?: error("No android emulators available to start. Please create one using Android Studio")
    val spawner = spawnExt
    spawner.spawn(projectDir, listOf(androidEmulatorPath, "-avd", avdName, "-netdelay", "none", "-netspeed", "full"))
    while (!androidEmulatorIsStarted()) {
        Thread.sleep(1000L)
    }
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

fun Project.getAndroidMinSdkVersion(): Int = project.findProperty("android.min.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_MIN_SDK
fun Project.getAndroidCompileSdkVersion(): Int = project.findProperty("android.compile.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_COMPILE_SDK
fun Project.getAndroidTargetSdkVersion(): Int = project.findProperty("android.target.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_TARGET_SDK

// https://apilevels.com/
const val ANDROID_DEFAULT_MIN_SDK = 33
const val ANDROID_DEFAULT_COMPILE_SDK = 36
const val ANDROID_DEFAULT_TARGET_SDK = 36

val GRADLE_JAVA_VERSION_STR = "21"

val ANDROID_JAVA_VERSION = JavaVersion.VERSION_21
val ANDROID_JAVA_VERSION_STR = ANDROID_JAVA_VERSION.toString()
val ANDROID_JVM_TARGET = JvmTarget.fromTarget(ANDROID_JAVA_VERSION_STR)
