package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.util.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import java.io.File
import java.util.*

val Project.androidSdkPath: String get() = AndroidSdk.getAndroidSdkPath(this)

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

    val resourcesSrcDirsBase = mainSourceSets.flatMap { it.resources.srcDirs } + listOf(file("src/androidMain/resources"))//, file("src/main/resources"))
    val resourcesSrcDirsBundle = project.korge.bundles.getPaths("android", resources = true, test = false)
    val resourcesSrcDirs = resourcesSrcDirsBase + resourcesSrcDirsBundle

    val kotlinSrcDirsBase = mainSourceSets.flatMap { it.kotlin.srcDirs } + listOf(file("src/androidMain/kotlin"))//, file("src/main/java"))
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


//fun writeAndroidManifest(outputFolder: File, korge: KorgeExtension, info: AndroidInfo = AndroidInfo(null)) {
//    val generated = AndroidGenerated(korge, info)
//
//    generated.writeKeystore(outputFolder)
//    val srcMain = "src/androidMain"
//    generated.writeAndroidManifest(File(outputFolder, srcMain))
//    generated.writeResources(File(outputFolder, "$srcMain/res"))
//    generated.writeMainActivity(File(outputFolder, "$srcMain/kotlin"))
//}

fun AndroidGenerated(korge: KorgeExtension, info: AndroidInfo = AndroidInfo(null)): AndroidGenerated {
    return AndroidGenerated(
        icons = korge.iconProvider,
        ifNotExists = korge.overwriteAndroidFiles,
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
}

fun AndroidGenerated(project: Project, isKorge: Boolean, info: AndroidInfo = AndroidInfo(null)): AndroidGenerated {
    return when {
        isKorge -> AndroidGenerated(project.korge, info)
        else -> AndroidGenerated(
            icons = KorgeIconProvider(File(project.korgeGradlePluginResources, "icons/korge.png"), File(project.korgeGradlePluginResources, "banners/korge.png")),
            ifNotExists = true,
            androidPackageName = AndroidConfig.getAppId(project, isKorge),
            realEntryPoint = "main",
            androidMsaa = 4,
            androidAppName = project.name,
            androidLibrary = false,
        )
    }
}

class AndroidGenerated constructor(
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
    companion object
    fun writeResources(folder: File) {
        writeFileBytes(File(folder, "mipmap-mdpi/icon.png")) { icons.getIconBytes() }
        writeFileBytes(File(folder, "drawable/app_icon.png")) { icons.getIconBytes() }
        writeFileBytes(File(folder, "drawable/app_banner.png")) { icons.getBannerBytes(432, 243) }
    }

    fun writeMainActivity(outputFolder: File) {
        writeFileText(File(outputFolder, "MainActivity.kt")) { AndroidMainActivityKt.genAndroidMainActivityKt(this@AndroidGenerated) }
    }

    fun writeAndroidManifest(outputFolder: File) {
        writeFileText(File(outputFolder, "AndroidManifest.xml")) { AndroidManifestXml.genAndroidManifestXml(this@AndroidGenerated) }
    }

    fun writeKeystore(outputFolder: File) {
        writeFileBytes(File(outputFolder, "korge.keystore")) { getResourceBytes("korge.keystore") }
    }

    private fun writeFileBytes(file: File, gen: () -> ByteArray) {
        file.conditionally(ifNotExists) { ensureParents().writeBytesIfChanged(gen()) }
    }
    private fun writeFileText(file: File, gen: () -> String) {
        file.conditionally(ifNotExists) { ensureParents().writeTextIfChanged(gen()) }
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

fun Project.getAndroidMinSdkVersion(): Int = project.findProperty("android.min.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_MIN_SDK
fun Project.getAndroidCompileSdkVersion(): Int = project.findProperty("android.compile.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_COMPILE_SDK
fun Project.getAndroidTargetSdkVersion(): Int = project.findProperty("android.target.sdk.version")?.toString()?.toIntOrNull() ?: ANDROID_DEFAULT_TARGET_SDK

//const val ANDROID_DEFAULT_MIN_SDK = 16 // Previously 18
const val ANDROID_DEFAULT_MIN_SDK = 18
const val ANDROID_DEFAULT_COMPILE_SDK = 30
const val ANDROID_DEFAULT_TARGET_SDK = 30

val ANDROID_JAVA_VERSION = JavaVersion.VERSION_1_8
//val ANDROID_JAVA_VERSION = JavaVersion.VERSION_11
//val ANDROID_JAVA_VERSION_STR = "1.8"
val ANDROID_JAVA_VERSION_STR = ANDROID_JAVA_VERSION.toString()

val GRADLE_JAVA_VERSION_STR = "11"

