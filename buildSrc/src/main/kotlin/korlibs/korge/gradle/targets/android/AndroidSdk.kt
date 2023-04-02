package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import java.util.*

object AndroidSdk {
    val ANDROID_SDK_PATH_KEY = "android.sdk.path"

    //Linux: ~/Android/Sdk
    //Mac: ~/Library/Android/sdk
    //Windows: %LOCALAPPDATA%\Android\sdk
    @JvmStatic
    fun getAndroidSdkPath(project: Project): String {
        val extensionAndroidSdkPath = project.findProperty(ANDROID_SDK_PATH_KEY)?.toString() ?: project.extensions.findByName(ANDROID_SDK_PATH_KEY)?.toString()
        if (extensionAndroidSdkPath != null) return extensionAndroidSdkPath
        val localPropertiesFile = project.projectDir["local.properties"]
        if (localPropertiesFile.exists()) {
            val props = Properties().apply { load(localPropertiesFile.readText().reader()) }
            if (props.getProperty("sdk.dir") != null) {
                return props.getProperty("sdk.dir")!!
            }
        }
        return guessAndroidSdkPath() ?: error("Can't find android sdk (ANDROID_HOME environment not set and Android SDK not found in standard locations)")
    }

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
