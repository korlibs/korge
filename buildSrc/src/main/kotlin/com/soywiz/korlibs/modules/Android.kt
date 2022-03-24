package com.soywiz.korlibs.modules

import org.gradle.api.*
import java.io.*

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

fun Project.hasAndroidSdk(): Boolean {
    val env = System.getenv("ANDROID_SDK_ROOT")
    if (env != null) return true
    val localPropsFile = File(rootProject.rootDir, "local.properties")
    if (!localPropsFile.exists()) {
        val sdkPath = guessAndroidSdkPath() ?: return false
        localPropsFile.writeText("sdk.dir=${sdkPath.replace("\\", "/")}")
    }
    return true
}
