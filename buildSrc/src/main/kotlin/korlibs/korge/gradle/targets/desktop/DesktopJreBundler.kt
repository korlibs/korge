package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.apple.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import java.net.*
import java.security.*

// https://stackoverflow.com/questions/13017121/unpacking-tar-gz-into-root-dir-with-gradle
// https://github.com/korlibs/universal-jre/
object DesktopJreBundler {
    // https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21%2B35

    data class UrlRef(val url: String, val sha256: String)

    val JRE_MACOS_LAUNCHER = UrlRef(
        "https://github.com/korlibs/universal-jre/releases/download/0.0.1/app",
        sha256 = "4123b08e24678885781b04125675aa2f7d2af87583a753d16737ad154934bf0b"
    )

    val JRE_MACOS_UNIVERSAL = UrlRef(
        "https://github.com/korlibs/universal-jre/releases/download/0.0.1/macos-universal-jdk-21+35-jre.tar.gz",
        sha256 = "6d2d0a2e35c649fc731f5d3f38d7d7828f7fad4b9b2ea55d4d05f0fd26cf93ca"
    )

    val JRE_DIR = File(korgeCacheDir, "jre")
    val UNIVERSAL = File(JRE_DIR, "universal")
    val UNIVERSAL_JRE = File(UNIVERSAL, "jdk-21+35-jre/Contents/jre")

    fun cachedFile(urlRef: UrlRef): File {
        val downloadUrl = URL(urlRef.url)
        val localFile = File(JRE_DIR, File(downloadUrl.file).name).ensureParents()
        if (!localFile.isFile) {
            println("Downloading $downloadUrl...")
            val bytes = downloadUrl.readBytes()
            val actualSha256 = MessageDigest.getInstance("SHA-256").digest(bytes).hex
            val expectedSha256 = urlRef.sha256
            if (actualSha256 != expectedSha256) {
                error("URL: ${urlRef.url} expected to have $expectedSha256 but was $actualSha256")
            }
            localFile.writeBytes(bytes)
        }
        return localFile
    }

    fun createMacosApp(project: Project, fatJar: File) {
        if (!UNIVERSAL_JRE.isDirectory) {
            project.copy {
                it.from(project.tarTree(cachedFile(JRE_MACOS_UNIVERSAL)))
                it.into(UNIVERSAL)
            }
        }

        val gameApp = File(project.buildDir, "platforms/jvm-macos/game.app/Contents").ensureParents()
        project.copy {
            it.from(UNIVERSAL_JRE)
            it.into(File(gameApp, "MacOS/jre"))
        }

        val korge = project.korge
        File(gameApp, "Resources/${korge.exeBaseName}.icns").ensureParents().writeBytes(IcnsBuilder.build(korge.getIconBytes()))
        File(gameApp, "Info.plist").writeText(InfoPlistBuilder.build(korge))
        val exec = File(gameApp, "MacOS/${File(korge.exeBaseName).name}").ensureParents()
        exec.writeBytes(cachedFile(JRE_MACOS_LAUNCHER).readBytes())
        exec.setExecutable(true)
        project.copy {
            it.from(fatJar)
            it.into(File(gameApp, "MacOS"))
            it.rename { "app.jar" }
            it.filePermissions {
                it.user.execute = true
                it.group.execute = true
                it.other.execute = true
            }
        }
    }
}
