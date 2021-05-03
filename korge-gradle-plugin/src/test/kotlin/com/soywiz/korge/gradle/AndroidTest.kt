package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.android.*
import kotlin.test.*

class AndroidTest : AbstractGradleIntegrationTest() {
    val ANDROID_SDK_PATH = "/fake/android/sdk/path"
    val ANDROID_EMULATOR_PATH = "$ANDROID_SDK_PATH/emulator/emulator"
    val ANDROID_ADB_PATH = "$ANDROID_SDK_PATH/platform-tools/adb"

    init {
        project.extensions.add(ANDROID_SDK_PATH_KEY, ANDROID_SDK_PATH)
    }

    @Test
    fun testPaths() {
        assertEquals(ANDROID_EMULATOR_PATH, project.androidEmulatorPath)
        assertEquals(ANDROID_ADB_PATH, project.androidAdbPath)
    }

    @Test
    fun testAndroidEmulatorListAvds() {
        project.defineExecResult(ANDROID_EMULATOR_PATH, "-list-avds", stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals(listOf("Pixel_2_XL_API_28"), project.androidEmulatorListAvds())
        }
        project.defineExecResult(ANDROID_EMULATOR_PATH, "-list-avds", stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals(listOf("Android_TV_720p_API_28", "Pixel_2_API_30"), project.androidEmulatorListAvds())
        }
    }

    @Test
    fun testAndroidEmulatorFirstAvd() {
        project.defineExecResult(ANDROID_EMULATOR_PATH, "-list-avds", stdout = "").also {
            assertEquals(null, project.androidEmulatorFirstAvd())
        }
        project.defineExecResult(ANDROID_EMULATOR_PATH, "-list-avds", stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals("Pixel_2_XL_API_28", project.androidEmulatorFirstAvd())
        }
        project.defineExecResult(ANDROID_EMULATOR_PATH, "-list-avds", stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals("Pixel_2_API_30", project.androidEmulatorFirstAvd())
        }
    }
}
