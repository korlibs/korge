package com.soywiz.kproject

import org.gradle.api.*
import java.io.*

object AndroidConfig {
    fun getAndroidManifestFile(
        project: Project,
        minSdk: Int = korlibs.korge.gradle.targets.android.ANDROID_DEFAULT_MIN_SDK,
        targetSdk: Int = korlibs.korge.gradle.targets.android.ANDROID_DEFAULT_TARGET_SDK,
        compileSdk: Int = korlibs.korge.gradle.targets.android.ANDROID_DEFAULT_COMPILE_SDK,
    ): File {
        return File(project.buildDir, "AndroidManifest.xml").also {
            if (!it.exists()) {
                it.parentFile.mkdirs()
                it.writeText(buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                    appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">")
                    appendLine("    <uses-sdk android:minSdkVersion=\"${minSdk}\" android:targetSdkVersion=\"${targetSdk}\" />")
                    appendLine("</manifest>")
                })
            }
        }
    }
}
