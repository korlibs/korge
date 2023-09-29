package com.soywiz.kproject

import org.gradle.api.*
import java.io.*

object AndroidConfig {
    fun getAndroidManifestFile(project: Project): File {
        return File(project.buildDir, "AndroidManifest.xml").also {
            if (!it.exists()) {
                it.parentFile.mkdirs()
                it.writeText(buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                    appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">")
                    appendLine("</manifest>")
                })
            }
        }
    }
}
