package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.apple.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import java.net.*

// https://stackoverflow.com/questions/13017121/unpacking-tar-gz-into-root-dir-with-gradle
object DesktopJreBundler {
    // https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21%2B35
    val JRE_MACOS_ARM64 = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21%2B35/OpenJDK21U-jre_aarch64_mac_hotspot_21_35.tar.gz"
    val JRE_MACOS_X64 = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21%2B35/OpenJDK21U-jre_x64_mac_hotspot_21_35.tar.gz"
    val JRE_WINDOWS_X64 = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21%2B35/OpenJDK21U-jre_x64_windows_hotspot_21_35.zip"

    fun cachedFile(url: String): File {
        val downloadUrl = URL(url)
        val localFile = File(korgeCacheDir, File(downloadUrl.file).name)
        if (!localFile.isFile) {
            println("Downloading $downloadUrl...")
            localFile.writeBytes(downloadUrl.readBytes())
        }
        return localFile
    }

    fun createMacosApp(project: Project, fatJar: File) {
        val gameApp = File(project.buildDir, "platforms/jvm-macos/game.app/Contents").ensureParents()
        // @TODO: Universal JRE?
        // @TODO: https://incenp.org/notes/2023/universal-java-app-on-macos.html
        project.copy {
            it.from(project.tarTree(cachedFile(JRE_MACOS_ARM64)))
            it.into(File(gameApp, "jdk-arm64"))
            it.filePermissions {
                it.unix("0777".toInt(8))
            }
        }
        project.copy {
            it.from(project.tarTree(cachedFile(JRE_MACOS_X64)))
            it.into(File(gameApp, "jdk-x86_64"))
            it.filePermissions {
                it.unix("0777".toInt(8))
            }
        }

        val korge = project.korge
        File(gameApp, "Resources/${korge.exeBaseName}.icns").ensureParents().writeBytes(IcnsBuilder.build(korge.getIconBytes()))
        File(gameApp, "Info.plist").writeText(InfoPlistBuilder.build(korge))
        val exec = File(gameApp, "MacOS/${File(korge.exeBaseName).name}").ensureParents()
        exec.writeText("""
            #!/bin/sh
            PARENT_PATH=${'$'}( cd "${'$'}(dirname "${'$'}{BASH_SOURCE[0]}")" ; pwd -P )
            JAVA_ARM="${'$'}PARENT_PATH/../jdk-arm64/jdk-21+35-jre/Contents/Home/bin/java"
            JAVA_X64="${'$'}PARENT_PATH/../jdk-x86_64/jdk-21+35-jre/Contents/Home/bin/java"
            #ARCH=`/usr/bin/uname -m`
            #ARCH=arm64
            ${'$'}JAVA_ARM --version
            STATUS=${'$'}?
            if [ ${'$'}STATUS -eq 0 ]; then
                ${'$'}JAVA_ARM -jar "${'$'}PARENT_PATH/app.jar"
            else
                ${'$'}JAVA_X64 -jar "${'$'}PARENT_PATH/app.jar"
            fi
        """.trimIndent())
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
