package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.*
import org.gradle.api.*
import java.io.*

object AndroidConfig {
    fun getAppId(project: Project, isKorge: Boolean): String {
        return if (isKorge) project.korge.id else "korlibs.${project.name.replace("-", "_")}"
        //val namespace = "com.soywiz.${project.name.replace("-", ".")}"
    }

    fun getNamespace(project: Project, isKorge: Boolean): String {
        //return if (isKorge) project.korge.id else "com.soywiz.${project.name.replace("-", ".")}"
        return getAppId(project, isKorge)
    }

    fun getAndroidManifestFile(project: Project, isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/AndroidManifest.xml")
        return File(project.buildDir, "AndroidManifest.xml")
    }

    fun getAndroidResFolder(project: Project, isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/res")
        return File(project.buildDir, "androidres")
    }
    fun getAndroidSrcFolder(project: Project, isKorge: Boolean): File {
        //return File(project.projectDir, "src/androidMain/kotlin")
        return File(project.buildDir, "androidsrc")
    }
}
