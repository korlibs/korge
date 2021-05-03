package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.targets.android.*
import io.mockk.*
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

    val listAvds = arrayOf(ANDROID_EMULATOR_PATH, "-list-avds")

    @Test
    fun testAndroidEmulatorListAvds() {
        project.defineExecResult(*listAvds, stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals(listOf("Pixel_2_XL_API_28"), project.androidEmulatorListAvds())
        }
        project.defineExecResult(*listAvds, stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals(listOf("Android_TV_720p_API_28", "Pixel_2_API_30"), project.androidEmulatorListAvds())
        }
    }

    @Test
    fun testAndroidEmulatorFirstAvd() {
        project.defineExecResult(*listAvds, stdout = "").also {
            assertEquals(null, project.androidEmulatorFirstAvd())
        }
        project.defineExecResult(*listAvds, stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals("Pixel_2_XL_API_28", project.androidEmulatorFirstAvd())
        }
        project.defineExecResult(*listAvds, stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals("Pixel_2_API_30", project.androidEmulatorFirstAvd())
        }
    }

    @Test
    fun testLogcatTask() {
        val expectedStdout = "logs\n"
        project.installAndroidRun(listOf(), direct = true)
        val stdout = captureStdout {
            project.defineExecResult(ANDROID_ADB_PATH, "logcat", stdout = expectedStdout)
            project.tasks.getByName(adbLogcatTaskName).execute()
        }
        assertEquals(expectedStdout, stdout)
    }

    @Test
    fun testAndroidEmulatorStart() {
        assertFailsWith<IllegalStateException>() {
            project.defineExecResult(*listAvds, stdout = "")
            project.androidEmulatorStart { _, _ -> fail() }
        }

        run {
            val DEVICE_NAME = "Pixel_2_XL_API_28"
            project.defineExecResult(*listAvds, stdout = "$DEVICE_NAME\n")
            project.defineExecResult(ANDROID_ADB_PATH, "devices", "-l", result = listOf(
                //TestableExecResult(""),
                TestableExecResult("List of devices attached\n" +
                    "emulator-5554          device product:sdk_gphone_x86 model:sdk_gphone_x86 device:generic_x86_arm transport_id:1"
                ),
            ))
            val spawnResult = arrayListOf<Any>()
            project.androidEmulatorStart { dir, command -> spawnResult.add(dir to command) }
            assertEquals<List<Any>>(listOf(project.projectDir to listOf(
                ANDROID_EMULATOR_PATH, "-avd", DEVICE_NAME, "-netdelay", "none", "-netspeed", "full"
            )), spawnResult)
        }
    }
}
