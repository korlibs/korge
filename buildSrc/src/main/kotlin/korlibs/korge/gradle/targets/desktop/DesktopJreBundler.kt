package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.apple.*
import korlibs.korge.gradle.targets.windows.*
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

    val JRE_WIN64_LAUNCHER = UrlRef(
        "https://github.com/korlibs/universal-jre/releases/download/0.0.1/launcher-win.exe",
        sha256 = "c0124f38329509145fbb278d3b592daa12bb556ab0341310d4f67b9eded0c270",
    )

    val JRE_MACOS_LAUNCHER = UrlRef(
        "https://github.com/korlibs/universal-jre/releases/download/0.0.1/app",
        sha256 = "4123b08e24678885781b04125675aa2f7d2af87583a753d16737ad154934bf0b",
    )

    val JRE_MACOS_UNIVERSAL = UrlRef(
        "https://github.com/korlibs/universal-jre/releases/download/0.0.1/macos-universal-jdk-21+35-jre.tar.gz",
        sha256 = "6d2d0a2e35c649fc731f5d3f38d7d7828f7fad4b9b2ea55d4d05f0fd26cf93ca",
    )

    val JRE_WIN64 = UrlRef(
        "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21%2B35/OpenJDK21U-jre_x64_windows_hotspot_21_35.zip",
        sha256 = "3753e9b1d7186191766954f7957cc0c3c4de9633366dbfdfa573e30b371b7ab7",
    )

    val LOCAL_JRE_DIR = File(korgeCacheDir, "jre")
    val LOCAL_UNIVERSAL = File(LOCAL_JRE_DIR, "universal")
    val LOCAL_WIN32 = File(LOCAL_JRE_DIR, "win32")
    val LOCAL_UNIVERSAL_JRE = File(LOCAL_UNIVERSAL, "jdk-21+35-jre/Contents/jre")
    val LOCAL_WIN32_JRE = File(LOCAL_WIN32, "jdk-21+35-jre")

    fun cachedFile(urlRef: UrlRef): File {
        val downloadUrl = URL(urlRef.url)
        val localFile = File(LOCAL_JRE_DIR, File(downloadUrl.file).name).ensureParents()
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

    fun createLinuxBundle(project: Project, fatJar: File) {
        val gameApp = File(project.buildDir, "platforms/jvm-linux").also { it.mkdirs() }

        // TODO: Paths must be absolute. So this file just serves as reference
        File(gameApp, "game.desktop").writeText("""
            [Desktop Entry]
            Type=Application
            Name=${project.korge.name}
            Comment=Launch MyApp
            Exec=java -jar game.jar
            Icon=game.png
            Terminal=false
            Categories=Games;
        """.trimIndent())

        val gameIconFile = File(gameApp, "game.png")
        gameIconFile.ensureParents().writeBytes(project.korge.getIconBytes(256))

        project.copy {
            it.from(fatJar)
            it.into(gameApp)
            it.rename { "game.jar" }
            it.filePermissions {
                it.user.execute = true
                it.group.execute = true
                it.other.execute = true
            }
        }
    }

    fun createWin32Bundle(project: Project, fatJar: File) {
        if (!LOCAL_WIN32_JRE.isDirectory) {
            project.copy {
                it.from(project.zipTree(cachedFile(JRE_WIN64)))
                it.into(LOCAL_WIN32)
            }
        }

        val gameApp = File(project.buildDir, "platforms/jvm-win32").also { it.mkdirs() }

        val gameIcoFile = File(gameApp, "game.ico")
        gameIcoFile.ensureParents().writeBytes(ICO2.encode(listOf(32, 256).map {
            project.korge.getIconBytes(it).decodeImage()
        }))

        ProcessBuilder(
            WindowsToolchain.resourceHackerExe.absolutePath,
            "-open", cachedFile(JRE_WIN64_LAUNCHER).absolutePath,
            "-save",
            File(gameApp, "game.exe").absolutePath,
            "-action",
            "addskip",
            "-res",
            gameIcoFile.absolutePath,
            "-mask",
            "ICONGROUP,MAINICON,",
        ).start().waitFor()

        project.copy {
            it.from(fatJar)
            it.into(gameApp)
            it.rename { "app.jar" }
            it.filePermissions {
                it.user.execute = true
                it.group.execute = true
                it.other.execute = true
            }
        }

        project.copy {
            it.from(LOCAL_WIN32_JRE)
            it.into(File(gameApp, "jre"))
        }
    }

    fun createMacosApp(project: Project, fatJar: File) {
        if (!LOCAL_UNIVERSAL_JRE.isDirectory) {
            project.copy {
                it.from(project.tarTree(cachedFile(JRE_MACOS_UNIVERSAL)))
                it.into(LOCAL_UNIVERSAL)
            }
        }

        val gameApp = File(project.buildDir, "platforms/jvm-macos/game.app/Contents").also { it.mkdirs() }
        project.copy {
            it.from(LOCAL_UNIVERSAL_JRE)
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
