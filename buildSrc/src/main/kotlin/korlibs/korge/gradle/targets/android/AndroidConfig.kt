package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*


class AndroidInfo(val map: Map<String, Any?>?) {
    //init { println("AndroidInfo: $map") }
    val androidInit: List<String> = (map?.get("androidInit") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidManifest: List<String> = (map?.get("androidManifest") as? List<String?>?)?.filterNotNull() ?: listOf()
    val androidDependencies: List<String> = (map?.get("androidDependencies") as? List<String>?)?.filterNotNull() ?: listOf()
}

/*
fun Project.toAndroidConfig(): AndroidConfig = AndroidConfig.fromProject(this)
class AndroidConfig(
    val buildDir: File,
    val id: String,
    val name: String,
) {
    companion object {
        fun fromProject(project: Project): AndroidConfig = AndroidConfig(
            buildDir = project.buildDir,
            id = project.korge.id,
            name = project.name,
        )
    }
}

 */

fun Project.toAndroidGenerated(isKorge: Boolean, info: AndroidInfo = AndroidInfo(null)): AndroidGenerated = AndroidGenerated(
    icons = if (isKorge) korge.iconProvider else KorgeIconProvider(File(korgeGradlePluginResources, "icons/korge.png"), File(korgeGradlePluginResources, "banners/korge.png")),
    ifNotExists = if (isKorge) korge.overwriteAndroidFiles else true,
    androidPackageName = AndroidGenerated.getAppId(this, isKorge),
    androidInit = korge.plugins.pluginExts.getAndroidInit() + info.androidInit,
    androidMsaa = if (isKorge) korge.androidMsaa else 4,
    fullscreen = if (isKorge) korge.fullscreen else true,
    orientation = korge.orientation,
    displayCutout = if (isKorge) korge.displayCutout else DisplayCutout.SHORT_EDGES,
    realEntryPoint = if (isKorge) korge.realEntryPoint else "main",
    androidAppName = korge.name,
    androidManifestChunks = korge.androidManifestChunks,
    androidManifestApplicationChunks = korge.androidManifestApplicationChunks,
    androidManifest = korge.plugins.pluginExts.getAndroidManifestApplication() + info.androidManifest,
    androidLibrary = if (isKorge) korge.androidLibrary else false,
    projectName = project.name,
    buildDir = project.buildDir,
)

data class AndroidGenerated constructor(
    val icons: KorgeIconProvider,
    val ifNotExists: Boolean,
    val androidPackageName: String,
    val realEntryPoint: String = "main",
    val androidMsaa: Int? = null,
    val fullscreen: Boolean? = null,
    val androidInit: List<String> = emptyList(),
    val orientation: Orientation = Orientation.DEFAULT,
    val displayCutout: DisplayCutout = DisplayCutout.DEFAULT,
    val androidAppName: String = "androidAppName",
    val androidManifestChunks: Set<String> = emptySet(),
    val androidManifestApplicationChunks: Set<String> = emptySet(),
    val androidManifest: List<String> = emptyList(),
    val androidLibrary: Boolean = true,
    val projectName: String,
    val buildDir: File,
) {
    fun writeResources(folder: File) {
        writeFileBytes(File(folder, "mipmap-mdpi/icon.png")) { icons.getIconBytes() }
        writeFileBytes(File(folder, "drawable/app_icon.png")) { icons.getIconBytes() }
        writeFileBytes(File(folder, "drawable/app_banner.png")) { icons.getBannerBytes(432, 243) }
        writeFileText(File(folder, "values/styles.xml")) { AndroidManifestXml.genStylesXml(this@AndroidGenerated) }
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

    fun getAppId(isKorge: Boolean): String {
        return if (isKorge) androidPackageName else "korlibs.${projectName.replace("-", "_")}"
        //val namespace = "com.soywiz.${project.name.replace("-", ".")}"
    }

    fun getNamespace(isKorge: Boolean): String {
        //return if (isKorge) project.korge.id else "com.soywiz.${project.name.replace("-", ".")}"
        return getAppId(isKorge)
    }

    fun getAndroidManifestFile(isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/AndroidManifest.xml")
        return File(buildDir, "AndroidManifest.xml")
    }

    fun getAndroidResFolder(isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/res")
        return File(buildDir, "androidres")
    }
    fun getAndroidSrcFolder(isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/kotlin")
        return File(buildDir, "androidsrc")
    }

    companion object {
        fun getAppId(project: Project, isKorge: Boolean): String {
            return if (isKorge) project.korge.id else "korlibs.${project.name.replace("-", "_")}"
        }
    }
}
