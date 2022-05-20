package com.soywiz.korge.gradle.targets.android

import org.gradle.api.*
import java.io.*

object AndroidSdk {
    @JvmStatic
    fun guessAndroidSdkPath(): String? {
        val userHome = System.getProperty("user.home")
        return listOfNotNull(
            System.getenv("ANDROID_HOME"),
            "$userHome/AppData/Local/Android/sdk",
            "$userHome/Library/Android/sdk",
            "$userHome/Android/Sdk",
            "$userHome/AndroidSDK",  // location of sdkmanager on linux
            "/usr/lib/android-sdk",  // location on debian based linux (sudo apt install android-sdk)
            "/Library/Android/sdk"   // some other flavor of linux
        ).firstOrNull { File(it).exists() }
    }

    @JvmStatic
    fun hasAndroidSdk(project: Project): Boolean {
        val env = System.getenv("ANDROID_SDK_ROOT")
        if (env != null) return true
        val localPropsFile = File(project.rootProject.rootDir, "local.properties")
        if (!localPropsFile.exists()) {
            val sdkPath = AndroidSdk.guessAndroidSdkPath() ?: return false
            localPropsFile.writeText("sdk.dir=${sdkPath.replace("\\", "/")}")
        }
        return true
    }
}
