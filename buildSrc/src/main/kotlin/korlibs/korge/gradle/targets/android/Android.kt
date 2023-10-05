package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.*

interface AndroidSdkProvider {
    val projectDir: File
    val androidSdkPath: String
    val spawnExt: SpawnExtension
}

val Project.androidSdkProvider: AndroidSdkProvider get() = object : AndroidSdkProvider {
    override val projectDir: File get() = this@androidSdkProvider.projectDir
    override val androidSdkPath: String get() = this@androidSdkProvider.androidSdkPath
    override val spawnExt: SpawnExtension get() = this@androidSdkProvider.spawnExt
}

fun AndroidSdkProvider.execLogger(vararg params: String) {
    println("EXEC: ${params.joinToString(" ")}")
    ProcessBuilder(*params).redirectErrorStream(true).directory(projectDir).start().waitFor()
}

fun AndroidSdkProvider.execOutput(vararg params: String): String {
    return ProcessBuilder(*params).redirectErrorStream(true).directory(projectDir).start().inputStream.readBytes().toString(Charsets.UTF_8)
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

/*
fun Project.androidGetResourcesFolders(): Pair<List<File>, List<File>> {
    val targets = listOf(kotlin.metadata())
    val mainSourceSets = targets.flatMap { it.compilations["main"].allKotlinSourceSets }

    val resourcesSrcDirsBase = mainSourceSets.flatMap { it.resources.srcDirs } + listOf(file("src/androidMain/resources"))//, file("src/main/resources"))
    //val resourcesSrcDirsBundle = project.korge.bundles.getPaths("android", resources = true, test = false)
    //val resourcesSrcDirs = resourcesSrcDirsBase + resourcesSrcDirsBundle
    val resourcesSrcDirs = resourcesSrcDirsBase

    val kotlinSrcDirsBase = mainSourceSets.flatMap { it.kotlin.srcDirs } + listOf(file("src/androidMain/kotlin"))//, file("src/main/java"))
    //val kotlinSrcDirsBundle = project.korge.bundles.getPaths("android", resources = false, test = false)
    //val kotlinSrcDirs = kotlinSrcDirsBase + kotlinSrcDirsBundle
    val kotlinSrcDirs = kotlinSrcDirsBase

    return Pair(resourcesSrcDirs, kotlinSrcDirs)
}

fun isKorlibsDependency(cleanFullName: String): Boolean {
    if (cleanFullName.startsWith("org.jetbrains")) return false
    if (cleanFullName.startsWith("junit:junit")) return false
    if (cleanFullName.startsWith("org.hamcrest:hamcrest-core")) return false
    if (cleanFullName.startsWith("org.jogamp")) return false
    return true
}
*/

//fun writeAndroidManifest(outputFolder: File, korge: KorgeExtension, info: AndroidInfo = AndroidInfo(null)) {
//    val generated = AndroidGenerated(korge, info)
//
//    generated.writeKeystore(outputFolder)
//    val srcMain = "src/androidMain"
//    generated.writeAndroidManifest(File(outputFolder, srcMain))
//    generated.writeResources(File(outputFolder, "$srcMain/res"))
//    generated.writeMainActivity(File(outputFolder, "$srcMain/kotlin"))
//}

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
//const val ANDROID_DEFAULT_MIN_SDK = 16 // Previously 18
//const val ANDROID_DEFAULT_MIN_SDK = 18
const val ANDROID_DEFAULT_MIN_SDK = 21 // Android 5.0
const val ANDROID_DEFAULT_COMPILE_SDK = 33
const val ANDROID_DEFAULT_TARGET_SDK = 33

val GRADLE_JAVA_VERSION_STR = "11"

val ANDROID_JAVA_VERSION = JavaVersion.VERSION_1_8
//val ANDROID_JAVA_VERSION = JavaVersion.VERSION_11
val ANDROID_JAVA_VERSION_STR = ANDROID_JAVA_VERSION.toString()
val ANDROID_JVM_TARGET = JvmTarget.fromTarget(ANDROID_JAVA_VERSION_STR)
