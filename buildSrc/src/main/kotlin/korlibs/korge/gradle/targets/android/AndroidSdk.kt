package korlibs.korge.gradle.targets.android

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
        return _getAndroidSdkOrNullCreateLocalProperties(project)
            ?: error("Can't find android sdk (ANDROID_HOME environment not set and Android SDK not found in standard locations)")
    }

    @JvmStatic
    fun hasAndroidSdk(project: Project): Boolean {
        return _getAndroidSdkOrNullCreateLocalProperties(project) != null
    }

    @JvmStatic
    private fun _getAndroidSdkOrNullCreateLocalProperties(project: Project): String? {
        // Project property (assume it exists without checking because tests require it)
        val extensionAndroidSdkPath = (
            project.findProperty(ANDROID_SDK_PATH_KEY)?.toString() ?: project.extensions.findByName(ANDROID_SDK_PATH_KEY)?.toString()
            )
        if (extensionAndroidSdkPath != null) return extensionAndroidSdkPath

        val localPropertiesFile = File(project.rootProject.rootDir, "local.properties")
        val props = Properties().apply { if (localPropertiesFile.exists()) load(localPropertiesFile.readText().reader()) }
        if (props.getProperty("sdk.dir") != null) return props.getProperty("sdk.dir")
        val sdk = __getAndroidSdkOrNull(project) ?: return null
        props.setProperty("sdk.dir", sdk.replace("\\", "/"))
        localPropertiesFile.writer().use { props.store(it, null) }
        return sdk
    }

    @JvmStatic
    private fun __getAndroidSdkOrNull(project: Project): String? {
        // Environment variable
        val env = System.getenv("ANDROID_SDK_ROOT")?.takeIf { File(it).isDirectory }
        if (env != null) return env

        // GUESS IT
        val userHome = System.getProperty("user.home")
        return listOfNotNull(
            System.getenv("ANDROID_HOME"),
            "$userHome/AppData/Local/Android/sdk",
            "$userHome/Library/Android/sdk",
            "$userHome/Android/Sdk",
            "$userHome/AndroidSDK",  // location of sdkmanager on linux
            "/usr/lib/android-sdk",  // location on debian based linux (sudo apt install android-sdk)
            "/Library/Android/sdk"   // some other flavor of linux
        ).firstOrNull { File(it).isDirectory }
    }
}
